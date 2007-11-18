package polyglot.types;

import polyglot.util.Position;

public class ConstructorType_c extends ProcedureType_c<ConstructorDef> implements ConstructorType {
    public ConstructorType_c(TypeSystem ts, Position pos, Ref<ConstructorDef> def) {
        super(ts, pos, def);
    }
    
    public ReferenceType container() {
        return get(def().container());
    }
    
    public Flags flags() {
        return def().flags();
    }
}
