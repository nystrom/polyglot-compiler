package ibex.lr;

import ibex.lr.GLRRule.Assoc;

class Shift extends Action {
    State dest;
    GLRTerminal terminal;
    int prec;
    Assoc assoc;

    Shift(State dest, GLRTerminal terminal, int prec, Assoc assoc) {
        this.dest = dest;
        this.terminal = terminal;
        this.prec = prec;
        this.assoc = assoc;
    }

    public String toString() {
        return "shift " + terminal + " and goto " + dest;
    }

    int encode() {
        return Action.encode(Action.SHIFT, dest.index());
    }

    public boolean equals(Object o) {
        if (o instanceof Shift) {
            return dest.equals(((Shift) o).dest);
        }
        return false;
    }

    public int hashCode() {
        return dest.hashCode();
    }
}
