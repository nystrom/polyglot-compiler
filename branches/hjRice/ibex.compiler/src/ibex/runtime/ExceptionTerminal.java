package ibex.runtime;

import java.io.IOException;

class ExceptionTerminal implements Terminal {
    IOException e;
    ExceptionTerminal(IOException e) { this.e = e; }
    IOException exception() { return e; }
    public int symbol() { return -2; }
    public String toString() { return "[" + e.getMessage() + "]"; }
}
