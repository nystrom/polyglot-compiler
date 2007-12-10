package polyglot.visit;

import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.Def;

public class FragmentAssembler extends NodeVisitor {
    protected Job job;

    public FragmentAssembler(Job job) {
        super();
        this.job = job;
    }

    public Job job() { return job; }

    public void finish(Node ast) {
        // ### Do NOT clear fragment map; it's used to compute the prereqs for this goal.
//        job.setFragmentMap(null);
    }

    public ASTFragment getFragment(Def def) {
        if (def == null) return null;
        Job job = Globals.currentJob();
        return job.fragmentMap().get(def);
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
    
    public Node override(Node parent, Node n) {
        if (n instanceof FragmentRoot) {
            ASTFragment f = getFragment(n);
            if (f != null) {
                Node m = f.node();
                n = m;
            }
        }
        
        return n.visitChildren(this);
    }
}
