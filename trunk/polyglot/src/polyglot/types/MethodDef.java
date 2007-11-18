/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */

package polyglot.types;


/**
 * A <code>MethodInstance</code> represents the type information for a Java
 * method.
 */
public interface MethodDef extends FunctionDef, MemberDef, Def
{
    MethodType asType();
    
    /**
     * The method's name.
     */
    String name();
    
    /**
     * Destructively set the method's name.
     * @param name
     */
    void setName(String name);
}    
