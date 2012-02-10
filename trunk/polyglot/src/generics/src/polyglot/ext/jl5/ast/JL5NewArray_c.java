package polyglot.ext.jl5.ast;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.ArrayInit;
import polyglot.ast.Expr;
import polyglot.ast.NewArray_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.AnyType;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.ContextVisitor;

public class JL5NewArray_c extends NewArray_c implements JL5NewArray {

    public JL5NewArray_c(Position pos, TypeNode baseType, List dims, int addDims, ArrayInit init){
        super(pos, baseType, dims, addDims, init);
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == init) {
            return this.type();
        }
        
        if (dims.contains(child)) {
            // This is to handle boxing/unboxing in dimensions
            return child.type().typeSystem().Int();
        }

        return child.type();
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        Type t = baseType.type();
        if (t instanceof TypeVariable){
            throw new SemanticException("Cannot create a generic array of " + baseType, baseType.position());
        }
        if (t instanceof ParameterizedType){
            for (Iterator it = ((ParameterizedType)t).typeArguments().iterator(); it.hasNext(); ){
                Type next = (Type)it.next();
                if (!(next instanceof AnyType)){
                    throw new SemanticException("Cannot create a generic array of " + baseType, baseType.position());
                }
            }
        }
        if (t instanceof ClassType && ((ClassType)t).isNested()){
            Type outer = ((ClassType)t).outer();
            while (outer != null){
                if (outer instanceof JL5ParsedClassType && ((JL5ParsedClassType)outer).isGeneric()){
                    throw new SemanticException("Cannot create a generic array of " + baseType, baseType.position());
                }
                outer = ((ClassType)outer).outer(); 
            }
        }
        return super.typeCheck(tc);
    }
}
