package ibex.runtime;

class EOF implements Terminal {
    private int sym;
    EOF(int sym) { this.sym = sym; }
    public int symbol() { return sym; }
    public String toString() { return "<EOF>"; }
}
