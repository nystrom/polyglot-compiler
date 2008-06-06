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
    protected Kind kind;

    /** Used for deserializing types. */
    protected PrimitiveType_c() { }

    public PrimitiveType_c(TypeSystem ts, Kind kind) {
            super(ts);
            this.kind = kind;
    }

    public Kind kind() {
            return kind;
    }

    public String toString() {
            return kind.toString();
    }

    public String translate(Resolver c) {
            return kind.toString();
    }

    public boolean isPrimitive() { return true; }
    public PrimitiveType toPrimitive() { return this; }

    public int hashCode() {
            return kind.hashCode();
    }

    public boolean equalsImpl(TypeObject t) {
        if (t instanceof PrimitiveType) {
            PrimitiveType p = (PrimitiveType) t;
            return kind() == p.kind();
        }
        return false;
    }

    public String wrapperTypeString(TypeSystem ts) {
            return ts.wrapperTypeString(this);
    }
    
    public String name() {
            return kind.toString();	
    }
    
    public String fullName() {
            return name();
    }
}
