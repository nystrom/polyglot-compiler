/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A immutable representation of a Java language <code>do</code> statement. 
 * It contains a statement to be executed and an expression to be tested 
 * indicating whether to reexecute the statement.
 */ 
public class Do_c extends Loop_c implements Do
{
    protected Stmt body;
    protected Expr cond;

    public Do_c(Position pos, Stmt body, Expr cond) {
	super(pos);
	assert(body != null && cond != null);
	this.body = body;
	this.cond = cond;
    }

    /** Get the body of the statement. */
    public Stmt body() {
	return this.body;
    }

    /** Set the body of the statement. */
    public Do body(Stmt body) {
	Do_c n = (Do_c) copy();
	n.body = body;
	return n;
    }

    /** Get the conditional of the statement. */
    public Expr cond() {
	return this.cond;
    }

    /** Set the conditional of the statement. */
    public Do cond(Expr cond) {
	Do_c n = (Do_c) copy();
	n.cond = cond;
	return n;
    }

    /** Reconstruct the statement. */
    protected Do_c reconstruct(Stmt body, Expr cond) {
	if (body != this.body || cond != this.cond) {
	    Do_c n = (Do_c) copy();
	    n.body = body;
	    n.cond = cond;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
        Node body = visitChild(this.body, v);
        if (body instanceof NodeList) body = ((NodeList) body).toBlock();
	Expr cond = (Expr) visitChild(this.cond, v);
	return reconstruct((Stmt) body, cond);
    }

    public Context enterChildScope(Node child, Context c) {
	if (child == this.body) {
	    Name label = null;
	    c = c.pushBreakLabel(label);
	    c = c.pushContinueLabel(label);
	}
            
        return super.enterChildScope(child, c);
    }

    public String toString() {
	return "do " + body + " while (" + cond + ")";
    }

    public Term continueTarget() {
        return cond;
    }

}
