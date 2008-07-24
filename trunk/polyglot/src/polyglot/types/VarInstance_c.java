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
        }
        return constantValue;
    }

    public boolean isConstant() {
    	if (!constantValueSet && def.known()) {
            isConstant = def().isConstant();
            constantValue = def().constantValue();
        }
        return isConstant;
    }

    public VarInstance<T> constantValue(Object o) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.isConstant = true;
        v.constantValue = constantValue;
        return v;
    }

    public VarInstance<T> notConstant() {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.isConstant = false;
        v.constantValue = null;
        return v;
    }

    Flags flags;

    public Flags flags() {
        if (flags == null) {
            flags = def().flags();
        }
        return flags;
    }

    public VarInstance<T> flags(Flags flags) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.flags = flags;
        return v;
    }

    String name;

    public String name() {
        if (name == null) {
            name = def().name();
        }
        return name;
    }

    public VarInstance<T> name(String name) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.name = name;
        return v;
    }

    Type type;

    public Type type() {
        if (type == null) {
            type = Types.get(def().type());
            assert !( type instanceof UnknownType);
        }
        return type;
    }

    public VarInstance<T> type(Type type) {
        VarInstance_c<T> v = (VarInstance_c<T>) copy();
        v.type = type;
        return v;
    }
}
