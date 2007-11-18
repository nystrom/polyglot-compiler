package polyglot.types;

import polyglot.util.Position;

public class FunctionType_c<T extends FunctionDef> extends ProcedureType_c<T> implements FunctionType<T> {
    public FunctionType_c(TypeSystem ts, Position pos, Ref<T> def) {
        super(ts, pos, def);
    }

    public Type returnType() {
        return def().returnType().get();
    }
}
