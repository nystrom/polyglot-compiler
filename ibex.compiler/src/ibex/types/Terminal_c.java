package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public abstract class Terminal_c extends TypeObject_c implements Terminal {
    public Terminal_c(IbexTypeSystem ts, Position pos) {
        super(ts, pos);
    }

    public abstract boolean matches(Rhs sym);
    public abstract Type type();
}
