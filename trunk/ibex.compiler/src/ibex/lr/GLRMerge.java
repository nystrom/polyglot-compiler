package ibex.lr;


import java.util.ArrayList;
import java.util.List;

class GLRMerge {
    GLRRule left;
    GLRRule right;
    protected GLRNonterminal lhs;
    protected int index;
    protected Kind kind;

    protected enum Kind {
        AND, SUB
    }

    GLRMerge(GLRNonterminal lhs, GLRRule left, GLRRule right, int index, Kind kind) {
        this.lhs = lhs;
        this.index = index;
        this.kind = kind;
        this.left = left;
        this.right = right;
    }

    List<GLRSymbol> symbols() {
        List<GLRSymbol> l = new ArrayList<GLRSymbol>(left.rhs().size() + right.rhs().size());
        l.addAll(left.rhs());
        l.addAll(right.rhs());
        return l;
    }

    GLRMerge copy() {
        return new GLRMerge(lhs, left, right, index, kind);
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

    public GLRRule left() {
        return left;
    }
    public GLRRule right() {
        return right;
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
        if (o instanceof GLRRule) {
            GLRRule r = (GLRRule) o;
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
