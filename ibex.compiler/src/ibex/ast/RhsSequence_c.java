package ibex.ast;

import ibex.types.IbexTypeSystem;

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

public class RhsSequence_c extends RhsExpr_c implements RhsSequence {
    public RhsSequence_c(Position pos, List<RhsExpr> terms) {
        super(pos);
        this.terms = TypedList.<RhsExpr>copyAndCheck(terms, RhsExpr.class, true);
    }

    private List<RhsExpr> terms;

    public List<RhsExpr> terms() {
        return terms;
    }

    public RhsSequence terms(List<RhsExpr> terms) {
        RhsSequence_c n = (RhsSequence_c) copy();
        n.terms = TypedList.<RhsExpr>copyAndCheck(terms, RhsExpr.class, true);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RhsExpr> terms = (List<RhsExpr>) visitList(this.terms, v);
        return terms(terms);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        Context context = tc.context();
        
        if (terms.isEmpty()) {
            return type(ts.Null());
        }

        boolean array = false;
        int count = 0;
        Type t1 = null;

        for (RhsExpr e : terms) {
            if (e instanceof RhsLookahead)
                continue;
            
            count++;
            
            if (t1 == null) {
                t1 = e.type();
                if (! (e instanceof RhsOption)
                        && (e instanceof RhsSequence || e instanceof RhsIteration || e instanceof RhsIterationList)
                        && t1.isArray()) {    
                    t1 = t1.toArray().base();
                    array = true;
                }
            }
            else {
                Type t2 = e.type();

                if (! (e instanceof RhsOption)
                        && (e instanceof RhsSequence || e instanceof RhsIteration || e instanceof RhsIterationList)
                        && t2.isArray()) {    
                    t2 = t2.toArray().base();
                    array = true;
                }

                if (ts.typeEquals(t1, t2, context)) {
                    // ok
                }
                else if (t1.isNumeric() && t2.isNumeric()) {
                    Type p = ts.promote(t1, t2);
                    t1 = p;
                }
                

                // If one of the second and third operands is of the null type and the
                // type of the other is a reference type, then the type of the
                // conditional expression is that reference type.
                else if (t1.isNull() && t2.isReference()) t1 = t2;
                else if (t2.isNull() && t1.isReference()) ; // ok
                
                else if (t1.isPrimitive() && t2.isReference()) {
                    Type r1 = ts.nullable(t1);
                    Type p = ts.leastCommonAncestor(r1, t2, context);
                    t1 = p;
                }
                else if (t1.isReference() && t2.isPrimitive()) {
                    Type r2 = ts.nullable(t2);
                    Type p = ts.leastCommonAncestor(t1, r2, context);
                    t1 = p;
                }
                else if (t1.isReference() && t2.isReference()) {
                    Type p = ts.leastCommonAncestor(t1, t2, context);
                    t1 = p;
                }
                else {
                    throw new SemanticException("Could not find least common ancestor type of " + t1 + " and " + t2 + ".", position());
                }
            }
        }
        
        assert count == 0 || t1 != null;
        
        if (count > 1 || array)
            return type(ts.arrayOf(t1));
        else if (count == 1)
            return type(t1);
        else
            return type(ts.Null());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        for (Iterator<RhsExpr> i = terms.iterator(); i.hasNext(); ) {
            RhsExpr e = i.next();
            
            print(e, w, tr);

            if (i.hasNext()) {
                w.allowBreak(0, " ");
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<RhsExpr> i = terms.iterator(); i.hasNext(); ) {
            RhsExpr e = i.next();

            if (e instanceof RhsBinary_c)
                sb.append("(");
            sb.append(e);
            if (e instanceof RhsBinary_c)
                sb.append(")");
            
            if (i.hasNext()) {
                sb.append(" ");
            }
        }
        
        return sb.toString();
    }
    
    public Term firstChild() {
        return listChild(terms, null);
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFGList(terms, this, EXIT);
        return succs;
    }
}
