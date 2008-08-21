/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.frontend;

import java.io.IOException;
import java.io.Reader;

import polyglot.ast.Node;
import polyglot.main.Report;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.Position;

/**
 * A pass which runs a parser.  After parsing it stores the AST in the Job.
 * so it can be accessed by later passes.
 */
public class ParserGoal extends SourceGoal_c
{
    protected Compiler compiler;

    public ParserGoal(Compiler compiler, Job job) {
        super("Parser", job);
	this.compiler = compiler;
    }

    public boolean runTask() {
	ErrorQueue eq = compiler.errorQueue();
        
	FileSource source = (FileSource) job().source();

	try {
	    if (Report.should_report("parser", 1))
		Report.report(1, "" + source);
	    
	    Reader reader = source.open();
            
            Parser p = job().extensionInfo().parser(reader, source, eq);

	    if (Report.should_report(Report.frontend, 2))
		Report.report(2, "Using parser " + p);
	    Node ast = p.parse();

	    source.close();

	    if (ast != null) {
		job().ast(ast);
		return true;
	    }

	    return false;
	}
	catch (IOException e) {
	    eq.enqueue(ErrorInfo.IO_ERROR, e.getMessage(),
                new Position(job().source().path(),
                             job().source().name(), 1, 1, 1, 1));

            return false;
	}
    }

    public String toString() {
	return super.toString() + "(" + job().source() + ")";
    }
}
