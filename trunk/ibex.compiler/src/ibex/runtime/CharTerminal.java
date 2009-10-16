package ibex.runtime;

class CharTerminal implements Terminal {
    char sym;
    CharTerminal(char sym) { this.sym = sym; }
    public int symbol() { return sym; }
    public String toString() { return String.valueOf((char) sym); }
}
