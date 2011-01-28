package polyglot.ext.jl5.visit;

import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;
import polyglot.visit.AmbiguityRemover;

public class JL5AmbiguityRemover extends AmbiguityRemover {

    public static class JL5Kind extends Kind {
        protected JL5Kind(String name){
            super(name);
        }
    }
    
    public static final Kind TYPE_VARS = new JL5Kind("disamb-type-vars");

    public JL5AmbiguityRemover(Job job, TypeSystem ts, NodeFactory nf, Kind kind){
        super(job, ts, nf, kind);
    }
}
