package ibex.ast;

import polyglot.ast.Call;

public interface RhsInvoke extends RhsExpr {
    Call call();
    RhsInvoke call(Call lit);
}
