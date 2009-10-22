package ibex.ast;

import ibex.types.IbexTypeSystem;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsOr_c extends RhsBinary_c implements RhsOr {

    public RhsOr_c(Position pos, RhsExpr c1, RhsExpr c2) {
        super(pos, c1, c2);
    }

    public RhsOr left(RhsExpr left) {
        return (RhsOr) super.left(left);
    }

    public RhsOr right(RhsExpr right) {
        return (RhsOr) super.right(right);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        return typeCheckOr(this, left, right, tc);
    }

    static Node typeCheckOr(RhsExpr e, RhsExpr left, RhsExpr right, ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();

        Type t1 = left.type();
        Type t2 = right.type();

        // Type-check using the same rules as ?:, except with autoboxing

        // From the JLS, section:
        // If the second and third operands have the same type (which may be
        // the null type), then that is the type of the conditional expression.
        if (ts.typeEquals(t1, t2, tc.context())) {
            return e.type(t1);
        }

        // Otherwise, if the second and third operands have numeric type, then
        // there are several cases:
        else if (t1.isNumeric() && t2.isNumeric()) {
            // - If one of the operands is of type byte and the other is of
            // type short, then the type of the conditional expression is
            // short.
            if (t1.isByte() && t2.isShort() || t1.isShort() && t2.isByte()) {
                return e.type(ts.Short());
            }

            // - Otherwise, binary numeric promotion (Sec. 5.6.2) is applied to the
            // operand types, and the type of the conditional expression is the
            // promoted type of the second and third operands. Note that binary
            // numeric promotion performs value set conversion (Sec. 5.1.8).
            else {
                return e.type(ts.promote(t1, t2));
            }
        }

        // If one of the second and third operands is of the null type and the
        // type of the other is a reference type, then the type of the
        // conditional expression is that reference type.
        else if (t1.isNull())
            return e.type(ts.nullable(t2));
        else if (t2.isNull())
            return e.type(ts.nullable(t1));

        // If the second and third operands are of different reference types,
        // then it must be possible to convert one of the types to the other
        // type (call this latter type T) by assignment conversion (Sec. 5.2); the
        // type of the conditional expression is T. It is a compile-time error
        // if neither type is assignment compatible with the other type.

        else if (t1.isReference() && t2.isReference()) {
            if (ts.isImplicitCastValid(t1, t2, tc.context())) {
                return e.type(t2);
            }
            else if (ts.isImplicitCastValid(t2, t1, tc.context())) {
                return e.type(t1);
            }
        }

        throw new SemanticException("Could not determine type; cannot assign " + t1 + " to " + t2 + " or vice versa.",
                                    e.position());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(left, true, w, tr);
        w.write(" |");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
        printSubExpr(right, true, w, tr);
    }

    public String toString() {
        return left + " | " + right;
    }
}