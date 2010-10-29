/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * A <code>Binary</code> represents a Java binary expression, an
 * immutable pair of expressions combined with an operator.
 */
public class Binary_c extends Expr_c implements Binary
{
    protected Expr left;
    protected Operator op;
    protected Expr right;
    protected Precedence precedence;

    public Binary_c(Position pos, Expr left, Operator op, Expr right) {
	super(pos);
	assert(left != null && op != null && right != null);
	this.left = left;
	this.op = op;
	this.right = right;
	this.precedence = op.precedence();
        if (op == ADD && (left instanceof StringLit || right instanceof StringLit)) {
            this.precedence = Precedence.STRING_ADD;
        }
    }

    /** Get the left operand of the expression. */
    public Expr left() {
	return this.left;
    }

    /** Set the left operand of the expression. */
    public Binary left(Expr left) {
	Binary_c n = (Binary_c) copy();
	n.left = left;
	return n;
    }

    /** Get the operator of the expression. */
    public Operator operator() {
	return this.op;
    }

    /** Set the operator of the expression. */
    public Binary operator(Operator op) {
	Binary_c n = (Binary_c) copy();
	n.op = op;
	return n;
    }

    /** Get the right operand of the expression. */
    public Expr right() {
	return this.right;
    }

    /** Set the right operand of the expression. */
    public Binary right(Expr right) {
	Binary_c n = (Binary_c) copy();
	n.right = right;
	return n;
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
	return this.precedence;
    }

    public Binary precedence(Precedence precedence) {
	Binary_c n = (Binary_c) copy();
	n.precedence = precedence;
	return n;
    }

    /** Reconstruct the expression. */
    protected Binary_c reconstruct(Expr left, Expr right) {
	if (left != this.left || right != this.right) {
	    Binary_c n = (Binary_c) copy();
	    n.left = left;
	    n.right = right;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr left = (Expr) visitChild(this.left, v);
	Expr right = (Expr) visitChild(this.right, v);
	return reconstruct(left, right);
    }
    
    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        Expr other;

        if (child == left) {
            other = right;
        }
        else if (child == right) {
            other = left;
        }
        else {
            return child.type();
        }

        TypeSystem ts = av.typeSystem();
        Context context = av.context();

        try {
            if (op == EQ || op == NE) {
                // Coercion to compatible types.
                if ((child.type().isReference() || child.type().isNull()) &&
                    (other.type().isReference() || other.type().isNull())) {
                    return ts.leastCommonAncestor(child.type(), other.type(), context);
                }

                if (child.type().isBoolean() && other.type().isBoolean()) {
                    return ts.Boolean();
                }

                if (child.type().isNumeric() && other.type().isNumeric()) {
                    return ts.promote(child.type(), other.type());
                }

                if (child.type().isImplicitCastValid(other.type(), context)) {
                    return other.type();
                }

                return child.type();
            }

            if (op == ADD && ts.typeEquals(type(), ts.String(), context)) {
                // Implicit coercion to String. 
                return ts.String();
            }

            if (op == GT || op == LT || op == GE || op == LE) {
                if (child.type().isNumeric() && other.type().isNumeric()) {
                    return ts.promote(child.type(), other.type());
                }

                return child.type();
            }

            if (op == COND_OR || op == COND_AND) {
                return ts.Boolean();
            }

            if (op == BIT_AND || op == BIT_OR || op == BIT_XOR) {
                if (other.type().isBoolean()) {
                    return ts.Boolean();
                }

                if (child.type().isNumeric() && other.type().isNumeric()) {
                    return ts.promote(child.type(), other.type());
                }

                return child.type();
            }

            if (op == ADD || op == SUB || op == MUL || op == DIV || op == MOD) {
                if (child.type().isNumeric() && other.type().isNumeric()) {
                    Type t = ts.promote(child.type(), other.type());

                    if (ts.isImplicitCastValid(t, av.toType(), context)) {
                        return t;
                    }
                    else {
                        return av.toType();
                    }
                }

                return child.type();
            }

            if (op == SHL || op == SHR || op == USHR) {
                if (child.type().isNumeric() && other.type().isNumeric()) {
                    if (child == left) {
                        Type t = ts.promote(child.type());

                        if (ts.isImplicitCastValid(t, av.toType(), context)) {
                            return t;
                        }
                        else {
                            return av.toType();
                        }
                    }
                    else {
                        return ts.promote(child.type());
                    }
                }

                return child.type();
            }

            return child.type();
        }
        catch (SemanticException e) {
        }

        return child.type();
    }

    /** Get the throwsArithmeticException of the expression. */
    public boolean throwsArithmeticException() {
	// conservatively assume that any division or mod may throw
	// ArithmeticException this is NOT true-- floats and doubles don't
	// throw any exceptions ever...
	return op == DIV || op == MOD;
    }

    public String toString() {
	return left + " " + op + " " + right;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	printSubExpr(left, true, w, tr);
	w.write(" ");
	w.write(op.toString());
	w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
	printSubExpr(right, false, w, tr);
    }

  public void dump(CodeWriter w) {
    super.dump(w);

    if (typeRef != null) {
      w.allowBreak(4, " ");
      w.begin(0);
      w.write("(type " + typeRef + ")");
      w.end();
    }

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(operator " + op + ")");
    w.end();
  }

  public Term firstChild() {
    return left;
  }

  public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
    if (op == COND_AND || op == COND_OR) {
      // short-circuit
      if (left instanceof BooleanLit) {
        BooleanLit b = (BooleanLit) left;
        if ((b.value() && op == COND_OR) || (! b.value() && op == COND_AND)) {
          v.visitCFG(left, this, EXIT);
        }
        else {
          v.visitCFG(left, right, ENTRY);
          v.visitCFG(right, this, EXIT);
        }
      }
      else {
        if (op == COND_AND) {
          // AND operator
          // short circuit means that left is false
          v.visitCFG(left, FlowGraph.EDGE_KEY_TRUE, right, 
                           ENTRY, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
        }
        else {
          // OR operator
          // short circuit means that left is true
          v.visitCFG(left, FlowGraph.EDGE_KEY_FALSE, right, 
                           ENTRY, FlowGraph.EDGE_KEY_TRUE, this, EXIT);
        }
        v.visitCFG(right, FlowGraph.EDGE_KEY_TRUE, this,
                          EXIT, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
      }
    }
    else {
      if (left.type().isBoolean() && right.type().isBoolean()) {        
          v.visitCFG(left, FlowGraph.EDGE_KEY_TRUE, right,
                           ENTRY, FlowGraph.EDGE_KEY_FALSE, right, ENTRY);
          v.visitCFG(right, FlowGraph.EDGE_KEY_TRUE, this,
                            EXIT, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
      }
      else {
          v.visitCFG(left, right, ENTRY);
          v.visitCFG(right, this, EXIT);
      }
    }

    return succs;
  }

  public List<Type> throwTypes(TypeSystem ts) {
    if (throwsArithmeticException()) {
      return Collections.<Type>singletonList(ts.ArithmeticException());
    }

    return Collections.EMPTY_LIST;
  }
  
}
