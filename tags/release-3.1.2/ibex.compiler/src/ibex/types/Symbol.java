package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;

public interface Symbol extends TypeObject, Rhs {
    Type type();
}
