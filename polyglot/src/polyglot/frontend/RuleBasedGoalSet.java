package polyglot.frontend;

import polyglot.util.InternalCompilerError;


public abstract class RuleBasedGoalSet implements GoalSet {
    public abstract boolean contains(Goal g);

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
        else if (that instanceof RuleBasedGoalSet) {
            return this.equals(that);
        }
        else if (that instanceof UnionGoalSet) {
            UnionGoalSet u = (UnionGoalSet) that;
            for (GoalSet s : u.components) {
                if (!containsAll(s)) {
                    return false;
                }
            }
            return true;
        }
        else {
            throw new InternalCompilerError("Unexpected GoalSet: " + that.getClass());                
        }
    }

    public GoalSet union(GoalSet that) {
        if (this.equals(that)) {
            return this;
        }
        return new UnionGoalSet(this, that);
    }
}
