/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006-2008 IBM Corporation
 * 
 */

package polyglot.visit;

import java.io.InputStreamReader;
import java.util.Iterator;

import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.interp.Evaluator;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.QName;
import polyglot.util.*;

/** The post compiler pass runs after all jobs complete.  It invokes the post-compiler on the output files stored in compiler.outputFiles(). */
public class Interpreted extends AllBarrierGoal
{
    ExtensionInfo ext;

    /**
     * Create a Translator.  The output of the visitor is a collection of files
     * whose names are added to the collection <code>outputFiles</code>.
     */
    public Interpreted(ExtensionInfo ext) {
	super("Interpreted",ext.scheduler());
	this.ext = ext;
    }

    public Goal prereqForJob(Job job) {
	if (scheduler.shouldCompile(job)) {
	    return scheduler.End(job);
	}
	else {
	    return new SourceGoal_c("DummyEnd", job) {
		public boolean runTask() { return true; }
	    }.intern(scheduler);
	}
    }

    public final static String postcompile = "interpreter";

    public boolean runTask() {
	Compiler compiler = ext.compiler();

	if (Report.should_report(postcompile, 2))
	    Report.report(2, "Output files: " + compiler.outputFiles());

	long start_time = System.currentTimeMillis();

	try {
	    Evaluator e = Evaluator.make();
	    String main = Globals.Options().main_class;
	    e.eval(QName.make(main), new String[0]);
	    return true;
	}
	finally {
	    if (Report.should_report("time", 1))
		Report.report(1, "Finished compiling Java output files. time=" +
		              (System.currentTimeMillis() - start_time));
	}
    }
}
