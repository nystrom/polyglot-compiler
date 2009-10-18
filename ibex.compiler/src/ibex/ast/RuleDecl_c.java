package ibex.ast;

import ibex.lr.GLR;
import ibex.types.RSeq;
import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;
import ibex.types.RAnd;
import ibex.types.Nonterminal;
import ibex.types.Nonterminal_c;
import ibex.types.IbexClassType;
import ibex.types.Rhs;
import ibex.types.RuleDef;
import ibex.types.RuleDef_c;
import ibex.types.RuleInstance;
import ibex.visit.Rewriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import JFlex.NFA;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.ClassMember;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.FloatLit;
import polyglot.ast.Id;
import polyglot.ast.IntLit;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.frontend.Globals;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.MemberDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeBuilder;

/** A rule is simple any method that takes an IMatchContext and returns a value or throws a MatchFailure */
public class RuleDecl_c extends MethodDecl_c implements RuleDecl {
    RuleDef rule;
    
    public RuleDecl_c(Position pos, FlagsNode flags, TypeNode type, Id name, RhsExpr rhs) {
        super(pos, flags, type, name, Collections.EMPTY_LIST, Collections.EMPTY_LIST, block(type, rhs));
    }
    
    static Block block(TypeNode type, RhsExpr e) {
        NodeFactory nf = Globals.NF();
        if (e == null)
            return null;
        Position pos = e.position();
        if (type.typeRef() != null && type.typeRef().known()) {
            Type t = type.type();
            if (t.isVoid())
                return nf.Block(pos, nf.Eval(pos, e), nf.Return(pos));
        }
        return nf.Block(pos, nf.Return(pos, e));
    }

    public TypeNode type() {
        return returnType();
    }
    
    public RuleDecl type(TypeNode type) {
        return (RuleDecl) returnType(type);
    }

    public RuleDecl rhs(RhsExpr rhs) {
        return (RuleDecl) body(block(returnType, rhs));
    }
    
    public RhsExpr rhs() {
        Block b = body;
        if (b == null)
            return null;
        List<Stmt> s = b.statements();
        Return r = (Return) s.get(s.size() - 1);
        if (r.expr() == null) {
            Eval e = (Eval) s.get(s.size() - 2);
            return (RhsExpr) e.expr();
        }
        return (RhsExpr) r.expr();
    }
    
    public MemberDef memberDef() {
        return rule;
    }
    
    protected RuleDef createRuleDef(TypeSystem ts, ClassDef ct, Flags flags) {
        List choices = Collections.EMPTY_LIST;
        RuleDef mi = new RuleDef_c(ts, position(), Types.ref(ct.asType()), flags, type().typeRef(), name.id(),
                                         choices);
        return mi;
    }
    
//    /** Visit the children of the method. */
//    public Node visitChildren(NodeVisitor v) {
//        RuleDecl_c n = (RuleDecl_c) super.visitChildren(v);
//        RhsExpr rhs = (RhsExpr) n.visitChild(n.rhs, v);
//        return rhs == n.rhs ? n : n.rhs(rhs);
//    }

    @Override
    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();
        
        RuleDecl_c n = (RuleDecl_c) super.buildTypesOverride(tb);
        
        IbexClassDef ct = (IbexClassDef) tb.currentClass();
        assert ct != null;
        assert n.methodDef() != null;
        
        Flags flags = n.flags.flags();
        RuleDef rule = createRuleDef(ts, ct, flags);
        rule.setType(n.type().typeRef());
        ct.addRule(rule);
        
//        RhsExpr rhs = (RhsExpr) n.visitChild(n.rhs, tb.pushCode(rule));
//        n = (RuleDecl_c) n.rhs(rhs);
        return n.ruleDef(rule);
    }
    
    public RuleDef ruleDef() {
        return rule;
    }
    
    public RuleDecl ruleDef(RuleDef rule) {
        if (rule == this.rule) return this;
        RuleDecl_c n = (RuleDecl_c) copy();
        n.rule = rule;
        return n;
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        RuleDecl_c n = (RuleDecl_c) super.typeCheck(tc);
        return n;
    }
    
    Node tc(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();

        IbexClassType superCT = (IbexClassType)
            tc.context().currentClass().superClass();

        // Check overriding.
        RuleInstance superSym = null;

        try {
            superSym = ts.findSymbol(superCT, name.id());
        }
        catch (SemanticException e) {
        }
        
        // Check that superNT has the same type.
        if (superSym != null) {
            if (! ts.typeEquals(rule.asInstance().type(), superSym.type(), tc.context())) {
                throw new SemanticException("Cannot override nonterminal " +
                    name + "; incompatible semantic value types.", position());
            }
        }
        
        return this;
    }
    
    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {
        // Check for duplicate choices.
        for (int i = 0; i < rule.choices().size(); i++) {
            Rhs rhs = rule.choices().get(i);

            for (int j = i+1; j < rule.choices().size(); j++) {
                Rhs rhsj = rule.choices().get(j);

                if (rhsj.matches(rhs)) {
                    throw new SemanticException("Duplicate rule: \"" + rhs +
                        "\" and \"" + rhsj + "\".", rhsj.position());
                }
            }
        }

        Flags flags = flags().flags();
      
        if (tc.context().currentClass().flags().isInterface()) {
            if (flags.isProtected() || flags.isPrivate()) {
                throw new SemanticException("Interface methods must be public.",
                                            position());
            }
            throw new SemanticException("Cannot declare a rule in an interface.", position());
        }
        
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();

        if (! tc.context().currentClass().isSubtype(ts.runtimeParserType(), tc.context())) {
            throw new SemanticException("A parser rule can only appear in a subtype of \"" + ts.runtimeParserType() + "\".", position());
        }

        if (flags.isAbstract() && ! tc.context().currentClass().flags().isAbstract()) {
            throw new SemanticException("Nonterminal \"" + name() + "\" is abstract, but is a member of non-abstract class \"" + tc.context().currentClass() + "\".",
                position());
        }

        if (flags.isStatic()) {
            throw new SemanticException("Nonterminals cannot be static.", position());
        }
        if (flags.isNative()) {
            throw new SemanticException("Nonterminals cannot be native.", position());
        }
        if (flags.isTransient()) {
            throw new SemanticException("Nonterminals cannot be transient.", position());
        }
        if (flags.isStrictFP()) {
            throw new SemanticException("Nonterminals cannot be strictfp.", position());
        }
        if (flags.isVolatile()) {
            throw new SemanticException("Nonterminals cannot be volatile.", position());
        }

//        boolean allHaveBody = true;
//        boolean noneHaveBody = true;
//
//        for (Iterator i = this.choices.iterator(); i.hasNext(); ) {
//            Choice g = (Choice) i.next();
//            if (g.kind() == RhsGroupNode.DROP) {
//                continue;
//            }
//            for (Iterator j = g.elements().iterator(); j.hasNext(); ) {
//                RhsNode n = (RhsNode) j.next();
//                if (n.action() != null) {
//                    noneHaveBody = false;
//                }
//                else {
//                    allHaveBody = false;
//                }
//            }
//        }
//
//        if (! flags.isAbstract() && ! allHaveBody && ! type.type().isVoid()) {
//            throw new SemanticException("Missing semantic action.", position());
//        }
//
//        if (flags.isAbstract() && ! noneHaveBody) {
//            throw new SemanticException("Abstract rules cannot have semantic actions.", position());
//        }

        return this;
    }

//    public Term codeBody() {
//        return rhs;
//    }

    public CodeDef codeDef() {
        return rule;
    }

    public RuleDef rule() {
        return rule;
    }

    public RuleDecl rule(RuleDef rule) {
        RuleDecl_c n = (RuleDecl_c) copy();
        n.rule = rule;
        return n;
    }
    
//    public Term firstChild() {
//        return type();
//    }
//
//    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
//        v.visitCFGList(formals(), returnType(), ENTRY);
//        
//        if (body() == null && rhs() == null) {
//            v.visitCFG(returnType(), this, EXIT);
//        }
//        else if (body() == null) {
//            v.visitCFG(returnType(), rhs(), ENTRY);
//            v.visitCFG(rhs(), this, EXIT);
//        }
//        else if (rhs() == null) {l
//            v.visitCFG(returnType(), body(), ENTRY);
//            v.visitCFG(body(), this, EXIT);
//        }
//        else {
//            v.visitCFG(returnType(), body(), ENTRY);
//            v.visitCFG(body(), rhs(), ENTRY);
//            v.visitCFG(rhs(), this, EXIT);
//        }
//        
//        return succs;
//    }


    @Override
    public String toString() {
        return flags.flags().translate() + type() + " " + name() + " ::= " + rhs();
    }

}
