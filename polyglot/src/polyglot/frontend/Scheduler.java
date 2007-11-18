/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

/*
 * Scheduler.java
 * 
 * Author: nystrom
 * Creation date: Dec 14, 2004
 */
package polyglot.frontend;

import java.util.*;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.main.Report;
import polyglot.main.Version;
import polyglot.types.*;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.visit.*;


/**
 * The <code>Scheduler</code> manages <code>Goal</code>s and runs
 * <code>Pass</code>es.
 * 
 * The basic idea is to have the scheduler try to satisfy goals.
 * To reach a goal, a pass is run.  The pass could modify an AST or it
 * could, for example, initialize the members of a class loaded from a
 * class file.  Passes may be rerun if a goal is not reached.  Goals are
 * processed via a worklist.  A goal may have <i>prerequisite</i>
 * dependencies and <i>corequisite</i> dependencies.  All prerequisites
 * must be reached before the goal is attempted.  A corequisite may be
 * reached while satisfying the goal itself, or vice versa.
 *
 * Recursive passes are not allowed.  If a goal cannot be reached a
 * SchedulerException (or more usually the subclass
 * MissingDependencyException) is thrown.  The scheduler catches the
 * exception and adds the goal back onto the worklist and adds the
 * missing dependency, if any, to the dependency graph.  Optionally, a
 * pass may catch the exception, but it must mark the goal as unreachable
 * on this run so that it will be added back to the worklist; the pass
 * must also add any missing dependencies.
 *
 * @author nystrom
 */
public abstract class Scheduler {
    protected ExtensionInfo extInfo;
    
    /** map used for interning goals. */
    protected Map<Goal,Goal> internCache = new HashMap<Goal,Goal>();
    
    public Goal intern(Goal goal) {
        Goal g = internCache.get(goal);
        if (g == null) {
            g = goal;
            internCache.put(g, g);
        }
        return g;
    }
    
    protected Set<Goal> inWorklist;
    protected LinkedList<Goal> worklist;
    
    /**
     * A map from <code>Source</code>s to <code>Job</code>s or to
     * the <code>COMPLETED_JOB</code> object if the Job previously
     * existed
     * but has now finished. The map contains entries for all
     * <code>Source</code>s that have had <code>Job</code>s added for them.
     */
    protected Map<Source, Option<Job>> jobs;
    
    protected Collection<Job> commandLineJobs;
    
    /** True if any pass has failed. */
    protected boolean failed;

    static interface Option<T> {
        T get();
    }

    static class Some<T> implements Option<T> {
        T t;
        Some(T t) { this.t = t; }
        public T get() { return t; }
    }
    
    static class None<T> implements Option<T> {
        None() { }
        public T get() { return null; }
    }
    
    protected static final Option<Job> COMPLETED_JOB = new None<Job>() { public String toString() { return "COMPLETED JOB"; } };

    /** The currently running pass, or null if no pass is running. */
    protected Pass currentPass;
    
    public Scheduler(ExtensionInfo extInfo) {
        this.extInfo = extInfo;

        this.jobs = new HashMap<Source, Option<Job>>();
        this.inWorklist = new HashSet<Goal>();
        this.worklist = new LinkedList<Goal>();
        this.currentPass = null;
    }
    
    public Collection<Job> commandLineJobs() {
        return this.commandLineJobs;
    }
    
    public void setCommandLineJobs(Collection<Job> c) {
        this.commandLineJobs = Collections.unmodifiableCollection(c);
    }
    
    /** Add <code>goal</code> to the worklist. */
    public void enqueue(Goal goal) {
        if (! inWorklist.contains(goal)) {
            inWorklist.add(goal);
            worklist.add(goal);
        }
    }
    
    public void enqueueAll(Collection<Goal> goals) {
        for (Goal goal : goals) {
            enqueue(goal);
        }
    }
    
    public boolean reached(Goal goal) {
        return goal.hasBeenReached();
    }

    protected void completeJob(Job job) {
        if (job != null) {
            jobs.put(job.source(), COMPLETED_JOB);
            if (Report.should_report(Report.frontend, 1)) {
                Report.report(1, "Completed job " + job);
            }
        }
    }

    protected List<Goal> worklist() {
        return worklist;
    }
    
    public List<Goal> prerequisites(Goal goal) {
        return goal.prereqs();
    }

    protected Goal End(Job job) {
        return new SourceGoal_c("End", job) {
            public Pass createPass() {
                return new AbstractPass(this) {
                    public boolean run() {
                        // The job has finished.  Let's remove it from the job map
                        // so it can be garbage collected, and free up the AST.
                        completeJob(job);
                        return true;
                    }
                };
            }
        }.intern(this);
    }

    protected Goal EndAll() {
        return new AllBarrierGoal("EndAll", this) {
            public Goal prereqForJob(Job job) {
                return End(job);
            }
        }.intern(this);
    }
    
    protected Goal EndCommandLine() {
        return new BarrierGoal(commandLineJobs()) {
            public Goal prereqForJob(Job job) {
                return End(job);
            }

            public String name() { return "EndCommandLine"; }
        }.intern(this);
    }

    /**
     * Attempt to complete all goals in the worklist (and any subgoals they
     * have). This method returns <code>true</code> if all passes were
     * successfully run and all goals in the worklist were reached. The worklist
     * should be empty at return.
     */ 
    public boolean runToCompletion() {
        boolean okay = true;

        while (okay && ! worklist.isEmpty()) {
            Goal goal = worklist.removeFirst();
            okay = attempt(goal);
        }

        if (Report.should_report(Report.frontend, 1))
            Report.report(1, "Finished all passes for " + this.getClass().getName() + " -- " +
                        (okay ? "okay" : "failed"));

        return okay;
    }
    
    /**         
     * Load a source file and create a job for it.  Optionally add a goal
     * to compile the job to Java.
     * 
     * @param source The source file to load.
     * @param compile True if the compile goal should be added for the new job.
     * @return The new job or null if the job has already completed.
     */         
    public Job loadSource(FileSource source, boolean compile) {
        // Add a new Job for the given source. If a Job for the source
        // already exists, then we will be given the existing job.
        Job job = addJob(source);

        if (job == null) {
            // addJob returns null if the job has already been completed, in
            // which case we can just ignore the request to read in the
            // source.
            return null;
        }               
        
        // Create a goal for the job; this will set up dependencies for
        // the goal, even if the goal isn't to be added to the work list.
        addDependenciesForJob(job, compile);
        
        return job;
    }
    
    public boolean sourceHasJob(Source s) {
        return jobs.get(s) != null;
    }
    
    public Goal currentGoal() {
        return currentPass != null ? currentPass.goal() : null;
    }
    
    public GoalSet currentPhase() {
        Collection<Goal> s = new ArrayList<Goal>();
        
        for (Goal g : internCache.values()) {
            if (reached(g)) {
                s.add(g);        
            }
        }
        
        return new SimpleGoalSet(s);
    }

    public GoalSet currentView() {
        return currentGoal().requiredView();
    }
    
    public Job currentJob() {
        return currentPass != null ? currentPass.job() : null;
    }
    
    public Pass currentPass() {
        return currentPass;
    }
    
    /**
     * Run passes until the <code>goal</code> is attempted. Callers should
     * check goal.hasBeenReached() and should be able to handle the goal not being
     * reached.
     * 
     * @return false if there was an error trying to reach the goal; true if
     *         there was no error, even if the goal was not reached.
     */ 
    public boolean attempt(Goal goal) {
        return attempt(goal, new HashSet<Goal>());
    }

    protected boolean attempt(Goal goal, Set<Goal> above) {
        if (Report.should_report(Report.frontend, 2))
            Report.report(2, "Running to goal " + goal);

        if (above.contains(goal)) {
            if (Report.should_report(Report.frontend, 4))
                Report.report(4, goal + " is being attempted by a caller; returning");
            return true;
        }

        boolean progress = true;
    
        Set<Goal> newAbove = new HashSet<Goal>();
        newAbove.addAll(above);
        newAbove.add(goal);

        // Loop over the goal and its coreqs as long as progress is made.
        while (progress && ! reached(goal)) {
            progress = false;

            if (Report.should_report(Report.frontend, 4))
                Report.report(4, "outer loop for " + goal);

            // Run the prereqs of the goal.
            for (Goal subgoal : prerequisites(goal)) {
                if (reached(subgoal)) {
                    continue;
                }

                if (Report.should_report(Report.frontend, 4))
                    Report.report(4, "running prereq: " + subgoal + "->" + goal);

                if (! attempt(subgoal, newAbove)) {
                    return false;
                }

                if (reached(goal)) {
                    return true;
                }
            }

            // Make sure all prerequisite subgoals have been completed.
            // If any has not, just return.
            boolean runPass = true;

            for (Goal subgoal : prerequisites(goal)) {
                if (! reached(subgoal)) {
                    runPass = false;
                }
            }

            if (! runPass) {
                return true;
            }

            // Now, run the goal itself.
            if (Report.should_report(Report.frontend, 4))
                Report.report(4, "running goal " + goal);
            
            boolean result = runPass(goal);
                
            if (! result) {
                return false;
            }
            
            if (reached(goal)) {
                return true;
            }
        }
        
        return true;
    }
   
    /**         
     * Run the pass <code>pass</code>.  All subgoals of the pass's goal
     * required to start the pass should be satisfied.  Running the pass
     * may not satisfy the goal, forcing it to be retried later with new
     * subgoals.
     */
    protected boolean runPass(Goal goal) {
        Pass pass = goal.createPass();
        
        Job job = pass.job();
                
        if (extInfo.getOptions().disable_passes.contains(pass.name())) {
            if (Report.should_report(Report.frontend, 1))
                Report.report(1, "Skipping pass " + pass);
            
            goal.setState(Goal.Status.SUCCESS);
            return true;
        }
        
        if (Report.should_report(Report.frontend, 1))
            Report.report(1, "Running pass " + pass + " for " + goal);

        if (reached(goal)) {
            throw new InternalCompilerError("Cannot run a pass for completed goal " + goal);
        }
        
        pass.resetTimers();

        boolean result = false;

        if (job == null || job.status()) {
            Pass oldPass = this.currentPass;
            this.currentPass = pass;
            Report.should_report.push(pass.name());

            // Stop the timer on the old pass. */
            if (oldPass != null) {
                oldPass.toggleTimers(true);
            }

            if (job != null) {
                job.setRunningPass(pass);
            }
            
            pass.toggleTimers(false);

            goal.setState(Goal.Status.RUNNING);

            long t = System.currentTimeMillis();
            String key = pass.toString();

            extInfo.getStats().accumPassTimes(key + " attempts", 1, 1);
            extInfo.getStats().accumPassTimes("total goal attempts", 1, 1);
            
            try {
                result = pass.run();

                if (! result) {
                    extInfo.getStats().accumPassTimes(key + " failures", 1, 1);
                    extInfo.getStats().accumPassTimes("total goal failures", 1, 1);

                    goal.setState(Goal.Status.FAIL);
                    if (Report.should_report(Report.frontend, 1))
                        Report.report(1, "Failed pass " + pass + " for " + goal);
                }
                else {
                    if (goal.state() == Goal.Status.RUNNING) {
                        extInfo.getStats().accumPassTimes(key + " reached", 1, 1);
                        extInfo.getStats().accumPassTimes("total goal reached", 1, 1);

                        goal.setState(Goal.Status.SUCCESS);
                        if (Report.should_report(Report.frontend, 1))
                            Report.report(1, "Completed pass " + pass + " for " + goal);
                    }
                    else {
                        extInfo.getStats().accumPassTimes(key + " unreached", 1, 1);
                        extInfo.getStats().accumPassTimes("total goal unreached", 1, 1);

                        goal.setState(Goal.Status.FAIL);                    
                        if (Report.should_report(Report.frontend, 1))
                            Report.report(1, "Completed (unreached) pass " + pass + " for " + goal);
                    }
                }
            }
            catch (SchedulerException e) {
                if (Report.should_report(Report.frontend, 1))
                    Report.report(1, "Did not complete pass " + pass + " for " + goal);

                extInfo.getStats().accumPassTimes(key + " aborts", 1, 1);
                extInfo.getStats().accumPassTimes("goal aborts", 1, 1);
                
                goal.setState(Goal.Status.FAIL);
                result = true;
            }
            
            t = System.currentTimeMillis() - t;
            extInfo.getStats().accumPassTimes(key, t, t);
            
            pass.toggleTimers(false);
            
            if (job != null) {
                job.setRunningPass(null);
            }

            Report.should_report.pop();
            this.currentPass = oldPass;

            // Restart the timer on the old pass. */
            if (oldPass != null) {
                oldPass.toggleTimers(true);
            }

            // pretty-print this pass if we need to.
            if (job != null && extInfo.getOptions().print_ast.contains(pass.name())) {
                System.err.println("--------------------------------" +
                                   "--------------------------------");
                System.err.println("Pretty-printing AST for " + job +
                                   " after " + pass.name());

                job.ast().prettyPrint(System.err);
            }

            // dump this pass if we need to.
            if (job != null && extInfo.getOptions().dump_ast.contains(pass.name())) {
                System.err.println("--------------------------------" +
                                   "--------------------------------");
                System.err.println("Dumping AST for " + job +
                                   " after " + pass.name());
                
                job.ast().dump(System.err);
            }

            // This seems to work around a VM bug on linux with JDK
            // 1.4.0.  The mark-sweep collector will sometimes crash.
            // Running the GC explicitly here makes the bug go away.
            // If this fails, maybe run with bigger heap.
            
            // System.gc();
        }   
            
        Stats stats = extInfo.getStats();
        stats.accumPassTimes(pass.name(), pass.inclusiveTime(),
                             pass.exclusiveTime());

        if (! result) {
            failed = true;
        }
        
        // Record the progress made before running the pass and then update
        // the current progress.
        if (Report.should_report(Report.time, 2)) {
            Report.report(2, "Finished " + pass +
                          " status=" + statusString(result) + " inclusive_time=" +
                          pass.inclusiveTime() + " exclusive_time=" +
                          pass.exclusiveTime());
        }
        else if (Report.should_report(Report.frontend, 1)) {
            Report.report(1, "Finished " + pass +
                          " status=" + statusString(result));
        }
        
        if (job != null) {
            job.updateStatus(result);
        }
                
        return result;             
    }           
                                   
    protected static String statusString(boolean okay) {
        if (okay) {
            return "done";
        }
        else {
            return "failed";
        }
    }
    
    /** Return all compilation units currently being compiled. */
    public Collection<Job> jobs() {
        ArrayList<Job> l = new ArrayList<Job>(jobs.size());
        
        for (Iterator<Option<Job>> i = jobs.values().iterator(); i.hasNext(); ) {
            Option<Job> o = i.next();
            if (o != COMPLETED_JOB) {
                l.add(o.get());
            }
        }
        
        return l;
    }

    /**
     * Add a new <code>Job</code> for the <code>Source source</code>.
     * A new job will be created if
     * needed. If the <code>Source source</code> has already been processed,
     * and its job discarded to release resources, then <code>null</code>
     * will be returned.
     */
    public Job addJob(Source source) {
        return addJob(source, null);
    }

    /**
     * Add a new <code>Job</code> for the <code>Source source</code>,
     * with AST <code>ast</code>.
     * A new job will be created if
     * needed. If the <code>Source source</code> has already been processed,
     * and its job discarded to release resources, then <code>null</code>
     * will be returned.
     */
    public Job addJob(Source source, Node ast) {
        Option<Job> o = jobs.get(source);
        Job job = null;
        
        if (o == COMPLETED_JOB) {
            // the job has already been completed.
            // We don't need to add a job
            return null;
        }
        else if (o == null) {
            // No appropriate job yet exists, we will create one.
            job = this.createSourceJob(source, ast);

            // record the job in the map and the worklist.
            jobs.put(source, new Some<Job>(job));
    
            if (Report.should_report(Report.frontend, 4)) {
                Report.report(4, "Adding job for " + source + " at the " +
                    "request of pass " + currentPass);
            }
        }
        else {
            job = o.get();
        }
        
        return job;
    }

    /** Get the goals for a particular job.  This creates the dependencies between them.  The list must include End(job). */
    public abstract List<Goal> goals(Job job);

    public void addDependenciesForJob(Job job, boolean compile) {
        ExtensionInfo extInfo = this.extInfo;

        List<Goal> goals = goals(job);

        Goal prev = null;

        for (Goal goal : goals) {
            if (prev != null) {
                goal.addPrereq(prev);
            }
            prev = goal;
        }
        
        assert prev == End(job);
        
        if (compile) {
            EndAll().addPrereq(prev);
        }
    }

    /**
     * Create a new <code>Job</code> for the given source and AST.
     * In general, this method should only be called by <code>addJob</code>.
     */
    protected Job createSourceJob(Source source, Node ast) {
        return new Job(extInfo, extInfo.jobExt(), source, ast);
    }

    public String toString() {
        return getClass().getName() + " worklist=" + worklist;
    }   
    
    public abstract Goal Parsed(Job job);
    public abstract Goal ImportTableInitialized(Job job);
    public abstract Goal TypesInitialized(Job job);
    public abstract Goal TypesInitializedForCommandLine();
    public abstract Goal Disambiguated(Job job);
    public abstract Goal TypeChecked(Job job);
    public abstract Goal ReachabilityChecked(Job job);
    public abstract Goal ExceptionsChecked(Job job);
    public abstract Goal ExitPathsChecked(Job job);
    public abstract Goal InitializationsChecked(Job job);
    public abstract Goal ConstructorCallsChecked(Job job);
    public abstract Goal ForwardReferencesChecked(Job job);
    public abstract Goal Serialized(Job job);
    public abstract Goal CodeGenerated(Job job);

    public abstract Goal SupertypesResolved(Symbol<ClassDef> cd);
    public abstract Goal SignaturesResolved(Symbol<ClassDef> cd);
    public abstract Goal FieldConstantsChecked(Symbol<FieldDef> f);

    public abstract Goal LookupGlobalType(TypeRef< Type> sym);
    public abstract Goal LookupGlobalTypeDef(TypeRef<ClassDef> sym, String name);
    public abstract Goal LookupGlobalTypeDefAndSetFlags(TypeRef<ClassDef> sym, String name, Flags flags);
}


