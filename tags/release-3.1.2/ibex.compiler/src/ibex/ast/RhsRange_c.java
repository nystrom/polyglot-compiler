package ibex.ast;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;


public class RhsRange_c extends RhsExpr_c implements RhsRange {
    private Expr lo;
    private Expr hi;

    public RhsRange_c(Position pos, Expr lo, Expr hi) {
        super(pos);
        this.lo = lo;
        this.hi = hi;
    }

    public Expr lo() { return lo; }
    public RhsRange lo(Expr lo) {
        RhsRange_c n = (RhsRange_c) copy();
        n.lo = lo;
        return n;
    }

    public Expr hi() { return hi; }
    public RhsRange hi(Expr hi) {
        RhsRange_c n = (RhsRange_c) copy();
        n.hi = hi;
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr lo = (Expr) visitChild(this.lo, v);
        Expr hi = (Expr) visitChild(this.hi, v);
        return lo(lo).hi(hi);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        Type l = lo.type();
        Type r = hi.type();
        
        if (! l.isChar() || ! lo.isConstant()) {
            throw new SemanticException("A range must have constant char operands.", lo.position());
        }

        if (! r.isChar() || ! hi.isConstant()) {
            throw new SemanticException("A range must have constant char operands.", hi.position());
        }
        
        char lv = (Character) lo.constantValue();
        char rv = (Character) hi.constantValue();
        
        if (lv == rv) {
            IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
            return nf.RhsLit(position(), lo).type(l);
        }
        
        if (lv > rv)
            throw new SemanticException("The low end of a character range must be <= the high end.", position());
        
        TypeSystem ts = tc.typeSystem();
        return isRegular(true).type(ts.Char());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(lo, true, w, tr);
        w.write("..");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, "");
        printSubExpr(hi, false, w, tr);
    }

    public String toString() {
        return lo + ".." + hi;
    }
    
    public Term firstChild() {
        return lo;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(lo, hi, ENTRY);
        v.visitCFG(hi, this, EXIT);
        return succs;
    }
}

