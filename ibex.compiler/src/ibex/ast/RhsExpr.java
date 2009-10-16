package ibex.ast;

import polyglot.ast.*;
import polyglot.types.LocalDef;

public interface RhsExpr extends Expr {

    /** Local def introduced by this element of the rhs, or null. */
    LocalDef localDef();
    RhsExpr localDef(LocalDef localDef);
    
    boolean isRegular();
    RhsExpr isRegular(boolean f);
}
