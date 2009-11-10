package ibex.ast;

import ibex.ast.RhsAnyChar_c.RDummy_c;
import ibex.types.IbexTypeSystem;
import ibex.types.Rhs;

import java.util.List;

import polyglot.ast.Expr_c;
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
import polyglot.visit.TypeBuilder;

public abstract class RhsExpr_c extends Expr_c implements RhsExpr {

    boolean isRegular;
    Rhs rhs;
    
    public RhsExpr_c(Position pos) {
        super(pos);
    }

    public boolean isRegular() {
        return isRegular;
    }
    
    public RhsExpr isRegular(boolean f) {
        RhsExpr_c n = (RhsExpr_c) copy();
        n.isRegular = f;
        return n;
    }
    
    public Rhs rhs() {
        return rhs;
    }
    
    public RhsExpr rhs(Rhs rhs) {
        RhsExpr_c n = (RhsExpr_c) copy();
        n.rhs = rhs;
        return n;
    }
    
    abstract public Node visitChildren(NodeVisitor v);

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        RhsExpr n = (RhsExpr) super.buildTypes(tb);
        IbexTypeSystem ts = (IbexTypeSystem) tb.typeSystem();
        Position pos = n.position();
        return n.rhs(new RDummy_c(ts, pos, ts.unknownType(pos)));
    }
    abstract public void prettyPrint(CodeWriter w, PrettyPrinter tr);

    abstract public String toString();

    abstract public List<Term> acceptCFG(CFGBuilder v, List<Term> succs);

    abstract public Term firstChild();

    abstract public Node typeCheck(ContextVisitor tc) throws SemanticException;
}
