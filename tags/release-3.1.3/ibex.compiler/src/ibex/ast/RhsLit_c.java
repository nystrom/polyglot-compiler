package ibex.ast;

import ibex.types.IbexClassType;
import ibex.types.IbexTypeSystem;
import ibex.types.Nonterminal;
import ibex.types.RuleInstance;
import ibex.visit.Rewriter;

import java.util.List;

import polyglot.ast.AmbExpr;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Term;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;


public class RhsLit_c extends RhsExpr_c implements RhsLit {

    private Expr lit;

    public RhsLit_c(Position pos, Expr lit) {
        super(pos);
        this.lit = lit;
    }
    
    @Override
    public Object constantValue() {
        return lit.constantValue();
    }
    
    @Override
    public boolean isConstant() {
        return lit.isConstant();
    }

    public Expr lit() {
        return lit;
    }

    public RhsLit lit(Expr lit) {
        RhsLit_c n = (RhsLit_c) copy();
        n.lit = lit;
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr lit = (Expr) visitChild(this.lit, v);
        return lit(lit);
    }
    
    @Override
    public Node typeCheckOverride(Node parent, ContextVisitor tc) throws SemanticException {
        NodeFactory nf = tc.nodeFactory();
        Position pos = position();
        
        // Check if the lit is actually a call to a nonterminal.
         Call c = null;
        if (lit instanceof Field) {
            Field f = (Field) lit;
            c = nf.Call(pos, f.target(), f.name());
        }
        if (lit instanceof AmbExpr) {
            AmbExpr a = (AmbExpr) lit;
            c = nf.Call(pos, a.name());
        }
        if (c != null) {
            NodeVisitor v = tc.enter(parent, c);
            c = (Call) c.visitChildren(v);
            try {
                return Rewriter.check(lit(Rewriter.check(c, tc)), tc);
            }
            catch (SemanticException e) {
                // Ignore
                if (lit instanceof Field) {
                    Field f = (Field) lit;
                    f = f.target(c.target());
                    return Rewriter.check(lit(Rewriter.check(f, tc)), tc);
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        
        if (lit instanceof Call) {
            Call call = (Call) lit;
            
            Nonterminal sym = null;
            
            MethodInstance mi = call.methodInstance();
            IbexClassType ct = (IbexClassType) mi.container();
            for (RuleInstance rule : ct.rules()) {
                if (rule.name() == mi.name())
                    sym = rule.def().asNonterminal();
            }
            
            if (sym == null)
                throw new SemanticException("Cannot find rule for " + mi);
            
            IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
            return Rewriter.check(nf.RhsInvoke(position(), call), tc);
        }
        
        if (lit.isConstant() && (lit.type().isChar() || lit.type().isSubtype(ts.String(), tc.context())))
            return isRegular(true).type(lit.type());
                
        throw new SemanticException("Rule item is neither a constant char nor a constant String nor a nonterminal.", position());
    }

    public Term firstChild() {
        return lit;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(lit, this, EXIT);
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(lit, false, w, tr);
    }
    
    public String toString() {
        return lit.toString();
    }


}
