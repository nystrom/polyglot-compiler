package polyglot.ext.jl5.ast;

import polyglot.ast.Conditional_c;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5Conditional_c extends Conditional_c implements JL5Conditional  {

    public JL5Conditional_c(Position pos, Expr cond, Expr consequent, Expr alternative){
        super(pos, cond, consequent, alternative);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        Context ctx = tc.context();
        if (!ts.typeEquals(cond.type(), ts.Boolean(), ctx) && !ts.typeEquals(cond.type(), ts.BooleanWrapper(), ctx)){
            throw new SemanticException("Condition of ternary expression must be of type boolean.", cond.position());
        }

        Expr e1 = consequent;
        Expr e2 = alternative;
        Type t1 = e1.type();
        Type t2 = e2.type();
        
        // From the JLS, section:
        // If the second and third operands have the same type (which may be
        // the null type), then that is the type of the conditional expression.
        if (ts.typeEquals(t1, t2, ctx) || ts.equivalent(t1, t2)) {
            return type(t1);
        }

        // Otherwise, if the second and third operands have numeric type, then
        // there are several cases:
        if (t1.isNumeric() && t2.isNumeric()) {
            // - If one of the operands is of type byte and the other is of
            // type short, then the type of the conditional expression is
            // short.
            if (t1.isByte() && t2.isShort() || t1.isShort() && t2.isByte()) {
                return type(ts.Short());
            }

            // - If one of the operands is of type T where T is byte, short, or
            // char, and the other operand is a constant expression of type int
            // whose value is representable in type T, then the type of the
            // conditional expression is T.

            if (t1.isIntOrLess() &&
                t2.isInt() &&
                ts.numericConversionValid(t1, e2.constantValue(), ctx)) {
                    return type(t1);
            }

            if (t2.isIntOrLess() &&
                t1.isInt() &&
                ts.numericConversionValid(t2, e1.constantValue(), ctx)) {
                    return type(t2);
            }

            // - Otherwise, binary numeric promotion (5.6.2) is applied to the
            // operand types, and the type of the conditional expression is the
            // promoted type of the second and third operands. Note that binary
            // numeric promotion performs value set conversion (5.1.8).
            return type(ts.promote(t1, t2));
        }

        // If one of the second and third operands is of the null type and the
        // type of the other is a reference type, then the type of the
        // conditional expression is that reference type.
        if (t1.isNull() && t2.isReference()) return type(t2);
        if (t2.isNull() && t1.isReference()) return type(t1);


        // if one is null and other is primitive return 
        // type will need rewriting later
        if (t1.isNull() && t2.isPrimitive()) return type(((JL5TypeSystem)ts).classOf(t2));
        if (t2.isNull() && t1.isPrimitive()) return type(((JL5TypeSystem)ts).classOf(t1));
        
        // If the second and third operands are of different reference types,
        // then it must be possible to convert one of the types to the other
        // type (call this latter type T) by assignment conversion (�5.2); the
        // type of the conditional expression is T. It is a compile-time error
        // if neither type is assignment compatible with the other type.

        if (t1.isReference() && t2.isReference()) {
            if (ts.isImplicitCastValid(t1, t2, ctx)) {
                return type(t2);
            }
            if (ts.isImplicitCastValid(t2, t1, ctx)) {
                return type(t1);
            }
        }

        throw new SemanticException(
            "Could not find a type for ternary conditional expression.",
            position());
    }

}
