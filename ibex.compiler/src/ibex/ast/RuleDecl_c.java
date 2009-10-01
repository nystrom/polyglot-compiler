package ibex.ast;

import ibex.visit.GrammarDesugarer;

import java.util.Collections;

import polyglot.ast.Block;
import polyglot.ast.CodeBlock;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.TypeNode;
import polyglot.frontend.Globals;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

/** A rule is simple any method that takes an IMatchContext and returns a value or throws a MatchFailure */
public class RuleDecl_c extends MethodDecl_c implements RuleDecl {
    public RuleDecl_c(Position pos, FlagsNode flags, TypeNode returnType, Id name, RhsExpr rhs) {
        super(pos, flags, returnType, name, Collections.singletonList(formal(pos)), Collections.singletonList(failure(pos)), block(rhs));
    }
    
    private static TypeNode failure(Position pos) {
        NodeFactory nf = Globals.NF();
        return nf.TypeNodeFromQualifiedName(pos, QName.make("ibex.runtime.MatchFailureException"));
    }
    private static Formal formal(Position pos) {
        NodeFactory nf = Globals.NF();
        return nf.Formal(pos, nf.FlagsNode(pos, Flags.FINAL), nf.TypeNodeFromQualifiedName(pos, QName.make("ibex.runtime.IMatchContext")), nf.Id(pos, Name.make("context$")));
    }

    private static Block block(RhsExpr rhs) {
        NodeFactory nf = Globals.NF();
        return nf.Block(rhs.position(), nf.Return(rhs.position(), rhs));
    }
    
    public RhsExpr rhs() {
        return (RhsExpr) ((Return) ((Block) body()).statements().get(0)).expr();
    }

    public RuleDecl rhs(RhsExpr e) {
        return (RuleDecl) body(block(e));
    }
    
    @Override
    public CodeBlock body(Block body) {
        return super.body(body);
    }

//    @Override
//    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
//        RuleDecl_c n = (RuleDecl_c) this.visit(new GrammarDesugarer(tb.currentClass()));
//        return n.superBuildTypesOverride(tb);
//    }
//
//    private Node superBuildTypesOverride(TypeBuilder tb) throws SemanticException {
//        return super.buildTypesOverride(tb);
//    }

//    private Node superTC(ContextVisitor tb) throws SemanticException {
//        return super.typeCheck(tb);
//    }
//    
//    @Override
//    public Node typeCheck(ContextVisitor tc) throws SemanticException {
//        RuleDecl_c n = (RuleDecl_c) this.visit(new GrammarDesugarer(tc.context().currentClassDef()));
//        return n.superTC(tc);
//    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        RuleDecl_c n = (RuleDecl_c) super.typeCheck(tc);
        if (tc.errorQueue().hasErrors())
            return n;
        n = (RuleDecl_c) n.visit(new GrammarDesugarer(tc.context().currentClassDef()));
        return n;
    }
}
