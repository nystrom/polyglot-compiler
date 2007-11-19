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
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Option;


/**
 * The <code>Scheduler</code> manages <code>Goal</code>s and runs
 * <code>Pass</code>es.
 * 
 * The basic idea is to have the scheduler try to satisfy goals. To reach a
 * goal, a pass is run. The pass could modify an AST or it could, for example,
 * initialize the members of a class loaded from a class file.
 * 
 * Goals are processed via a worklist. A goal may have <i>prerequisite</i>
 * dependencies. All prerequisites must be reached before the goal is attempted.
 * The compilation completes when the EndAll goal is reached. A goal will be
 * attempted at most once. If it fails, all goals dependent on it are
 * unreachable.
 * 
 * Passes are allowed to spawn other passes. A <i>reentrant pass</i> is allowed
 * to spawn itself.
 * 
 * Passes are (mostly) transactional. If a pass fails, its effects on the AST
 * and on the system resolver are rolled back.
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

    protected static final Option<Job> COMPLETED_JOB = new Option.None<Job>() { public String toString() { return "COMPLETED JOB"; } };

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
    
    GoalSet reached = new SimpleGoalSet(Collections.<Goal>emptySet());
    
    public GoalSet reachedGoals() {
        return reached;
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
     * Run passes until the <code>goal</code> is attempted.  Returns true iff the goal is reached.
     */ 
    public boolean attempt(Goal goal) {
        State s = pushGlobalState(goal);
        boolean fatal = true;
        try {
            boolean result = attemptGoalAndPrereqs(goal, new HashSet<Goal>());
            fatal = false;
            return result;
        }
        finally {
            if (! fatal) {
                popGlobalState(goal, s);
            }
        }
    }

    public static class State {
        SystemResolver resolver;
        State(SystemResolver resolver) {
            this.resolver = resolver;
        }
    }
    
    public State pushGlobalState(Goal goal) {
        TypeSystem ts = Globals.TS();
//        SystemResolver resolver = ts.saveSystemResolver();
        SystemResolver resolver = ts.systemResolver();
        return new State(resolver);
    }

    public void popGlobalState(Goal goal, State s) {
        TypeSystem ts = Globals.TS();
        if (reached(goal)) {
//            try {
//                s.resolver.putAll(ts.systemResolver());
//            }
//            catch (SemanticException e) {
//                ts.restoreSystemResolver(s.resolver);
//                goal.setState(Goal.Status.FAIL);
//            }
        }
        else {
//            ts.restoreSystemResolver(s.resolver);
        }
    }

    protected boolean attemptGoalAndPrereqs(Goal goal, Set<Goal> above) {
        if (Report.should_report(Report.frontend, 2))
            Report.report(2, "Running to goal " + goal);

        if (above.contains(goal)) {
            throw new InternalCompilerError("Goal " + goal + " is a prerequisite of itself.");
        }

        Set<Goal> newAbove = new HashSet<Goal>();
        newAbove.addAll(above);
        newAbove.add(goal);

        assert ! reached(goal) : goal + " already reached";
        
        switch (goal.state()) {
        case NEW: break;
        case RUNNING: break;
        case SUCCESS: return true;
        case FAIL: return false;
        case UNREACHABLE: return false;
        }
        
        Goal.Status oldState = goal.state();
        
        if (goal.state() == Goal.Status.NEW) {
            // Run the prerequisites of the goal.
            for (Goal subgoal : prerequisites(goal)) {
                if (reached(subgoal)) {
                    continue;
                }

                if (Report.should_report(Report.frontend, 4))
                    Report.report(4, "running prereq: " + subgoal + "->" + goal);

                if (! attemptGoalAndPrereqs(subgoal, newAbove)) {
                    goal.setState(Goal.Status.UNREACHABLE);
                    return false;
                }
            }
        }
        
        assert goal.state() == oldState;
        
        // Make sure all prerequisite subgoals have been completed.
        for (Goal subgoal : prerequisites(goal)) {
            switch (subgoal.state()) {
            case NEW:
            case RUNNING:
                throw new InternalCompilerError("Cannot reach " + goal + "; prerequisite " + subgoal + " has already run.");
            case SUCCESS:
                break;
            case FAIL:
            case UNREACHABLE:
                goal.setState(Goal.Status.UNREACHABLE);
                return false;
            }
        }

        // Now, run the goal itself.
        if (Report.should_report(Report.frontend, 4))
            Report.report(4, "running goal " + goal);

            boolean result = runPass(goal);
            
            if (! result || ! reached(goal)) {
                return false;
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
            
            markReached(goal);
            return true;
        }
        
        if (Report.should_report(Report.frontend, 1))
            Report.report(1, "Running pass " + pass + " for " + goal);

        if (reached(goal)) {
            throw new InternalCompilerError("Cannot run a pass for completed goal " + goal);
        }
        
        if (goal.state() == Goal.Status.RUNNING) {
            if (! pass.isReentrant()) {
                throw new InternalCompilerError("Cannot run a non-reentrant pass for " + goal);
            }
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

            extInfo.getStats().accumulate(key + " attempts", 1, 1);
            extInfo.getStats().accumulate("total goal attempts", 1, 1);
            
            try {
                result = pass.run();

                if (result && goal.state() == Goal.Status.RUNNING) {
                    extInfo.getStats().accumulate(key + " reached", 1, 1);
                    extInfo.getStats().accumulate("total goal reached", 1, 1);

                    markReached(goal);

                    if (Report.should_report(Report.frontend, 1))
                        Report.report(1, "Completed pass " + pass + " for " + goal);
                }
                else {
                    extInfo.getStats().accumulate(key + " unreached", 1, 1);
                    extInfo.getStats().accumulate("total goal unreached", 1, 1);

                    goal.setState(Goal.Status.FAIL);                    

                    if (Report.should_report(Report.frontend, 1))
                        Report.report(1, "Completed (unreached) pass " + pass + " for " + goal);
                }
            }
            catch (SchedulerException e) {
                if (Report.should_report(Report.frontend, 1))
                    Report.report(1, "Did not complete pass " + pass + " for " + goal);

                extInfo.getStats().accumulate(key + " aborts", 1, 1);
                extInfo.getStats().accumulate("goal aborts", 1, 1);
                
                goal.setState(Goal.Status.FAIL);
                result = true;
            }
            finally {
                t = System.currentTimeMillis() - t;
                extInfo.getStats().accumulate(key, t, t);
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
        }   
            
        Stats stats = extInfo.getStats();
        stats.accumulate(pass.name(), pass.inclusiveTime(),
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

    public void markReached(Goal goal) {
        goal.setState(Goal.Status.SUCCESS);
        reached = reached.union(new SimpleGoalSet(Collections.singleton(goal)));
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
            jobs.put(source, new Option.Some<Job>(job));
    
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


