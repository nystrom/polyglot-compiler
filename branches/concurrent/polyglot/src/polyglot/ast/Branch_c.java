/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Branch</code> is an immutable representation of a branch
 * statment in Java (a break or continue).
 */
public class Branch_c extends Stmt_c implements Branch
{
    protected Branch.Kind kind;
    protected Id label;

    public Branch_c(Position pos, Branch.Kind kind, Id label) {
	super(pos);
	assert(kind != null); // label may be null
	this.kind = kind;
	this.label = label;
    }

    /** Get the kind of the branch. */
    public Branch.Kind kind() {
	return this.kind;
    }

    /** Set the kind of the branch. */
    public Branch kind(Branch.Kind kind) {
	Branch_c n = (Branch_c) copy();
	n.kind = kind;
	return n;
    }
    
    /** Get the target label of the branch. */
    public Id labelNode() {
        return this.label;
    }
    
    /** Set the target label of the branch. */
    public Branch labelNode(Id label) {
        Branch_c n = (Branch_c) copy();
        n.label = label;
        return n;
    }

    /** Reconstruct the expression. */
    protected Branch_c reconstruct(Id label) {
        if (label != this.label) {
            Branch_c n = (Branch_c) copy();
            n.label = label;
            return n;
        }
        
        return this;
    }
    
    /** Visit the children of the constructor. */
    public Node visitChildren(NodeVisitor v) {
        Id label = (Id) visitChild(this.label, v);
        return reconstruct(label);
    }

    public String toString() {
	switch (kind) {
	case BREAK:
	    return "break" + (label != null ? " " + label.toString() : "");
	case CONTINUE:
	    return "continue" + (label != null ? " " + label.toString() : "");
	default:
	    throw new InternalCompilerError("Unknown branch kind.", position());
	}
    }
}
