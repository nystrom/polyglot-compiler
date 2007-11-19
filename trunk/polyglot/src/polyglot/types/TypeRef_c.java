package polyglot.types;

import java.io.*;

import polyglot.frontend.*;
import polyglot.frontend.Goal.Status;
import polyglot.util.TypeInputStream;

public class TypeRef_c<T extends TypeObject> extends Symbol_c<T> implements TypeRef<T>, Serializable {
    Goal resolver;
    
    public TypeRef_c(T v) {
        super(v, GoalSet.EMPTY);
    }

    public TypeRef_c(T v, Goal resolver) {
        super(v, GoalSet.EMPTY);
        this.resolver = resolver;
    }

    public TypeRef_c() {
        this(null);
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
            this.history = null;
        }
    }
    
    public String toString() {
        if (history == null) {
            return name + ":" + resolver;
        }
        return super.toString();
    }
}
