/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Special</code> is an immutable representation of a
 * reference to <code>this</code> or <code>super</code in Java.  This
 * reference can be optionally qualified with a type such as 
 * <code>Foo.this</code>.
 */
public class Special_c extends Expr_c implements Special
{
    protected Special.Kind kind;
    protected TypeNode qualifier;

    public Special_c(Position pos, Special.Kind kind, TypeNode qualifier) {
	super(pos);
	assert(kind != null); // qualifier may be null
	this.kind = kind;
	this.qualifier = qualifier;
    }

    /** Get the kind of the special expression, either this or super. */
    public Special.Kind kind() {
	return this.kind;
    }

    /** Set the kind of the special expression, either this or super. */
    public Special kind(Special.Kind kind) {
	Special_c n = (Special_c) copy();
	n.kind = kind;
	return n;
    }

    /** Get the qualifier of the special expression. */
    public TypeNode qualifier() {
	return this.qualifier;
    }

    /** Set the qualifier of the special expression. */
    public Special qualifier(TypeNode qualifier) {
	Special_c n = (Special_c) copy();
	n.qualifier = qualifier;
	return n;
    }

    /** Reconstruct the expression. */
    protected Special_c reconstruct(TypeNode qualifier) {
	if (qualifier != this.qualifier) {
	    Special_c n = (Special_c) copy();
	    n.qualifier = qualifier;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	TypeNode qualifier = (TypeNode) visitChild(this.qualifier, v);
	return reconstruct(qualifier);
    }

    public String toString() {
	switch (kind) {
	case SUPER:
	    return (qualifier != null ? qualifier + "." : "") + "super";
	case THIS:
	    return (qualifier != null ? qualifier + "." : "") + "this";
	default:
	    throw new InternalCompilerError("Unknown special kind.", position());
	}
    }

    public void dump(CodeWriter w) {
      super.dump(w);

      if (kind != null) {
        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(kind " + kind + ")");
        w.end();
      }
    }
    
}
