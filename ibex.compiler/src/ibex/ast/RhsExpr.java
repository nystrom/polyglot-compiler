package ibex.ast;

import polyglot.ast.*;
import polyglot.types.LocalDef;

public interface RhsExpr extends Expr {
    boolean isRegular();
    RhsExpr isRegular(boolean f);
}
