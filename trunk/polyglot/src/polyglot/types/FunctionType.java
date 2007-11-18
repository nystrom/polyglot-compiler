/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */
package polyglot.types;


/**
 * A <code>ProcedureInstance</code> contains the type information for a Java
 * procedure (either a method or a constructor).
 */
public interface FunctionType<T extends ProcedureDef> extends ProcedureType<T> {
    Type returnType();
}
