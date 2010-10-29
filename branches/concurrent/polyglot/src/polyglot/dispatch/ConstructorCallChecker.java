/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.dispatch;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.ConstructorCall;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.ConstructorDef;
import polyglot.types.Context;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;

/** Visitor that checks that constructor calls are not recursive. */
public class ConstructorCallChecker extends Visitor {
    public ConstructorCallChecker(Job job, TypeSystem ts, NodeFactory nf) {}

    protected Map<ConstructorDef, ConstructorDef> constructorInvocations = new HashMap<ConstructorDef, ConstructorDef>();

    public Node visit(Node n) {
	return acceptChildren(n);
    }

    public Node visit(ConstructorCall cc) {
	if (cc.kind() == ConstructorCall.THIS) {
	    // the constructor calls another constructor in the same class
	    Context ctxt = cc.context();

	    if (ctxt.currentCode() instanceof ConstructorDef) {
		ConstructorDef srcCI = (ConstructorDef) ctxt.currentCode();
		ConstructorDef destCI = cc.constructorInstance().def();

		constructorInvocations.put(srcCI, destCI);

		while (destCI != null) {
		    destCI = (ConstructorDef) constructorInvocations.get(destCI);
		    if (destCI != null && srcCI == destCI) {
			// loop in the constructor invocations!
			Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Recursive constructor invocation.", cc.position());
			break;
		    }
		}
	    }
	    else {
		Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Constructor call occurring in a non-constructor.", cc.position());
	    }
	}
	return acceptChildren(cc);
    }
}
