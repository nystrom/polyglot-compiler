package ibex.ast;

import ibex.types.ActionDef;
import polyglot.ast.Block;
import polyglot.ast.Formal;

public interface RhsAction extends RhsExpr {
    ActionDef actionDef();
    RhsAction actionDef(ActionDef ad);
    
    RhsExpr item();
    RhsAction item(RhsExpr item);

    Formal formal();
    RhsAction formal(Formal formal);
    
    Block body();
    RhsAction body(Block stmt);

}
