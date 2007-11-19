package polyglot.frontend;

import java.util.List;


public interface Goal {
    public static enum Status {
        NEW, RUNNING, SUCCESS, FAIL, UNREACHABLE;
    };

    Goal intern(Scheduler s);
    
    GoalSet requiredView();
    String name();
    Pass createPass();
    
    /** Mark the goal as reached or not reached. */
    public Status state();
    public void setState(Status state);

    /** Return true if this goal is reachable. */
    public boolean isReachable();

    /** Set a flag indicating that this rule is unreachable. */
    public void setUnreachable();
    
    /** Return true if this goal has been reached. */
    public boolean hasBeenReached();
    
    public List<Goal> prereqs();
    public void addPrereq(Goal goal);
}
