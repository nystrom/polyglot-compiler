package jltools.visit;

import jltools.ast.*;
import jltools.frontend.*;
import jltools.types.*;
import jltools.util.*;

import java.io.*;

public class TranslationVisitor extends NodeVisitor
{
  protected ImportTable it;
  protected Target target;
  protected TypeSystem ts;
  protected ErrorQueue eq;
  protected int outputWidth;

  public TranslationVisitor(ImportTable it,
			    Target target,
			    TypeSystem ts,
			    ErrorQueue eq,
			    int outputWidth)
  {
    this.it = it;
    this.target = target;
    this.ts = ts;
    this.eq = eq;
    this.outputWidth = outputWidth;
  }

  public Node override(Node n)
  {
    if (n instanceof SourceFileNode) {
	SourceFileNode sfn = (SourceFileNode) n;

	try {
	    Writer ofw = target.getOutputWriter(sfn.getPackageName());
	    CodeWriter w = new CodeWriter(ofw, outputWidth);
	    LocalContext c = ts.getLocalContext(it, this);
	    n.translate(c, w);
	    w.flush();
	    System.out.flush();
	    target.closeDestination();
	}
	catch (IOException e) {
	  eq.enqueue(ErrorInfo.IO_ERROR,
		     "I/O error while translating: " + e.getMessage());
	}
    }

    // Never recurse: Node.translate() does that for us!
    return n;
  }
}
