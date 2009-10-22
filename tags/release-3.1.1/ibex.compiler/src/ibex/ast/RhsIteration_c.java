package ibex.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

public abstract class RhsIteration_c extends RhsUnary_c implements RhsIteration {
    
    public RhsIteration_c(Position pos, RhsExpr item) {
        super(pos, item);
    }
    
    public RhsIteration item(RhsExpr item) {
        return (RhsIteration) super.item(item);
    }

}
