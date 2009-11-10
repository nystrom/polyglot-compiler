package ibex.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

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
    
    public RhsBind item(RhsExpr e) {
        return decl(decl.init(e));
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
    public Node typeCheckOverride(Node parent, ContextVisitor tc) throws SemanticException {
        if (decl().type() instanceof UnknownTypeNode_c) {
            Expr e = (Expr) this.visitChild(item(), tc);
            Type t = e.type();
            if (t.isClass() && t.toClass().isAnonymous())
                if (t.toClass().interfaces().size() > 0)
                    t = t.toClass().interfaces().get(0);
                else
                    t = t.toClass().superClass();
            NodeFactory nf = tc.nodeFactory();
            LocalDecl ld = decl().type(nf.CanonicalTypeNode(decl.type().position(), t));
            ld = (LocalDecl) ld.del().disambiguate(tc).del().typeCheck(tc).del().checkConstants(tc);
            return decl(ld).typeCheck(tc);
        }
        return null;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        return rhs(item().rhs()).type(item().type());
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
