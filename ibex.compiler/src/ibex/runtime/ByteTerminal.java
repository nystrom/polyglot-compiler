package ibex.runtime;

class ByteTerminal implements Terminal {
    int sym;
    byte val;
    ByteTerminal(int sym, byte val) { this.sym = sym; this.val = val; }
    public int symbol() { return sym; }
    public String toString() { return String.valueOf((int) val); }
}
