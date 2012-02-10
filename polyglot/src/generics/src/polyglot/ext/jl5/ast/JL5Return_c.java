package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.Return_c;
import polyglot.types.Context;
import polyglot.types.MethodInstance;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;

public class JL5Return_c extends Return_c implements JL5Return {

    public JL5Return_c(Position pos, Expr expr){
        super(pos, expr);
    }
}
