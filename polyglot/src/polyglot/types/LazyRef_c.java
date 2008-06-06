package polyglot.types;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import polyglot.frontend.*;
import polyglot.frontend.Goal.Status;
import polyglot.util.TypeInputStream;

public class LazyRef_c<T> extends AbstractRef_c<T> implements LazyRef<T>, Serializable {
    Goal resolver;
    
    /** Create a lazy ref initialized with error value v. */
    public LazyRef_c(T v) {
    	this(v, new AbstractGoal_c("Error") {
			public boolean run() {
				return false;
			}
		});
    }

    /** Create a lazy ref initialized with error value v. */
    public LazyRef_c(T v, Goal resolver) {
        super(v);
		this.resolver = resolver;
    }

    /** Goal that, when satisfied, will resolve the reference */
    public Goal resolver() {
        return resolver;
    }
    
    public void setResolver(Goal resolver) {
        this.resolver = resolver;
    }

    public T get() {
    	if (! known()) {
    		if (resolver == null) {
    			assert false;
    		}
    		Scheduler scheduler = Globals.Scheduler();

    		if (! scheduler.reached(resolver)) {
    			try {
    				boolean result = scheduler.attempt(resolver);
    				// If successful, the ref should be filled.
    				assert ! result || known : "resolver=" + resolver + " result=" + result + " known=" + known;
    			}
    			catch (CyclicDependencyException e) {
    			}
    			
    			// Should have already reported an error.  Return the out-of-date value (which should be an error value).
    			known = true;
    		}

    	}
    	
    	return super.get();
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
    	return super.get().toString();
    }
}
