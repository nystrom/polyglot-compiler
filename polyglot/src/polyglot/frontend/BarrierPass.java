package polyglot.frontend;

import polyglot.main.Report;

/**
 * A <code>BarrierPass</code> is a special pass that ensures that
 * all jobs complete a goal pass before any job continues.
 */
public class BarrierPass extends AbstractPass
{
    Job job;

    public BarrierPass(Pass.ID id, Job job) {
      	super(id);
	this.job = job;
    }

    /** Run all the other jobs with the same parent up to this pass. */
    public boolean run() {
        if (Report.should_report(Report.frontend, 1))
	    Report.report(1, job + " at barrier " + id);
        if (Report.should_report(Report.frontend, 2))
	    Report.report(2, "children of " + job.sourceJob() + " = " + job.sourceJob().children());

        return !job.compiler().errorQueue().hasErrors();
    }
}
