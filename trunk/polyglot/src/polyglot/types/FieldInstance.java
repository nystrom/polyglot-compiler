package polyglot.types;

import java.util.List;

public interface FieldInstance extends VarInstance<FieldDef>, MemberInstance<FieldDef> {
    FieldInstance container(ReferenceType container);
    FieldInstance flags(Flags flags);
    FieldInstance name(String name);
    FieldInstance type(Type type);
    FieldInstance constantValue(Object o);
    FieldInstance notConstant();
}
