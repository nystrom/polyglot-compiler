package ibex.ast;

import polyglot.ast.Block;
import polyglot.ast.Formal;

public interface RhsAction extends RhsExpr {

    RhsExpr item();
    RhsAction item(RhsExpr item);

    Formal formal();
    RhsAction formal(Formal formal);
    
    Block body();
    RhsAction body(Block stmt);

}
