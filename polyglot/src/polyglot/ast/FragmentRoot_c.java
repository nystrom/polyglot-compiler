package polyglot.ast;

import java.util.*;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.main.Report;
import polyglot.types.Def;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.*;
import polyglot.visit.TypeChecker.Mode;
import polyglot.visit.TypeChecker.Scope;

public abstract class FragmentRoot_c extends Term_c implements FragmentRoot {

    public FragmentRoot_c(Position pos) {
        super(pos);
    }
    
    public abstract List<Def> defs();

    public Node visitSignature(NodeVisitor v) throws SemanticException {
        return this;
    }
    public Node typeCheckSupersOfRootFromInside(Node parent, TypeChecker tc, TypeChecker childtc) throws SemanticException {
        return this;
    }
    public Node typeCheckSigsOfRootFromInside(Node parent, TypeChecker tc, TypeChecker childtc) throws SemanticException {
        return visitSignature(childtc);
    }
    public Node typeCheckRootFromInside(Node parent, TypeChecker tc, TypeChecker childtc) throws SemanticException {
        return this;
    }
    
    public List<Goal> pregoals(TypeChecker tc, Def def) {
            List<Goal> goals = Arrays.asList(new Goal[] {
                                                         Globals.Scheduler().SupertypeDef(tc.job(), def),
                                                         Globals.Scheduler().SignatureDef(tc.job(), def),
                                                         Globals.Scheduler().TypeCheckDef(tc.job(), def)
            });
            return goals;
    }
    public List<Goal> postgoals(TypeChecker tc, Def def) {
        return Collections.<Goal>emptyList();
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

        FragmentRoot_c n = this;

        Scope scope = tc.scope();
        Mode mode = tc.mode(n);
        
        Def def = this.defs().iterator().next();

        switch (mode) {
        case NON_ROOT:
            assert false;
            return this;
        case INNER_ROOT:
            switch (scope) {
            case SUPER:
                assert false;
                break;
            case SIGNATURES:
                assert false;
                break;
            case BODY:
                boolean result = runGoals(tc, pregoals(tc, def));
                n = (FragmentRoot_c) tc.getFragment(def).node();
                
                if (result) {
                    n = (FragmentRoot_c) tc.leave(parent, this, n, childtc);
                    tc.updateRoot(n);

                    result = runGoals(tc, postgoals(tc, def));
                    n = (FragmentRoot_c) tc.getFragment(def).node();
                    if (! result)
                        throw new SemanticException();
                }
                break;
            }
            break;
        case CURRENT_ROOT:
            switch (scope) {
            case SUPER:
                n = (FragmentRoot_c) n.typeCheckSupersOfRootFromInside(parent, tc, childtc);
                break;
            case SIGNATURES:
                n = (FragmentRoot_c) n.typeCheckSigsOfRootFromInside(parent, tc, childtc);
                break;
            case BODY:
                // Create a dummy context visitor to visit the signature (most importantly the formals) to set up the context.
                ContextVisitor v = new ContextVisitor(tc.job(), tc.typeSystem(), tc.nodeFactory());
                v = v.context(childtc.context());
                n.visitSignature(v);
                
                n = (FragmentRoot_c) n.typeCheckRootFromInside(parent, tc, childtc);
                break;
            }
            tc.updateRoot(n);
            break;
        }

        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::override " + this + " -> " + n);

        return n;
    }

    public boolean runGoals(TypeChecker tc, List<Goal> goals) throws SemanticException {
        for (Goal g : goals) {
            if (! Globals.Scheduler().reached(g)) {
                boolean result = Globals.Scheduler().attempt(g);
                if (! result) return false;
            }
        }
        return true;
    }

}
