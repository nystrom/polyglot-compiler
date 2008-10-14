package polyglot.types;

import java.util.List;

import polyglot.util.Position;

public class FunctionInstance_c<T extends FunctionDef> extends ProcedureInstance_c<T> implements FunctionInstance<T> {
    public FunctionInstance_c(TypeSystem ts, Position pos, Ref<? extends T> def) {
        super(ts, pos, def);
    }
    
    protected Type returnType;

    public FunctionInstance<T> returnType(Type returnType) {
        FunctionInstance_c<T> p = (FunctionInstance_c<T>) copy();
        p.returnType = returnType;
        return p;
    }
    
    public Type returnType() {
        if (returnType == null) {
            Type t = def().returnType().get();
//            if (t instanceof UnknownType) {
//        	assert false;
//        	return t;
//            }
            returnType = t;
        }
        return returnType;
    }

    public FunctionInstance<T> formalTypes(List<Type> formalTypes) {
        return (FunctionInstance<T>) super.formalTypes(formalTypes);
    }
    
    public FunctionInstance<T> throwTypes(List<Type> throwTypes) {
        return (FunctionInstance<T>) super.throwTypes(throwTypes);
    }

}
