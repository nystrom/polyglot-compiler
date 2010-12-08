/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Throw</code> is an immutable representation of a <code>throw</code>
 * statement. Such a statement contains a single <code>Expr</code> which
 * evaluates to the object being thrown.
 */
public class Throw_c extends Stmt_c implements Throw
{
    protected Expr expr;

    public Throw_c(Position pos, Expr expr) {
	super(pos);
	assert(expr != null);
	this.expr = expr;
    }

    /** Get the expression to throw. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to throw. */
    public Throw expr(Expr expr) {
	Throw_c n = (Throw_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the statement. */
    protected Throw_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Throw_c n = (Throw_c) copy();
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
	return "throw " + expr + ";";
    }

}
