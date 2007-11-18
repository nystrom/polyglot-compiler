/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.CannotResolvePlaceHolderException;

/**
 * A place holder type when serializing the Polylgot type information. 
 * When serializing the type information for some class <code>C</code>, 
 * Placeholders are used to prevent serializing the class type information
 * for classes that <code>C</code> depends on.  
 */
public class PlaceHolder_c implements NamedPlaceHolder
{
    /**
     * The name of the place holder.
     */
    protected String name;

    /** Used for deserializing types. */
    protected PlaceHolder_c() { }
    
    /** Creates a place holder type for the type. */
    public PlaceHolder_c(Named t) {
        this(t.fullName());
    }
    
    public PlaceHolder_c(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public int hashCode() {
        return name.hashCode();
    }
    
    public boolean equals(Object o) {
        return o == this || (o instanceof PlaceHolder_c && name.equals(((PlaceHolder_c) o).name));
    }
    
    /**
     * Resolve the place holder into a TypeObject.  This method
     * should <strong>never</strong> throw a SchedulerException.
     * Instead, it should return null if the object cannot be resolved 
     * until after another pass runs.  The method is responsible for setting
     * up the appropriate dependencies to reattempt the current goal.
     */
    public TypeObject resolve(TypeSystem ts) throws CannotResolvePlaceHolderException {
        return resolveUnsafe(ts);
    }
    
    public TypeObject resolveUnsafe(TypeSystem ts) throws CannotResolvePlaceHolderException {
        try {
            return ts.systemResolver().find(name);
        }
        catch (SemanticException e) {
            // The type could not be found.
            throw new CannotResolvePlaceHolderException(e);
        }
    }
    
    /** A potentially safer alternative implementation of resolve. */
    public TypeObject resolveSafe(TypeSystem ts) throws CannotResolvePlaceHolderException {
        Named n = ts.systemResolver().check(name);

        if (n != null) {
            return n;
        }

        // The class has not been loaded yet.
        throw new CannotResolvePlaceHolderException("Could not resolve " + name);
    }
    
    public String toString() {
	return "PlaceHolder(" + name + ")";
    }
}
