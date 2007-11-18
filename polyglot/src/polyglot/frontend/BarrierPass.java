package polyglot.frontend;

import java.util.Collection;

public abstract class BarrierPass extends AbstractPass {
    Collection<Job> jobs;
    boolean abortOnFailure;
    
    public BarrierPass(Goal goal, Collection<Job> jobs) {
        this(goal, jobs, true);
    }
    
    public BarrierPass(Goal goal, Collection<Job> jobs, boolean abortOnFailure) {
        super(goal);
        this.jobs = jobs;
        this.abortOnFailure = abortOnFailure;
    }
    
    protected abstract boolean runForJob(Job job);
    protected boolean runAfterJobs(boolean okay) { return okay; }
    
    public final boolean run() {
        boolean okay = true;
        for (Job job : jobs) {
            okay &= runForJob(job);
            if (!okay && abortOnFailure) {
                break;
            }
        }
        return okay && runAfterJobs(okay);
    }
}