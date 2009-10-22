package ibex.lr;

class Shift extends Action {
    State dest;
    GLRTerminal terminal;

    Shift(State dest, GLRTerminal terminal) {
        this.dest = dest;
        this.terminal = terminal;
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
