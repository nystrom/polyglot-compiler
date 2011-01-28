package polyglot.ext.jl5.types;

import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class AnySuperType_c extends Wildcard_c implements AnySuperType, SignatureType{

    public AnySuperType_c(TypeSystem ts, ReferenceType bound){
        super(ts);
        bound(bound);
    }

    
    public ReferenceType upperBound(){
        return null;
    }

    public String translate(Resolver c){
        return "? super "+bound.translate(c);
    }

    public String toString(){
        return "? super "+bound.toString();
    }
    

    public String signature(){
        return "-"+((SignatureType)bound).signature();
    }


    public ReferenceType lowerBound() {
        return bound();
    }


    @Override
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof AnySuperType) {
            AnySuperType other = (AnySuperType) t;
            return ts.equals(bound(), other.bound());
        }
        return false;
    }
    
    
}
