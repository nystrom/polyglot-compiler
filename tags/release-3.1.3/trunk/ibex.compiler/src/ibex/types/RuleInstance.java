package ibex.types;

import java.util.List;

import polyglot.types.CodeInstance;
import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.StructType;
import polyglot.types.Type;

public interface RuleInstance extends MemberInstance<RuleDef>, CodeInstance<RuleDef> {

    RuleInstance container(StructType container);

    StructType container();

    RuleInstance flags(Flags flags);

    Flags flags();

    RuleInstance name(Name name);

    Name name();

    RuleInstance type(Type type);

    RuleInstance typeRef(Ref<? extends Type> type);

    Type type();

    Ref<? extends Type> typeRef();
    
    public List<Type> throwTypes();

    boolean isRegular();
}
