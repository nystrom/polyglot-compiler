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

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * An immutable representation of a Java language <code>for</code>
 * statement.  Contains a statement to be executed and an expression
 * to be tested indicating whether to reexecute the statement.
 */
public class For_c extends Loop_c implements For
{
    protected List<ForInit> inits;
    protected Expr cond;
    protected List<ForUpdate> iters;
    protected Stmt body;

    public For_c(Position pos, List inits, Expr cond, List iters, Stmt body) {
	super(pos);
	assert(inits != null && iters != null && body != null); // cond may be null, inits and iters may be empty
	this.inits = TypedList.copyAndCheck(inits, ForInit.class, true);
	this.cond = cond;
	this.iters = TypedList.copyAndCheck(iters, ForUpdate.class, true);
	this.body = body;
    }

    /** List of initialization statements */
    public List<ForInit> inits() {
	return Collections.unmodifiableList(this.inits);
    }

    /** Set the inits of the statement. */
    public For inits(List inits) {
	For_c n = (For_c) copy();
	n.inits = TypedList.copyAndCheck(inits, ForInit.class, true);
	return n;
    }

    /** Loop condition */
    public Expr cond() {
	return this.cond;
    }

    /** Set the conditional of the statement. */
    public For cond(Expr cond) {
	For_c n = (For_c) copy();
	n.cond = cond;
	return n;
    }

    /** List of iterator expressions. */
    public List<ForUpdate> iters() {
	return Collections.unmodifiableList(this.iters);
    }

    /** Set the iterator expressions of the statement. */
    public For iters(List<ForUpdate> iters) {
	For_c n = (For_c) copy();
	n.iters = TypedList.copyAndCheck(iters, ForUpdate.class, true);
	return n;
    }

    /** Loop body */
    public Stmt body() {
	return this.body;
    }

    /** Set the body of the statement. */
    public For body(Stmt body) {
	For_c n = (For_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the statement. */
    protected For_c reconstruct(List inits, Expr cond, List iters, Stmt body) {
	if (! CollectionUtil.allEqual(inits, this.inits) || cond != this.cond || ! CollectionUtil.allEqual(iters, this.iters) || body != this.body) {
	    For_c n = (For_c) copy();
	    n.inits = TypedList.copyAndCheck(inits, ForInit.class, true);
	    n.cond = cond;
	    n.iters = TypedList.copyAndCheck(iters, ForUpdate.class, true);
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	List inits = visitList(this.inits, v);
	Expr cond = (Expr) visitChild(this.cond, v);
	List iters = visitList(this.iters, v);
        Node body = visitChild(this.body, v);
	if (body instanceof NodeList) body = ((NodeList) body).toBlock();
	return reconstruct(inits, cond, iters, (Stmt) body);
    }

    
    public Context enterChildScope(Node child, Context c) {
	if (child == this.body) {
	    Name label = null;
	    c = c.pushBreakLabel(label);
	    c = c.pushContinueLabel(label);
	}
            
        return super.enterChildScope(child, c);
    }

    public Context enterScope(Context c) {
	return c.pushBlock();
    }

    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append("for (");
	String sep = "";
	for (ForInit s : inits()) {
	    sb.append(sep);
	    sep = ", ";
	    sb.append(s);
	}
	sb.append("; ");
	if (cond() != null)
	    sb.append(cond());
	sep = "";
	for (ForUpdate s : iters()) {
	    sb.append(sep);
	    sep = ", ";
	    sb.append(s);
	}
	sb.append(") ");
	sb.append(body());
	return sb.toString();
    }
}
