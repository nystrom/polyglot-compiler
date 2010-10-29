package polyglot.dispatch;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class ConstantFolder {
    NodeFactory nf;
    TypeSystem ts;
    Job job;
    
    ConstantFolder(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    public Node visit(Node n) {
	return n.acceptChildren(this);
    }

    public Node visit(Binary n) {
	// Don't fold String +. Strings are often broken up for better formatting.
	if (n.operator() == Binary.ADD) {
	    ConstantValueVisitor cvv = new ConstantValueVisitor(job, ts, nf);

	    Object l = n.left().accept(cvv);
	    Object r = n.right().accept(cvv);
	    
	    if (l instanceof String && r instanceof String) {
		return n;
	    }
	}

	return visit((Expr) n);
    }

    public Node visit(Expr n) {
	ConstantValueVisitor cvv = new ConstantValueVisitor(job, ts, nf);
	Object v = n.accept(cvv);
	
	if (v == ConstantValueVisitor.NOT_CONSTANT) {
	    return n.acceptChildren(this);
	}
    
	Position pos = n.position();

	if (v == null) {
	    return nf.NullLit(pos).type(ts.Null());
	}
	if (v instanceof String) {
	    return nf.StringLit(pos, (String) v).type(ts.String());
	}
	if (v instanceof Boolean) {
	    return nf.BooleanLit(pos, (Boolean) v).type(ts.Boolean());
	}
	if (v instanceof Double) {
	    return nf.FloatLit(pos, FloatLit.DOUBLE, (Double) v).type(ts.Double());
	}
	if (v instanceof Float) {
	    return nf.FloatLit(pos, FloatLit.FLOAT, (Float) v).type(ts.Float());
	}
	if (v instanceof Long) {
	    return nf.IntLit(pos, IntLit.LONG, (Long) v).type(ts.Long());
	}
	if (v instanceof Integer) {
	    return nf.IntLit(pos, IntLit.INT, (Integer) v).type(ts.Int());
	}
	if (v instanceof Character) {
	    return nf.CharLit(pos, (Character) v).type(ts.Char());
	}

	return n;
    }
}
