/**
 * 
 */
package polyglot.frontend;

import java.util.*;

import polyglot.util.InternalCompilerError;

public class SimpleGoalSet implements GoalSet {
    Collection<Goal> goals;

    public SimpleGoalSet(Collection<Goal> goals) {
        this.goals = goals;
    }
    
    public boolean contains(Goal goal) {
        for (Goal g : goals) {
            if (g == goal) {
                return true;
            }
            if (g.requiredView().contains(goal)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsAll(GoalSet that) {
        if (that instanceof SimpleGoalSet) {
            SimpleGoalSet s = (SimpleGoalSet) that;
            for (Goal g : s.goals) {
                if (! contains(g)) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }
    
    public GoalSet union(GoalSet that) {
        Set<Goal> goals = new HashSet<Goal>();
        goals.addAll(this.goals);
        if (that instanceof SimpleGoalSet) {
            SimpleGoalSet s = (SimpleGoalSet) that;
            goals.addAll(s.goals);
            return new SimpleGoalSet(goals);
        }
        else {
            return new UnionGoalSet(this, that);
        }
    }
    
    public String toString() {
        return goals.toString();
    }
}