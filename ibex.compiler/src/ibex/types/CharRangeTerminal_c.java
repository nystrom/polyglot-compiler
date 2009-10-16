package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class CharRangeTerminal_c extends Terminal_c implements CharRangeTerminal {
    char lo;
    char hi;

    public CharRangeTerminal_c(IbexTypeSystem ts, Position pos, char lo, char hi) {
        super(ts, pos);
    }

    public boolean matches(Rhs sym) {
        return false;
    }

    public Type type() {
        return ts.Char();
    }

    /* (non-Javadoc)
     * @see ibex.types.Range#lo()
     */
    public char lo() {
        return lo;
    }

    /* (non-Javadoc)
     * @see ibex.types.Range#setLo(char)
     */
    public void setLo(char c) {
        lo = c;
    }

    /* (non-Javadoc)
     * @see ibex.types.Range#hi()
     */
    public char hi() {
        return hi;
    }

    /* (non-Javadoc)
     * @see ibex.types.Range#setHi(char)
     */
    public void setHi(char c) {
        hi = c;
    }
    
    @Override
    public String toString() {
        return lo + ".." + hi;
    }
}
