package jltools.frontend;

import jltools.ast.*;
import jltools.types.*;
import jltools.util.*;
import jltools.visit.*;
import jltools.main.Options;
import jltools.main.Report;

import java.io.*;
import java.util.*;

/**
 * This is the main entry point for the compiler. It contains a work list that
 * contains entries for all classes that must be compiled (or otherwise worked
 * on).
 */
public class Compiler
{
    /** Command-line options */
    private Options options;

    /**
     * The system class resolver.  The class resolver contains a map from class
     * names to ClassTypes.  A Job looks up classes first in its import table
     * and then in the system resolver.  The system resolver first tries to
     * find the class in parsed class resolver
     */
    private Resolver systemResolver;

    /**
     * The parsed class resolver.  This resolver contains classes parsed from
     * source files.
     */
    private TableResolver parsedResolver;

    /**
     * The loaded class resolver.  This resolver automatically loads types from
     * class files and from source files not mentioned on the command line.
     */
    private LoadedClassResolver loadedResolver;

    /** The type system. */
    protected TypeSystem type_system;

    /** The node factory creates AST node objects. */
    protected NodeFactory node_factory;

    /**
     * The source loader is responsible for loading source files from the
     * source path.
     */
    protected SourceLoader source_loader;

    /**
     * The target factory is responsible for naming and opening output files
     * given a package name and a class or source file name.
     */
    protected TargetFactory target_factory;

    /** The error queue handles outputting error messages. */
    ErrorQueue eq;

    /** A map from sources to source jobs. */
    Map jobs;

    /** A list of all the source jobs. */
    LinkedList worklist;

    /** The currently running job, or null. */
    Job currentJob;

    /**
     * The output files generated by the compiler.  This is used to to call the
     * post-compiler (e.g., javac).
     */
    private Collection outputFiles = new HashSet();

    /**
     * Initialize the compiler.
     *
     * @param options Contains jltools options
     */
    public Compiler(Options options_) {
	options = options_;

	// These must be done after the extension is initialized.
	source_loader = options.extension.sourceLoader();
	target_factory = options.extension.targetFactory();

	type_system = options.extension.typeSystem();
	node_factory = options.extension.nodeFactory();

	eq = new ErrorQueue(System.err, options.error_count,
		            options.extension.compilerName());

	// Create the compiler and set up the resolvers.
	parsedResolver = new TableResolver();
	loadedResolver = new LoadedClassResolver(this, type_system,
						 options.no_source_check);

	CompoundResolver compoundResolver =
	    new CompoundResolver(parsedResolver, loadedResolver);

	systemResolver = new CachingResolver(compoundResolver);

	/* Other setup. */
	jobs = new HashMap();
        worklist = new LinkedList();
        currentJob = null;

	// This must be done last.
	options.extension.initCompiler(this);
    }

    /** Return a set of output filenames resulting from a compilation. */
    public Collection outputFiles() {
	return outputFiles;
    }

    /**
     * Compile a source file until the types in the source file are
     * constructed.
     */
    public boolean readSource(Source source) throws IOException {
	Job job = jobForSource(source);

        // Run the new job up to its owner's (the current job's) barrier.
        Pass.ID barrier;
        
        if (currentJob != null) {
            if (currentJob.lastBarrier() == null) {
                throw new InternalCompilerError("Job which has not reached a " +
                                                "barrier cannot read another " +
                                                "source file.");
            }

            barrier = currentJob.lastBarrier().id();
        }
        else {
            barrier = Pass.BUILD_TYPES_ALL;
        }

        return runToPass(job, barrier);
    }

    /** Compile the source file for a given class to completion. */
    public boolean readClass(String className) throws IOException {
	Source source = source_loader.classSource(className);
	return readSource(source);
    }

    /** Get a job for the source. */
    public SourceJob jobForSource(Source source) {
	SourceJob job = (SourceJob) jobs.get(source);

	if (job == null) {
	    job = options.extension.createJob(currentJob, source);
	    jobs.put(source, job);
            worklist.add(job);
	}

	return job;
    }

    /**
     * Compile all the files listed in the set of strings <code>source</code>.
     * Return true on success. The method <code>outputFiles</code> can be
     * used to obtain the output of the compilation.  This is the main entry
     * point for the compiler, called from main().
     */
    public boolean compile(Collection sources) {
	boolean okay = false;

	try {
	    try {
		for (Iterator i = sources.iterator(); i.hasNext(); ) {
		    String sourceName = (String) i.next();
		    Source source = source_loader.fileSource(sourceName);
		    Job u = jobForSource(source);
		}

		okay = finish();
	    }
	    catch (FileNotFoundException e) {
		eq.enqueue(ErrorInfo.IO_ERROR,
		    "Cannot find source file \"" + e.getMessage() + "\".");
	    }
	    catch (IOException e) {
		eq.enqueue(ErrorInfo.IO_ERROR, e.getMessage());
	    }
	    catch (InternalCompilerError e) {
		e.printStackTrace();
		eq.enqueue(ErrorInfo.INTERNAL_ERROR, e.message(), e.position());
	    }
	}
	catch (ErrorLimitError e) {
	}

	eq.flush();
	return okay;
    }

    /** Run a job until the <code>goal</code> pass completes. */
    public boolean runToPass(Job job, Pass.ID id) {
	report(1, "Running " + job + " to pass named " + id);

        if (job.completed(id)) {
            return true;
        }

        Pass pass = job.passByID(id);

        return runToPass(job, pass);
    }

    public boolean runToPass(Job job, Pass goal) {
	report(1, "Running " + job + " to pass " + goal);

        boolean okay = job.status();

	while (! job.pendingPasses().isEmpty()) {
	    Pass pass = (Pass) job.pendingPasses().get(0);

	    report(2, "Trying to run pass " + pass);

            if (job.isRunning()) {
                // We're currently running.  We can't reach the goal.
                throw new InternalCompilerError(job + " cannot reach pass " +
                                                pass);
            }

            long start_time = System.currentTimeMillis();

            if (okay) {
                Job oldCurrentJob = currentJob;
                currentJob = job;

                job.setIsRunning(true);
                okay &= pass.run();
                job.setIsRunning(false);

                currentJob = oldCurrentJob;
            }

            job.finishPass(pass, okay);

            report(2, "Finished " + pass + " status=" + str(okay));
            reportTime(1, "Finished " + pass + " status=" + str(okay) +
                    " time=" + (System.currentTimeMillis() - start_time));

            if (pass == goal) {
                break;
            }
        }

	report(1, "Pass " + goal + " " + str(okay));

	return okay;
    }

    private static String str(boolean okay) {
        if (okay) {
            return "done";
        }
        else {
            return "failed";
        }
    }

    public boolean runNextPass(Job job) {
	if (! job.pendingPasses().isEmpty()) {
	    Pass pass = (Pass) job.pendingPasses().get(0);
	    return runToPass(job, pass);
	}

	return true;
    }

    public boolean runAllPasses(Job job) {
      	List passes = job.pendingPasses();

	if (! passes.isEmpty()) {
	    // Run to the last pass.
	    Pass pass = (Pass) passes.get(passes.size()-1);
	    return runToPass(job, pass);
	}

	return true;
    }

    /** Run all jobs in the source work group to completion. */
    protected boolean finish() {
	boolean okay = true;

        // Run the jobs breadth-first rather than depth first to ensure
        // inter-dependent jobs in the worklist are kept in sync.
        while (okay && ! worklist.isEmpty()) {
            SourceJob job = (SourceJob) worklist.removeFirst();
            report(1, "Running job " + job);
            okay &= runNextPass(job);

            if (! job.completed()) {
                worklist.add(job);
            }
        }

	report(1, "Finished all passes -- " + (okay ? "okay" : "failed"));

	return okay;
    }

    /** Should fully qualified class names be used in the output? */
    public boolean useFullyQualifiedNames() {
	return options.fully_qualified_names;
    }

    /** Get the compiler's source loader */
    public SourceLoader sourceLoader() {
	return source_loader;
    }

    /** Get the compiler's target factory */
    public TargetFactory targetFactory() {
	return target_factory;
    }

    /** Get the compiler's node extension factory */
    public NodeFactory nodeFactory() {
	return node_factory;
    }

    /** Get the compiler's type system */
    public TypeSystem typeSystem() {
	return type_system;
    }

    /** Get information about the language extension being compiled. */
    public ExtensionInfo extensionInfo() {
	return options.extension;
    }

    /** Get the compiler's system resolver */
    public Resolver systemResolver() {
	return systemResolver;
    }

    /** Get the compiler's parsed-file resolver */
    public LoadedClassResolver loadedResolver() {
	return loadedResolver;
    }

    /** Get the compiler's parsed-file resolver */
    public TableResolver parsedResolver() {
	return parsedResolver;
    }

    /** Maximum number of characters on each line of output */
    public int outputWidth() {
	return options.output_width;
    }

    /** Should class info be serialized into the output? */
    public boolean serializeClassInfo() {
	return options.serialize_type_info;
    }

    /** Should the AST be dumped? */
    public boolean dumpAst() {
        return options.dump_ast;
    }

    /** Get the compiler's error queue. */
    public ErrorQueue errorQueue() {
	return eq;
    }

    private static Collection topics = new ArrayList(1);
    private static Collection timeTopics = new ArrayList(1);

    static {
	topics.add("frontend");
	timeTopics.add("time");
    }

    /** Debug reporting for the frontend. */
    public static void report(int level, String msg) {
	Report.report(topics, level, msg);
    }

    /** Reports the time taken by every pass. */
    public static void reportTime(int level, String msg) {
	Report.report(timeTopics, level, msg);
    }

    static {
      // FIXME: if we get an io error (due to too many files open, for example)
      // it will throw an exception. but, we won't be able to do anything with
      // it since the exception handlers will want to load
      // jltools.util.CodeWriter and jltools.util.ErrorInfo to print and
      // enqueue the error; but the classes must be in memory since the io
      // can't open any files; thus, we force the classloader to load the class
      // file.
      try {
	ClassLoader loader = Compiler.class.getClassLoader();
	loader.loadClass("jltools.util.CodeWriter");
	loader.loadClass("jltools.util.ErrorInfo");
      }
      catch (ClassNotFoundException e) {
	throw new InternalCompilerError(e.getMessage());
      }
    }
}