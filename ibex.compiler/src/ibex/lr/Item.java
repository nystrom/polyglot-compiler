package ibex.lr;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class Item {
    GLRNormalRule rule;
    int dot;
    Set<GLRTerminal> lookahead;

    Item(GLRNormalRule rule, int dot, Set<GLRTerminal> lookahead) {
        this.rule = rule;
        this.dot = dot;
        if (lookahead == null || lookahead.isEmpty())
            this.lookahead = null;
        else
            this.lookahead = new TreeSet<GLRTerminal>(lookahead);
    }

    GLRSymbol afterDot() {
        if (dot >= rule.rhs().size()) return null;
        return (GLRSymbol) rule.rhs().get(dot);
    }

    Item shiftDot() {
        return new Item(rule, dot+1, lookahead);
    }

    public boolean equals(Object o) {
        if (o instanceof Item) {
            Item i = (Item) o;

            if (LRConstruction.LALR) {
                // LALR(1) does not consider the lookahead
                return rule.equals(i.rule) && dot == i.dot;
            }
            else {
                // LR(1) does consider the lookahead
                return rule.equals(i.rule) && dot == i.dot
                    && equals(lookahead, i.lookahead);
            }
        }

        return false;
    }

    private boolean equals(Set<GLRTerminal> lookahead2, Set<GLRTerminal> lookahead3) {
        if (lookahead2 == lookahead3)
            return true;
        if (lookahead2 == null)
            return false;
        if (lookahead3 == null)
            return false;
        return lookahead2.equals(lookahead3);
    }

    public int hashCode() {
        // DO NOT include the lookahead in the hash code; since we
        // modify the lookahead in place when computing the closure of a
        // set of items, the hash code could change.
        //
        // Also, when building the LALR(1) state vs.  LR(1), two items
        // should hash to the same location even if they have different
        // lookahead sets.
        return rule.hashCode() + dot;
    }

    public String toString() {
        String s = "<" + rule.index + ": " + rule.lhs + " ::=";
        int index = 0;
        for (Iterator<GLRSymbol> i = rule.rhs.iterator(); i.hasNext(); ) {
            GLRSymbol y = (GLRSymbol) i.next();
            if (index == dot) s += " .";
            index++;
            s += " " + y;
        }
        if (index == dot) s += " .";
        return s + ", " + (lookahead == null ? "{}" : lookahead) + ">";
    }

    public boolean addLookaheadAll(Set<GLRTerminal> first) {
        if (! LRConstruction.LALR)
            return false;
        
        if (first == null)
            return false;
        if (first.isEmpty())
            return false;
        
        if (lookahead == null)
            lookahead = new TreeSet<GLRTerminal>();
        return lookahead.addAll(first);
    }
}
