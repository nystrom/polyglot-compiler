/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Context;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Catch</code> represents one half of a <code>try-catch</code>
 * statement.  Specifically, the second half.
 */
public class Catch_c extends Stmt_c implements Catch
{
    protected Formal formal;
    protected Block body;

    public Catch_c(Position pos, Formal formal, Block body) {
	super(pos);
	assert(formal != null && body != null);
	this.formal = formal;
	this.body = body;
    }

    /** Get the catchType of the catch block. */
    public Type catchType() {
        return formal.declType();
    }

    /** Get the formal of the catch block. */
    public Formal formal() {
	return this.formal;
    }

    /** Set the formal of the catch block. */
    public Catch formal(Formal formal) {
	Catch_c n = (Catch_c) copy();
	n.formal = formal;
	return n;
    }

    /** Get the body of the catch block. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the catch block. */
    public Catch body(Block body) {
	Catch_c n = (Catch_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the catch block. */
    protected Catch_c reconstruct(Formal formal, Block body) {
	if (formal != this.formal || body != this.body) {
	    Catch_c n = (Catch_c) copy();
	    n.formal = formal;
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the catch block. */
    public Node visitChildren(NodeVisitor v) {
	Formal formal = (Formal) visitChild(this.formal, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(formal, body);
    }

    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    public String toString() {
	return "catch (" + formal + ") " + body;
    }
}
