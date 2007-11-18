package polyglot.frontend;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public abstract class AllBarrierPass extends AbstractPass {
    Scheduler scheduler;
    boolean abortOnFailure;
    
    public AllBarrierPass(Goal goal, Scheduler scheduler) {
        this(goal, scheduler, true);
    }
    
    public AllBarrierPass(Goal goal, Scheduler scheduler, boolean abortOnFailure) {
        super(goal);
        this.scheduler = scheduler;
        this.abortOnFailure = abortOnFailure;
    }
    
    protected boolean runForJob(Job job) { return true; }
    protected boolean runAfterJobs(boolean okay) { return okay; }
    
    public final boolean run() {
        boolean okay = true;
        LinkedList<Job> worklist = new LinkedList<Job>(scheduler.jobs());
        Set<Job> attempted = new HashSet<Job>(scheduler.jobs());

        OUTER:
        while (true) {
            while (! worklist.isEmpty()) {
                Job job = worklist.removeFirst();
                
                okay &= runForJob(job);

                attempted.add(job);

                if (!okay && abortOnFailure) {
                    break OUTER;
                }
            }

            // Add any jobs added while running the subtasks.
            for (Job job : scheduler.jobs()) {
                if (! attempted.contains(job)) {
                    worklist.add(job);
                }
            }
        }
        
        return okay && runAfterJobs(okay);
    }
}