package polyglot.types;

import polyglot.frontend.Goal;

public interface LazyRef<T> extends Ref<T> {
    Goal resolver();
    void setResolver(Goal resolver);
}
