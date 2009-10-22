package ibex.lr;

import ibex.types.Nonterminal;
import ibex.types.RSeq;

import java.util.ArrayList;
import java.util.List;

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

    public int encodeLeft() {
        if (kind == kind.AND)
            return (right.index() << 3) | 3;
        if (kind == kind.SUB)
            return (right.index() << 3) | 2;
        return 0;
    }

    public int encodeRight() {
        if (kind == kind.AND)
            return (left.index() << 3) | 3;
        if (kind == kind.SUB)
            return (left.index() << 3) | 1;
        return 0;
    }

    public String toString() {
        String s = index + ":";
        s += " (" + left + ")";
        if (kind == Kind.AND)
            s += " &";
        else
            s += " -";
        s += " (" + right + ")";
        return s;
    }

    public GLRNormalRule left() {
        return left;
    }
    public GLRNormalRule right() {
        return right;
    }
}
