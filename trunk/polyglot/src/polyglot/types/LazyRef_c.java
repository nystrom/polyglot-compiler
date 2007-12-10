package polyglot.types;

import java.io.*;

import polyglot.frontend.*;
import polyglot.frontend.Goal.Status;
import polyglot.util.*;

public class LazyRef_c<T extends TypeObject> extends AbstractRef_c<T> implements LazyRef<T>, Symbol<T>, Serializable {
    Goal resolver;
    
//    public LazyRef_c() {
//        super();
//        this.resolver = null;
//    }
    
    /** Create a lazy ref initialized with error value v. */
    public LazyRef_c(T v) {
        this(v, null, Globals.currentView());
    }

    /** Create a lazy ref initialized with error value v. */
    public LazyRef_c(T v, Goal resolver) {
        this(v, resolver, Globals.currentView());
    }

    /** Create a lazy ref initialized with error value v. */
    public LazyRef_c(T v, Goal resolver, GoalSet view) {
        super(v, view);
        this.resolver = resolver;
    }

    /** Goal that, when satisfied, will resolve the reference */
    public Goal resolver() {
        return resolver;
    }
    
    public void setResolver(Goal resolver) {
        this.resolver = resolver;
    }
    
    protected void complete(GoalSet view) {
        if (view.contains(resolver)) {
            Scheduler scheduler = Globals.Scheduler();
            
            if (! scheduler.reached(resolver)) {
                try {
                    if (scheduler.attempt(resolver)) {
                        assert history.value != null;

                        update(history.value, view);

                        assert history.validIn(view) : this + " invalid in " + view + " but " + resolver + " reached";

                        return;
                    }
                }
                catch (CyclicDependencyException e) {
                }
                // Should have already reported an error.  Return the out-of-date value (which should be an error value).
            }
        }
        else {
            Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Cannot resolve " + this + "; resolver cannot reach view\n  resolver = " + resolver + " view = " + view);
        }
        
        // We failed; update the latest value with the current view.
        update(history.value, view);
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        assert resolver != null : "resolver for " + this + " is null";
        assert resolver instanceof Serializable : "resolver for " + this + " not Serializable";
        assert resolver.hasBeenReached() : "resolver for " + this + " not reached";
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        if (in instanceof TypeInputStream) {
            // Mark the resolver as NEW to force re-resolution
            this.resolver.setState(Status.NEW);
        }
    }
    
    public String toString() {
        return history.value.toString();
//        return "lazy(" + history.value + (resolver != null ? ", " + resolver.name() : "") + ")";
    }
}
