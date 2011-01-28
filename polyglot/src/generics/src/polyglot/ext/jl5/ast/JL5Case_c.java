package polyglot.ext.jl5.ast;

import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Case_c;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

public class JL5Case_c extends Case_c implements JL5Case  {


    public JL5Case_c(Position pos, Expr expr){
        super(pos, expr);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
    
        if (expr == null) {
	        return this;
	    }

        TypeSystem ts = tc.typeSystem();

	    // enum constants are allowed
	Type et = expr.type();
	if (et.isClass() && JL5Flags.isEnumModifier(et.toClass().flags()))
	    return this;

        if (! ts.isImplicitCastValid(expr.type(), ts.Int())){
	    throw new SemanticException("Case label must be an enum, byte, char, short, or int.", position());
        }

        Object o = null;

        if (expr instanceof Field) {
            FieldInstance fi = ((Field) expr).fieldInstance();

            if (fi == null) {
                throw new InternalCompilerError(
                "Undefined FieldInstance after type-checking.");
            }

            if (! fi.isConstant()) {
                throw new SemanticException("Case label must be an integral constant.", position());
            }

            o = fi.constantValue();
        }
        else if (expr instanceof Local) {
            LocalInstance li = ((Local) expr).localInstance();

            if (li == null) {
                throw new InternalCompilerError(
                "Undefined LocalInstance after type-checking.");
            }

            if (! li.isConstant()) {
                    /* FIXME: isConstant() is incorrect 
                throw new SemanticException("Case label must be an integral constant.",
                            position());
                    */
                    return this;
            }

            o = li.constantValue();
        }
        else {
            o = expr.constantValue();
        }
        if (o instanceof Number && ! (o instanceof Long) &&
            ! (o instanceof Float) && ! (o instanceof Double)) {

            return value(((Number) o).longValue());
        }
        else if (o instanceof Character) {
            return value(((Character) o).charValue());
        }

        throw new SemanticException("Case label must be an integral constant.",
                                    position());

    }
}
