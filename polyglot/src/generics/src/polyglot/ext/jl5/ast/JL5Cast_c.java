package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.TypeNode;
import polyglot.ast.Cast_c;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;

public class JL5Cast_c extends Cast_c implements JL5Cast {

    public JL5Cast_c(Position pos, TypeNode castType, Expr expr){
        super(pos, castType, expr);
    }

    // the original of this method makes all numerics be doubles
    // which is bad
    public Type childExpectedType(Expr child, AscriptionVisitor av){
        TypeSystem ts = av.typeSystem();
    
        return child.type();
        /*if (child == expr){
            if (castType.type().isReference()){
                return ts.Object();
            }
        }*/
    }
}
