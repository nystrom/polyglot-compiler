package polyglot.types;

import polyglot.util.Position;

public class FieldType_c extends VarType_c<FieldDef> implements FieldType {
    public FieldType_c(TypeSystem ts, Position pos, Ref<FieldDef> def) {
        super(ts, pos, def);
    }
    
    public ReferenceType container() {
        return get(def().container());
    }
   
}
