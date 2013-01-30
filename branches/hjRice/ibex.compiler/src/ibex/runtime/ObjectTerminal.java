package ibex.runtime;

class ObjectTerminal implements Terminal {
    Object sym;
    ObjectTerminal(Object sym) { this.sym = sym; }
    public int symbol() { return sym.hashCode(); }
    public String toString() { return String.valueOf(sym); }
}
