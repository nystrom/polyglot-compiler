package polyglot.frontend;

import java.util.List;

import polyglot.types.Ref;


public interface Goal extends Ref<Goal.Status> {
    public static enum Status {
        NEW, RUNNING, SUCCESS, FAIL, UNREACHABLE, RUNNING_RECURSIVE, RUNNING_WILL_FAIL, TYPE_INIT_COMPLETED;
    };
    
    Status state();
    
    Goal intern(Scheduler s);
    
    String name();

    /** Return true if this goal is reachable. */
    public boolean isReachable();

    /** Return true if this goal has been reached. */
    public boolean hasBeenReached();
    
    /** Mark this pass as failed. */
    public void fail();
    
    public List<Goal> prereqGoals();
    public void addPrereq(Goal goal);

    boolean runTask();
}
