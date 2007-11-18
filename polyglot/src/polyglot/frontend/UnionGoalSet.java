package polyglot.frontend;

import java.util.*;

/** Union of many goal sets.  The sets are assumed to be disjoint. */
public class UnionGoalSet implements GoalSet {
    List<GoalSet> components;
    
    public UnionGoalSet(GoalSet gs1, GoalSet gs2) {
        this(Arrays.asList(new GoalSet[] { gs1, gs2 }));
    }

    public UnionGoalSet(List<GoalSet> gss) {
        components = new ArrayList<GoalSet>();

        Set<Goal> goals = new HashSet<Goal>();
        
        for (GoalSet gs : gss) {
            if (gs instanceof UnionGoalSet) {
                components.addAll(((UnionGoalSet) gs).components);
            }
            else if (gs instanceof SimpleGoalSet) {
                goals.addAll(((SimpleGoalSet) gs).goals);
            }
            else {
                components.add(gs);
            }
        }
        
        if (! goals.isEmpty()) {
            components.add(new SimpleGoalSet(goals));
        }
    }

    public boolean contains(Goal g) {
        for (GoalSet gs : components) {
            if (gs.contains(g)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(GoalSet that) {
        for (GoalSet gs : components) {
            if (gs.containsAll(that)) {
                return true;
            }
        }
        return false;
    }

    public GoalSet union(GoalSet that) {
        return new UnionGoalSet(this, that);
    }
    
    public String toString() {
        return components.toString(); 
   }
}
