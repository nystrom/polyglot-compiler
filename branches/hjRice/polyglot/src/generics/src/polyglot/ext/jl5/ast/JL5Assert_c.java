package polyglot.ext.jl5.ast;

import polyglot.ast.Assert_c;
import polyglot.ast.Expr;
import polyglot.util.Position;

/**
 * Type checking code has been removed as now the TypeSystem call isBoolean()
 * returns true if the cond type class is a boxed boolean.
 * @author vcave
 *
 */
public class JL5Assert_c extends Assert_c implements JL5Assert  {

    public JL5Assert_c(Position pos, Expr cond, Expr errorMsg){
        super(pos, cond, errorMsg);
    }
}
