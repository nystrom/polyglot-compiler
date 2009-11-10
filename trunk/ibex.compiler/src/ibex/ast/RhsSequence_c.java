package ibex.ast;

import ibex.types.IbexTypeSystem;
import ibex.types.RSeq_c;
import ibex.types.Rhs;
import ibex.types.TupleType_c;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
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
        assert terms.size() != 1;
        this.items = TypedList.<RhsExpr>copyAndCheck(terms, RhsExpr.class, true);
    }

    private List<RhsExpr> items;
    
    public List<RhsExpr> items() {
        return items;
    }

    public RhsSequence items(List<RhsExpr> items) {
        RhsSequence_c n = (RhsSequence_c) copy();
        assert items.size() != 1;
        n.items = TypedList.<RhsExpr>copyAndCheck(items, RhsExpr.class, true);
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        List<RhsExpr> terms = (List<RhsExpr>) visitList(this.items, v);
        return items(terms);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        
        List<Ref<Type>> types = new ArrayList<Ref<Type>>(this.items.size());
        List<Rhs> items = new ArrayList<Rhs>();
        for (RhsExpr e : this.items) {
            if (e instanceof RhsLookahead)
                continue;
            
            Type t = e.type();
            
            if (t.isVoid() || t.isNull())
                continue;

            types.add(Types.ref(t));
            items.add(e.rhs());
        }
        
        TupleType_c type = new TupleType_c(ts, position(), types);
        Rhs rhs = new RSeq_c(ts, position(), items, type);
        
        return rhs(rhs).type(type);
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        for (Iterator<RhsExpr> i = items.iterator(); i.hasNext(); ) {
            RhsExpr e = i.next();
            
            print(e, w, tr);

            if (i.hasNext()) {
                w.allowBreak(0, " ");
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<RhsExpr> i = items.iterator(); i.hasNext(); ) {
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
        return listChild(items, null);
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFGList(items, this, EXIT);
        return succs;
    }
}
