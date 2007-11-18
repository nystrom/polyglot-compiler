package polyglot.frontend;

import java.util.*;


/** A monotonic type state. */
public interface GoalSet {
    public static final GoalSet EMPTY = new SimpleGoalSet(Collections.<Goal>emptySet());
    
    boolean contains(Goal g);
    boolean containsAll(GoalSet that);
    GoalSet union(GoalSet that);
}
