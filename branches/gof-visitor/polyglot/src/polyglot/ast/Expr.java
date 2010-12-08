/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.types.Ref;
import polyglot.types.Type;

/**
 * An <code>Expr</code> represents any Java expression.  All expressions
 * must be subtypes of Expr.
 */
public interface Expr extends Receiver, Term
{
    /**
     * Return an equivalent expression, but with the type <code>type</code>.
     */
    Expr type(Type type);
    
    Ref<Type> typeRef();

    /**
     * Return whether the expression evaluates to a constant.
     * This is not valid until after disambiguation.
     */
    boolean isConstant();

    /** Returns the constant value of the expression, if any. */
    Object constantValue();
    
}
