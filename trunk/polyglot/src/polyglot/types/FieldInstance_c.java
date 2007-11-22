package polyglot.types;

import polyglot.util.Position;

public class FieldInstance_c extends VarInstance_c<FieldDef> implements FieldInstance {
    public FieldInstance_c(TypeSystem ts, Position pos, Ref<FieldDef> def) {
        super(ts, pos, def);
    }
    
    public ReferenceType container() {
        return Types.get(def().container());
    }
}
