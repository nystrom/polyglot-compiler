package ibex.lr;

import ibex.types.*;
import polyglot.types.Name;
import polyglot.util.*;
import java.util.*;

abstract class GLRSymbol {
    Name name;
    int index;

    GLRSymbol(Name name, int index) {
        this.name = name;
        this.index = index;
    }

    public Name name() { return name; }
    public int index() { return index; }

    abstract Set<GLRTerminal> first();
    abstract boolean isNullable();

    public boolean equals(Object o) {
        if (o instanceof GLRSymbol) {
            GLRSymbol s = (GLRSymbol) o;
            return index == s.index;
        }
        return false;
    }

    public int hashCode() {
        return index;
    }

    public String toString() {
        return name.toString(); //  + "@" + index;
    }
}
