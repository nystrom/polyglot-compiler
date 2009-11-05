package ibex.lr;

import ibex.types.Nonterminal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.types.Name;

class GLRNonterminal extends GLRSymbol {
    Nonterminal nonterm;
    List<GLRNormalRule> rules;
    List<GLRMergeRule> merges;
    Set<GLRTerminal> first;
    Set<GLRTerminal> follow;
    boolean nullable;

    GLRNonterminal(Nonterminal nonterm, Name name, int index) {
        super(name, index);
        this.nonterm = nonterm;
        this.rules = new ArrayList<GLRNormalRule>();
        this.merges = new ArrayList<GLRMergeRule>();
        this.first = new HashSet<GLRTerminal>();
        this.follow = new HashSet<GLRTerminal>();
        this.nullable = false;
    }

    GLRNonterminal copy() {
        return new GLRNonterminal(nonterm, name, index);
    }

    List<GLRNormalRule> rules() { return rules; }
    List<GLRMergeRule> merges() { return merges; }

    Set<GLRTerminal> first() { return first; }
    Set<GLRTerminal> follow() { return follow; }
    boolean isNullable() { return nullable; }
    void setNullable(boolean f) { nullable = f; }

    public boolean equals(Object o) {
        return o instanceof GLRNonterminal && super.equals(o);
    }
}
