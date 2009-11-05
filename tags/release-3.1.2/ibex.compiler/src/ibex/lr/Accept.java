package ibex.lr;

class Accept extends Action {
    GLRNonterminal nonterminal;

    Accept(GLRNonterminal nonterminal) {
        this.nonterminal = nonterminal;
    }

    public String toString() {
        return "accept " + nonterminal;
    }

    int encode() {
        return Action.encode(Action.ACCEPT, nonterminal.index());
    }

    public boolean equals(Object o) {
        if (o instanceof Accept) {
            return nonterminal.equals(((Accept) o).nonterminal);
        }
        return false;
    }

    public int hashCode() {
        return nonterminal.hashCode();
    }
}
