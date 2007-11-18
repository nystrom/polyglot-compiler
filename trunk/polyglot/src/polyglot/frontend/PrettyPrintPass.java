/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import polyglot.ast.Node;
import polyglot.util.CodeWriter;
import polyglot.visit.PrettyPrinter;

/** An output pass generates output code from the processed AST. */
public class PrettyPrintPass extends AbstractPass
{
    protected PrettyPrinter pp;
    protected CodeWriter w;

    /**
     * Create a PrettyPrinter.  The output of the visitor is a collection of files
     * whose names are added to the collection <code>outputFiles</code>.
     */
    public PrettyPrintPass(Goal goal, Job job, CodeWriter w, PrettyPrinter pp) {
	super(goal, job);
        this.pp = pp;
        this.w = w;
    }

    public boolean run() {
        Node ast = this.job().ast();

        if (ast == null) {
            w.write("<<<< null AST >>>>");
        }
        else {
            pp.printAst(ast, w);
        }

        return true;
    }
}
