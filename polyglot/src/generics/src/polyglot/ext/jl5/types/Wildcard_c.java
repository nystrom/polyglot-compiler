package polyglot.ext.jl5.types;

import polyglot.types.Type_c;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public abstract class Wildcard_c extends Type_c implements Wildcard {

    protected ReferenceType bound;
    
    public Wildcard_c() {
        super();
    }

    public Wildcard_c(TypeSystem ts, Position pos) {
        super(ts, pos);
    }

    public Wildcard_c(TypeSystem ts) {
        super(ts);
    }
    

    public ReferenceType bound() {
        return bound;
    }

    public void bound(ReferenceType bound) {
        this.bound = bound;
    }

    public final boolean isSubtypeImpl(Type t) {
        return false;
    }
    
}
