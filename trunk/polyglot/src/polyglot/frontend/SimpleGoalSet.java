/**
 * 
 */
package polyglot.frontend;

import java.util.*;

public class SimpleGoalSet implements GoalSet {
    Set<Goal> goals;

    public SimpleGoalSet(Collection<Goal> goals) {
        this.goals = new LinkedHashSet<Goal>(goals);
    }
    
    public boolean contains(Goal goal) {
        if (goals.contains(goal)) {
            return true;
        }
        
        for (Goal g : goals) {
            if (g.requiredView().contains(goal)) {
                // memoize
                goals.add(goal);
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