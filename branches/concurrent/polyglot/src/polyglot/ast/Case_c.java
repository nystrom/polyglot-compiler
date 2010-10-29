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
 * A <code>Case</code> is a representation of a Java <code>case</code>
 * statement.  It can only be contained in a <code>Switch</code>.
 */
public class Case_c extends Stmt_c implements Case
{
    protected Expr expr;
    protected long value;

    public Case_c(Position pos, Expr expr) {
	super(pos);
	assert(true); // expr may be null for default case
	this.expr = expr;
    }

    /** Returns true iff this is the default case. */
    public boolean isDefault() {
	return this.expr == null;
    }

    /**
     * Get the case label.  This must should a constant expression.
     * The case label is null for the <code>default</code> case.
     */
    public Expr expr() {
	return this.expr;
    }

    /** Set the case label.  This must should a constant expression, or null. */
    public Case expr(Expr expr) {
	Case_c n = (Case_c) copy();
	n.expr = expr;
	return n;
    }

    /**
     * Returns the value of the case label.  This value is only valid
     * after type-checking.
     */
    public long value() {
	return this.value;
    }

    /** Set the value of the case label. */
    public Case value(long value) {
	Case_c n = (Case_c) copy();
	n.value = value;
	return n;
    }

    /** Reconstruct the statement. */
    protected Case_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Case_c n = (Case_c) copy();
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
        if (expr == null) {
	    return "default:";
	}
	else {
	    return "case " + expr + ":";
	}
    }
}
