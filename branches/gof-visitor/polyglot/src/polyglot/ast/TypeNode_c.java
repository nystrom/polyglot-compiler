/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.*;
import polyglot.util.Position;

/**
 * A <code>TypeNode</code> is the syntactic representation of a 
 * <code>Type</code> within the abstract syntax tree.
 */
public abstract class TypeNode_c extends Term_c implements TypeNode
{
    /**
     * A reference to the computed type for the node. Is is VERY
     * important that this reference not change as the node is transformed.
     * Other TypeObjects have a copy of the reference. When the TypeNode is
     * disambiguated, the reference should be updated rather than a new
     * reference created.
     */
    protected Ref< Type> type;

    public TypeNode_c(Position pos) {
    	super(pos);
    }
    
    /** Get the type as a qualifier. */
    public Ref< Qualifier> qualifierRef() {
        return (Ref<Qualifier>) (Ref) typeRef();
    }

    /** Get the type this node encapsulates. */
    public Ref< Type> typeRef() {
	return this.type;
    }

    public Type type() {
        return Types.get(this.type);
    }

    /** Set the type this node encapsulates. */
    public TypeNode typeRef(Ref<Type> type) {
	TypeNode_c n = (TypeNode_c) copy();
	assert(type != null);
	n.type = type;
	return n;
    }

    public String toString() {
	if (type != null) {
	    return type.toString();
	}
	else {
	    return "<unknown type>";
	}
    }

    public String nameString() {
        Type t = type();
        if (t instanceof Named) {
            return ((Named) t).name().toString();
        }
        return null;
    }
}
