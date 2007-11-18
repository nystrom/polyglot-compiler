package polyglot.types;

import polyglot.util.Position;

public class LocalType_c extends VarType_c<LocalDef> implements LocalType {
    public LocalType_c(TypeSystem ts, Position pos, Ref<LocalDef> def) {
        super(ts, pos, def);
    }
}
