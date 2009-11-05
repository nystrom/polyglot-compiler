package ibex.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

public abstract class RhsBinary_c extends RhsExpr_c {
    RhsExpr left;
    RhsExpr right;

    public RhsBinary_c(Position pos, RhsExpr c1, RhsExpr c2) {
        super(pos);
        this.left = c1;
        this.right = c2;
    }

    public RhsExpr left() { return left; }
    public RhsExpr left(RhsExpr left) {
        RhsBinary_c n = (RhsBinary_c) copy();
        n.left = left;
        return n;
    }

    public RhsExpr right() { return right; }
    public RhsExpr right(RhsExpr right) {
        RhsBinary_c n = (RhsBinary_c) copy();
        n.right = right;
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        RhsExpr c1 = (RhsExpr) visitChild(this.left, v);
        RhsExpr c2 = (RhsExpr) visitChild(this.right, v);
        RhsBinary_c b = this;
        b = (RhsBinary_c) b.left(c1);
        b = (RhsBinary_c) b.right(c2);
        return b;
    }

    public Term firstChild() {
        return left;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(left, right, ENTRY);
        v.visitCFG(right, this, EXIT);
        return succs;
    }

}
