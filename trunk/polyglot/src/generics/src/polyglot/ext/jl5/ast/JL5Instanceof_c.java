package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.Instanceof_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5Instanceof_c extends Instanceof_c implements JL5Instanceof {

    public JL5Instanceof_c(Position pos, Expr expr, TypeNode compareType){
        super(pos, expr, compareType);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        if (compareType().type() instanceof ParameterizedType){
            throw new SemanticException("Type arguments not allowed here.", compareType().position());
        }
        if (compareType().type() instanceof TypeVariable){
            throw new SemanticException("Type variable not allowed here.", compareType().position());
        }
        return super.typeCheck(tc);
    }
}
