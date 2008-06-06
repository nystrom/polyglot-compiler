package polyglot.types;

import polyglot.frontend.AbstractGoal_c;
import polyglot.frontend.Goal;

public class Types {

    public static <T> T get(Ref<T> ref) {
        return ref != null ? ref.get() : null;
    }

    public static <T> Ref<T> ref(T v) {
	    if (v instanceof TypeObject)
		    return (Ref<T>) new Ref_c((TypeObject) v);
	    else if (v == null)
		    return null;
	    else {
		    Ref<T> ref = lazyRef(v, new AbstractGoal_c("OK!") {
			    public boolean run() {
				    return true;
			    }
		    });
		    ref.update(v);
		    return ref;
	    }
    }
    

//    public static <T extends TypeObject> LazyRef<T> lazyRef() {
//        return new LazyRef_c<T>();
//    }

    /** Create a lazy reference to a type object, with an initial value.
     * @param defaultValue initial value
     * @param resolver goal used to bring the reference up-to-date
     * 
     * ### resolver should be a map
     */
    public static <T> LazyRef<T> lazyRef(T defaultValue) {
        return new LazyRef_c<T>(defaultValue);
    }

    public static <T> LazyRef<T> lazyRef(T defaultValue, Goal resolver) {
        return new LazyRef_c<T>(defaultValue, resolver);
    }

}
