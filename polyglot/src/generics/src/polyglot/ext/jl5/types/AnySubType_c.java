package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class AnySubType_c extends Wildcard_c implements AnySubType, SignatureType{

    public AnySubType_c(TypeSystem ts, ClassType bound){
        super(ts);
        bound(bound);
    }
    
    public String translate(Resolver c){
        return "? extends "+bound.translate(c);
    }

    public String toString(){
        return "? extends "+bound.toString();
    }

    public String signature(){
        return "+"+((SignatureType)bound).signature();
    }

    public ClassType lowerBound() {
        return null;
    }

    public ClassType upperBound() {
        return bound();
    }

    @Override
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof AnySubType) {
            AnySubType other = (AnySubType) t;
            return ts.equals((TypeObject)bound(), (TypeObject)other.bound());
        }
        return false;
    }

    
}
