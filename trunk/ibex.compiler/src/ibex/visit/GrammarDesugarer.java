package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.RhsAnyChar;
import ibex.ast.RhsCase;
import ibex.ast.RhsExpr;
import ibex.ast.RhsInvoke;
import ibex.ast.RhsLit;
import ibex.ast.RhsOption;
import ibex.ast.RhsOption_c;
import ibex.ast.RhsPlus;
import ibex.ast.RhsPlusList;
import ibex.ast.RhsRange;
import ibex.ast.RhsSequence;
import ibex.ast.RhsStar;
import ibex.ast.RhsStarList;
import ibex.ast.RuleDecl;
import ibex.visit.RhsDesugarer.ListUtil;

import java.util.Collections;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.LocalDecl;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.frontend.Globals;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * This visitor desugars RuleDecl nodes into MethodDecl nodes.
 * Note this is an _untyped_ translation and must be done before buildTypes.
 */
public class GrammarDesugarer extends NodeVisitor {
    
    private ClassDef currentClass;

    public GrammarDesugarer(ClassDef currentClass) {
        nf = (IbexNodeFactory) Globals.NF();
        ts = Globals.TS();
        this.
        currentClass = currentClass;
    }

    // static class RhsVarCollector extends Visitor {
    // Map<Name,Integer> varCounts;
    //
    // RhsVarCollector(final Map<Name,Integer> vars) {
    // this.varCounts = vars;
    // }
    //
    // @Override
    // public void visit(final RhsExpr n) {
    // n.visitChildren(this);
    // }
    //
    // @Override
    // public void visit(final RhsNonterminal n) {
    // final Name name = n.getName().getName();
    // Integer i = varCounts.get(name);
    // if (i == null) {
    // i = 0;
    // }
    // i++;
    // varCounts.put(name, i);
    // }
    // }
    //
    // static class RhsLabeler extends ResultVisitor<Node> {
    // Map<Name,Integer> vars;
    // Map<Name,Integer> varCounts;
    //
    // RhsLabeler(final Map<Name,Integer> varCounts_, final Map<Name,Integer>
    // vars_) {
    // this.vars = vars_;
    // this.varCounts = varCounts_;
    // }
    //
    // @Override
    // public Node visit(final Node n) {
    // return n;
    // }
    //
    // @Override
    // public Node visit(final RhsDef n) {
    // final RhsExpr e = n.getExp();
    // if (e instanceof RhsNonterminal) {
    // return n;
    // }
    // if (e instanceof RhsIteration && ((RhsIteration) e).getExp() instanceof
    // RhsNonterminal) {
    // return n;
    // }
    // return n.rewriteChildren(this);
    // }
    //
    // @Override
    // public Node visit(final RhsExpr n) {
    // return n.rewriteChildren(this);
    // }
    //
    // @Override
    // public Node visit(final RhsIteration n) {
    // if (n.getExp() instanceof RhsNonterminal) {
    // final RhsNonterminal nt = (RhsNonterminal) n.getExp();
    // return visitNT(nt.getName().getName(), n);
    // }
    // return n.rewriteChildren(this);
    // }
    //
    // @Override
    // public Node visit(final RhsSequence n) {
    // return n.rewriteChildren(this);
    // }
    //
    // @Override
    // public Node visit(final RhsNonterminal n) {
    // return visitNT(n.getName().getName(), n);
    // }
    //
    // public RhsExpr visitNT(final Name name, final RhsExpr e) {
    // final AstFactory F = new AstPositionFactory(e.getPosition());
    //
    // if (varCounts.get(name) > 1) {
    // Integer i = vars.get(name);
    // if (i == null) {
    // i = 0;
    // }
    // i++;
    // vars.put(name, i);
    // return F.RhsDef(F.VarDef(F.Id(Name.mk(name.toString() + i)), true,
    // ListUtil.list(), null), e);
    // }
    // return F.RhsDef(F.VarDef(F.Id(name), true, ListUtil.list(), null), e);
    // }
    // }

    @Override
    public Node leave(Node old, Node n, NodeVisitor v) {
        try {
            if (n instanceof RhsSequence) {
                return visit((RhsSequence) n);
            }
            if (n instanceof RhsCase) {
                return visit((RhsCase) n);
            }
            if (n instanceof RhsOption) {
                return visit((RhsOption) n);
            }
            if (n instanceof RuleDecl) {
                return visit((RuleDecl) n);
            }
        }
        catch (SemanticException e) {
            Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), e.position() != null ? e.position() : n.position());
        }
        return n;
    }

    public Node visit(RhsSequence n) {
        if (n.terms().size() == 1) {
            return n.terms().get(0);
        }
        return n;
    }

    IbexNodeFactory nf;
    TypeSystem ts;

    public Node visit(RhsOption n) throws SemanticException {
        if (n.item() instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) n.item();
            if (seq.terms().size() == 0)
                return seq;
            if (seq.terms().size() == 1)
                return nf.RhsOption(n.position(), seq.terms().get(0)).type(RhsOption_c.nullable(seq.terms().get(0).type()));
        }
        if (n.item() instanceof RhsPlus) {
            // (A+)? = A*
            final RhsPlus plus = (RhsPlus) n.item();
            return nf.RhsStar(n.position(), plus.item()).type(n.type());
        }
        if (n.item() instanceof RhsStar) {
            // (A*)? = A*
            return n.item();
        }
        if (n.item() instanceof RhsOption) {
            // (A?)? = A?
            return n.item();
        }
        return n;
    }
    
    public Node visit(RhsStar n) {
        if (n.item() instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) n.item();
            if (seq.terms().size() == 0)
                return seq;
//            if (seq.terms().size() == 1)
//                return nf.RhsStar(n.position(), seq.terms().get(0));
        }
        if (n.item() instanceof RhsPlus) {
            // (A+)* = A*
            final RhsPlus plus = (RhsPlus) n.item();
            return nf.RhsStar(n.position(), plus.item()).type(plus.type());
        }
        if (n.item() instanceof RhsStar) {
            // (A*)* = A*
            final RhsStar star = (RhsStar) n.item();
            return nf.RhsStar(n.position(), star.item()).type(star.type());
        }
        if (n.item() instanceof RhsOption) {
            // (A?)* = A*
            final RhsOption opt = (RhsOption) n.item();
            return nf.RhsStar(n.position(), opt.item()).type(ts.arrayOf(opt.item().type()));
        }
        return n;
    }
    
    public Node visit(RhsPlus n) {
        if (true)
            return nf.RhsSequence(n.position(), ListUtil.list(n.item(), nf.RhsStar(n.position(), n.item()).type(n.type()))).type(n.type());
        if (n.item() instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) n.item();
            if (seq.terms().size() == 0)
                return seq;
//            if (seq.terms().size() == 1)
//                return nf.RhsPlus(n.position(), seq.terms().get(0));
        }
        if (n.item() instanceof RhsPlus) {
            // (A+)+ = A+
            return n.item();
        }
        if (n.item() instanceof RhsStar) {
            // (A*)+ = A*
            return n.item();
        }
        if (n.item() instanceof RhsOption) {
            // (A?)+ = A*
            final RhsOption opt = (RhsOption) n.item();
            return nf.RhsStar(n.position(), opt.item()).type(ts.arrayOf(opt.item().type()));
        }
        return n;
    }

    public Node visit(final RhsCase n) {
        if (n.cases().size() == 1) {
            return n.cases().get(0);
        }
        return n;
    }

    class Result {
        RhsExpr e;
        List<LocalDecl> defs;
        List<Expr> exps;

        Result(final RhsExpr e_, final List<LocalDecl> defs_, final List<Expr> exps_) {
            assert defs_.size() == exps_.size();
            this.e = e_;
            this.defs = defs_;
            this.exps = exps_;
        }
    }

    public RhsExpr rewriteDefsAndActions(final RhsExpr n) {
        return n;
        //        
        // final IbexNodeFactory F = nf;
        // final Local x = F.Local(n.position(), F.Id(n.position(),
        // Name.make("rhs")));
        //
        // final Result r = n.accept(new FunctionVisitor<Expr,Result>() {
        // @Override
        // public Result visit(Node n1, Expr e) {
        // return null;
        // }
        // @Override
        // public Result visit(RhsLookahead n1, Expr e) {
        // // Drop any defs nested within a lookahead.
        // Result r1 = n1.rewriteChild(this, n1.getExp(), e);
        // return new Result(n1.newExp(r1.e), Collections.EMPTY_LIST,
        // Collections.EMPTY_LIST);
        // }
        // @Override
        // public Result visit(RhsExpr n1, Expr e) {
        // return new Result(n1, Collections.EMPTY_LIST,
        // Collections.EMPTY_LIST);
        // }
        // @Override
        // public Result visit(RhsTag n1, Expr e) {
        // Result r1 = n1.rewriteChild(this, n1.getExp(), e);
        // return new Result(n1.newExp(r1.e), r1.defs, r1.exps);
        // }
        // @Override
        // public Result visit(RhsDef n1, Expr e) {
        // List<IVarDef> defs = new ArrayList<IVarDef>();
        // List<Expr> exps = new ArrayList<Expr>();
        // Result r1 = n1.rewriteChild(this, n1.getExp(), e);
        // defs.addAll(r1.defs);
        // exps.addAll(r1.exps);
        // defs.add(n1.getDef());
        // exps.add(e);
        // return new Result(r1.e, defs, exps);
        // }
        // @Override
        // public Result visit(RhsSequence n1, Expr e) {
        // List<RhsExpr> elements = new ArrayList<RhsExpr>();
        // List<IVarDef> defs = new ArrayList<IVarDef>();
        // List<Expr> exps = new ArrayList<Expr>();
        // AstPositionFactory F_ = new AstPositionFactory(n1.getPosition());
        // boolean isList = isList(n1);
        // int j = 0;
        // for (int i = 0; i < n1.getElements().size(); i++) {
        // Expr e2 = e;
        // if (isList) {
        // IVarDef def = F_.VarDef(F_.Id(Name.mkNew()), true, ListUtil.list(),
        // e);
        // IVar var = F_.Var(def.getName());
        // Expr get = F_.MethodCall(var, F_.Id(Name.mk("get")), ListUtil.list(),
        // ListUtil.list(F_.Int(j)));
        // Expr len = F_.MethodCall(var, F_.Id(Name.mk("length")),
        // ListUtil.list(), ListUtil.list());
        // Expr lt = F_.MethodCall(F_.Int(j), F_.Id(Name.mk("<")),
        // ListUtil.list(), ListUtil.list(len));
        // Expr guarded = F_.If(F_.Equals(var, F_.Nil()), F_.Nil(), F_.If(lt,
        // get, F_.Nil()));
        // e2 = F_.Block(ListUtil.list(def), guarded);
        // }
        // Result r1 = n1.rewriteChild(this, n1.getElements().get(i), e2);
        // if (!(n1.getElements().get(i) instanceof RhsLookahead)) {
        // j++;
        // }
        // elements.add(r1.e);
        // defs.addAll(r1.defs);
        // exps.addAll(r1.exps);
        // }
        // return new Result(n1.newElements(elements), defs, exps);
        // }
        // private boolean isList(RhsSequence n1) {
        // int k = 0;
        // for (int i = 0; i < n1.getElements().size(); i++) {
        // if (!(n1.getElements().get(i) instanceof RhsLookahead)) {
        // k++;
        // }
        // }
        // boolean isList = k > 1;
        // return isList;
        // }
        // @Override
        // public Result visit(RhsOption n_, Expr e) {
        // Result res = n_.rewriteChild(this, n_.getExp(), e);
        // return new Result(n_.newExp(res.e), res.defs, res.exps);
        // }
        // @Override
        // public Result visit(RhsIteration n_, Expr e) {
        // if (n_.getExp() instanceof RhsSequence && isList((RhsSequence)
        // n_.getExp())) {
        // AstPositionFactory F_ = new AstPositionFactory(n_.getPosition());
        // IVarDef vdef = F_.VarDef(F_.Id(Name.mkNew("e")), true,
        // ListUtil.list(), null);
        // IVar v = F_.Var(vdef.getName());
        //
        // Result r_ = n_.rewriteChild(this, n_.getExp(), v);
        //
        // List<Expr> exps = new ArrayList<Expr>();
        // for (Expr ex : r_.exps) {
        // IVarDef def = F_.VarDef(F_.Id(Name.mkNew()), true, ListUtil.list(),
        // e);
        // IVar var = F_.Var(def.getName());
        // Expr e2 = F_.MethodCall(var, F_.Id(Name.mk("map")), ListUtil.list(),
        // ListUtil.list(F_.Closure(ListUtil.list(vdef), ex)));
        // e2 = F_.If(F_.Equals(var, F_.Nil()),
        // F_.ListConstructor(ListUtil.list()), e2);
        // e2 = F_.Block(ListUtil.list(def), e2);
        // exps.add(e2);
        // }
        //
        // return new Result(n_.newExp(r_.e), r_.defs, exps);
        // }
        // Result r1 = n_.rewriteChild(this, n_.getExp(), e);
        // return new Result(n_.newExp(r1.e), r1.defs, r1.exps);
        // }
        // @Override
        // public Result visit(RhsAction n_, Expr e) {
        // RhsExpr exp = n_.getExp();
        // Expr body = n_.getBody();
        //
        // AstPositionFactory F_ = new AstPositionFactory(n_.getPosition());
        // IVar x_ = F_.Var(F_.Id(Name.mkNew("rhs")));
        // Result r_ = n_.rewriteChild(this, exp, x_);
        //
        // List<IDef> defs = new ArrayList<IDef>(r_.defs.size());
        // for (int i = 0; i < r_.defs.size(); i++) {
        // defs.add(r_.defs.get(i).newExp(r_.exps.get(i)));
        // }
        //
        // RhsExpr m = n_.newExp(F_.RhsDef(F_.VarDef(x_.getName(), true,
        // ListUtil.list(), null), r_.e)).newBody(F_.Block(defs, body));
        // return new Result(m, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        // }
        // }, x);
        //
        // return r.e;
    }

    public RuleDecl rewriteDefsAndActions(final RuleDecl n) {
        return n.rhs(rewriteDefsAndActions(n.rhs()));
    }

    public RuleDecl addDefs(RuleDecl n) {
        return n;
    }

    public Node visit(RuleDecl n) throws SemanticException {
        // if (Globals.reporter().shouldTrace(1, "desugar.grammar"))
        // Globals.reporter().trace(1, "desugar.grammar", "translating " +
        // n.pretty());

        n = addDefs(n);
        n = rewriteDefsAndActions(n);

        // if (Globals.reporter().shouldTrace(1, "desugar.grammar"))
        // Globals.reporter().trace(1, "desugar.grammar",
        // "without left recursion " + n.pretty());

        final NodeFactory F = nf;
        final RhsExpr rhs = n.rhs();
        final RuleDecl n0 = n;
        
        RhsDesugarer v = new RhsDesugarer(n.position(), currentClass, n.formals().get(0).localDef());
        v = v.cont(v.new Cont() {
            @Override
            public Stmt onSuccess(Expr result) {
                assert nf != null;
                assert n0 != null;
                return nf.Return(n0.position(),
                                 outer().accept(result));
            }

            @Override
            public Stmt onFailure() {
                try {
                    Position pos = n0.position();
                    Type Failure = (Type) ts.systemResolver().find(QName.make("ibex.runtime.MatchFailureException"));
                    New neu = nf.New(pos, nf.CanonicalTypeNode(pos, Failure), ListUtil.list(), null);
                    ConstructorInstance ci = ts.findConstructor(Failure, ts.ConstructorMatcher(Failure, Collections.EMPTY_LIST, ts.emptyContext()));
                    neu = neu.constructorInstance(ci);
                    neu = (New) neu.type(Failure);
                    return outer().seq(outer().restore(rhs), F.Throw(n0.position(), neu));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError(e);
                }
            }
        });

        final Stmt e = (Stmt) rhs.visit(v);

        final Block body = v.block(v.seq(v.save(rhs, false), e));

        return n.body(body);
    }

    static String descriptiveString(final RhsExpr r) {
        if (r instanceof RhsStar) {
            RhsStar n = (RhsStar) r;
            return "zero or more " + descriptiveString(n.item());
        }
        else if (r instanceof RhsPlus) {
            RhsPlus n = (RhsPlus) r;
            return "one or more " + descriptiveString(n.item());
        }
        else if (r instanceof RhsOption) {
            RhsOption n = (RhsOption) r;
            return "zero or one " + descriptiveString(n.item());
        }
        else if (r instanceof RhsCase) {
            RhsCase n = (RhsCase) r;
            if (n.cases().size() == 0) {
                return "empty";
            }
            String sep = "";
            StringBuffer sb = new StringBuffer();
            int i = 0;
            for (RhsExpr e : n.cases()) {
                if (i > 3) {
                    sb.append("...");
                    break;
                }
                sb.append(sep);
                sb.append(descriptiveString(e));
                sep = " or ";
                i++;
            }
            return sb.toString();
        }
        else if (r instanceof RhsSequence) {
            RhsSequence n = (RhsSequence) r;
            if (n.terms().size() == 0) {
                return "empty";
            }
            return descriptiveString(n.terms().get(0));
        }
        else if (r instanceof RhsAnyChar) {
            return "a character";
        }
        else if (r instanceof RhsLit) {
            RhsLit n = (RhsLit) r;
            return n.lit().toString();
        }
        else if (r instanceof RhsInvoke) {
            RhsInvoke n = (RhsInvoke) r;
            return n.call().name().id().toString();
        }
        else if (r instanceof RhsRange) {
            RhsRange n = (RhsRange) r;
            return "'" + n.from() + "'..'" + n.to() + "'";
        }
        else {
            return "some text";
        }
    }
}
