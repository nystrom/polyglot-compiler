package jltools.frontend;

import java.io.*;
import jltools.ast.*;
import jltools.util.*;

/**
 * A pass which runs a parser.  After parsing it stores the AST in the Job.
 * so it can be accessed by later passes.
 */
public class ParserPass extends AbstractPass
{
    SourceJob job;
    Compiler compiler;

    public ParserPass(Pass.ID id, Compiler compiler, SourceJob job) {
        super(id);
	this.compiler = compiler;
	this.job = job;
    }

    public boolean run() {
	ErrorQueue eq = compiler.errorQueue();
	Source source = job.source();

	try {
	    Reader reader = source.open();

	    Parser p = compiler.extensionInfo().parser(reader, source, eq);

	    jltools.frontend.Compiler.report(2, "Using parser " + p);

	    Node ast = p.parse();

	    source.close();

	    if (ast != null) {
		job.ast(ast);
		return true;
	    }

	    return false;
	}
	catch (IOException e) {
	    eq.enqueue(ErrorInfo.IO_ERROR, e.getMessage());
	    return false;
	}
    }

    public String toString() {
	return id + "(" + job.source() + ")";
    }
}