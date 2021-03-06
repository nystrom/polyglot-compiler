package polyglot.types;

import polyglot.util.Position;

public class VarInstance_c<T extends VarDef> extends Use_c<T> implements VarInstance<T> {
    public VarInstance_c(TypeSystem ts, Position pos, Ref<? extends T> def) {
        super(ts, pos, def);
    }

    boolean constantValueSet;
    boolean isConstant;
    Object constantValue;

    public Object constantValue() {
        if (!constantValueSet && def.known()) {
            isConstant = def().isConstant();
            constantValue = def().constantValue();
            constantValueSet = true;
        }
        return constantValue;
    }

    public boolean isConstant() {
    	if (!constantValueSet && def.known()) {
            isConstant = def().isConstant();
            constantValue = def().constantValue();
            constantValueSet = true;
        }
        return isConstant;
    }

    public VarInstance<T> constantValue(Object o) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.constantValueSet = true;
        v.isConstant = true;
        v.constantValue = o;
        return v;
    }

    public VarInstance<T> notConstant() {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.constantValueSet = true;
        v.isConstant = false;
        v.constantValue = null;
        return v;
    }

    Flags flags;

    public Flags flags() {
        if (flags == null) {
            return def().flags();
        }
        return flags;
    }

    public VarInstance<T> flags(Flags flags) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.flags = flags;
        return v;
    }

    Name name;

    public Name name() {
        if (name == null) {
            return def().name();
        }
        return name;
    }

    public VarInstance<T> name(Name name) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.name = name;
        return v;
    }

    protected Type type;

    public Type type() {
        if (type == null) {
            return Types.get(def().type());
        }
        return type;
    }

    public VarInstance<T> type(Type type) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.type = type;
        return v;
    }
}
