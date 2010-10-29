/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.util.Enum;

/**
 * A <code>Branch</code> is an immutable representation of a branch
 * statement in Java (a break or continue).
 */
public interface Branch extends Stmt
{
    /** Branch kind: either break or continue. */
    static enum Kind {
	BREAK("break"), CONTINUE("continue");

	String name;

	Kind(String name) {
	    this.name = name;
	}
    }

    public static final Kind BREAK = Kind.BREAK;
    public static final Kind CONTINUE = Kind.CONTINUE;

    /**
     * The kind of branch.
     */
    Kind kind();

    /**
     * Set the kind of branch.
     */
    Branch kind(Kind kind);
    
    /**
     * Target label of the branch.
     */
    Id labelNode();
    
    /**
     * Set the target label of the branch.
     */
    Branch labelNode(Id label);
}
