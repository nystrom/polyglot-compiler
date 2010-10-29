package polyglot.types;

import polyglot.util.Position;

public class ConstructorInstance_c extends ProcedureInstance_c<ConstructorDef> implements ConstructorInstance {
    public ConstructorInstance_c(TypeSystem ts, Position pos, Ref<? extends ConstructorDef> def) {
        super(ts, pos, def);
    }

    public Type container() {
	return Types.get(def().container());
    }
    
    public Flags flags() {
	return def().flags();
    }
}
