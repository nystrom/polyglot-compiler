/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ext.jl5.types.reflect;

import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.types.ClassContextResolver;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

/**
 * A <code>ClassContextResolver</code> looks up type names qualified with a class name.
 * For example, if the class is "A.B", the class context will return the class
 * for member class "A.B.C" (if it exists) when asked for "C".
 */
public class JL5ClassContextResolver extends ClassContextResolver {
    protected Type type;
    
    /**
     * Construct a resolver.
     * @param ts The type system.
     * @param type The type in whose context we search for member types.
     */
    public JL5ClassContextResolver(TypeSystem ts, Type type) {
    	// The code in the super class compares the current parameterized type 
    	// with the type obtained through the type system using the full name 
    	// of the class. Hence we end up checking for equality on a parameterized
    	// version and a raw version, which doesn't work.
    	// Here we make sure we always use the raw type.
        super(ts, ((type instanceof ParameterizedType) ? ((ParameterizedType) type).baseType() : type));
    }

}
