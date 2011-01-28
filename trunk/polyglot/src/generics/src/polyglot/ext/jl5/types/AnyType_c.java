package polyglot.ext.jl5.types;

import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class AnyType_c extends Wildcard_c implements AnyType, SignatureType{

    
    public AnyType_c(TypeSystem ts){
        super(ts);
    }

    public String translate(Resolver c){
        return "?";
    }

    public String toString(){
        return "?";
    }

    public ReferenceType upperBound(){
        return null;
    }
    

    public String signature(){
        return "*";
    }

    public ReferenceType lowerBound() {
        return null;
    }

    @Override
    public boolean equalsImpl(TypeObject t) {
        return (t instanceof AnyType);
    }
    
    
}
