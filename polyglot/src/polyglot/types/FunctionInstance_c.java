package polyglot.types;

import polyglot.util.Position;

public class FunctionInstance_c<T extends FunctionDef> extends ProcedureInstance_c<T> implements FunctionInstance<T> {
    public FunctionInstance_c(TypeSystem ts, Position pos, Ref<? extends T> def) {
        super(ts, pos, def);
    }
    
    public Type returnType() {
	return def().returnType().get();
    }

    public Ref<? extends Type> returnTypeRef() {
	return def().returnType();
    }
}
