package ibex.runtime;

class ByteTerminal implements Terminal {
    byte sym;
    ByteTerminal(byte sym) { this.sym = sym; }
    public int symbol() { return sym; }
    public String toString() { return String.valueOf((int) sym); }
}
