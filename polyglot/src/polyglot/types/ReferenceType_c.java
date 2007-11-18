/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.util.Position;

/**
 * A <code>ReferenceType</code> represents a reference type --
 * a type on which contains methods and fields and is a subtype of
 * Object.
 */
public abstract class ReferenceType_c extends Type_c implements ReferenceType
{
    protected ReferenceType_c() {
	super();
    }

    public ReferenceType_c(TypeSystem ts) {
	this(ts, null);
    }

    public ReferenceType_c(TypeSystem ts, Position pos) {
	super(ts, pos);
    }

    public boolean isReference() { return true; }
    public ReferenceType toReference() { return this; }

    /** Get a list of all the type's MemberInstances. */
    public List members() {
        List l = new ArrayList();
        l.addAll(methods());
        l.addAll(fields());
        return l;
    }

    /**
     * Returns a list of MethodInstances for all the methods declared in this.
     * It does not return methods declared in supertypes.
     */
    public abstract List<MethodType> methods();

    /**
     * Returns a list of FieldInstances for all the fields declared in this.
     * It does not return fields declared in supertypes.
     */
    public abstract List<FieldType> fields();

    /** 
     * Returns the supertype of this class.  For every class except Object,
     * this is non-null.
     */
    public abstract Type superType();

    /**
     * Returns a list of the types of this class's interfaces.
     */
    public abstract List<Type> interfaces();

    /** Return true if t has a method mi */
    public boolean hasMethod(MethodType mi) {
        for (Iterator j = methods().iterator(); j.hasNext(); ) {
            MethodType mj = (MethodType) j.next();

            if (ts.isSameMethod(mi, mj)) {
                return true;
            }
        }

        return false;
    }

    public boolean descendsFrom(Type ancestor) {
        if (ancestor.isNull()) {
            return false;
        }

        if (ts.typeEquals(this, ancestor)) {
            return false;
        }

        if (! ancestor.isReference()) {
            return false;
        }

        if (ts.typeEquals(ancestor, ts.Object())) {
            return true;
        }

        // Next check interfaces.
        for (Iterator<Type> i = interfaces().iterator(); i.hasNext(); ) {
            Type parentType = (Type) i.next();

            if (ts.isSubtype(parentType, ancestor)) {
                return true;
            }
        }

        return false;
    }

    public boolean isImplicitCastValidImpl(Type toType) {
        return ts.isSubtype(this, toType);
    }

    public List<MethodType> methodsNamed(String name) {
        List<MethodType> l = new LinkedList();

        for (Iterator<MethodType> i = methods().iterator(); i.hasNext(); ) {
            MethodType mi = (MethodType) i.next();
            if (mi.name().equals(name)) {
                l.add(mi);
            }
        }

        return l;
    }

    public List<MethodType> methods(String name, List<Type> argTypes) {
        List<MethodType> l = new LinkedList();

        for (Iterator<MethodType> i = methodsNamed(name).iterator(); i.hasNext(); ) {
            MethodType mi = (MethodType) i.next();
            if (mi.hasFormals(argTypes)) {
                l.add(mi);
            }
        }

        return l;
    }

    /**
     * Requires: all type arguments are canonical.  ToType is not a NullType.
     *
     * Returns true iff a cast from this to toType is valid; in other
     * words, some non-null members of this are also members of toType.
     **/
    public boolean isCastValid(Type toType) {
        if (! toType.isReference()) return false;
        return ts.isSubtype(this, toType) || ts.isSubtype(toType, this);
    }
}
