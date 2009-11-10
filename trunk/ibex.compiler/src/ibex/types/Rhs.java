package ibex.types;

import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeObject;

public interface Rhs extends TypeObject {
    public boolean matches(Rhs c);
    public Type type();
    public List<Type> throwTypes();
}
