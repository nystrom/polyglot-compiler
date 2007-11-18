/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.CodeWriter;
import polyglot.util.Position;

/**
 * Abstract implementation of a <code>Type</code>.  This implements most of
 * the "isa" and "cast" methods of the type and methods which just dispatch to
 * the type system.
 */
public abstract class Type_c extends TypeObject_c implements Type
{
    /** Used for deserializing types. */
    protected Type_c() { }
    
    /** Creates a new type in the given a TypeSystem. */
    public Type_c(TypeSystem ts) {
        this(ts, null);
    }

    /** Creates a new type in the given a TypeSystem at a given position. */
    public Type_c(TypeSystem ts, Position pos) {
        super(ts, pos);
    }

    /**
     * Return a string into which to translate the type.
     * @param c A resolver in which to lookup this type to determine if
     * the type is unique in the given resolver.
     */
    public abstract String translate(Resolver c);

    public boolean isType() { return true; }
    public boolean isPackage() { return false; }
    public Type toType() { return this; }
    public Package toPackage() { return null; }

    /* To be filled in by subtypes. */
    public boolean isPrimitive() { return false; }
    public boolean isNumeric() { return false; }
    public boolean isIntOrLess() { return false; }
    public boolean isLongOrLess() { return false; }
    public boolean isVoid() { return false; }
    public boolean isBoolean() { return false; }
    public boolean isChar() { return false; }
    public boolean isByte() { return false; }
    public boolean isShort() { return false; }
    public boolean isInt() { return false; }
    public boolean isLong() { return false; }
    public boolean isFloat() { return false; }
    public boolean isDouble() { return false; }

    public boolean isReference() { return false; }
    public boolean isNull() { return false; }
    public boolean isClass() { return false; }
    public boolean isArray() { return false; }

    /**
     * Return true if a subclass of Throwable.
     */
    public boolean isThrowable() {
	return false;
    }

    /**
     * Return true if an unchecked exception.
     */
    public boolean isUncheckedException() {
	return false;
    }
    
    /** Returns a non-null iff isClass() returns true. */
    public ClassType toClass() {
	return null;
    }

    /** Returns a non-null iff isNull() returns true. */
    public NullType toNull() {
	return null;
    }

    /** Returns a non-null iff isReference() returns true. */
    public ReferenceType toReference() {
	return null;
    }

    /** Returns a non-null iff isPrimitive() returns true. */
    public PrimitiveType toPrimitive() {
	return null;
    }

    /** Returns a non-null iff isArray() returns true. */
    public ArrayType toArray() {
	return null;
    }

    /**
     * Return a <code>dims</code>-array of this type.
     */
    public ArrayType arrayOf(int dims) {
	return ts.arrayOf(this, dims); 
    }  

    /**
     * Return an array of this type.
     */
    public ArrayType arrayOf() {
	return ts.arrayOf(this);
    }  
    
    public boolean typeEquals(Type t) {
        return this == t;
    }
    
    /**
     * Return true if this type is a subtype of <code>ancestor</code>.
     */
    public boolean isSubtype(Type t) {
	return ts.typeEquals(this, t) || ts.descendsFrom(this, t);
    }
    
    /**
     * Return true if this type descends from <code>ancestor</code>.
     */
    public boolean descendsFrom(Type t) {
        return false;
    }

    /**
     * Return true if this type can be cast to <code>toType</code>.
     */
    public boolean isCastValid(Type toType) {
	return false;
    }
    
    /**
     * Return true if a value of this type can be assigned to a variable of
     * type <code>toType</code>.
     */
    public boolean isImplicitCastValid(Type toType) {
        return false;
    }

    /**
     * Return true a literal <code>value</code> can be converted to this type.
     * This method should be removed.  It is kept for backward compatibility.
     */
    public boolean numericConversionValid(long value) {
        return false;
    }
    
    /**
     * Return true a literal <code>value</code> can be converted to this type.
     */
    public boolean numericConversionValid(Object value) {
        return false;
    }
    
    /**
     * Return true if the types can be compared; that is, if they have
     * the same type system.
     */
    public boolean isComparable(Type t) {
	return t.typeSystem() == ts;
    }

    /**
     * Yields a string representing this type.  The string
     * should be consistent with equality.  That is,
     * if this.equals(anotherType), then it should be
     * that this.toString().equals(anotherType.toString()).
     *
     * The string does not have to be a legal Java identifier.
     * It is suggested, but not required, that it be an
     * easily human readable representation, and thus useful
     * in error messages and generated output.
     */
    public abstract String toString();

    public void print(CodeWriter w) {
	w.write(toString());
    }
}
