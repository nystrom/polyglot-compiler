package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.RhsAnd;
import ibex.ast.RhsAnyChar;
import ibex.ast.RhsBind;
import ibex.ast.RhsExpr;
import ibex.ast.RhsInvoke;
import ibex.ast.RhsLit;
import ibex.ast.RhsLookahead;
import ibex.ast.RhsMinus;
import ibex.ast.RhsOption;
import ibex.ast.RhsOr;
import ibex.ast.RhsOrdered;
import ibex.ast.RhsPlus;
import ibex.ast.RhsPlusList;
import ibex.ast.RhsRange;
import ibex.ast.RhsSequence;
import ibex.ast.RhsStar;
import ibex.ast.RhsStarList;
import ibex.ast.RuleDecl;
import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;
import ibex.types.RuleDef;
import ibex.types.RuleDef_c;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.dispatch.Dispatch;
import polyglot.dispatch.PassthruError;
import polyglot.frontend.Job;
import polyglot.types.Flags;
import polyglot.types.MethodDef;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class GrammarNormalizer extends ContextVisitor {
    IbexTypeSystem ts;
    IbexNodeFactory nf;

    public GrammarNormalizer(Job job, IbexTypeSystem ts, IbexNodeFactory nf) {
        super(job, ts, nf);
        this.ts = ts;
        this.nf = nf;
    }

    @Override
    protected NodeVisitor enterCall(Node n) throws SemanticException {
        if (n instanceof ClassBody)
            orphans = new ArrayList<RuleDecl>();
        return this;
    }

    @Override
    public Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof RhsExpr || n instanceof ClassBody) {
            try {
                Node m = (Node) new Dispatch.Dispatcher("visit").invoke(v, n, parent);
                return m;
            }
            catch (PassthruError e) {
                SemanticException x = (SemanticException) e.getCause();
                throw x;
            }
        }
        return n;
    }

    public Node visit(ClassBody n, Node parent) {
        if (orphans.isEmpty()) {
            return n;
        }
        else {
            List<ClassMember> members = new ArrayList<ClassMember>(n.members().size() + orphans.size());
            members.addAll( n.members());
            members.addAll(orphans);
            return n.members(members);
        }
    }

    public static <T extends Node> T check(T n, ContextVisitor tc) throws SemanticException {
        return (T) n.del().disambiguate(tc).del().typeCheck(tc).del().checkConstants(tc);
    }

    <T extends Node> T check(T n) throws SemanticException {
        return check(n, this);
    }

    List<RuleDecl> orphans;

    public Node visit(Node n, Node parent) {
        return n;
    }

    public Node visit(RhsRange e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Character o1 = (Character) e.lo().constantValue();
        Character o2 = (Character) e.hi().constantValue();

        Position pos = e.position();

        RhsExpr rhs = check(nf.RhsLit(pos, this.<Expr> check(nf.CharLit(pos, o1))));
        if (o1.equals(o2)) {
            return rhs;
        }

        for (int c = ((char) o1) + 1; c <= o2; c++) {
            RhsExpr rc = check(nf.RhsLit(pos, check(nf.CharLit(pos, (char) c))));
            rhs = check(nf.RhsOr(pos, rhs, rc));
        }

        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        d = d.rhs(rhs);
        orphans.add(d);
        return sym;
    }

    private RuleDecl makeRule(Type type, Position pos, Name name) {
        if (name == null)
            name = Name.makeFresh("A$");

        IbexClassDef def = (IbexClassDef) context.currentClassDef();
        RuleDecl d = nf.RuleDecl(pos, nf.FlagsNode(pos, Flags.PRIVATE), nf.CanonicalTypeNode(pos, type), nf.Id(pos, name), (RhsExpr) null);
        RuleDef rd = new RuleDef_c(ts, d.position(), Types.ref(def.asType()), d.flags().flags(), d.type().typeRef(), d.name().id(), Collections.EMPTY_LIST);
        d = d.rule(rd);
        MethodDef md = ts.methodDef(pos, rd.container(), rd.flags(), rd.type(), rd.name(), Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        d = (RuleDecl) d.methodDef(md);
        def.addRule(rd);
        def.addMethod(md);
        return d;
    }

    public Node visit(RhsOr e, Node parent) throws SemanticException {
        if (parent instanceof RuleDecl)
            return e;

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();
        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        d = d.rhs(e);
        orphans.add(d);
        return sym;
    }

    public Node visit(RhsAnd e, Node parent) throws SemanticException {
        if (parent instanceof RuleDecl)
            return e;

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();
        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        d = d.rhs(e);
        orphans.add(d);
        return sym;
    }

    public Node visit(RhsMinus e, Node parent) throws SemanticException {
        if (parent instanceof RuleDecl)
            return e;

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();
        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        d = d.rhs(e);
        orphans.add(d);
        return sym;
    }

    public Name ruleName(RhsExpr e) {
        if (e instanceof RhsAnyChar) {
            return Name.make("Any$");
        }
        if (e instanceof RhsRange) {
            RhsRange r = (RhsRange) e;
            int lo = (int) (char) (Character) r.lo().constantValue();
            int hi = (int) (char) (Character) r.hi().constantValue();
            return Name.make("Range$" + lo + "$" + hi);
        }
        if (e instanceof RhsLit) {
            RhsLit r = (RhsLit) e;
            Object v = r.constantValue();
            if (v instanceof Character) {
                int value = (int) (char) (Character) v;
                return Name.make("Lit$" + value);
            }
            else if (v instanceof String) {
                String s = (String) v;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (Character.isJavaIdentifierPart(c)) {
                        sb.append(c);
                    }
                    else {
                        sb.append("$" + (int) c);
                    }
                }
                return Name.make("Lit$" + sb.toString());
            }
        }
        if (e instanceof RhsStar) {
            RhsStar r = (RhsStar) e;
            Name name = ruleName(r.item());
            if (name == null) return null;
            return Name.make("Star$" + name.toString() + "$1");
        }
        if (e instanceof RhsPlus) {
            RhsPlus r = (RhsPlus) e;
            Name name = ruleName(r.item());
            if (name == null) return null;
            return Name.make("Plus$" + name.toString() + "$1");
        }
        if (e instanceof RhsOption) {
            RhsOption r = (RhsOption) e;
            Name name = ruleName(r.item());
            if (name == null) return null;
            return Name.make("Opt$" + name.toString() + "$1");
        }
        if (e instanceof RhsLookahead) {
            RhsLookahead r = (RhsLookahead) e;
            Name name = ruleName(r.item());
            if (name == null) return null;
            return Name.make("Ahead$" + name.toString() + "$1");
        }
        if (e instanceof RhsInvoke) {
            RhsInvoke r = (RhsInvoke) e;
            return r.call().name().id();
        }
        if (e instanceof RhsOr) {
            RhsOr r = (RhsOr) e;
            Name lname = ruleName(r.left());
            if (lname == null) return null;
            Name rname = ruleName(r.right());
            if (rname == null) return null;
            return Name.make("Or$" + lname.toString() + "$" + rname.toString() + "$1");
        }
        if (e instanceof RhsAnd) {
            RhsAnd r = (RhsAnd) e;
            Name lname = ruleName(r.left());
            if (lname == null) return null;
            Name rname = ruleName(r.right());
            if (rname == null) return null;
            return Name.make("And$" + lname.toString() + "$" + rname.toString() + "$1");
        }
        if (e instanceof RhsMinus) {
            RhsMinus r = (RhsMinus) e;
            Name lname = ruleName(r.left());
            if (lname == null) return null;
            Name rname = ruleName(r.right());
            if (rname == null) return null;
            return Name.make("Minus$" + lname.toString() + "$" + rname.toString() + "$1");
        }
        if (e instanceof RhsSequence) {
            RhsSequence r = (RhsSequence) e;
            StringBuilder sb = new StringBuilder();
            sb.append("Seq");
            for (RhsExpr ei : r.terms()) {
                Name name = ruleName(ei);
                if (name == null)
                    return null;
                sb.append("$");
                sb.append(name);
            }
            sb.append("$1");
            return Name.make(sb.toString());
        }
        return null;
    }

    RhsInvoke findRule(RhsExpr e) throws SemanticException {
        Name name = ruleName(e);
        if (name != null) {
            for (RuleDecl d : orphans) {
                if (d.name().id() == name) {
                    Position pos = e.position();
                    RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
                    return sym;
                }
            }
        }
        return null;
    }

    public Node visit(RhsAnyChar e, Node parent) throws SemanticException {
        Position pos = e.position();

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));

        RhsExpr rhs = check(nf.RhsLit(pos, check(nf.CharLit(pos, (char) 0))));

        for (int c = 1; c != 0; c++) {
            RhsExpr rc = check(nf.RhsLit(pos, check(nf.CharLit(pos, (char) c))));
            rhs = check(nf.RhsOr(pos, rhs, rc));
        }

        d = d.rhs(rhs);

        orphans.add(d);
        return sym;
    }

    public Node visit(RhsOption e, Node parent) throws SemanticException {
        // A? -> B
        // B ::= /* empty */ | A

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();
        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        RhsExpr rhs = check(nf.RhsOr(pos, check(nf.RhsSequence(pos)), e.item()));
        d = d.rhs(rhs);
        orphans.add(d);
        return sym;
    }

    public Node visit(RhsStar e, Node parent) throws SemanticException {
        // A* -> B
        // B ::= /* empty */ | A B

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();

        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));

        RhsExpr seq = (RhsExpr) visit(check(nf.RhsSequence(pos, e.item(), sym)), parent);
        RhsExpr rhs = check(nf.RhsOr(pos, check(nf.RhsSequence(pos)), check(seq)));
        d = d.rhs(rhs);
        orphans.add(d);
        return (Node) sym.copy();
    }

    public Node visit(RhsLookahead e, Node parent) throws SemanticException {
        if (parent instanceof RuleDecl) {
            if (e.item() instanceof RhsInvoke || e.item() instanceof RhsLit || e.item() instanceof RhsRange) {
                return e;
            }
        }

        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();

        if (parent instanceof RuleDecl) {
            // [alpha] -> [A] where A ::= alpha
            RuleDecl d = makeRule(e.type(), pos, ruleName(e));
            RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
            d = d.rhs((RhsExpr) e.item());
            orphans.add(d);
            return e.item(sym);
        }
        else {
            // [alpha] -> A where A ::= [alpha]
            RuleDecl d = makeRule(e.type(), pos, ruleName(e));
            RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
            d = d.rhs(e);
            orphans.add(d);
            return sym;
        }
    }

    public Node visit(RhsBind e, Node parent) throws SemanticException {
        // ( x = alpha )  ->  { x = A } where A ::= alpha
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        Position pos = e.position();

        if (e.item() instanceof RhsInvoke || e.item() instanceof RhsLit || e.item() instanceof RhsRange) {
            return e;
        }

        // !alpha -> !A
        // where A ::= alpha
        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
        d = d.rhs((RhsExpr) e.decl().init());
        orphans.add(d);
        return e.decl(e.decl().init(sym));
    }

    public Node visit(RhsPlus e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        // A+ -> B
        // B ::= A | A B
        Position pos = e.position();

        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));

        RhsExpr seq = (RhsExpr) visit(check(nf.RhsSequence(pos, e.item(), sym)), parent);
        RhsExpr rhs = check(nf.RhsOr(pos, e.item(), check(seq)));
        d = d.rhs(rhs);
        orphans.add(d);
        return (Node) sym.copy();
    }

    public Node visit(RhsPlusList e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        // A ++ x -> B
        // B ::= A | A x B
        Position pos = e.position();

        RuleDecl d = makeRule(e.type(), pos, ruleName(e));
        RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));

        RhsExpr seq = (RhsExpr) visit(check(nf.RhsSequence(pos, e.item(), e.sep(), sym)), parent);
        RhsExpr rhs = check(nf.RhsOr(pos, e.item(), check(seq)));
        d = d.rhs(rhs);
        orphans.add(d);
        return (Node) sym.copy();
    }

    public Node visit(RhsStarList e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        // A ** x -> (A ++ x)?
        Position pos = e.position();
        RhsExpr pluslist = (RhsExpr) visit(check(nf.RhsPlusList(pos, e.item(), e.sep())), parent);
        return visit(check(nf.RhsOption(pos, pluslist)), parent);
    }

    public Node visit(RhsOrdered e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        // A / B --> A | B - A
        Position pos = e.position();

        RhsExpr e3 = check(nf.RhsMinus(pos, e.right(), e.left()));
        return check(nf.RhsOr(pos, e.left(), e3));
    }

    public Node visit(RhsSequence e, Node parent) throws SemanticException {
        RhsInvoke cache = findRule(e);
        if (cache != null) {
            return cache;
        }

        List<RhsExpr> es = new ArrayList<RhsExpr>();
        LinkedList<RhsExpr> q = new LinkedList<RhsExpr>();
        q.addAll(e.terms());

        while (! q.isEmpty()) {
            RhsExpr ei = q.removeFirst();
            if (ei instanceof RhsSequence) {
                RhsSequence s = (RhsSequence) ei;
                q.addAll(s.terms());
            }
            else if (ei instanceof RhsOr || ei instanceof RhsAnd || ei instanceof RhsMinus) {
                Position pos = e.position();
                RuleDecl d = makeRule(e.type(), pos, ruleName(e));
                RhsInvoke sym = check(nf.RhsInvoke(pos, check(nf.Call(pos, d.name()))));
                RhsExpr rhs = ei;
                d = d.rhs(rhs);
                orphans.add(d);
                es.add(sym);
            }
            else {
                es.add(ei);
            }
        }

        if (es.size() == 1)
            return es.get(0);

        return e.terms(es);
    }
}