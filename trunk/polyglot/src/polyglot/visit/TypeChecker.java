/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

/** Visitor which performs type checking on the AST. */
public class TypeChecker extends ContextVisitor
{
    protected Def rootDef;
    protected int key;
    
    public Goal goal() {
        return Globals.currentGoal();
    }
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
        this(job, ts, nf, null);
    }
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf, Def def) {
        this(job, ts, nf, def, 0);
    }

    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf, Def def, int key) {
        super(job, ts, nf);
        this.rootDef = def;
        this.key = key;
    }
    
    public ASTFragment getFragment(Def def) {
        if (def == null) return null;
        Job job = Globals.currentJob();
        return job.fragmentMap().get(def);
    }
    
    public boolean isCurrentFragmentRoot(Node n) {
        return currentFragment() != null && currentFragment() == getFragment(n);
    }
    
    public ASTFragment currentFragment() {
        return getFragment(rootDef);
    }
    
    public ASTFragment getFragment(Node n) {
        if (n instanceof FragmentRoot) {
            FragmentRoot r = (FragmentRoot) n;
            for (Def def : r.defs()) {
                return getFragment(def);
            }
        }
        return null;
    }
    
    public Def getDef(ASTFragment f) {
        for (Def def : f.node().defs()) {
            return def;
        }
        return null;
    }
    
    public Node override(Node parent, Node n) {
        if (n instanceof FragmentRoot) {
            ASTFragment f = getFragment(n);
            if (f != null) {
                if (f.node() != n) {
                    // Substitute nodes from the fragment map.
                    return this.visitEdge(parent, f.node());
                }
            }
        }
        
        try {
            if (Report.should_report(Report.visit, 2))
                Report.report(2, ">> " + this + "::override " + n);
            
            Node m = n.del().typeCheckOverride(parent, this);
            
            updateRoot(m);

            return m;
        }
        catch (SemanticException e) {
            if (e.getMessage() != null) {
                Position position = e.position();
                
                if (position == null) {
                    position = n.position();
                }
                
                this.errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                     e.getMessage(), position);
            }
            else {
                // silent error; these should be thrown only
                // when the error has already been reported 
            }
            
            return n;
        }
    }

    public void updateRoot(Node m) {
        // Update the fragment map with the new node.
        if (m instanceof FragmentRoot) {
            FragmentRoot r = (FragmentRoot) m;
            for (Def def : r.defs()) {
                ASTFragment f = getFragment(def);
                if (f == null)
                    assert false;
                else
                f.setNode(r);
            }
        }
    }
 
    protected NodeVisitor enterCall(Node n) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::enter " + n);

        TypeChecker v = (TypeChecker) n.del().typeCheckEnter(this);
        
        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::enter " + n + " -> " + v);
        
        return v;
    }
    
    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n);
        
        TypeChecker tc = (TypeChecker) v;

        updateRoot(n);
        
        Node m = n;

        AmbiguityRemover ar = new AmbiguityRemover(tc.job(), tc.typeSystem(), tc.nodeFactory());
        ar = (AmbiguityRemover) ar.context(tc.context());
        m = m.del().disambiguate(ar);
        m = m.del().typeCheck(tc);
        m = m.del().checkConstants(tc);

        updateRoot(m);

        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::leave " + n + " -> " + m);
        
        return m;
    }   
}
