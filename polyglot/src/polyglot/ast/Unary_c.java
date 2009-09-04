/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * A <code>Unary</code> represents a Java unary expression, an
 * immutable pair of an expression and an operator.
 */
public class Unary_c extends Expr_c implements Unary
{
    protected Unary.Operator op;
    protected Expr expr;

    public Unary_c(Position pos, Unary.Operator op, Expr expr) {
	super(pos);
	assert(op != null && expr != null);
	this.op = op;
	this.expr = expr;
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
	return Precedence.UNARY;
    }

    /** Get the sub-expression of the expression. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the sub-expression of the expression. */
    public Unary expr(Expr expr) { Unary_c n = (Unary_c) copy(); n.expr = expr;
      return n; }

    /** Get the operator. */
    public Unary.Operator operator() {
	return this.op;
    }

    /** Set the operator. */
    public Unary operator(Unary.Operator op) {
	Unary_c n = (Unary_c) copy();
	n.op = op;
	return n;
    }

    /** Reconstruct the expression. */
    protected Unary_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Unary_c n = (Unary_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();
        Context context = av.context();
        try {
            if (child == expr) {
                if (op == POST_INC || op == POST_DEC ||
                    op == PRE_INC || op == PRE_DEC) {

                    if (ts.isImplicitCastValid(child.type(), av.toType(), context)) {
                        return ts.promote(child.type());
                    }
                    else {
                        return av.toType();
                    }
                }
                else if (op == NEG || op == POS) {
                    if (ts.isImplicitCastValid(child.type(), av.toType(), context)) {
                        return ts.promote(child.type());
                    }
                    else {
                        return av.toType();
                    }
                }
                else if (op == BIT_NOT) {
                    if (ts.isImplicitCastValid(child.type(), av.toType(), context)) {
                        return ts.promote(child.type());
                    }
                    else {
                        return av.toType();
                    }
                }
                else if (op == NOT) {
                    return ts.Boolean();
                }
            }
        }
        catch (SemanticException e) {
        }

        return child.type();
    }

    /** Check exceptions thrown by the statement. */
    public String toString() {
        if (op == NEG && expr instanceof IntLit && ((IntLit) expr).boundary()) {
            return op.toString() + ((IntLit) expr).positiveToString();
        }
        else if (op.isPrefix()) {
	    return op.toString() + expr.toString();
	}
	else {
	    return expr.toString() + op.toString();
	}
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (op == NEG && expr instanceof IntLit && ((IntLit) expr).boundary()) {
	    w.write(op.toString());
            w.write(((IntLit) expr).positiveToString());
        }
        else if (op.isPrefix()) {
	    w.write(op.toString());
	    printSubExpr(expr, false, w, tr);
	}
	else {
	    printSubExpr(expr, false, w, tr);
	    w.write(op.toString());
	}
    }

    public Term firstChild() {
        return expr;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        if (expr.type().isBoolean()) {
            v.visitCFG(expr, FlowGraph.EDGE_KEY_TRUE, this,
                             EXIT, FlowGraph.EDGE_KEY_FALSE, this, EXIT);
        } else {
            v.visitCFG(expr, this, EXIT);
        }
        
        return succs;
    }
}
