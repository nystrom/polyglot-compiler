/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.LinkedHashMap;
import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.*;

/** Visitor which performs type checking on the AST. */
public class ASTFragmenter extends ContextVisitor
{
    public ASTFragmenter(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    protected Map<Def,ASTFragment> fragmentMap;
    
    public Map<Def,ASTFragment> fragmentMap() { return fragmentMap; }
    
    @Override
    public NodeVisitor begin() {
        fragmentMap = new LinkedHashMap<Def,ASTFragment>();
        return super.begin();
    }
    
    @Override
    public void finish(Node ast) {
        super.finish(ast);
        job.setFragmentMap(fragmentMap);
    }
    
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n);

        Context c = context().freeze();
        
        if (n instanceof FragmentRoot) {
            FragmentRoot r = (FragmentRoot) n;
            ASTFragment f = new ASTFragment(parent, r, c); // create ONE fragment per node
            for (Def def : r.defs()) {
                assert def != null;
                fragmentMap.put(def, f);
            }
        }

        return super.leaveCall(old, n, v);
    }   
}
