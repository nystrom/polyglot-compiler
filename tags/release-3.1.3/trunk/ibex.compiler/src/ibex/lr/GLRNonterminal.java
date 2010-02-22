package ibex.lr;

import ibex.types.Nonterminal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.types.Name;

class GLRNonterminal extends GLRSymbol {
    List<GLRRule> rules;
    List<GLRMerge> merges;
    Set<GLRTerminal> first;
    Set<GLRTerminal> follow;
    boolean nullable;
    Kind kind;
    
    enum Kind { NORMAL, POS, NEG }

    public GLRNonterminal(Name name, int index) {
        this(Kind.NORMAL, name, index);
    }
    GLRNonterminal(Kind kind, Name name, int index) {
        super(name, index);
        this.kind = kind;
        this.rules = new ArrayList<GLRRule>();
        this.merges = new ArrayList<GLRMerge>();
        this.first = new HashSet<GLRTerminal>();
        this.follow = new HashSet<GLRTerminal>();
        this.nullable = false;
    }

    GLRNonterminal copy() {
        return new GLRNonterminal(kind, name, index);
    }

    List<GLRRule> rules() { return rules; }
    List<GLRMerge> merges() { return merges; }

    Set<GLRTerminal> first() { return first; }
    Set<GLRTerminal> follow() { return follow; }
    boolean isNullable() { return nullable; }
    void setNullable(boolean f) { nullable = f; }

    public boolean equals(Object o) {
        return o instanceof GLRNonterminal && super.equals(o);
    }
    
    @Override
    public String toString() {
        if (kind == Kind.POS)
            return "+" + super.toString();
        if (kind == Kind.NEG)
            return "-" + super.toString();
        return super.toString();
    }
}
