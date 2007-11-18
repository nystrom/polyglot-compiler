package polyglot.types;

import polyglot.frontend.GoalSet;
import polyglot.frontend.Pass;

/** Reference to a type object. */
public class Ref_c<T extends TypeObject> extends TypeObject_c implements Ref<T> {
    T cache;
    
    public static <T extends TypeObject> Ref_c<T> ref(T v) {
        return v != null ? new Ref_c<T>(v) : null;
    }

    public Ref_c(T v) {
        super(v.typeSystem(), v.position());
        cache = v;
    }
    
    /** Return true if the reference is not null. */
    public boolean nonnull() {
        return cache != null;
    }
    
    public T get() {
        return cache;
    }

    public T get(GoalSet view) {
        return cache;
    }
    
    public String toString() {
        if (cache == null) return "null";
        return cache.toString();
    }
}

