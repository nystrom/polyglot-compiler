package polyglot.dispatch;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.frontend.Compiler;
import polyglot.frontend.Globals;
import polyglot.types.Flags;
import polyglot.util.*;

public class ASTDumper extends Visitor {

    CodeWriter w;

    public ASTDumper(CodeWriter w) {
	this.w = w;
    }

    public ASTDumper(OutputStream os) {
	this.w = Compiler.createCodeWriter(os);
    }

    public ASTDumper(Writer w) {
	this.w = Compiler.createCodeWriter(w);
    }

    public ASTDumper() {
	this(System.out);
    }

    @Override
    public Node accept(Node n, Object... args) {
	return super.accept(n, args);
    }

    /**
     * Print an AST node using the given code writer. The code writer is flushed
     * by this method.
     */
    public void dumpAst(Node ast) {
	if (ast != null) {
	    accept(ast);
	}

	try {
	    w.flush();
	}
	catch (IOException e) {
	}
    }

    V v = new V();

    public Node visit(Node_c n) {
	w.write("(");
	n.accept(v);
	w.allowBreak(4);
	w.begin(0);

	acceptChildren(n);

	w.end();
	w.write(")");
	w.allowBreak(0);
	return n;
    }

    class V extends Visitor {
	public Node visit(Node_c n) {
	    w.write(StringUtil.getShortNameComponent(n.getClass().getName()));

	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(position " + (n.position() != null ? n.position().toString() : "UNKNOWN") + ")");
	    w.end();

	    return n;
	}

	public Node visit(Expr_c n) {
	    visit((Node_c) n);

	    if (n.typeRef() != null) {
		w.allowBreak(4, " ");
		w.begin(0);
		w.write("(type " + n.typeRef() + ")");
		w.end();
	    }

	    return n;
	}
    }
}
