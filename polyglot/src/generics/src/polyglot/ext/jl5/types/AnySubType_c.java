package polyglot.ext.jl5.types;

import polyglot.ast.TypeNode;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class AnySubType_c extends Wildcard_c implements AnySubType, SignatureType{

    public AnySubType_c(TypeSystem ts, ReferenceType bound){
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

    public ReferenceType lowerBound() {
        return null;
    }

    public ReferenceType upperBound() {
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
