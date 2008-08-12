package polyglot.frontend;

import java.util.*;

import polyglot.main.Report;
import polyglot.types.LazyRef_c;
import polyglot.util.StringUtil;

public abstract class AbstractGoal_c extends LazyRef_c<Goal.Status> implements Goal {
	String name;
	public List<Goal> prereqs;

	public Goal intern(Scheduler scheduler) {
		return scheduler.intern(this);
	}

	protected AbstractGoal_c() {
		super(Status.NEW);
		this.name = StringUtil.getShortNameComponent(getClass().getName().replace('$', '.'));
		setResolver(this);
	}

	protected AbstractGoal_c(String name) {
		super(Status.NEW);
		this.name = name;
		setResolver(this);
	}

	public void run() {
		AbstractGoal_c goal = this;
		if (Report.should_report(Report.frontend, 2))
			Report.report(2, "Running to goal " + goal);

		for (int i = 0; i < prereqs().size(); i++) {
			Goal g = prereqs().get(i);
			
			if (g.state() == Goal.Status.RUNNING || g.state() == Goal.Status.RUNNING_RECURSIVE)
			    Report.report(4, "running prereq: " + g + "->" + goal);
			    
			if (Report.should_report(Report.frontend, 4))
				Report.report(4, "running prereq: " + g + "->" + goal);

			Status s = g.get();
			switch (s) {
			case NEW:
			case FAIL:
			case RUNNING:
			case RUNNING_RECURSIVE:
			case UNREACHABLE:
				update(Status.UNREACHABLE);
				continue;
			case SUCCESS:
				break;
			}
		}
		

		if (getCached() == Status.RUNNING || getCached() == Status.RUNNING_RECURSIVE)
			updateCache(Status.RUNNING_RECURSIVE);
		else if (getCached() == Status.NEW)
			updateCache(Status.RUNNING);
		else
		        return;
		
		if (Report.should_report(Report.frontend, 4))
			Report.report(4, "running goal " + goal);

		if (Report.should_report(Report.frontend, 5)) {
			if (Globals.Scheduler().currentGoal() != null) {
				Report.report(5, "CURRENT = " + Globals.Scheduler().currentGoal());
				Report.report(5, "SPAWN   = " + goal);
			}
		}

		boolean result = false;
		try {
			result = Globals.Scheduler().runPass(this);
		}
		catch (CyclicDependencyException e) {
		}

		if (result)
			update(Status.SUCCESS);
		else
			update(Status.FAIL);
	}

	public abstract boolean runTask();

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

	protected String stateString() {
		Status state = state();
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
