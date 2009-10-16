package ibex.types;

import polyglot.types.Type;
import polyglot.util.Position;

public class CharTerminal_c extends Terminal_c implements CharTerminal {
    char value;
    
    public CharTerminal_c(IbexTypeSystem ts, Position pos, char value) {
        super(ts, pos);
        this.value = value;
    }

    public boolean matches(Rhs sym) {
        if (sym instanceof CharTerminal) {
            CharTerminal t = (CharTerminal) sym;
            return t.value() == value;
        }
        return false;
    }

    public Type type() {
        return ts.Char();
    }

    public String toString() {
        return String.valueOf(value);
    }

    public char value() {
        return value;
    }
}
