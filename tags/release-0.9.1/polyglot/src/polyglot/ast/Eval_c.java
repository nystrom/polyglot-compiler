package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;

/**
 * An <code>Eval</code> is a wrapper for an expression in the context of
 * a statement.
 */
public class Eval_c extends Stmt_c implements Eval
{
    protected Expr expr;

    public Eval_c(Del ext, Position pos, Expr expr) {
	super(ext, pos);
	this.expr = expr;
    }

    /** Get the expression of the statement. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression of the statement. */
    public Eval expr(Expr expr) {
	Eval_c n = (Eval_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the statement. */
    protected Eval_c reconstruct(Expr expr) {
	if (expr != this.expr) {
	    Eval_c n = (Eval_c) copy();
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            return ts.Void();
        }

        return child.type();
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(expr);
    }

    public String toString() {
	return expr.toString();
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	boolean semi = tr.appendSemicolon(true);

	tr.print(expr, w);

	if (semi) {
	    w.write(";");
	}

	tr.appendSemicolon(semi);
    }
}