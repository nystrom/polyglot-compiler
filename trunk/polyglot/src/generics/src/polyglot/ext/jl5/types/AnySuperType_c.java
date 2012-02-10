package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class AnySuperType_c extends Wildcard_c implements AnySuperType, SignatureType{

    public AnySuperType_c(TypeSystem ts, Ref<ClassType> bound){
        super(ts);
        bound(bound);
    }

    public Ref<ClassType> upperBound(){
        return null;
    }

    public String translate(Resolver c){
        return "? super "+bound.get().translate(c);
    }

    public String toString(){
        return "? super "+bound.toString();
    }
    

    public String signature(){
        return "-"+((SignatureType)bound).signature();
    }


    public Ref<ClassType> lowerBound() {
        return boundRef();
    }

    @Override
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof AnySuperType) {
            AnySuperType other = (AnySuperType) t;
            return ts.equals((TypeObject)bound(), (TypeObject)other.bound());
        }
        return false;
    }
}
