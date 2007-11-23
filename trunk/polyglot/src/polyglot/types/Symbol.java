package polyglot.types;

import polyglot.frontend.Goal;
import polyglot.frontend.GoalSet;


public interface Symbol<T extends TypeObject> extends Ref<T> {
    public void update(T v);
    public void update(T v, GoalSet view);
    public void update(T v, Goal goal);
}
