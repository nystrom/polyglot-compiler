package polyglot.types;

import polyglot.util.Position;

public class VarInstance_c<T extends VarDef> extends Use_c<T> implements VarType<T> {
    public VarInstance_c(TypeSystem ts, Position pos, Ref<T> def) {
        super(ts, pos, def);
    }
    
    public Object constantValue() {
        return def().constantValue();
    }

    public boolean constantValueSet() {
        return def().constantValueSet();
    }

    public boolean isConstant() {
        return def().isConstant();
    }

    public Flags flags() {
        return def().flags();
    }

    public String name() {
        return def().name();
    }

    public Type type() {
        return get(def().type());
    }
}
