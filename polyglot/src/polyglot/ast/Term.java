/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.Collection;

import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.SubtypeSet;

/**
 * A <code>Term</code> represents any Java expression or statement on which
 * dataflow can be performed.
 */
public interface Term extends Node
{
    /**
     * Returns true if the term is reachable.  This attribute is not
     * guaranteed correct until after the reachability pass.
     *
     * @see polyglot.visit.ReachChecker
     */
    public boolean reachable();

    /** True if the term is reachable. */
    public Ref<Boolean> reachableRef();

    /**
     * List of Types with all exceptions possibly thrown by this term.
     * The list is not necessarily correct until after exception-checking.
     * <code>polyglot.ast.NodeOps.throwTypes()</code> is similar, but exceptions
     * are not propagated to the containing node.
     */
    public Ref<Collection<Type>> throwsRef();
}
