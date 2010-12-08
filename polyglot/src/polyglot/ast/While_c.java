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
 * An immutable representation of a Java language <code>while</code>
 * statement.  It contains a statement to be executed and an expression
 * to be tested indicating whether to reexecute the statement.
 */ 
public class While_c extends Loop_c implements While
{
    protected Expr cond;
    protected Stmt body;

    public While_c(Position pos, Expr cond, Stmt body) {
	super(pos);
	assert(cond != null && body != null);
	this.cond = cond;
	this.body = body;
    }

    /** Get the conditional of the statement. */
    public Expr cond() {
	return this.cond;
    }

    /** Set the conditional of the statement. */
    public While cond(Expr cond) {
	While_c n = (While_c) copy();
	n.cond = cond;
	return n;
    }

    /** Get the body of the statement. */
    public Stmt body() {
	return this.body;
    }

    /** Set the body of the statement. */
    public While body(Stmt body) {
	While_c n = (While_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the statement. */
    protected While_c reconstruct(Expr cond, Stmt body) {
	if (cond != this.cond || body != this.body) {
	    While_c n = (While_c) copy();
	    n.cond = cond;
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr cond = (Expr) visitChild(this.cond, v);
	Node body = visitChild(this.body, v);
        if (body instanceof NodeList) body = ((NodeList) body).toBlock();
	return reconstruct(cond, (Stmt) body);
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
	return "while (" + cond + ") ...";
    }

    public Term continueTarget() {
        return cond;
    }
    

}
