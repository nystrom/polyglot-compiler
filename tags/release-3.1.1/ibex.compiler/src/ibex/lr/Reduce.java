package ibex.lr;

class Reduce extends Action {
    GLRNormalRule rule;

    Reduce(GLRNormalRule rule) {
        this.rule = rule;
    }

    public String toString() {
        return "reduce with " + rule;
    }

    int encode() {
        return Action.encode(Action.REDUCE, rule.index());
    }

    public boolean equals(Object o) {
        if (o instanceof Reduce) {
            return rule.equals(((Reduce) o).rule);
        }
        return false;
    }

    public int hashCode() {
        return rule.hashCode();
    }
}
