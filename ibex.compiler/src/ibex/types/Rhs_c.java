package ibex.types;

import java.util.Collections;
import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public abstract class Rhs_c extends TypeObject_c {

    public Rhs_c(TypeSystem ts, Position pos) {
        super(ts, pos);
    }

    public List<Type> throwTypes() {
        return Collections.EMPTY_LIST;
    }

}