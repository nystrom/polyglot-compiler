package polyglot.ext.jl5;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.ext.jl5.visit.JL5AmbiguityRemover;
import polyglot.frontend.Goal;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.VisitorGoal;
import polyglot.types.TypeSystem;

public class JL5Scheduler extends JLScheduler {

	public JL5Scheduler(polyglot.frontend.ExtensionInfo extInfo) {
		super(extInfo);
	}

	public List<Goal> goals(Job job) {
		List<Goal> goals = new ArrayList<Goal>();

		goals.add(Parsed(job));
		goals.add(TypesInitialized(job));
		goals.add(ImportTableInitialized(job));

		goals.add(PreTypeCheck(job));
		goals.add(TypesInitializedForCommandLineBarrier());
		goals.add(TypeChecked(job));
		goals.add(GenericTypeHandled(job));
		goals.add(ReassembleAST(job));

		goals.add(ConformanceChecked(job));
		goals.add(ReachabilityChecked(job));
		goals.add(ExceptionsChecked(job));
		goals.add(ExitPathsChecked(job));
		goals.add(InitializationsChecked(job));
		goals.add(ConstructorCallsChecked(job));
		goals.add(ForwardReferencesChecked(job));
		goals.add(ApplicationChecked(job));
		goals.add(Serialized(job));
		goals.add(CodeGenerated(job));
		goals.add(End(job));
		// CHECK original pass scheduling info
		//            public static final polyglot.frontend.Pass.ID TYPE_CHECK_ALL = new polyglot.frontend.Pass.ID("type-check-all");
		//            public static final polyglot.frontend.Pass.ID APPLICATION_CHECK = new polyglot.frontend.Pass.ID("application-check");
		//            public static final polyglot.frontend.Pass.ID GENERIC_TYPE_HANDLER = new polyglot.frontend.Pass.ID("generic-type-handler");
		//            public static final polyglot.frontend.Pass.ID TYPE_VARS_ALL = new polyglot.frontend.Pass.ID("type-vars-all");
		//          afterPass(passes, Pass.ADD_MEMBERS_ALL, new VisitorPass(GENERIC_TYPE_HANDLER, job, new JL5AmbiguityRemover(job, ts, nf, JL5AmbiguityRemover.TYPE_VARS)));
		//          afterPass(passes, GENERIC_TYPE_HANDLER, new GlobalBarrierPass(TYPE_VARS_ALL, job));
		//          afterPass(passes, Pass.TYPE_CHECK, new BarrierPass(TYPE_CHECK_ALL, job));
		//          beforePass(passes, Pass.REACH_CHECK, new VisitorPass(APPLICATION_CHECK, job, new ApplicationChecker(job, ts, nf)));

		return goals;
	}

	public Goal GenericTypeHandled(Job job) {
		TypeSystem ts = job.extensionInfo().typeSystem();
		NodeFactory nf = job.extensionInfo().nodeFactory();
		return new VisitorGoal("GenericTypeHandled", job, new JL5AmbiguityRemover(job, ts, nf)).intern(this);
	}

	public Goal ApplicationChecked(Job job) {
		TypeSystem ts = job.extensionInfo().typeSystem();
		NodeFactory nf = job.extensionInfo().nodeFactory();
		return new VisitorGoal("ApplicationChecked", job, new ApplicationChecker(job, ts, nf)).intern(this);
	}
}