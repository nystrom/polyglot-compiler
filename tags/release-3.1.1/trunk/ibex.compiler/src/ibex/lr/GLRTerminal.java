package ibex.lr;

import java.util.Collections;
import java.util.Set;

import polyglot.types.Name;

public class GLRTerminal extends GLRSymbol implements Label, Comparable<GLRTerminal> {
    GLRTerminal(Name name, int index) {
        super(name, index);
    }
    
    public int compareTo(GLRTerminal o) {
        if (index < o.index)return -1;
        if (index > o.index)return 1;
        return 0;
    }

    GLRTerminal copy() {
        return new GLRTerminal(name, index);
    }

    Set<GLRTerminal> first() { return Collections.singleton(this); }
    boolean isNullable() { return false; }

    public boolean equals(Object o) {
        return o instanceof GLRTerminal && super.equals(o);
    }

    public int hashCode() {
        return index + 101;
    }
}
