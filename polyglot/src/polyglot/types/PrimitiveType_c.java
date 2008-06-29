/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;


/**
 * An <code>PrimitiveType_c</code> represents a primitive type.
 */
public class PrimitiveType_c extends Type_c implements PrimitiveType
{
    protected String name;

    /** Used for deserializing types. */
    protected PrimitiveType_c() { }

    public PrimitiveType_c(TypeSystem ts, String name) {
            super(ts);
            this.name = name;
    }

    public boolean isGloballyAccessible() {
	    return true;
    }

    public String toString() {
	return name;
    }

    public String translate(Resolver c) {
	return name;
    }

    public boolean isPrimitive() { return true; }
    public PrimitiveType toPrimitive() { return this; }

    public int hashCode() {
	return name.hashCode();
    }

    public boolean equalsImpl(TypeObject t) {
        if (t instanceof PrimitiveType) {
            PrimitiveType p = (PrimitiveType) t;
            return name.equals(p.name());
        }
        return false;
    }

    public String wrapperTypeString(TypeSystem ts) {
            return ts.wrapperTypeString(this);
    }
    
    public String name() {
	return name;
    }
    
    public String fullName() {
            return name();
    }
}
