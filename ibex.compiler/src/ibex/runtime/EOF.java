package ibex.runtime;

class EOF implements Terminal {
    public int symbol() { return 256; }
    public String toString() { return "<EOF>"; }
}
