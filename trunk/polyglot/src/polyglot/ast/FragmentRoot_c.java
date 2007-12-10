package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.frontend.*;
import polyglot.frontend.JLScheduler.SignatureDef;
import polyglot.frontend.JLScheduler.SupertypeDef;
import polyglot.main.Report;
import polyglot.types.Def;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.*;

public abstract class FragmentRoot_c extends Term_c implements FragmentRoot {

    public FragmentRoot_c(Position pos) {
        super(pos);
    }
    
    public abstract List<Def> defs();

    public Node visitSignature(NodeVisitor v) throws SemanticException {
        return this;
    }
    public Node typeCheckBody(Node parent, TypeChecker tc, TypeChecker childtc) throws SemanticException {
        return this;
    }
    
    public List<Goal> pregoals(TypeChecker tc, Def def) {
        return Collections.<Goal>emptyList();
    }
    
    public List<Goal> postgoals(TypeChecker tc, Def def) {
        return Collections.<Goal>emptyList();
    }
    
    protected static enum Mode {
        CURRENT_ROOT,
        INNER_ROOT,
        NON_ROOT
    }

    public Node typeCheckOverride(Node parent, TypeChecker tc) throws SemanticException {
        TypeChecker childtc;
        NodeVisitor childv = tc.enter(parent, this);
        if (childv instanceof TypeChecker) {
            childtc = (TypeChecker) childv;
        }
        else {
            return this;
        }

        Goal goal = Globals.currentGoal();
        Mode mode = mode(tc);
        
        Def def = this.defs().iterator().next();

        Node n = this;
        
        switch (mode) {
        case NON_ROOT:
            assert false;
            return this;
        case INNER_ROOT:
            n = this.typeCheckInnerRoot(parent, tc, childtc, goal, def);
            tc.updateRoot(n);
            break;
        case CURRENT_ROOT:
            n = this.typeCheckCurrentRoot(parent, tc, childtc, goal);
            tc.updateRoot(n);
            break;
        }

        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::override " + this + " -> " + this);

        return n;
    }

    protected Node typeCheckCurrentRoot(Node parent, TypeChecker tc, final TypeChecker childtc, Goal goal) throws SemanticException {
        FragmentRoot_c n = this;
        if (goal instanceof SignatureDef) {
            SignatureDef s = (SignatureDef) goal;
            final int key = s.key();
            n = (FragmentRoot_c) n.visitSignature(new NodeVisitor() {
                int k = 0;
                public Node override(Node parent, Node child) {
                    if (key == k++) {
                        return parent.visitChild(child, childtc);
                    }
                    return child;
                }
            });
        }
        else {
            // Create a dummy context visitor to visit the signature (most importantly the formals) to set up the context.
            ContextVisitor v = new ContextVisitor(tc.job(), tc.typeSystem(), tc.nodeFactory());
            v = v.context(childtc.context());
            n = (FragmentRoot_c) n.visitSignature(v);
            n = (FragmentRoot_c) n.typeCheckBody(parent, tc, childtc);
        }
        tc.updateRoot(n);
        return n;
    }

    protected Node typeCheckInnerRoot(Node parent, TypeChecker tc, TypeChecker childtc, Goal goal, Def def) throws SemanticException {
        FragmentRoot_c n = this;
        
        if (goal instanceof SupertypeDef) {
        }
        else if (goal instanceof SignatureDef) {
        }
        else {
            boolean result = runGoals(tc, pregoals(tc, def));
            if (! result)
                throw new SemanticException();

            // Look up the node again since running other passes may have updated it.
            n = (FragmentRoot_c) tc.getFragment(def).node();
            
            n = (FragmentRoot_c) tc.leave(parent, this, n, childtc);
            tc.updateRoot(n);

            result = runGoals(tc, postgoals(tc, def));
            if (! result)
                throw new SemanticException();

            n = (FragmentRoot_c) tc.getFragment(def).node();
        }
        return n;
    }

    public boolean runGoals(TypeChecker tc, List<Goal> goals) throws SemanticException {
        for (Goal g : goals) {
            if (! Globals.Scheduler().reached(g)) {
                try {
                    boolean result = Globals.Scheduler().attempt(g);
                    if (! result) return false;
                }
                catch (CyclicDependencyException e) {
                    throw new SemanticException();
                }
            }
        }
        return true;
    }

    public Mode mode(TypeChecker typeChecker) {
        ASTFragment currentFragment = typeChecker.currentFragment();
        ASTFragment fragment = typeChecker.getFragment(this);
        boolean isRoot = currentFragment != null && currentFragment == fragment;
    
        Mode mode;
        
        if (isRoot) {
            mode = Mode.CURRENT_ROOT;
        }
        else if (fragment != null) {
            mode = Mode.INNER_ROOT;
        }
        else {
            mode = Mode.NON_ROOT;
        }
    
        return mode;
    }

}
