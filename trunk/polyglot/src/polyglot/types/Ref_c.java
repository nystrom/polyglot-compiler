package polyglot.types;

import polyglot.frontend.GoalSet;

/** Reference to a type object. */
public class Ref_c<T extends TypeObject> extends TypeObject_c implements Ref<T> {
    T v;
    
    public Ref_c(T v) {
        super(v.typeSystem(), v.position());
        assert v != null;
        this.v = v;
    }
    
    /** Return true if the reference is not null. */
    public boolean nonnull() {
        return v != null;
    }
    
    public T get() {
        return v;
    }

    public T get(GoalSet view) {
        return v;
    }
    
    public String toString() {
        if (v == null) return "null";
        return v.toString();
    }
}

