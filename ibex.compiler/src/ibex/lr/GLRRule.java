package ibex.lr;

import ibex.types.Nonterminal;
import ibex.types.RSeq;

import java.util.Iterator;
import java.util.List;

public abstract class GLRRule {

    protected Nonterminal nonterm;
    protected RSeq nontermRhs;
    protected GLRNonterminal lhs;
    protected int index;
    protected Kind kind;

    protected enum Kind {
        NORMAL, POS_LOOKAHEAD, NEG_LOOKAHEAD, AND, SUB
    }
    
    abstract List<GLRSymbol> symbols();

    public GLRRule(Nonterminal nonterm, RSeq nontermRhs, GLRNonterminal lhs, int index, Kind kind) {
        this.nonterm = nonterm;
        this.nontermRhs = nontermRhs;
        this.lhs = lhs;
        this.index = index;
        this.kind = kind;
    }
    
    abstract GLRRule copy();

    public GLRRule() {
        super();
    }

    protected GLRNonterminal lhs() {
        return lhs;
    }

    protected int index() {
        return index;
    }

    Action action() {
        return null;
    }

    public boolean equals(Object o) {
        if (o instanceof GLRNormalRule) {
            GLRNormalRule r = (GLRNormalRule) o;
            return index == r.index;
        }
        return false;
    }

    public int hashCode() {
        return index;
    }


    public Kind kind() {
        return kind;
    }

}