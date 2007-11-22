/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.frontend.JLScheduler.FragmentGoal;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

/** Visitor which performs type checking on the AST. */
public class TypeChecker extends ContextVisitor
{
    Scope scope;
    
    public static enum Scope {
        SUPER,
        SIGNATURES,
        BODY,
    }

    public static enum Mode {
        CURRENT_ROOT,
        INNER_ROOT,
        NON_ROOT
    }
    
    public Mode mode(Node n) {
        ASTFragment currentFragment = currentFragment();
        boolean isRoot = currentFragment != null && currentFragment == getFragment(n);

        Mode mode;
        
        if (isRoot) {
            mode = Mode.CURRENT_ROOT;
        }
        else if (getFragment(n) != null) {
            mode = Mode.INNER_ROOT;
        }
        else {
            mode = Mode.NON_ROOT;
        }

        return mode;
    }
    
    public Scope scope() {
        return scope;
    }
    
    final static Scope SUPER_OF_INNER = Scope.SIGNATURES;
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
        this(job, ts, nf, true, true);
    }
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf, boolean visitSigs, boolean visitBodies) {
        super(job, ts, nf);
        if (visitBodies) scope = Scope.BODY;
        else if (visitSigs) scope = Scope.SIGNATURES;
        else scope = Scope.SUPER;
    }
    
    public TypeChecker visitSupers() {
        TypeChecker tc = (TypeChecker) copy();
        tc.scope = Scope.SUPER;
        return tc;
    }
    
    public TypeChecker visitSignatures() {
        TypeChecker tc = (TypeChecker) copy();
        tc.scope = Scope.SIGNATURES;
        return tc;
    }
    
    public TypeChecker visitBodies() {
        TypeChecker tc = (TypeChecker) copy();
        tc.scope = Scope.BODY;
        return tc;
    }
    
    public boolean shouldVisitSupers() { return scope == Scope.SUPER; }
    public boolean shouldVisitSignatures() { return scope == Scope.SIGNATURES; }
    public boolean shouldVisitBodies() { return scope == Scope.BODY; }
    
    public ASTFragment getFragment(Def def) {
        Job job = Globals.currentJob();
        return job.fragmentMap().get(def);
    }
    
    public boolean isCurrentFragmentRoot(Node n) {
        return currentFragment() != null && currentFragment() == getFragment(n);
    }
    
    public ASTFragment currentFragment() {
        Goal g = Globals.Scheduler().currentGoal();
        if (g instanceof JLScheduler.FragmentGoal) {
            JLScheduler.FragmentGoal fg = (JLScheduler.FragmentGoal) g;
            return getFragment(fg.def());
        }
        return null;
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
    
    Def getDef(ASTFragment f) {
        for (Def def : f.node().defs()) {
            return def;
        }
        return null;
    }
    
    public Node override(Node parent, Node n) {
        FragmentRoot asRoot = null;
        if (n instanceof FragmentRoot) {
            ASTFragment f = getFragment(n);
            if (f != null) {
                if (f.node() != n) {
                    // Substitute nodes from the fragment map.
                    return this.visitEdge(parent, f.node());
                }
                asRoot = f.node();
            }
        }
        
        Mode mode = mode(n);

//        if (mode != Mode.CURRENT_ROOT) {
//            switch (scope) {
//            case SIGNATURES_OF_ROOT:
//                if (parent instanceof FieldDecl && ((FieldDecl) parent).init() == n) {
//                    return n;
//                }
//                if (parent instanceof CodeDecl && ((CodeDecl) parent).body() == n) {
//                    return n;
//                }
//                break;
//            case BODY_OF_ROOT:
//                break;
//            }
//        }

        try {
            if (Report.should_report(Report.visit, 2))
                Report.report(2, ">> " + this + "::override " + n);
            
            Node m = n.del().typeCheckOverride(parent, this);
            
            updateRoot(m);
            asRoot = (FragmentRoot) m;
            
            if (Report.should_report(Report.visit, 2))
                Report.report(2, "<< " + this + "::override " + n + " -> " + m);
            
            // If this is a fragment root, but not the root of the current fragment, spawn a subgoal.
            // But ONLY if type-checking.
            if (asRoot != null && mode == Mode.INNER_ROOT && scope == Scope.SUPER) {
                assert asRoot != null;
                for (Def def : asRoot.defs()) {
                    FragmentGoal g = (FragmentGoal) Globals.Scheduler().SupertypeDef(job(), def);
                    if (! Globals.Scheduler().reached(g)) {
                        boolean result = Globals.Scheduler().attempt(g);
                        if (! result) {
                            throw new SemanticException("Could not type check " + def + ".");
                        }
                    }
                    return getFragment(def).node();
                }
            }
            
            // If this is a fragment root, but not the root of the current fragment, spawn a subgoal.
            // But ONLY if type-checking.
            if (asRoot != null && mode == Mode.INNER_ROOT && scope == Scope.SIGNATURES || scope == Scope.BODY) {
                assert asRoot != null;
                for (Def def : asRoot.defs()) {
                    FragmentGoal g = (FragmentGoal) Globals.Scheduler().SignatureDef(job(), def);
                    if (! Globals.Scheduler().reached(g)) {
                        boolean result = Globals.Scheduler().attempt(g);
                        if (! result) {
                            throw new SemanticException("Could not type check " + def + ".");
                        }
                    }
                    return getFragment(def).node();
                }
            }

            if (asRoot != null && mode == Mode.INNER_ROOT && scope == Scope.BODY) {
                assert asRoot != null;
                for (Def def : asRoot.defs()) {
                    FragmentGoal g = (FragmentGoal) Globals.Scheduler().TypeCheckDef(job(), def);
                    if (! Globals.Scheduler().reached(g)) {
                        boolean result = Globals.Scheduler().attempt(g);
                        if (! result) {
                            throw new SemanticException("Could not type check " + def + ".");
                        }
                    }
                    return getFragment(def).node();
                }
            }

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

    protected void updateRoot(Node m) {
        // Update the fragment map with the new node.
        if (m instanceof FragmentRoot) {
            FragmentRoot r = (FragmentRoot) m;
            for (Def def : r.defs()) {
                ASTFragment f = getFragment(def);
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

        Mode mode = mode(old);

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
