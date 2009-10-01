package ibex.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public abstract class RhsLookahead_c extends RhsExpr_c implements RhsLookahead {
    protected RhsExpr item;

    public RhsLookahead_c(Position pos, RhsExpr item) {
        super(pos);
        this.item = item;
    }
    
    public RhsExpr item() { return item; }
    public RhsLookahead item(RhsExpr item) {
        RhsLookahead_c n = (RhsLookahead_c) copy();
        n.item = item;
        return n;
    }

    public Node visitChildren(NodeVisitor v) {
        RhsExpr item = (RhsExpr) visitChild(this.item, v);
        return item(item);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return type(ts.Null());
    }

    public Term firstChild() {
        return item;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(item, this, EXIT);
        return succs;
    }


}
