/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006-2008 IBM Corporation
 * 
 */

package polyglot.visit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

import polyglot.bytecode.rep.IClassGen;
import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.interp.BytecodeCache;
import polyglot.interp.BytecodeCache.CEntry;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.types.QName;
import polyglot.util.*;

/** The post compiler pass runs after all jobs complete.  It invokes the post-compiler on the output files stored in compiler.outputFiles(). */
public class OutputBytecode extends AllBarrierGoal
{
    ExtensionInfo ext;

    /**
     * Create a Translator.  The output of the visitor is a collection of files
     * whose names are added to the collection <code>outputFiles</code>.
     */
    public OutputBytecode(ExtensionInfo ext) {
	super("OutputBytecode",ext.scheduler());
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

    public boolean runTask() {
	File output_directory = Globals.Options().output_directory;
	
	if (! output_directory.exists()) {
	    ext.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR, "Output directory " + output_directory.getPath() + " not found.");
	    return false;
	}
	
	BytecodeCache bc = ext.bytecodeCache();

	for (CEntry e : bc.entries()) {
	    QName qn = e.fullName;
	    Source s = e.source;
	    byte[] b = e.bytes;
	    String cn = e.className;
	    File f;
	    if (false && qn != null && s != null)
		f = ext.targetFactory().outputFile(qn.qualifier(), qn.name(), s);
	    else 
		f = new File(Globals.Options().output_directory, cn.replace('.', File.separatorChar) + ".class");
	    try {
		if (! f.getParentFile().exists()) {
		    // Create the parent directory.  Don't bother checking if successful, the file open below will fail if not.
		    f.getParentFile().mkdirs();
		}
		FileOutputStream out = new FileOutputStream(f);
		out.write(b);
		out.close();
	    }
	    catch (IOException ex) {
		ext.compiler().errorQueue().enqueue(ErrorInfo.IO_ERROR, ex.getMessage());
	    }
	}

	return true;
    }

}
