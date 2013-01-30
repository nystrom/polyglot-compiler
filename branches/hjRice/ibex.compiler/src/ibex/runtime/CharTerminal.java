package ibex.runtime;

class CharTerminal implements Terminal {
    int sym;
    char val;
    CharTerminal(int sym, char val) { this.sym = sym; this.val = val; }
    public int symbol() { return sym; }
    public String toString() {
        if (32 <= val && val < 127)
            return "'" + val + "'";
        return "'\\x" + Integer.toHexString(val) + "'";
    }
}
