package ibex.ast;

import ibex.types.IbexTypeSystem;

import java.util.List;

import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsAnyChar_c extends RhsExpr_c implements RhsAnyChar {

    public RhsAnyChar_c(Position pos) {
        super(pos);
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        return this;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();

        RhsExpr n = (RhsExpr) isRegular(true).type(ts.Char());

        LocalDef li = ts.localDef(position(), Flags.FINAL, Types.ref(ts.Char()), Name.make("_"));
        // Formal parameters are never compile-time constants.
        li.setNotConstant();

        LocalDecl ld = nf.LocalDecl(position(), nf.FlagsNode(position(), li.flags()), nf.CanonicalTypeNode(position(), li.type()), nf.Id(position(), li.name()));
        ld = ld.localDef(li);
        ld = ld.init(n);
        return nf.RhsSyntheticBind(position(), ld).type(n.type());
    }

    public Term firstChild() {
        return null;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("_");
    }

    public String toString() {
        return "_";
    }

}
