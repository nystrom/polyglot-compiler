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
 * A <code>Return</code> represents a <code>return</code> statement in Java.
 * It may or may not return a value.  If not <code>expr()</code> should return
 * null.
 */
public class Return_c extends Stmt_c implements Return
{
    protected Expr expr;

    public Return_c(Position pos, Expr expr) {
	super(pos);
	assert(true); // expr may be null
	this.expr = expr;
    }

    /** Get the expression to return, or null. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to return, or null. */
    public Return expr(Expr expr) {
	Return_c n = (Return_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the statement. */
    protected Return_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Return_c n = (Return_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }
  
    public String toString() {
	return "return" + (expr != null ? " " + expr : "") + ";";
    }

}
