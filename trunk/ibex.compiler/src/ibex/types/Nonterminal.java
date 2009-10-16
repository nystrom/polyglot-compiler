package ibex.types;

import polyglot.types.Name;


/** This class represents a nonterminal and the rhs choices. */
public interface Nonterminal extends Symbol {
    RuleInstance rule();
    Name name();
}
