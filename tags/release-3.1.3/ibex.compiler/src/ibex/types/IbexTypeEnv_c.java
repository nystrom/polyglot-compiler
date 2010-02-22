package ibex.types;

import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeEnv;
import polyglot.types.TypeEnv_c;

public class IbexTypeEnv_c extends TypeEnv_c implements TypeEnv {
    public IbexTypeEnv_c(Context context) {
        super(context);
    }

//    public boolean isCastValid(Type fromType, Type toType) {
//        return super.isCastValid(fromType, toType) || fromType instanceof TupleType && toType.isClass() || fromType.isClass() && toType instanceof TupleType;
//    }
    public boolean isSubtype(Type type1, Type type2) {
        return super.isSubtype(type1, type2) || type1 instanceof TupleType && (typeEquals(type1, type2)) || typeEquals(type2, ts.Object());
    }
    public boolean typeEquals(Type type1, Type type2) {
        if (type1 instanceof TupleType && type2 instanceof TupleType) {
            TupleType t1 = (TupleType) type1;
            TupleType t2 = (TupleType) type2;
            if (t1.elementTypes().size() == t2.elementTypes().size()) {
                for (int i = 0; i < t1.elementTypes().size(); i++) {
                    Type e1 = t1.elementTypes().get(i);
                    Type e2 = t2.elementTypes().get(i);
                    if (! typeEquals(e1, e2))
                        return false;
                }
                return true;
            }
            return false;
        }
        return super.typeEquals(type1, type2);
    }
    
    @Override
    public Type leastCommonAncestor(Type type1, Type type2) throws SemanticException {
        if (type1 instanceof TupleType && type2 instanceof TupleType) {
            if (typeEquals(type1, type2))
                return type1;
        }
        if (type1.isReference() && type2 instanceof TupleType)
            return ts.Object();
        if (type2.isReference() && type1 instanceof TupleType)
            return ts.Object();
        return super.leastCommonAncestor(type1, type2);
    }

}
