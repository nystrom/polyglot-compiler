package polyglot.frontend;

import java.util.*;

import polyglot.util.StringUtil;

public abstract class AbstractGoal_c implements Goal {
    Status state;
    String name;
    public List<Goal> prereqs;
    
    public Goal intern(Scheduler scheduler) {
        return scheduler.intern(this);
    }
    
    protected AbstractGoal_c() {
        this.state = Status.NEW;
        this.name = StringUtil.getShortNameComponent(getClass().getName().replace('$', '.'));
    }

    protected AbstractGoal_c(String name) {
        this.state = Status.NEW;
        this.name = name;
    }
    
    public abstract boolean run();
    
    public List<Goal> prereqs() {
        if (prereqs == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(prereqs);
        }
    }
    
    public void addPrereq(final Goal goal) {
        if (prereqs == null) {
            prereqs = new ArrayList<Goal>();
        }
        
        prereqs.add(goal);
    }
    
    public boolean hasBeenReached() {
        return state == Status.SUCCESS;
    }

    public boolean isReachable() {
        switch (state) {
        case NEW:
        case RUNNING:
        case RUNNING_RECURSIVE:
        case SUCCESS:
            return true;
        case FAIL:
        case UNREACHABLE:
            return false;
        default:
            return false;
        }
    }
    
    public String name() {
        return name;
    }

    public void setState(Status state) {
        this.state = state;
    }

    public void setUnreachable() {
        setState(Status.UNREACHABLE);
    }

    public Status state() {
        return this.state;
    }
    
    public int hashCode() {
        return name().hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof Goal) {
            Goal g = (Goal) o;
            return name().equals(g.name());
        }
        return false;
    }

    protected String stateString() {
        switch (state) {
        case NEW:
            return "new";
        case RUNNING:
            return "running";
        case RUNNING_RECURSIVE:
            return "running-recursive";
        case SUCCESS:
            return "success";
        case FAIL:
            return "failed";
        case UNREACHABLE:
            return "unreachable";
        }
        return "unknown-goal-state";
    }

    public String toString() {
        return name() + " (" + stateString() + ")";
    }
}
