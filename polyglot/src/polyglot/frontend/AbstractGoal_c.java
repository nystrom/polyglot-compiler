package polyglot.frontend;

import java.util.*;

import polyglot.main.Report;
import polyglot.types.Futures;
import polyglot.types.Ref_c;
import polyglot.util.StringUtil;

public abstract class AbstractGoal_c extends Ref_c<Goal.Status> implements Goal {
	String name;
	public List<Goal> prereqs;
//	public static Status stat;

	public Goal intern(Scheduler scheduler) {
		return scheduler.intern(this);
	}

	protected AbstractGoal_c() {
		super(Status.NEW, false);
		this.name = StringUtil.getShortNameComponent(getClass().getName()
				.replace('$', '.'));
	}

	protected AbstractGoal_c(String name) {
		super(Status.NEW, false);
		this.name = name;
	}
	
//	public void start(){
//		final Goal thisGoal = this;
//		Futures.async(
//				new Runnable(){
//					public void run(){
//						runpass(thisGoal);
//					}
//				}
//			);
//	}
	
	public void runpass(Goal g){
		Globals.Scheduler().runPass(g);
		
	}
	public Goal.Status compute() {
//		System.out.println("Abstract goal Current Thread: " + Thread.currentThread().hashCode() + " Goal = " + this.name);
		AbstractGoal_c goal = this;
		if (Report.should_report(Report.frontend, 2))
			Report.report(2, "Running to goal " + goal);

		LinkedList<Goal> worklist = new LinkedList<Goal>(prereqGoals());

		Set<Goal> prereqs = new LinkedHashSet<Goal>();
		prereqs.addAll(worklist);

		while (!worklist.isEmpty()) {

			Goal g = worklist.removeFirst();
			assert g != this;
			// Check if already run
			if (g.forced())
				continue;

			if (Report.should_report(Report.frontend, 4))
				Report.report(4, "running prereq: " + g + "->" + goal);
			
			Status s = Status.SUCCESS;
			if(!g.hasBeenReached())
				s = g.get();
				
			 
//			System.out.println("Done forcing goal: " + g.name() + " On Job: " + Globals.Scheduler().currentJob
//					+ " in Thread: " + Thread.currentThread().hashCode());

			// Make sure any new prereqs added during the recursion are in the
			// queue.
			List<Goal> listOfPrereqs1 = prereqGoals();
			for (Goal g2 : listOfPrereqs1) {
				if (!prereqs.contains(g2)) {
					prereqs.add(g2);
					worklist.add(g2);
				}
			}

			switch (s) {
			case FAIL:
				update(Status.UNREACHABLE);
				continue;
			case NEW:
			case RUNNING:
			case RUNNING_RECURSIVE:
			case RUNNING_WILL_FAIL:
			case UNREACHABLE:
				update(Status.UNREACHABLE);
				continue;
			case SUCCESS:				
				break;
			case TYPE_INIT_COMPLETED:
				return Status.TYPE_INIT_COMPLETED;
			}
		}

		Status oldStatus = getCached();

		if (forced()) {
			switch (oldStatus) {
			case NEW:
			case FAIL:
			case RUNNING:
			case RUNNING_RECURSIVE:
			case RUNNING_WILL_FAIL:
			case UNREACHABLE:
				return Status.FAIL;
			case SUCCESS:
				return Status.SUCCESS;
			}
		}

		switch (oldStatus) {
		case RUNNING:
			updateSoftly(Status.RUNNING_RECURSIVE);
			break;
		case RUNNING_RECURSIVE:
			// no change
			break;
		case NEW:
			updateSoftly(Status.RUNNING);
			break;
		case RUNNING_WILL_FAIL:
			// no change
			break;
		default:
			break;
		}

		boolean recursive = oldStatus == Status.RUNNING_RECURSIVE;

		if (Report.should_report(Report.frontend, 4))
			Report.report(4, "running goal " + goal);

		if (Report.should_report(Report.frontend, 5)) {
			if (Globals.Scheduler().currentGoal() != null) {
				Report.report(5, "CURRENT = "
						+ Globals.Scheduler().currentGoal());
				Report.report(5, "SPAWN   = " + goal);
			}
		}

		boolean result = false;
			
		result = Globals.Scheduler().runPass(this);
		
		
		if(result && this.name.equalsIgnoreCase("PreTypeCheck")){ //"TypeChecked"
			return Status.TYPE_INIT_COMPLETED;
		}
		
		if (state() == Goal.Status.RUNNING_WILL_FAIL)
			result = false;

		if (result) {
			switch (oldStatus) {
			case RUNNING:
			case RUNNING_RECURSIVE:
				return oldStatus;
			case NEW:
				return Status.SUCCESS;
			default:
				return oldStatus;
			}
		} else {
			switch (oldStatus) {
			case RUNNING:
			case RUNNING_RECURSIVE:
				return Status.RUNNING_WILL_FAIL;
			case NEW:
				return Status.FAIL;
			default:
				return oldStatus;
			}
		}
	}

	public abstract boolean runTask();

	public List<Goal> prereqGoals() {
		if (prereqs == null) {
			return Collections.emptyList();
		} else {
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
		return getCached() == Status.SUCCESS;
	}

	public boolean isReachable() {
		Status state = getCached();
		switch (state) {
		case NEW:
		case RUNNING:
		case RUNNING_RECURSIVE:
		case SUCCESS:
			return true;
		case RUNNING_WILL_FAIL:
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

	public Status state() {
		return getCached();
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

	public boolean isRunning() {
		switch (state()) {
		case RUNNING:
		case RUNNING_RECURSIVE:
		case RUNNING_WILL_FAIL:
			return true;
		default:
			return false;
		}
	}

	public void fail() {
		switch (state()) {
		case SUCCESS:
			assert false;
			break;
		case RUNNING:
		case RUNNING_RECURSIVE:
			updateSoftly(Goal.Status.RUNNING_WILL_FAIL);
			break;
		case NEW:
			update(Goal.Status.UNREACHABLE);
			break;
		case RUNNING_WILL_FAIL:
		case FAIL:
		case UNREACHABLE:
			break;
		}
	}

	protected String stateString() {
		Status state = state();
		switch (state) {
		case NEW:
			return "new";
		case RUNNING:
			return "running";
		case RUNNING_RECURSIVE:
			return "running-recursive";
		case RUNNING_WILL_FAIL:
			return "running-will-fail";
		case SUCCESS:
			return "success";
		case FAIL:
			return "failed";
		case UNREACHABLE:
			return "unreachable";
		case TYPE_INIT_COMPLETED:
			return "type checked";
		}
		return "unknown-goal-state";
	}

	public String toString() {
		return name() + " (" + stateString() + ")";
	}
}
