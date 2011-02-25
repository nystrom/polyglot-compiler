package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.If_c;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5If_c extends If_c implements JL5If  {

    public JL5If_c(Position pos, Expr cond, Stmt consequent, Stmt alternative){
        super(pos, cond, consequent, alternative);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        // ts.Boolean() handles both primitive and box typed
        return super.typeCheck(tc);
    }
}
