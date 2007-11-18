/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.LocalType;

/** 
 * A local variable expression.
 */
public interface Local extends NamedVariable
{
    /** Get the name of the local variable. */
    Id id();
    
    /** Set the name of the local variable. */
    Local id(Id name);

    /** Get the name of the local variable. */
    String name();

    /** Set the name of the local variable. */
    Local name(String name);

    /**
     * Get the type object for the local.  This field may not be valid until
     * after type checking.
     */
    LocalType localInstance();
    /** Set the type object for the local. */
    Local localInstance(LocalType li);
}
