package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject;

public interface Rhs extends TypeObject {
    public boolean matches(Rhs c);
    public Type type();
}
