package ibex.types;

import polyglot.types.*;
import java.util.List;

public interface IbexClassType extends ClassType {
    List<RuleInstance> rules();
    boolean isParser();
    List<Nonterminal> allNonterminals();
    List<Terminal> allTerminals();
}
