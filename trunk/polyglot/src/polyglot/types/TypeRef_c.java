package polyglot.types;

import polyglot.frontend.*;

public class TypeRef_c<T extends TypeObject> extends Symbol_c<T> implements TypeRef<T> {
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
            scheduler.attempt(resolver);
            assert resolver.hasBeenReached();
            assert history.value != null;
            assert history.validIn(view) : this + " invalid in " + view + " but " + resolver + " reached";
        }
    }

}
