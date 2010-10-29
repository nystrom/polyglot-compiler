/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

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

}
