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
 * An <code>Assert</code> is an assert statement.
 */
public class Assert_c extends Stmt_c implements Assert
{
    protected Expr cond;
    protected Expr errorMessage;

    public Assert_c(Position pos, Expr cond, Expr errorMessage) {
	super(pos);
	assert(cond != null); // errorMessage may be null
	this.cond = cond;
	this.errorMessage = errorMessage;
    }

    /** Get the condition to check. */
    public Expr cond() {
	return this.cond;
    }

    /** Set the condition to check. */
    public Assert cond(Expr cond) {
	Assert_c n = (Assert_c) copy();
	n.cond = cond;
	return n;
    }

    /** Get the error message to report. */
    public Expr errorMessage() {
	return this.errorMessage;
    }

    /** Set the error message to report. */
    public Assert errorMessage(Expr errorMessage) {
	Assert_c n = (Assert_c) copy();
	n.errorMessage = errorMessage;
	return n;
    }

    /** Reconstruct the statement. */
    protected Assert_c reconstruct(Expr cond, Expr errorMessage) {
	if (cond != this.cond || errorMessage != this.errorMessage) {
	    Assert_c n = (Assert_c) copy();
	    n.cond = cond;
	    n.errorMessage = errorMessage;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr cond = (Expr) visitChild(this.cond, v);
	Expr errorMessage = (Expr) visitChild(this.errorMessage, v);
	return reconstruct(cond, errorMessage);
    }

    public String toString() {
	return "assert " + cond.toString() +
                (errorMessage != null
                    ? ": " + errorMessage.toString() : "") + ";";
    }
}
