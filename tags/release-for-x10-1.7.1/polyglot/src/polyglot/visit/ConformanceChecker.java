/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

/** Visitor which performs type checking on the AST. */
public class ConformanceChecker extends ContextVisitor
{
    public ConformanceChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    protected Node leaveCall(Node old, final Node n, NodeVisitor v) throws SemanticException {
        ContextVisitor cc = (ContextVisitor) v;
        return n.del().conformanceCheck(cc);
    }   
}
