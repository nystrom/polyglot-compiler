/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.io.*;

import polyglot.frontend.GoalSet;
import polyglot.frontend.Scheduler;
import polyglot.util.*;

/**
 * Abstract implementation of a type object.  Contains a reference to the
 * type system and to the object's position in the code.
 */  
public abstract class TypeObject_c implements TypeObject
{
    protected transient TypeSystem ts;
    protected Position position;
    protected GoalSet phase;
    protected Symbol<? extends TypeObject> sym;

    public static <T> T get(Ref<T> ref) {
        return ref != null ? ref.get() : null;
    }
    
    public void complete(GoalSet phase) { }

    public void setPhase(GoalSet phase) {
        this.phase = phase;    
    }

    public GoalSet phase() {
        return phase;
    }

    /** Used for deserializing types. */
    protected TypeObject_c() {
    }
    
    /** Creates a new type in the given a TypeSystem. */
    public TypeObject_c(TypeSystem ts) {
        this(ts, null);
    }

    public TypeObject_c(TypeSystem ts, Position pos) {
	this.ts = ts;
	this.position = pos;
    }

    public Object copy() {
        try {
            return (TypeObject_c) super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.");
        }
    }

    public TypeSystem typeSystem() {
        return ts;
    }

    public Position position() {
        return position;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException,
					       ClassNotFoundException {
	if (in instanceof TypeInputStream) {
	    ts = ((TypeInputStream) in).getTypeSystem();
	}

        in.defaultReadObject();
    }

    /**
     * Return whether o is structurally equivalent to o.
     * Implementations should override equalsImpl().
     */
    public final boolean equals(Object o) {
        return o instanceof TypeObject && ts.equals(this, (TypeObject) o);
    }

    public int hashCode() {
        return super.hashCode();
    }
    
    /**
     * Default implementation is pointer equality.
     */
    public boolean equalsImpl(TypeObject t) {
        return t == this;
    }

    /**
     * Overload equalsImpl to find inadvertent overriding errors.
     * Make package-scope and void to break callers.
     */ 
    final void equalsImpl(Object o) { }
    final void typeEqualsImpl(Object o) { }
    final void typeEqualsImpl(TypeObject o) { }

    public <T extends TypeObject> Symbol<T> symbol() {
        return (Symbol<T>) sym;
    }

    public void setSymbol(Symbol<? extends TypeObject> sym) {
        this.sym = sym;
    }
}
