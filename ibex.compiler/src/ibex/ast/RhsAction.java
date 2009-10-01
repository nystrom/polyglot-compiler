package ibex.ast;

import polyglot.ast.Block;

public interface RhsAction extends RhsExpr {

    RhsExpr item();
    RhsAction item(RhsExpr item);
    
    Block body();
    RhsAction body(Block stmt);

}
