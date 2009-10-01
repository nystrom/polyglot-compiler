package ibex.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

public abstract class RhsIteration_c extends RhsExpr_c implements RhsIteration {

    RhsExpr item;
    
    public RhsIteration_c(Position pos, RhsExpr item) {
        super(pos);
        this.item = item;
    }
    
    public RhsExpr item() { return item; }
    public RhsIteration item(RhsExpr item) {
        RhsIteration_c n = (RhsIteration_c) copy();
        n.item = item;
        return n;
    }

    public Node visitChildren(NodeVisitor v) {
        RhsExpr item = (RhsExpr) visitChild(this.item, v);
        return item(item);
    }

    public Term firstChild() {
        return item;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(item, this, EXIT);
        return succs;
    }


}
