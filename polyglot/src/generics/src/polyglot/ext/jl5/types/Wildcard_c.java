package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import polyglot.types.Type_c;
import polyglot.util.Position;

public abstract class Wildcard_c extends Type_c implements Wildcard {

    protected Ref<ClassType> bound;
    
    public Wildcard_c() {
        super();
    }

    public Wildcard_c(TypeSystem ts, Position pos) {
        super(ts, pos);
    }

    public Wildcard_c(TypeSystem ts) {
        super(ts);
    }
    
    public ClassType bound() {
        return (bound != null) ? bound.get() : null;
    }

    public Ref<ClassType> boundRef() {
        return bound;
    }
    
    public void bound(Ref<ClassType> bound) {
        this.bound = bound;
    }

    
}
