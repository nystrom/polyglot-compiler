package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.util.*;
import jltools.types.*;
import jltools.visit.*;
import java.util.*;

/**
 * A <code>SourceCollection</code> represents a collection of source files.
 */
public class SourceCollection_c extends Node_c implements SourceCollection
{
    protected List sources;

    public SourceCollection_c(Del ext, Position pos, List sources) {
	super(ext, pos);
	this.sources = TypedList.copyAndCheck(sources, SourceFile.class, true);
    }

    public String toString() {
	return sources.toString();
    }

    /** Get the source files. */
    public List sources() {
	return this.sources;
    }

    /** Set the statements of the block. */
    public SourceCollection sources(List sources) {
	SourceCollection_c n = (SourceCollection_c) copy();
	n.sources = TypedList.copyAndCheck(sources, SourceFile.class, true);
	return n;
    }

    /** Reconstruct the collection. */
    protected SourceCollection_c reconstruct(List sources) {
	if (! CollectionUtil.equals(sources, this.sources)) {
	    SourceCollection_c n = (SourceCollection_c) copy();
	    n.sources = TypedList.copyAndCheck(sources, Stmt.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the block. */
    public Node visitChildren(NodeVisitor v) {
        List sources = visitList(this.sources, v);
	return reconstruct(sources);
    }

    /** Write the source files to an output file. */
    public void translate(CodeWriter w, Translator tr) {
        for (Iterator i = sources.iterator(); i.hasNext(); ) {
            SourceFile s = (SourceFile) i.next();
            s.del().translate(w, tr);
        }
    }
}
