package ibex.runtime;

class ErrorTerminal implements Terminal {
    int sym;
    String str;
    ErrorTerminal(int sym, String str) { this.sym = sym; this.str = str; }
    public int symbol() { return sym; }
    public String toString() { return str; }
}
