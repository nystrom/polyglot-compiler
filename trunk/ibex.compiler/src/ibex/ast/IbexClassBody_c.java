package ibex.ast;

import ibex.ast.RhsAnyChar_c.RDummy_c;
import ibex.types.CharTerminal_c;
import ibex.types.IbexTypeSystem;
import ibex.types.RAnd_c;
import ibex.types.RLookahead_c;
import ibex.types.RSeq;
import ibex.types.RSeq_c;
import ibex.types.RSub_c;
import ibex.types.Rhs;
import ibex.types.RuleDef;
import ibex.types.Terminal_c;
import ibex.visit.GrammarNormalizer;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class IbexClassBody_c extends ClassBody_c {

    public IbexClassBody_c(Position pos, List<ClassMember> members) {
        super(pos, members);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexClassBody_c n = (IbexClassBody_c) super.typeCheck(tc);
//        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
//        IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
//        GrammarNormalizer v = new GrammarNormalizer(tc.job(), ts, nf);
//        v = (GrammarNormalizer) v.context(tc.context());
//        n = (IbexClassBody_c) n.visit(v);
//
//        if (tc.errorQueue().hasErrors())
//            return n;
//        
//
//        return n.updateRuleDefs(tc);
        return n;
    }

    public static void check(Rhs rhs, RhsExpr e) {
        assert rhs != null : e + " (" + e.getClass().getName() + ")";
        if (rhs instanceof RDummy_c)
            assert !( rhs instanceof RDummy_c) : e + " (" + e.getClass().getName() + ")";
        if (rhs instanceof RSub_c) {
            RSub_c r = (RSub_c) rhs;
            if (e instanceof RhsMinus) {
                RhsMinus s = (RhsMinus) e;
                check(r.choice1(), s.left());
                check(r.choice2(), s.right());
            }
            else {
                check(r.choice1(), e);
                check(r.choice2(), e);
            }
        }
        if (rhs instanceof RAnd_c) {
            RAnd_c r = (RAnd_c) rhs;
            if (e instanceof RhsAnd) {
                RhsAnd s = (RhsAnd) e;
                check(r.choice1(), s.left());
                check(r.choice2(), s.right());
            }
            else {
                check(r.choice1(), e);
                check(r.choice2(), e);
            }
        }
        if (rhs instanceof RSeq_c) {
            RSeq_c r = (RSeq_c) rhs;
            if (e instanceof RhsSequence) {
                RhsSequence s = (RhsSequence) e;
                if (s.items().size() == r.items().size())
                    for (int i = 0; i < r.items().size(); i++) {
                        check(r.items().get(i), s.items().get(i));
                    }
            }
            for (Rhs ri : r.items()) {
                check(ri, e);
            }
        }
    }
    private Node updateRuleDefs(ContextVisitor tc) {
        List<ClassMember> members = new ArrayList<ClassMember>();
        for (ClassMember cm : members()) {
            if (cm instanceof RuleDecl) {
                RuleDecl d = (RuleDecl) cm;
                RuleDef def = d.rule();
                RhsExpr e = d.rhs();

                final List<Rhs> choices = new ArrayList<Rhs>();

                e.visit(new NodeVisitor() {
                    @Override
                    public Node override(Node n) {
                        if (n instanceof RhsOr)
                            return null;
                        if (n instanceof RhsExpr) {
                            RhsExpr e = (RhsExpr) n;
                            System.out.println("adding " + e.rhs() + " for " + e);
                            check(e.rhs(), e);
                            choices.add(e.rhs());
                            return n;
                        }
                        return null;
                    }
                });

                def.setChoices(choices);
                members.add(d);
            }
            else {
                members.add(cm);
            }
        }

        return this.members(members);
    }
}
