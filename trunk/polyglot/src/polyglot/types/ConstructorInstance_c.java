package polyglot.types;

import polyglot.util.Position;

public class ConstructorInstance_c extends ProcedureInstance_c<ConstructorDef> implements ConstructorInstance {
    public ConstructorInstance_c(TypeSystem ts, Position pos, Ref<ConstructorDef> def) {
        super(ts, pos, def);
    }
    
    public ReferenceType container() {
        return get(def().container());
    }
    
    public Flags flags() {
        return def().flags();
    }
}
