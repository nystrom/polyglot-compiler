package polyglot.types;

import polyglot.util.Position;

public class FieldInstance_c extends VarInstance_c<FieldDef> implements FieldInstance {
    public FieldInstance_c(TypeSystem ts, Position pos, Ref<? extends FieldDef> def) {
        super(ts, pos, def);
    }

    ReferenceType container;

    public ReferenceType container() {
        if (container == null) {
            container = Types.get(def().container());
        }
        return container;
    }

    public FieldInstance container(ReferenceType container) {
        FieldInstance_c v = (FieldInstance_c) copy();
        v.container = container;
        return v;
    }

    public FieldInstance flags(Flags flags) {
        return (FieldInstance) super.flags(flags);
    }

    public FieldInstance name(String name) {
        return (FieldInstance) super.name(name);
    }

    public FieldInstance type(Type type) {
        return (FieldInstance) super.type(type);
    }

    public FieldInstance constantValue(Object o) {
        return (FieldInstance) super.constantValue(o);
    }

    public FieldInstance notConstant() {
        return (FieldInstance) super.notConstant();
    }
}
