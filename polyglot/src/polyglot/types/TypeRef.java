package polyglot.types;

import polyglot.frontend.Goal;

public interface TypeRef<T extends TypeObject> extends Symbol<T> {
    Goal resolver();
    void setResolver(Goal resolver);
}
