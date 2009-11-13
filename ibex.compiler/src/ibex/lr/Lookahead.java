package ibex.lr;

class Lookahead extends Action {
    GLRRule rule;
    boolean neg;

    Lookahead(GLRRule rule, boolean terminal) {
        this.rule = rule;
        this.neg = terminal;
    }

    public String toString() {
        return (neg ? "neg" : "pos") + " lookahead on " + rule;
    }

    int encode() {
        if (neg)
            return Action.encode(Action.POS_LOOKAHEAD, rule.index());
        else
            return Action.encode(Action.NEG_LOOKAHEAD, rule.index());
    }

    public boolean equals(Object o) {
        if (o instanceof Lookahead) {
            return rule.equals(((Lookahead) o).rule) && neg == ((Lookahead) o).neg;
        }
        return false;
    }

    public int hashCode() {
        return rule.hashCode();
    }
}
