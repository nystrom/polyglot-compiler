package ibex.lr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ibex.types.Nonterminal;
import ibex.types.RSeq;

class GLRMergeRule extends GLRRule {
    GLRNormalRule left;
    GLRNormalRule right;

    GLRMergeRule(Nonterminal nonterm, RSeq nontermRhs, GLRNonterminal lhs, GLRNormalRule left, GLRNormalRule right, int index, Kind kind) {
        super(nonterm, nontermRhs, lhs, index, kind);
        this.left = left;
        this.right = right;
    }

    List<GLRSymbol> symbols() {
        List<GLRSymbol> l = new ArrayList<GLRSymbol>(left.rhs().size() + right.rhs().size());
        l.addAll(left.rhs());
        l.addAll(right.rhs());
        return l;
    }
    
    GLRMergeRule copy() {
        return new GLRMergeRule(nonterm, nontermRhs, lhs, left, right, index, kind);
    }
    
    public int encode(int i) {
        if (i == 0) {
            if (kind == kind.AND)
                return (right.lhs.index() << 3) | 3;
            if (kind == kind.SUB)
                return (right.lhs.index() << 3) | 2;
        }
        else {
            if (kind == kind.AND)
                return (left.lhs.index() << 3) | 3;
            if (kind == kind.SUB)
                return (left.lhs.index() << 3) | 1;
        }
        return 0;
    }

    public String toString() {
        String s = index + ": " + lhs + " ::=";
        s += " " + left;
        s += " " + right;
        return s;
    }

    public GLRNormalRule left() {
        return left;
    }
    public GLRNormalRule right() {
        return right;
    }
}
