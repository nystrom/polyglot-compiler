/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.main.Report;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.util.StringUtil;
import polyglot.visit.NodeVisitor;

/** A pass which runs a visitor. */
public class VisitorPass extends AbstractPass
{
    protected NodeVisitor v;

    public VisitorPass(Goal goal, Job job) {
	this(goal, job, null);
    }

    public VisitorPass(Goal goal, Job job, NodeVisitor v) {
        super(goal, job, StringUtil.getShortNameComponent(v.getClass().getName()));
        this.v = v;
    }

    public void visitor(NodeVisitor v) {
	this.v = v;
    }

    public NodeVisitor visitor() {
	return v;
    }
  
    public boolean run() {
	Node ast = job().ast();
	if (ast == null) {
	    throw new InternalCompilerError("Null AST for job " + this.job() + ": did the parser run?");
	}

	Map<Node,Node> oldAstMap = job().astMap();
	HashMap<Node,Node> map = new HashMap<Node, Node>(oldAstMap);
	job().setAstMap(map);
	
	try {
	    NodeVisitor v_ = v.begin();

	    if (v_ != null) {
	        ErrorQueue q = job().compiler().errorQueue();
	        int nErrsBefore = q.errorCount();

	        if (Report.should_report(Report.frontend, 3))
	            Report.report(3, "Running " + v_ + " on " + ast);

	        ast = ast.visit(v_);
	        v_.finish(ast);

	        int nErrsAfter = q.errorCount();

	        if (nErrsBefore != nErrsAfter) {
	            // because, if they're equal, no new errors occurred,
	            // so the run was successful.
	            return false;
	        }

	        return true;
	    }

	    return false;
	}
	finally {
	    job().ast(ast);
	    job().setAstMap(oldAstMap);
	}
    }
    
    public String name() {
        if (v != null)
            return v.toString();
        else 
            return super.name();
    }
}
