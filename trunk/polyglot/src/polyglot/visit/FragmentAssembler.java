package polyglot.visit;

import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.Def;

public class FragmentAssembler extends NodeVisitor {
    protected Job job;

    public FragmentAssembler(Job job) {
        super();
        this.job = job;
    }

    public Job job() { return job; }

    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        Map<Def, ASTFragment> fragmentMap = job.fragmentMap();
        
        if (n instanceof FragmentRoot) {
            FragmentRoot r = (FragmentRoot) n;
            
            for (Def def : r.defs()) {
                return fragmentMap.get(def).node();
            }
        }

        return n;
    }

}
