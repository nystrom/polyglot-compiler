package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.Id;

public interface ElementValuePair extends Expr{
    public Id name();
    public Expr value();
}
