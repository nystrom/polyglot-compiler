package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;

public interface Symbol extends TypeObject {
    Type type();
    boolean matches(Symbol sym);
}
