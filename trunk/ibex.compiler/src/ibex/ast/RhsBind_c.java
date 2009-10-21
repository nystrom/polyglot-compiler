package ibex.ast;

import java.util.List;

import com.sun.tools.javac.code.Flags;

import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.types.LocalDef;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

public class RhsBind_c extends RhsExpr_c implements RhsBind {

    private LocalDecl decl;
    private boolean synthetic;

    public RhsBind_c(Position pos, LocalDecl decl, boolean synthetic) {
        super(pos);
        this.decl = decl;
        this.synthetic = synthetic;
    }
    
    public boolean synthetic() {
        return synthetic;
    }

    public RhsExpr item() {
        return (RhsExpr) decl().init();
    }
    
    public LocalDecl decl() { return decl; }
    public RhsBind decl(LocalDecl decl) {
        RhsBind_c n = (RhsBind_c) copy();
        n.decl = decl;
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        LocalDecl decl = (LocalDecl) visitChild(this.decl, v);
        return decl(decl);
    }

    @Override
    public Context enterChildScope(Node child, Context c) {
        return c.pushBlock();
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        return type(item().type());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("(");
        boolean x = tr.appendSemicolon(false);
        try {
            print(decl, w, tr);
        }
        finally {
            tr.appendSemicolon(x);
        }
        w.write(")");
    }

    public String toString() {
        String s = decl.toString();
        int n = s.length();
        return "(" + s.substring(0,n-1) + ")";
    }
    
    @Override
    public Term firstChild() {
        return decl;
    }

    @Override
    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(decl, this, EXIT);
        return succs;
    }
}
