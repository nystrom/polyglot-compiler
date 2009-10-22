package ibex.lr;

import ibex.types.*;
import polyglot.util.*;
import java.util.*;

class GLRNormalRule extends GLRRule {
    List<GLRSymbol> rhs;
    
    GLRNormalRule(Nonterminal nonterm, RSeq nontermRhs, GLRNonterminal lhs, List<GLRSymbol> rhs, int index) {
        this(nonterm, nontermRhs, lhs, rhs, index, Kind.NORMAL);
    }

    GLRNormalRule(Nonterminal nonterm, RSeq nontermRhs, GLRNonterminal lhs, List<GLRSymbol> rhs, int index, Kind kind) {
        super(nonterm, nontermRhs, lhs, index, kind);
        this.rhs = rhs;
    }
    
    List<GLRSymbol> symbols() {
        return rhs();
    }
    
    protected List<GLRSymbol> rhs() {
        return rhs;
    }

    GLRNormalRule copy() {
        return new GLRNormalRule(nonterm, nontermRhs, lhs, rhs, index, kind);
    }
    
    public int encode() {
        return (lhs.index() << 8) | rhs.size();
    }

    public String toString() {
        String s = index + ": " + lhs + " ::=";
        for (Iterator<GLRSymbol> i = rhs.iterator(); i.hasNext();) {
            GLRSymbol y = (GLRSymbol) i.next();
            s += " " + y;
        }
        return s;
    }
}
