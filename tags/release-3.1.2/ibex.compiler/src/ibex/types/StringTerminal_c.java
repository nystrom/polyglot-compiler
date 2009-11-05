package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public class StringTerminal_c extends Terminal_c implements StringTerminal {
    String value;

    public StringTerminal_c(IbexTypeSystem ts, Position pos, String value) {
        super(ts, pos);
        this.value = value;
    }

    public boolean matches(Rhs sym) {
        if (sym instanceof StringTerminal) {
            StringTerminal t = (StringTerminal) sym;
            return t.value().equals(value);
        }
        return false;
    }

    public Type type() {
        return ts.String();
    }

    public String toString() {
        return value;
    }

    public String value() {
        return value;
    }
}
