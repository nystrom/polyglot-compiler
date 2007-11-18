/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.List;

/**
 * A <code>ReferenceType</code> represents a reference type: a type which
 * contains methods and fields and which is a subtype of Object.
 */
public interface ReferenceType extends Type
{ 
    /**
     * Return the type's super type.
     */
    Type superType();

    /**
     * Return the type's interfaces.
     * @return A list of <code>Type</code>.
     * @see polyglot.types.Type
     */
    List<Type> interfaces();

    /**
     * Return a list of a all the type's members.
     * @return A list of <code>MemberInstance</code>.
     * @see polyglot.types.MemberDef
     */
    List<MemberType> members();
    
    /**
     * Return the type's fields.
     * @return A list of <code>FieldInstance</code>.
     * @see polyglot.types.FieldDef
     */
    List<FieldType> fields();
    
    /**
     * Return the field named <code>name</code>, or null.
     */
    FieldType fieldNamed(String name);

    /**
     * Return the type's methods.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.MethodDef
     */
    List<MethodType> methods();

    /**
     * Return the methods named <code>name</code>, if any.
     * @param name Name of the method to search for.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.MethodDef
     */
    List<MethodType> methodsNamed(String name);

    /**
     * Return the methods named <code>name</code> with the given formal
     * parameter types, if any.
     * @param name Name of the method to search for.
     * @param argTypes A list of <code>Type</code>.
     * @return A list of <code>MethodInstance</code>.
     * @see polyglot.types.Type
     * @see polyglot.types.MethodDef
     */
    List<MethodType> methods(String name, List<Type> argTypes);

    /**
     * Return the true if the type has the given method.
     */
    boolean hasMethod(MethodType mi);
}
