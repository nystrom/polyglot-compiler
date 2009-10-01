package ibex.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsCase_c extends RhsExpr_c implements RhsCase {

    private List<RhsExpr> cases;

    public RhsCase_c(Position pos, List<RhsExpr> cases) {
        super(pos);
        this.cases = TypedList.<RhsExpr>copyAndCheck(cases, RhsExpr.class, true);
    }

    public List<RhsExpr> cases() { return cases; }
    public RhsCase cases(List<RhsExpr> cases) {
        RhsCase_c n = (RhsCase_c) copy();
        n.cases = TypedList.<RhsExpr>copyAndCheck(cases, RhsExpr.class, true);
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RhsExpr> cases = (List<RhsExpr>) visitList(this.cases, v);
        return cases(cases);
    }

    @Override
    public Context enterChildScope(Node child, Context c) {
        return c.pushBlock();
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
        
        if (cases.size() == 0) 
            return nf.RhsSequence(position(), Collections.EMPTY_LIST).del().typeCheck(tc);
        
        Type t1 = null;
        
        for (RhsExpr e2 : cases) {
            if (t1 == null) {
                t1 = e2.type();
            }
            else {
                Type t2 = e2.type();

                // Type-check using the same rules as ?:

                // From the JLS, section:
                // If the second and third operands have the same type (which may be
                // the null type), then that is the type of the conditional expression.
                if (ts.typeEquals(t1, t2, tc.context())) {
                    // ok
                }

                // Otherwise, if the second and third operands have numeric type, then
                // there are several cases:
                else if (t1.isNumeric() && t2.isNumeric()) {
                    // - If one of the operands is of type byte and the other is of
                    // type short, then the type of the conditional expression is
                    // short.
                    if (t1.isByte() && t2.isShort() || t1.isShort() && t2.isByte()) {
                        t1 = ts.Short();
                    }

                    // - Otherwise, binary numeric promotion (Sec. 5.6.2) is applied to the
                    // operand types, and the type of the conditional expression is the
                    // promoted type of the second and third operands. Note that binary
                    // numeric promotion performs value set conversion (Sec. 5.1.8).
                    else {
                        t1 = ts.promote(t1, t2);
                    }
                }

                // If one of the second and third operands is of the null type and the
                // type of the other is a reference type, then the type of the
                // conditional expression is that reference type.
                else if (t1.isNull() && t2.isReference())
                    t1 = t2;
                else if (t2.isNull() && t1.isReference())
                    ; // ok

                // If the second and third operands are of different reference types,
                // then it must be possible to convert one of the types to the other
                // type (call this latter type T) by assignment conversion (Sec. 5.2); the
                // type of the conditional expression is T. It is a compile-time error
                // if neither type is assignment compatible with the other type.

                else if (t1.isReference() && t2.isReference()) {
                    if (ts.isImplicitCastValid(t1, t2, tc.context())) {
                        t1 = t2;
                    }
                    else if (ts.isImplicitCastValid(t2, t1, tc.context())) {
                        // ok
                    }
                    else {
                        throw new SemanticException("Could not determine type of / expression; cannot assign " + t1 + " to " + t2 + " or vice versa.",
                                                    position());
                    }
                }
                else {
                    throw new SemanticException("Could not determine type of / expression; cannot assign " + t1 + " to " + t2 + " or vice versa.",
                                                position());
                }
            }
        }
        
        return type(t1);

    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        for (Iterator<RhsExpr> i = cases.iterator(); i.hasNext(); ) {
            RhsExpr e = i.next();
            printSubExpr(e, true, w, tr);
            if (i.hasNext()) {
                w.write(" /");
                w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<RhsExpr> i = cases.iterator(); i.hasNext(); ) {
            RhsExpr e = i.next();
            sb.append(e);
            if (i.hasNext()) {
                sb.append(" / ");
            }
        }
        return sb.toString();
    }
    
    public Term firstChild() {
        return listChild(cases, null);
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFGList(cases, this, EXIT);
        return succs;
    }
}
