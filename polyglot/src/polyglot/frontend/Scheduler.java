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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import funicular.runtime.Activity;

import polyglot.ast.Node;
import polyglot.dispatch.ASTDumper;
import polyglot.dispatch.NewPrettyPrinter;
import polyglot.frontend.Goal.Status;
import polyglot.main.Report;
import polyglot.types.Futures;
import polyglot.types.QName;
import polyglot.util.InternalCompilerError;
import polyglot.util.Option;
import polyglot.util.Position;
import polyglot.visit.Interpreted;
import polyglot.visit.OutputBytecode;
import polyglot.visit.PostCompiled;

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

	private ConcurrentHashMap<Job, List<Goal>> map = new ConcurrentHashMap<Job, List<Goal>>();
	protected ConcurrentHashMap<Job, ReentrantLock> lockOnJob = new ConcurrentHashMap<Job, ReentrantLock>();
	static LinkedList<Job> allJobs; // will become empty after typecheck runs

	/** map used for interning goals. */
	protected Map<Goal, Goal> internCache = new HashMap<Goal, Goal>();

	public ReentrantLock getLockOnJob(Job job) {
		return lockOnJob.get(job);
	}

	// TODO: remove this, we only need to intern the goal status, not the goal
	// itself.
	// Actually, the lazy ref to the goal status is the goal. The run() method
	// is the resolver for the lazy ref.
	public Goal intern(Goal goal) {
		Globals.Stats().accumulate("intern", 1);
		Globals.Stats().accumulate(
				"intern:"
						+ (goal instanceof VisitorGoal ? ((VisitorGoal) goal).v
								.getClass().getName() : goal.getClass()
								.getName()), 1);
		Goal g = internCache.get(goal);
		if (g == null) {
			g = goal;
			internCache.put(g, g);
		} else {
			assert goal.getClass() == g.getClass();
		}
		return g;
	}

	/**
	 * A map from <code>Source</code>s to <code>Job</code>s or to the
	 * <code>COMPLETED_JOB</code> object if the Job previously existed but has
	 * now finished. The map contains entries for all <code>Source</code>s that
	 * have had <code>Job</code>s added for them.
	 */
	protected Map<Source, Option<Job>> jobs;

	protected Collection<Job> commandLineJobs;

	/** True if any pass has failed. */
	protected boolean failed;

	protected static final Option<Job> COMPLETED_JOB = Option.<Job> None();

	/** The currently running pass, or null if no pass is running. */
	protected Goal currentGoal;

	public Scheduler(ExtensionInfo extInfo) {
		this.extInfo = extInfo;
		this.jobs = new ConcurrentHashMap<Source, Option<Job>>();
		this.currentGoal = null;
		this.currentJob = null;
		this.allJobs= new LinkedList<Job>();
	}

	public Collection<Job> commandLineJobs() {
		return this.commandLineJobs;
	}

	public void setCommandLineJobs(Collection<Job> c) {
		this.commandLineJobs = Collections.unmodifiableCollection(c);
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

	public List<Goal> prerequisites(Goal goal) {
		return goal.prereqGoals();
	}

	Goal EndAll;

	public Goal End(Job job) {
		return new SourceGoal_c("End", job) {
			public boolean runTask() {
				// The job has finished. Let's remove it from the job map
				// so it can be garbage collected, and free up the AST.
				completeJob(job);
				return true;
			}
		}.intern(this);
	}

	Collection<Job> shouldCompile = new LinkedHashSet<Job>();

	public boolean shouldCompile(Job job) {
		if (commandLineJobs().contains(job))
			return true;
		if (Globals.Options().compile_command_line_only)
			return false;
		return shouldCompile.contains(job);
	}

	Goal DummyEndAll = null;

	protected Goal DummyEndAll() {
		if (DummyEndAll == null)
			DummyEndAll = new AllBarrierGoal("DummyEndAll", this) {
				@Override
				public Goal prereqForJob(Job job) {
					if (scheduler.shouldCompile(job)) {
						return scheduler.End(job);
					} else {
						return new SourceGoal_c("DummyEnd", job) {
							public boolean runTask() {
								return true;
							}
						}.intern(scheduler);
					}
				}
			};
		return DummyEndAll;
	}

	protected Goal OutputBytecode() {
		if (Globals.Options().interpret || Globals.Options().output_source) {
			return DummyEndAll();
		}
		if (EndAll == null)
			EndAll = new OutputBytecode(extInfo);
		return EndAll;
	}

	protected Goal PostCompiled() {
		if (Globals.Options().interpret || !Globals.Options().output_source) {
			return DummyEndAll();
		}
		if (EndAll == null)
			EndAll = new PostCompiled(extInfo);
		return EndAll;
	}

	protected Goal Interpreted() {
		if (!Globals.Options().interpret) {
			return DummyEndAll();
		}
		if (EndAll == null)
			EndAll = new Interpreted(extInfo);
		return EndAll;
	}

	protected Goal EndAll() {
		if (!Globals.Options().interpret) {
			if (!Globals.Options().output_source) {
				return OutputBytecode();
			} else {
				return PostCompiled();
			}
		} else {
			return Interpreted();
		}
	}

	protected Goal EndCommandLine() {
		return EndAll();
	}

	public boolean runFirst() {
		final Collection<Job> jobs = this.jobs();
		final Scheduler thisScheduler = this;
		Futures.finish(new Runnable() {
			public void run() {
				for (final Job job : jobs) {
						Futures.async(new Runnable() {
							public void run() {
								addDependenciesForJob(job, true);
								List<Goal> goals = map.get(job);
								Goal goal = goals.get(goals.size()-1);
								goal.get();
//								thisScheduler.runPass(CompileGoalScala(job));
////								CompileGoalScala goal = new CompileGoalScala(job);
//								Goal goal = goals.get(0);
//								runPass(goal);
//								goal.runTask(job);
								
							}
						});
					} 
				}
		});

		// run typecheck
//		allJobs.addAll(jobs);
//		Set<Job> oldJobs = new LinkedHashSet<Job>();
//		oldJobs.addAll(jobs);
//
		
		while (!allJobs.isEmpty()){			
//			Set<Job> copyJobs = new LinkedHashSet<Job>();
//			copyJobs.addAll(allJobs);
			Job job = allJobs.removeFirst();
			List<Goal> goalsForThisJob = map.get(job);
			
			for (Goal goal : goalsForThisJob) {
				if (!goal.hasBeenReached() && goal.name().equals("TypeChecked")) {
					runPass(goal);
////					runPass(TypeCheckGoalScala(job));
//					Set<Job> newJobs = map.keySet();
//					for (Job job2 : newJobs) {
//						if(oldJobs.contains(job2)){
//							continue;
//						}
//						if (!copyJobs.contains(job2)) {
//							allJobs.add(job2);
//						}
//					}
//					break;
//			}
				}
			}
		}
//
		return true;
	}

	public boolean runToCompletion() {
		Futures.finish(new Runnable() {
			public void run() {
				runFirst();
			}
		});
//
		final Collection<Job> jobs = this.jobs();
		final Scheduler thisScheduler = this;
		
//		Futures.finish(new Runnable(){
//			public void run(){
//				for (final Job job : jobs) {
//					final Job jobCopy1 = new Job(job.extensionInfo(), job.source(), job.ast());
//					final Job jobCopy2 = new Job(job.extensionInfo(), job.source(), job.ast());
//						Futures.async(new Runnable(){
//							public void run(){
//								runPass(ConformanceCheckGoalScala(jobCopy1));
//							}
//						});
//						Futures.async(new Runnable(){
//							public void run(){
//								runPass(ReachAndExceptionCheckGoal(jobCopy2));
//							}
//						});
//					
//				}
//			}
//		});
		
//		 && !goal.state().equals(Status.TYPE_INIT_COMPLETED) 
//			

		Futures.finish(new Runnable() {
			public void run() {
				for (final Job job : jobs) {
					final List<Goal> goalsForThisJob = map.get(job);
					for (final Goal goal : goalsForThisJob) {
						if (!goal.hasBeenReached() && !goal.state().equals(Status.TYPE_INIT_COMPLETED) 
								&& !goal.name().equalsIgnoreCase("ThrowsSetup") 
//								&&!goal.name().equalsIgnoreCase("ReachChecked")
								) {
							Futures.async(new Runnable(){
								public void run(){
//									runPass(goal);
									goal.start();
								}
							});
							
						}
					}
				}
			}
		});

		// boolean okay = runToCompletion(EndAll());
		boolean okay = runPass(EndAll());
		return okay;
	}

	/**
	 * Attempt to complete all goals in the worklist (and any subgoals they
	 * have). This method returns <code>true</code> if all passes were
	 * successfully run and all goals in the worklist were reached. The worklist
	 * should be empty at return.
	 */
	public boolean runToCompletion(Goal endGoal) {

		boolean okay = false;

		okay = attempt(endGoal);

		if (Report.should_report(Report.frontend, 1))
			Report.report(1, "Finished all passes for "
					+ this.getClass().getName() + " -- "
					+ (okay ? "okay" : "failed"));

		return okay;
	}

	/**
	 * Load a source file and create a job for it. Optionally add a goal to
	 * compile the job to Java.
	 * 
	 * @param source
	 *            The source file to load.
	 * @param compile
	 *            True if the compile goal should be added for the new job.
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
		return currentGoal;
	}

	public Job currentJob;

	private ConcurrentHashMap<String, Job> mapNameToJob = new ConcurrentHashMap<String, Job>();

	public Job currentJob() {
		return currentJob;
	}
	public Job currentJob(Position pos) {
		String path = pos.path();
		Job job = mapNameToJob.get(path);
		return job;
	}

	/**
	 * Run passes until the <code>goal</code> is attempted. Returns true iff the
	 * goal is reached.
	 */
	public boolean attempt(Goal goal) {
		assert currentGoal() == null
				|| currentGoal().getCached() == Goal.Status.RUNNING
				|| currentGoal().getCached() == Goal.Status.RUNNING_RECURSIVE
				|| currentGoal().getCached() == Goal.Status.RUNNING_WILL_FAIL : "goal "
				+ currentGoal() + " state " + currentGoal().state();

		Status state = goal.get();

		return state == Goal.Status.SUCCESS
				|| state == Goal.Status.TYPE_INIT_COMPLETED;
	}

	public List<Goal> prereqs(Goal g) {
		return g.prereqGoals();
	}

	public boolean attempt(Job job, Goal goal) {
		assert currentGoal() == null
				|| currentGoal().getCached() == Goal.Status.RUNNING
				|| currentGoal().getCached() == Goal.Status.RUNNING_RECURSIVE
				|| currentGoal().getCached() == Goal.Status.RUNNING_WILL_FAIL : "goal "
				+ currentGoal() + " state " + currentGoal().state();

		if (!map.contains(job)) {
			// goals(job);
			addDependenciesForJob(job, true);
		}
		Goal goalToAttempt = null;
		List<Goal> list = map.get(job);
		Iterator<Goal> iter = list.iterator();
		while (iter.hasNext()) {
			Goal next = (Goal) iter.next();
			if (next.name().equals(goal.name())) {
				goalToAttempt = next;
				break;
			}
		}
		return attempt(goalToAttempt);
	}

	/**
	 * Run the pass <code>pass</code>. All subgoals of the pass's goal required
	 * to start the pass should be satisfied. Running the pass may not satisfy
	 * the goal, forcing it to be retried later with new subgoals.
	 */
	public boolean runPass(Goal goal) {
		Job job = goal instanceof SourceGoal ? ((SourceGoal) goal).job() : null;

		if (extInfo.getOptions().disable_passes.contains(goal.name())) {
			 if (Report.should_report(Report.frontend, 1))
			 Report.report(1, "Skipping pass " + goal);

			goal.updateSoftly(Goal.Status.SUCCESS);
			return true;
		}

		if (Report.should_report(Report.frontend, 1))
			Report.report(1, "Running pass for " + goal);

		if (reached(goal)) {
			throw new InternalCompilerError(
					"Cannot run a pass for completed goal " + goal);
		}

		boolean result = false;

		if (true || job == null || job.status()) {
			Report.should_report.push(goal.name());

			if (job != null) {
				// We're starting to run the pass.
				// Record the initial error count.
				job.initialErrorCount = job.compiler().errorQueue()
						.errorCount();
			}

			Goal oldGoal = currentGoal;
			Job oldJob = currentJob;
			currentGoal = goal;
			currentJob = (currentGoal instanceof SourceGoal_c) ? ((SourceGoal_c) currentGoal)
					.job() : null;

			long t = System.currentTimeMillis();
			String key = goal.toString();

			// extInfo.getStats().accumulate(key + " attempts", 1);
			// extInfo.getStats().accumulate("total goal attempts", 1);

			try {
				result = goal.runTask();

				 if (result && goal.getCached() == Goal.Status.RUNNING) {
				 extInfo.getStats().accumulate(key + " reached", 1);
				 extInfo.getStats().accumulate("total goal reached", 1);

				goal.updateSoftly(Status.SUCCESS);

				 if (Report.should_report(Report.frontend, 1))
				 Report.report(1, "Completed pass for " + goal);
				 }
				 else {
				 extInfo.getStats().accumulate(key + " unreached", 1);
				 extInfo.getStats().accumulate("total goal unreached", 1);

				 if (Report.should_report(Report.frontend, 1))
				 Report.report(1, "Completed (unreached) pass for " + goal);
				 }
			} finally {
				t = System.currentTimeMillis() - t;
				extInfo.getStats().accumulate(key, t);

				currentGoal = oldGoal;
				currentJob = oldJob;

				if (job != null) {
					// We've stopped running a pass.
					// Check if the error count changed.
					int errorCount = job.compiler().errorQueue().errorCount();

					if (errorCount > job.initialErrorCount) {
						job.reportedErrors = true;
					}
				}

				Report.should_report.pop();
			}

			// pretty-print this pass if we need to.
			if (job != null
					&& extInfo.getOptions().print_ast.contains(goal.name())) {
				System.err.println("--------------------------------"
						+ "--------------------------------");
				System.err.println("Pretty-printing AST for " + job + " after "
						+ goal.name());

				new NewPrettyPrinter(System.err).printAst(job.ast());
			}

			// dump this pass if we need to.
			if (job != null
					&& extInfo.getOptions().dump_ast.contains(goal.name())) {
				System.err.println("--------------------------------"
						+ "--------------------------------");
				System.err.println("Dumping AST for " + job + " after "
						+ goal.name());

				new ASTDumper(System.err).dumpAst(job.ast());
			}
		}

		if (!result) {
			failed = true;
		}

		// Record the progress made before running the pass and then update
		// the current progress.
		if (Report.should_report(Report.frontend, 1)) {
			Report.report(1, "Finished " + goal + " status="
					+ statusString(result));
		}

		if (job != null) {
			job.updateStatus(result);
		}

		return result;
	}

	protected static String statusString(boolean okay) {
		if (okay) {
			return "done";
		} else {
			return "failed";
		}
	}

	/** Return all compilation units currently being compiled. */
	public Collection<Job> jobs() {
		ArrayList<Job> l = new ArrayList<Job>(jobs.size());

		for (Iterator<Option<Job>> i = jobs.values().iterator(); i.hasNext();) {
			Option<Job> o = i.next();
			if (o != COMPLETED_JOB) {
				l.add(o.get());
			}
		}

		return l;
	}

	/**
	 * Add a new <code>Job</code> for the <code>Source source</code>. A new job
	 * will be created if needed. If the <code>Source source</code> has already
	 * been processed, and its job discarded to release resources, then
	 * <code>null</code> will be returned.
	 */
	public Job addJob(Source source) {
		return addJob(source, null);
	}

	/**
	 * Add a new <code>Job</code> for the <code>Source source</code>, with AST
	 * <code>ast</code>. A new job will be created if needed. If the
	 * <code>Source source</code> has already been processed, and its job
	 * discarded to release resources, then <code>null</code> will be returned.
	 */
	public Job addJob(Source source, Node ast) {
		Option<Job> o = jobs.get(source);
		Job job = null;

		if (o == COMPLETED_JOB) {
			// the job has already been completed.
			// We don't need to add a job
			return null;
		} else if (o == null) {
			// No appropriate job yet exists, we will create one.
			job = this.createSourceJob(source, ast);

			// record the job in the map and the worklist.
			jobs.put(source, new Option.Some<Job>(job));

			if (Report.should_report(Report.frontend, 4)) {
				Report.report(4, "Adding job for " + source + " at the "
						+ "request of goal " + currentGoal);
			}
		} else {
			job = o.get();
		}

		lockOnJob.put(job, new ReentrantLock());

		return job;
	}

	/**
	 * Get the goals for a particular job. This creates the dependencies between
	 * them. The list must include End(job).
	 */
	public abstract List<Goal> goals(Job job);

	public void addDependenciesForJob(Job job, boolean compile) {

		List<Goal> goals = goals(job);
		Goal prev = null;
		Goal typeChecked = null;
		Goal reachChecked = null;
		Goal pretypechecked2 = null;
		Goal throwsSetup = null;
		// Be careful: the list might include goals already run.
		for (Goal goal : goals) {
			// assert goal instanceof SourceGoal;

//			if (goal.name().equalsIgnoreCase("PreTypeCheck2")) {
//				pretypechecked2 = goal;
//				goal.addPrereq(typeChecked);
//				prev = goal;
//				continue;
//			}
			
			if (goal.name().equals("ReachChecked")) {
				reachChecked = goal;
				goal.addPrereq(typeChecked);
//				goal.addPrereq(pretypechecked2);
				prev = goal;
				continue;
			}
			
			if(goal.name().equals("ThrowsSetup")){
				throwsSetup = goal;
				goal.addPrereq(typeChecked);//reachChecked
				prev = goal;
				continue;
			}
			
			if (throwsSetup != null
					&& goal.name().equals("ExceptionsChecked")) {
				goal.addPrereq(throwsSetup); //pretypechecked2);// reachChecked);
				prev = goal;
				continue;
			}
			if (goal.name().equals("TypeChecked")) {
				typeChecked = goal;
			}
			if (typeChecked != null && !goal.name().equals("TypeChecked")) {
				goal.addPrereq(typeChecked);
				prev = goal;
				continue;
			}
			if (prev != null) {
				goal.addPrereq(prev);
			}
			if (!goal.hasBeenReached()) {
				prev = goal;
			}
		}

		assert prev == End(job);

		if (compile) {
			shouldCompile.add(job);
			EndAll().addPrereq(prev);
		}

//		 ArrayList<String> list = new ArrayList<String>();
//		 list.add("CodeGenerated");
//		 list.add("PreTypeCheck2");
//		 list.add("ConformanceChecked");
//		 list.add("ExceptionsChecked");
//		 list.add("ReachChecked");

		// set resolvers to we can run the futures.
		for (Goal goal : goals) {
			// if(list.contains(goal.name())){
			final Goal g1 = goal;
			g1.setResolver(new Runnable() {
				public void run() {
//					runPass(g1);
					g1.get();
				}
			}, null);
			// }
		}

		this.map.put(job, goals);
		allJobs.add(job);
		String pos = job.source().path();
		mapNameToJob.put(pos, job);
		

	}

	/**
	 * Create a new <code>Job</code> for the given source and AST. In general,
	 * this method should only be called by <code>addJob</code>.
	 */
	protected Job createSourceJob(Source source, Node ast) {
		return new Job(extInfo, source, ast);
	}

	public String toString() {
		return getClass().getName();
	}

	public abstract Goal Parsed(Job job);

	public abstract Goal ImportTableInitialized(Job job);

	public abstract Goal TypesInitialized(Job job);

	public abstract Goal TypesInitializedForCommandLine();

	public abstract Goal PreTypeCheck(Job job);

	public abstract Goal TypeChecked(Job job);

	public abstract Goal ExceptionsChecked(Job job);

	public abstract Goal CodeGenerated(Job job);

	public abstract Goal BytecodeCached(QName sym);
}
