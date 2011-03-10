/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.Collection;
import java.util.List;

import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;

/**
 * A <code>Term</code> represents any Java expression or statement on which
 * dataflow can be performed.
 */
public abstract class Term_c extends Node_c implements Term
{
    private Ref<Boolean> reachableRef;
    private Ref<Collection<Type>> throwsRef;

    public Term_c(Position pos) {
	super(pos);
	reachableRef = Types.<Boolean> lazyRef(null, Types.Granularity.LOWER_LEVEL);
	throwsRef = Types.<Collection<Type>> lazyRef(null, Types.Granularity.LOWER_LEVEL);
    }
    
    public Ref<Boolean> reachableRef() { return reachableRef; }
    public Ref<Collection<Type>> throwsRef() { return throwsRef; }

    public final Term firstChild() { return null; }
    
    class CFGBuilder { }
    /**
     * Visit this term in evaluation order.
     */
    public final List<Term> acceptCFG(CFGBuilder v, List<Term> succs) { return succs; }

    /**
     * Return true if this term is eachable.  This attribute is not
     * guaranteed correct until after the reachability pass
     *
     * @see polyglot.visit.ReachChecker
     */
    public boolean reachable() {
        return reachableRef.get();
    }

    protected SubtypeSet exceptions;
    
    public SubtypeSet exceptions() {
        return exceptions;
    }
    
    public Term exceptions(SubtypeSet exceptions) {
        Term_c n = (Term_c) copy();
        n.exceptions = new SubtypeSet(exceptions);
        return n;
    }
}
