/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

/**
 * An <code>ArrayInit</code> is an immutable representation of
 * an array initializer, such as { 3, 1, { 4, 1, 5 } }.  Note that
 * the elements of these array may be expressions of any type (e.g.,
 * <code>Call</code>).
 */
public interface ArrayInit extends Expr
{
    /**
     * Get the initializer elements.
     * @return A list of {@link polyglot.ast.Expr Expr}.
     */
    List<Expr> elements();

    /**
     * Set the initializer elements.
     * @param elements A list of {@link polyglot.ast.Expr Expr}.
     */
    ArrayInit elements(List<Expr> elements);
}
