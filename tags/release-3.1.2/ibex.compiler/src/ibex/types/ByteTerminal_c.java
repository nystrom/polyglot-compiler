package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.util.Position;

public class ByteTerminal_c extends Terminal_c implements ByteTerminal {
    byte value;

    public ByteTerminal_c(IbexTypeSystem ts, Position pos, byte value) {
        super(ts, pos);
        this.value = value;
    }

    public boolean matches(Rhs sym) {
        if (sym instanceof ByteTerminal) {
            ByteTerminal t = (ByteTerminal) sym;
            return t.value() == value;
        }
        return false;
    }
    
    @Override
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof ByteTerminal) {
            ByteTerminal c = (ByteTerminal) t;
            return value == c.value();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return value;
    }

    public Type type() {
        return ts.Byte();
    }

    public String toString() {
        return String.valueOf(value);
    }

    public byte value() {
        return value;
    }
}
