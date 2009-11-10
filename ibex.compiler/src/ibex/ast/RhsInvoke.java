package ibex.ast;

import ibex.types.Nonterminal;
import polyglot.ast.Call;

public interface RhsInvoke extends RhsExpr {
    Call call();
    RhsInvoke call(Call lit);
    
    Nonterminal symbol();
}
