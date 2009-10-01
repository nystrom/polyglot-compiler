package ibex.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;


public class RhsLit_c extends RhsExpr_c implements RhsLit {

    private Expr lit;

    public RhsLit_c(Position pos, Expr lit) {
        super(pos);
        this.lit = lit;
    }

    public Expr lit() {
        return lit;
    }

    public RhsLit lit(Expr lit) {
        RhsLit_c n = (RhsLit_c) copy();
        n.lit = lit;
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr lit = (Expr) visitChild(this.lit, v);
        return lit(lit);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return type(lit.type());
    }

    public Term firstChild() {
        return lit;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(lit, this, EXIT);
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(lit, false, w, tr);
    }
    
    public String toString() {
        return lit.toString();
    }


}
