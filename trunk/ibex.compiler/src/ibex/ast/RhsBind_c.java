package ibex.ast;

import java.util.List;

import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsBind_c extends RhsExpr_c implements RhsBind {

    private LocalDecl decl;

    public RhsBind_c(Position pos, LocalDecl decl) {
        super(pos);
        this.decl = decl;
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
        TypeSystem ts = tc.typeSystem();
        return type(ts.Void());
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
