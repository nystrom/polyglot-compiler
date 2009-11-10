package ibex.types;

import polyglot.types.*;

import java.util.List;

/** This class represents a nonterminal and the rhs choices. */
public interface RuleDef extends MemberDef, CodeDef, Def {
    Nonterminal asNonterminal();
    RuleInstance asInstance();

    Name name();
    void setName(Name name);

    List<Ref<? extends Type>> throwTypes();
    void setThrowTypes(List<Ref<? extends Type>> throwTypes);

    Ref<? extends Type> type();
    void setType(Ref<? extends Type> type);

    List<Rhs> choices();
    void setChoices(List<Rhs> choices);

    boolean isRegular();
    void setRegular(boolean isRegular);
}
