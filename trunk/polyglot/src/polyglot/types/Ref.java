package polyglot.types;

import polyglot.frontend.GoalSet;

public interface Ref<T> {
    public T get();
    public T get(GoalSet view);
    
    public boolean nonnull();
}
