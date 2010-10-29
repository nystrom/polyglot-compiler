package polyglot.types;

import polyglot.util.Position;

public class VarInstance_c<T extends VarDef> extends Use_c<T> implements VarInstance<T> {
    public VarInstance_c(TypeSystem ts, Position pos, Ref<? extends T> def) {
        super(ts, pos, def);
    }

    public Object constantValue() {
	return def().constantValue();
    }

    public boolean isConstant() {
	return def().isConstant();
    }

    public Flags flags() {
	return def().flags();
    }

    public Name name() {
	return def().name();
    }

    public Type type() {
	return Types.get(def().type());
    }
}
