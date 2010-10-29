package polyglot.types;

import polyglot.util.Position;

public class LocalInstance_c extends VarInstance_c<LocalDef> implements LocalInstance {
    public LocalInstance_c(TypeSystem ts, Position pos, Ref<? extends LocalDef> def) {
        super(ts, pos, def);
    }
}
