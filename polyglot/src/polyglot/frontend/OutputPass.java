/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import polyglot.ast.Node;
import polyglot.util.InternalCompilerError;
import polyglot.visit.Translator;

/** An output pass generates output code from the processed AST. */
public class OutputPass extends AbstractPass
{
    protected Translator translator;

    /**
     * Create a Translator.  The output of the visitor is a collection of files
     * whose names are added to the collection <code>outputFiles</code>.
     */
    public OutputPass(Goal goal, Job job, Translator translator) {
	super(goal, job);
        this.translator = translator;
    }

    public boolean run() {
        Node ast = job().ast();

        if (ast == null) {
            throw new InternalCompilerError("AST is null");
        }

        if (translator.translate(ast)) {
            return true;
        }
        
        return false;
    }
}
