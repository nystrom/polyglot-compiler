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
    private Expr from;
    private Expr to;

    public RhsRange_c(Position pos, Expr from, Expr to) {
        super(pos);
        this.from = from;
        this.to = to;
    }

    public Expr from() { return from; }
    public RhsRange from(Expr from) {
        RhsRange_c n = (RhsRange_c) copy();
        n.from = from;
        return n;
    }

    public Expr to() { return to; }
    public RhsRange to(Expr to) {
        RhsRange_c n = (RhsRange_c) copy();
        n.to = to;
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr from = (Expr) visitChild(this.from, v);
        Expr to = (Expr) visitChild(this.to, v);
        return from(from).to(to);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        Expr left = from;
        Expr right = to;
        
        Type l = from.type();
        Type r = right.type();
        
        if (! l.isChar() || ! left.isConstant()) {
            throw new SemanticException("A range must have constant char operands.", left.position());
        }

        if (! r.isChar() || ! right.isConstant()) {
            throw new SemanticException("A range must have constant char operands.", right.position());
        }
        
        Object lv = left.constantValue();
        Object rv = right.constantValue();
        
        char lo = (Character) lv;
        char hi = (Character) rv;
        
        if (lo > hi)
            throw new SemanticException("The low end of a character range must be <= the high end.", position());
        
        TypeSystem ts = tc.typeSystem();
        return type(ts.Char());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(from, true, w, tr);
        w.write("..");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, "");
        printSubExpr(to, false, w, tr);
    }

    public String toString() {
        return from + ".." + to;
    }
    
    public Term firstChild() {
        return from;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(from, to, ENTRY);
        v.visitCFG(to, this, EXIT);
        return succs;
    }
}

