package ibex.lr;

import ibex.types.*;
import polyglot.util.*;
import java.util.*;
import polyglot.main.Report;

public class Conflict {
    State I;
    GLRTerminal t;
    LRConstruction lrc;

    Conflict(State I, GLRTerminal t, LRConstruction lrc) {
        this.I = I;
        this.t = t;
        this.lrc = lrc;
    }

    Collection actions() {
        return lrc.actions(I, t);
    }
    
    State state() {
        return I;
    }

    GLRTerminal terminal() {
        return t;
    }

    public boolean equals(Object o) {
        if (o instanceof Conflict) {
            Conflict c = (Conflict) o;
            return I.equals(c.I) && t.equals(c.t);
        }
        return false;
    }

    public int hashCode() {
        return I.hashCode() + t.hashCode();
    }

    public String toString() {
        return "conflict(" + I + ", " + t + ")";
    }

    public void dump() {
        System.out.println("conflict in " + I + " at " + t);
        I.dump();
        System.out.println();
        for (Iterator i = lrc.actions(I, t).iterator(); i.hasNext(); ) {
            Action a = (Action) i.next();
            System.out.println("    on " + t + ", " + a);
        }
        System.out.println();
    }
}
