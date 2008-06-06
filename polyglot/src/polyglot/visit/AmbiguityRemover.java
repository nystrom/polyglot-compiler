/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.Arrays;
import java.util.Collection;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.*;

/**
 * A visitor which traverses the AST and remove ambiguities found in fields,
 * method signatures and the code itself.
 */
public class AmbiguityRemover extends ContextVisitor
{
	TypeChecker tc;
	
    public AmbiguityRemover(TypeChecker tc) {
        super(tc.job(), tc.typeSystem(), tc.nodeFactory());
        this.tc = tc;
        this.context = tc.context();
    }
    
    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n + " (" + n.getClass().getName() + ")");

        Node m = n.del().disambiguate((AmbiguityRemover) v);

        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::leave " + n + " -> " + m + (m != null ? (" (" + m.getClass().getName() + ")") : ""));
        
        return m;
    }

	public TypeChecker typeChecker() {
		return tc;
	}
}
