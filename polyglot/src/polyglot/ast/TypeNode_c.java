/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.ast.*;

import polyglot.util.*;
import polyglot.types.*;
import polyglot.visit.*;

/**
 * A <code>TypeNode</code> is the syntactic representation of a 
 * <code>Type</code> within the abstract syntax tree.
 */
public abstract class TypeNode_c extends Term_c implements TypeNode
{
    protected Ref<? extends Type> type;

    public TypeNode_c(Position pos) {
    	super(pos);
    }
    
    /** Get the type as a qualifier. */
    public Ref<? extends Qualifier> qualifierRef() {
        return typeRef();
    }

    /** Get the type this node encapsulates. */
    public Ref<? extends Type> typeRef() {
	return this.type;
    }

    public Type type() {
        return Types.get(this.type);
    }

    /** Set the type this node encapsulates. */
    protected TypeNode typeRef(Ref<? extends Type> type) {
	TypeNode_c n = (TypeNode_c) copy();
	n.type = type;
	return n;
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        if (type == null) {
            TypeSystem ts = tb.typeSystem();
            return typeRef(new ErrorRef_c<Type>(ts, position()));
        }
        else {
            return this;
        }
    }

    public Term firstChild() {
        return null;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        return succs;
    }

    public String toString() {
	if (type != null) {
	    return type.toString();
	}
	else {
	    return "<unknown type>";
	}
    }

    public abstract void prettyPrint(CodeWriter w, PrettyPrinter tr);

    public String name() {
        Type t = type();
        if (t instanceof Named) {
            return ((Named) t).name();
        }
        return null;
    }
}
