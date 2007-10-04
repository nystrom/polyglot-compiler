/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */

package polyglot.types;

import java.util.List;

/**
 * A <code>ConstructorInstance</code> contains type information for a
 * constructor.
 */
public interface ConstructorInstance extends ProcedureInstance, MemberInstance, Declaration
{
    /** Non-destructive updates. */
    ConstructorInstance flags(Flags flags);
    ConstructorInstance formalTypes(List l);
    ConstructorInstance throwTypes(List l);
    ConstructorInstance container(ClassType container);
    ConstructorInstance orig();
}