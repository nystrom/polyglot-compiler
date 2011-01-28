package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ext.jl.ast.If_c;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

public class JL5If_c extends If_c implements JL5If  {

    public JL5If_c(Position pos, Expr cond, Stmt consequent, Stmt alternative){
        super(pos, cond, consequent, alternative);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        if (!ts.equals(cond.type(), ts.Boolean()) && !ts.equals(cond.type(), ts.BooleanWrapper())){
            throw new SemanticException("Condition of if must have boolean type.", cond.position());
        }
        return this;
    }
}
