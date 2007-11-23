package polyglot.types;

import java.io.*;

import polyglot.frontend.*;
import polyglot.frontend.Goal.Status;
import polyglot.util.InternalCompilerError;
import polyglot.util.TypeInputStream;

public class LazyRef_c<T extends TypeObject> extends AbstractRef_c<T> implements LazyRef<T>, Symbol<T>, Serializable {
    Goal resolver;
    
    public LazyRef_c() {
        super();
        this.resolver = null;
    }
    
    public LazyRef_c(T v) {
        this(v, null, Globals.currentPhase());
    }

    public LazyRef_c(T v, Goal resolver) {
        this(v, resolver, Globals.currentPhase());
    }

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
                scheduler.attempt(resolver);
                assert resolver.hasBeenReached();
            }
            
            assert history.value != null;

            update(history.value, view);
            
            assert history.validIn(view) : this + " invalid in " + view + " but " + resolver + " reached";
        }
        else {
            // MIGHT get here, but not in the base compiler
            assert false;
        }
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
        return "lazy(" + (nonnull() ? history.value : "") + (resolver != null ? ", " + resolver.name() : "") + ")";
    }
}
