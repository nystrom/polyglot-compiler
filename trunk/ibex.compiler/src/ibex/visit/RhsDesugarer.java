package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.MatchContext;
import ibex.ast.RhsAction;
import ibex.ast.RhsAnyChar;
import ibex.ast.RhsBind;
import ibex.ast.RhsCase;
import ibex.ast.RhsExpr;
import ibex.ast.RhsInvoke;
import ibex.ast.RhsIteration;
import ibex.ast.RhsIterationList;
import ibex.ast.RhsLit;
import ibex.ast.RhsLookahead;
import ibex.ast.RhsNegLookahead;
import ibex.ast.RhsOption;
import ibex.ast.RhsPlus;
import ibex.ast.RhsPlusList;
import ibex.ast.RhsPosLookahead;
import ibex.ast.RhsRange;
import ibex.ast.RhsSequence;
import ibex.ast.RhsStar;
import ibex.ast.RhsStarList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FloatLit;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.Formal;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.Try;
import polyglot.ast.Unary;
import polyglot.frontend.Globals;
import polyglot.types.ClassDef;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

public class RhsDesugarer extends NodeVisitor {
    RhsDesugarer.Cont cont;
    IbexNodeFactory nf;
    TypeSystem ts;
    private ClassDef currentClass;
    private LocalDef contextDef;

    RhsDesugarer(final Position pos, ClassDef currentClass, LocalDef contextDef) {
        this.nf = (IbexNodeFactory) Globals.NF();
        this.ts = Globals.TS();
        this.cont = null;
        this.currentClass = currentClass;
        this.contextDef = contextDef;
    }

    RhsDesugarer.Cont cont() {
        return cont;
    }

    RhsDesugarer cont(RhsDesugarer.Cont c) {
        RhsDesugarer v = (RhsDesugarer) copy();
        v.cont = c;
        return v;
    }

    @Override
    public Node override(Node n) {
        try {
            if (n instanceof MatchContext) {
                Local l = nf.Local(n.position(), nf.Id(n.position(), Name.make("context$")));
                LocalInstance li = contextDef.asInstance();
                return l.localInstance(li).type(li.type());
            }
            if (n instanceof RhsExpr) {
                if (n instanceof RhsAction)
                    return visit((RhsAction) n);
                if (n instanceof RhsAnyChar)
                    return visit((RhsAnyChar) n);
                if (n instanceof RhsBind)
                    return visit((RhsBind) n);
                if (n instanceof RhsCase)
                    return visit((RhsCase) n);
                if (n instanceof RhsInvoke)
                    return visit((RhsInvoke) n);
                if (n instanceof RhsLit)
                    return visit((RhsLit) n);
                if (n instanceof RhsNegLookahead)
                    return visit((RhsNegLookahead) n);
                if (n instanceof RhsPlus)
                    return visit((RhsPlus) n);
                if (n instanceof RhsPlusList)
                    return visit((RhsPlusList) n);
                if (n instanceof RhsPosLookahead)
                    return visit((RhsPosLookahead) n);
                if (n instanceof RhsOption)
                    return visit((RhsOption) n);
                if (n instanceof RhsRange)
                    return visit((RhsRange) n);
                if (n instanceof RhsSequence)
                    return visit((RhsSequence) n);
                if (n instanceof RhsStar)
                    return visit((RhsStar) n);
                if (n instanceof RhsStarList)
                    return visit((RhsStarList) n);
                assert false : "missing case for " + n;
                return nf.Empty(n.position());
            }
        }
        catch (SemanticException e) {
            Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), e.position());
            return n;
        }
        return null;
    }

    public Block block(Stmt s) {
        if (s instanceof Block)
            return (Block) s;
        return nf.Block(s.position(), s);
    }
    public Stmt seq(final Stmt... es) {
        return seq(Arrays.<Stmt> asList(es));
    }

    public Stmt seq(final List<Stmt> es) {
        final List<Stmt> exps = new ArrayList<Stmt>();
        Position pos = null;

        for (final Stmt e : es) {
            if (e instanceof Block) {
                final Block seq = (Block) e;
                exps.addAll(seq.statements());
            }
            else if (e != null) {
                exps.add(e);
            }

            if (pos == null)
                pos = e.position();
            else
                pos = new Position(pos, e.position());
        }

        if (exps.isEmpty())
            return nf.Empty(Position.COMPILER_GENERATED);

        if (exps.size() == 1)
            return exps.get(0);

        return nf.Block(pos, exps);
    }

    static class ListUtil {
        public static List list() {
            return Collections.EMPTY_LIST;
        }

        public static List list(Object e) {
            return Collections.singletonList(e);
        }

        public static List list(Object e1, Object e2) {
            return Arrays.asList(new Object[] { e1, e2 });
        }

        public static List list(Object e1, Object e2, Object e3) {
            return Arrays.asList(new Object[] { e1, e2, e3 });
        }
    }

    abstract class Cont {
        Cont() {
        }

        RhsDesugarer outer() {
            return RhsDesugarer.this;
        }

        abstract Stmt onFailure();
        abstract Stmt onSuccess(Expr e);

        @Override
        public String toString() {
            return "{ onSuccess = " + onSuccess(nf.StringLit(Position.COMPILER_GENERATED, "RESULT").type(ts.String())) + " onFailure = " + onFailure() + " }";
        }
    }

    public Stmt eval(Node n, Expr e) {
        Position pos = n.position();
        if (e instanceof Assign || e instanceof New || e instanceof Call)
            return nf.Eval(pos, e);
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            if (u.operator() == Unary.POST_DEC || u.operator() == Unary.POST_INC || u.operator() == Unary.PRE_DEC || u.operator() == Unary.PRE_INC)
                return nf.Eval(pos, e);
        }
        return freshVar(pos, "junk", true, e.type(), e);
    }

    public Stmt save(final RhsExpr n, final boolean lookahead) {
        try {
            Position pos = n.position();
            String name = lookahead ? "saveForLookahead" : "save";
            Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
            MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make(name), Collections.EMPTY_LIST, ts.emptyContext()));
            Call newCall = nf.Call(pos, matchContext(), nf.Id(pos, mi.name()));
            newCall = newCall.methodInstance(mi);
            newCall = (Call) newCall.type(mi.returnType());
            return eval(n, newCall);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }

    public Stmt restore(final RhsExpr n) {
        try {
            Position pos = n.position();
            String name = "restore";
            Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
            MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make(name), Collections.EMPTY_LIST, ts.emptyContext()));
            Call newCall = nf.Call(pos, matchContext(), nf.Id(pos, mi.name()));
            newCall = newCall.methodInstance(mi);
            newCall = (Call) newCall.type(mi.returnType());
            return eval(n, newCall);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }

    public Expr accept(final Expr n) {
        try {
            Position pos = n.position();
            String name = "accept";
            Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
            MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make(name), Collections.singletonList(n.type()), ts.emptyContext()));
            Call newCall = nf.Call(pos, matchContext(), nf.Id(pos, mi.name()), n);
            newCall = newCall.methodInstance(mi);
            newCall = (Call) newCall.type(mi.returnType());
            if (ts.isSubtype(mi.returnType(), n.type(), ts.emptyContext())) {
                return newCall;
            }
            else if (n.type().isNull()) {
                return newCall;
            }
            else {
                return nf.Cast(pos, nf.CanonicalTypeNode(pos, Types.ref(n.type())), newCall).type(n.type());
            }
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }

    public Expr mkNull(final Node n) {
        return nf.NullLit(n.position()).type(ts.Null());
    }

    public Expr fail() {
        Position pos = Position.COMPILER_GENERATED;

        try {
            Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
            MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make("fail"), ListUtil.list(), null));
            Call newCall = nf.Call(pos, matchContext(), nf.Id(pos, Name.make("fail")));
            newCall = newCall.methodInstance(mi);
            newCall = (Call) newCall.type(mi.returnType());
            return newCall;
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }


    }

    public Expr applyRule(String key, final Call call) throws SemanticException {
        Expr n = call;
        Position pos = n.position();
        
        n = requalifyThis(n);

        Type Action = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IAction"));
        Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
        Type Failure = (Type) ts.systemResolver().find(QName.make("ibex.runtime.MatchFailureException"));

        Formal formal = nf.Formal(pos, nf.FlagsNode(pos, Flags.FINAL), nf.CanonicalTypeNode(pos, MatchContext), nf.Id(pos, Name.make("context$")));
        formal = formal.localDef(ts.localDef(pos, formal.flags().flags(), formal.type().typeRef(), formal.name().id()));

        MethodDecl md = nf.MethodDecl(pos, nf.FlagsNode(pos, Flags.PUBLIC), nf.CanonicalTypeNode(pos, ts.Object()), nf.Id(pos, Name.make("apply")), ListUtil.list(formal), ListUtil.list(nf.CanonicalTypeNode(pos, Failure)),
                                       nf.Block(pos, nf.Return(pos, n)));
        md = md.methodDef(ts.methodDef(pos, Types.ref(currentClass.asType()), Flags.PUBLIC, Types.ref(ts.Object()), Name.make("apply"),
                                       ListUtil.list(formal.type().typeRef()), ListUtil.list(Types.ref(Failure))));
        ClassBody cb = nf.ClassBody(pos, ListUtil.list(md));
        New neu = nf.New(pos, nf.CanonicalTypeNode(pos, Action), ListUtil.list(), cb);
        ClassDef anonDef = ts.createClassDef();
        anonDef.kind(ClassDef.ANONYMOUS);
        anonDef.flags(Flags.NONE);
        anonDef.setPackage(currentClass.package_());
        anonDef.setJob(currentClass.job());
        anonDef.outer(Types.ref(currentClass));
        anonDef.superType(Types.ref(Action));
        anonDef.addMethod(md.methodDef());
        neu = neu.anonType(anonDef);
        ConstructorDef ci = ts.defaultConstructor(pos, Types.ref(anonDef.asType()));
        anonDef.addConstructor(ci);
        neu = neu.constructorInstance(ci.asInstance());
        neu = (New) neu.type(anonDef.asType());
        MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make("applyRule"), ListUtil.list(ts.String(), Action), null));
        Call newCall = nf.Call(pos, matchContext(), nf.Id(pos, mi.name()),
                                nf.StringLit(pos, key).type(ts.String()), neu);
        newCall = newCall.methodInstance(mi);
        newCall = (Call) newCall.type(mi.returnType());
        return unbox(newCall, call.type());
    }

    private <T extends Node> T requalifyThis(T n) {
        return (T) n.visit(new NodeVisitor() {
                @Override
                public Node leave(Node old, Node n, NodeVisitor v) {
                    if (n instanceof Special) {
                        Special s = (Special) n;
                        return s.qualifier(nf.CanonicalTypeNode(s.position(), currentClass.asType()));
                    }
                    return n;
                }
            });
    }
    
    public Expr unbox(Expr e, Type t) {
        if (ts.isSubtype(e.type(), t, ts.emptyContext())) {
            return e;
        }
        if (t.isNull() || e.type().isNull()) {
            return e;
        }
        if (t.isReference()) {
            Position pos = e.position();
            return nf.Cast(pos, nf.CanonicalTypeNode(pos, t), e).type(t);
        }
        if (t.isBoolean()) {
            return unboxObjectToPrimitive(e, "java.lang.Boolean", "booleanValue");
        }
        if (t.isChar()) {
            return unboxObjectToPrimitive(e, "java.lang.Character", "charValue");
        }
        if (t.isByte()) {
            return unboxObjectToPrimitive(e, "java.lang.Byte", "byteValue");
        }
        if (t.isShort()) {
            return unboxObjectToPrimitive(e, "java.lang.Short", "shortValue");
        }
        if (t.isInt()) {
            return unboxObjectToPrimitive(e, "java.lang.Integer", "intValue");
        }
        if (t.isLong()) {
            return unboxObjectToPrimitive(e, "java.lang.Long", "longValue");
        }
        if (t.isFloat()) {
            return unboxObjectToPrimitive(e, "java.lang.Float", "floatValue");
        }
        if (t.isDouble()) {
            return unboxObjectToPrimitive(e, "java.lang.Double", "doubleValue");
        }
        throw new InternalCompilerError("unexpected type " + t);
    }

    private Expr unboxObjectToPrimitive(Expr e, String Boxed, String unboxedValue) {
        try {
            Position pos = e.position();
            Type T = (Type) ts.systemResolver().find(QName.make(Boxed));
//            if (e.type().isNull()) {
//                if (Boxed.equals("java.lang.Boolean"))
//                    return nf.BooleanLit(pos, false).type(ts.Boolean());
//                if (Boxed.equals("java.lang.Character"))
//                    return nf.CharLit(pos, '0').type(ts.Boolean());
//                if (Boxed.equals("java.lang.Byte"))
//                    return nf.Cast(pos, nf.CanonicalTypeNode(pos, ts.Byte()), nf.IntLit(pos, IntLit.INT, 0).type(ts.Int())).type(ts.Byte());
//                if (Boxed.equals("java.lang.Short"))
//                    return nf.Cast(pos, nf.CanonicalTypeNode(pos, ts.Short()), nf.IntLit(pos, IntLit.INT, 0).type(ts.Int())).type(ts.Short());
//                if (Boxed.equals("java.lang.Integer"))
//                    return nf.IntLit(pos, IntLit.INT, 0).type(ts.Int());
//                if (Boxed.equals("java.lang.Long"))
//                    return nf.IntLit(pos, IntLit.LONG, 0L).type(ts.Long());
//                if (Boxed.equals("java.lang.Float"))
//                    return nf.FloatLit(pos, FloatLit.FLOAT, 0.0f).type(ts.Float());
//                if (Boxed.equals("java.lang.Double"))
//                    return nf.FloatLit(pos, FloatLit.DOUBLE, 0.0).type(ts.Double());
//            }
            Expr boxed;
            if (ts.isSubtype(e.type(), T, ts.emptyContext()))
                boxed = e;
            else
                boxed = nf.Cast(pos, nf.CanonicalTypeNode(pos, T), e).type(T);
            MethodInstance mi = ts.findMethod(boxed.type(), ts.MethodMatcher(boxed.type(), Name.make(unboxedValue), ListUtil.list(), null));
            Call unboxed = nf.Call(Position.COMPILER_GENERATED, boxed, nf.Id(Position.COMPILER_GENERATED, Name.make(unboxedValue)));
            unboxed = unboxed.methodInstance(mi);
            unboxed = (Call) unboxed.type(mi.returnType());
            return unboxed;
        }
        catch (SemanticException ex) {
            throw new InternalCompilerError(ex);
        }
    }

    public Expr matchContext() {
        return nf.Local(Position.COMPILER_GENERATED, nf.Id(Position.COMPILER_GENERATED, Name.make("context$"))).localInstance(contextDef.asInstance()).type(contextDef.asInstance().type());
    }

    public Expr nextMatchesAny() throws SemanticException {
        Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
        MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make("nextMatchesAny"), ListUtil.list(), null));
        Call newCall = nf.Call(Position.COMPILER_GENERATED, matchContext(), nf.Id(Position.COMPILER_GENERATED, Name.make("nextMatchesAny")));
        newCall = newCall.methodInstance(mi);
        newCall = (Call) newCall.type(mi.returnType());

        return unboxChar(newCall);
    }

    private Expr unboxChar(Expr e) throws SemanticException {
        if (e.type().isChar()) {
            return e;
        }
        else {
            return unboxObjectToPrimitive(e, "java.lang.Character", "charValue");
        }
    }

    public Expr nextMatchesRange(final Expr lo, final Expr hi) throws SemanticException {
        Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
        MethodInstance mi = ts.findMethod(MatchContext,
                                          ts.MethodMatcher(MatchContext, Name.make("nextMatchesRange"), ListUtil.list(lo.type(), hi.type()), null));
        Call newCall = nf.Call(Position.COMPILER_GENERATED, matchContext(), nf.Id(Position.COMPILER_GENERATED, Name.make("nextMatchesRange")), lo, hi);
        newCall = newCall.methodInstance(mi);
        newCall = (Call) newCall.type(mi.returnType());

        return unboxChar(newCall);
    }

    public Expr nextMatches(final char c) throws SemanticException {
        return nextMatches(String.valueOf(c));
    }

    public Expr nextMatches(final String c) throws SemanticException {
        return nextMatches(nf.StringLit(Position.COMPILER_GENERATED, c).type(ts.String()));
    }

    public Expr nextMatches(final Expr c) throws SemanticException {
        Type MatchContext = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
        MethodInstance mi = ts.findMethod(MatchContext, ts.MethodMatcher(MatchContext, Name.make("nextMatches"), ListUtil.list(c.type()), null));
        Call newCall = nf.Call(Position.COMPILER_GENERATED, matchContext(), nf.Id(Position.COMPILER_GENERATED, Name.make("nextMatches")), c);
        newCall = newCall.methodInstance(mi);
        newCall = (Call) newCall.type(mi.returnType());
        
        if (c.type().isChar())
            return unboxChar(newCall);
        
        
        if (ts.isSubtype(newCall.type(), ts.String(), ts.emptyContext())) {
            return newCall;
        }
        Type CharSeq = (Type) ts.systemResolver().find(QName.make("java.lang.CharSequence"));
        if (ts.isSubtype(newCall.type(), CharSeq, ts.emptyContext())) {
            return nf.Cast(c.position(), nf.CanonicalTypeNode(c.position(), Types.ref(ts.String())), newCall).type(ts.String());
        }
        
        throw new InternalCompilerError("nextMatches did not return a CharSequence or char", c.position());
    }

    public Expr isnull(final Expr e) {
        return nf.Binary(Position.COMPILER_GENERATED, e, Binary.EQ, nf.NullLit(Position.COMPILER_GENERATED).type(ts.Null())).type(ts.Boolean());
    }

    public Expr nonnull(final Expr e) {
        return nf.Binary(Position.COMPILER_GENERATED, e, Binary.NE, nf.NullLit(Position.COMPILER_GENERATED).type(ts.Null())).type(ts.Boolean());
    }

    public Expr matched(final Expr e) throws SemanticException {
        return nf.Binary(e.position(), e, Binary.NE, fail()).type(ts.Boolean());
    }

    public Expr notMatched(final Expr e) {
        return nf.Binary(e.position(), e, Binary.EQ, fail()).type(ts.Boolean());
    }

    public Expr not(final Expr e) {
        return nf.Unary(e.position(), Unary.NOT, e).type(ts.Boolean());
    }

    public Stmt visit(final RhsLit n) throws SemanticException {
        // [ result, matched ] = r.nextMatches(x);
        // if (matched)
        // yes(result)
        // else
        // no
        Type MatchResult = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchResult"));
        final LocalDecl resultDef = freshVar(n.position(), "matchResult", true, n.lit().type(), nextMatches(n.lit()));
        final Local result = local(resultDef);
        Position pos = n.position();
        return seq(tryCatch(seq(resultDef, cont.onSuccess(result)), cont.onFailure()));
//        return nf.Block(n.position(), resultDef, nf.If(n.position(), matched(result), alt.onSuccess(result), alt.onFailure()));
    }
    
    public Try tryCatch(Stmt try_, Stmt catch_) {
        try {
            Position pos = try_.position();
            Type Failure = (Type) ts.systemResolver().find(QName.make("ibex.runtime.MatchFailureException"));
            LocalDecl ld = freshVar(pos, "exception", true, Failure);
            Formal formal = nf.Formal(pos, ld.flags(), ld.type(), ld.name());
            formal = formal.localDef(ld.localDef());
            return nf.Try(pos, block(try_), Collections.singletonList(nf.Catch(try_.position(), formal, block(catch_))));
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }

    public Stmt visit(final RhsRange n) throws SemanticException {
        // [ result, matched ] = r.nextMatchesRange(x, y);
        // if (matched)
        // yes(result)
        // else
        // no
        final char lo = (Character) n.from().constantValue();
        final char hi = (Character) n.to().constantValue();

        assert lo <= hi;

        Expr call;

        if (lo == hi) {
            call = nextMatches(lo);
        }
        else {
            call = nextMatchesRange(n.from(), n.to());
        }

        return
        tryCatch(cont.onSuccess(call), cont.onFailure());
//
//        Type MatchResult = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchResult"));
//        final LocalDecl resultDef = freshVar(n.position(), "matchResult", true, MatchResult, call);
//        final Expr result = local(resultDef);
//
//        return nf.Block(n.position(), resultDef, nf.If(n.position(), matched(result), alt.onSuccess(result), alt.onFailure()));
    }

    public Local local(LocalDecl d) {
        return (Local) nf.Local(d.position(), d.name()).localInstance(d.localDef().asInstance()).type(d.localDef().type().get());
    }

    public Stmt visit(final RhsAnyChar n) throws SemanticException {
        // [ result, matched ] = r.nextMatchesAny();
        // if (matched)
        // yes(result)
        // else
        // no
        
        Expr call = nextMatchesAny();
        return tryCatch(cont.onSuccess(call), cont.onFailure());
    }

    public Stmt visit(final RhsAction n) {
        
        Position pos = n.position();
        ClassDef anonDef = ts.createClassDef();
        anonDef.kind(ClassDef.ANONYMOUS);
        anonDef.flags(Flags.NONE);
        anonDef.setPackage(currentClass.package_());
        anonDef.setJob(currentClass.job());
        anonDef.outer(Types.ref(currentClass));
        anonDef.superType(Types.ref(ts.Object()));
        
        Stmt s = requalifyThis(n.body());
        
        MethodDecl md = nf.MethodDecl(pos, nf.FlagsNode(pos, Flags.NONE), nf.CanonicalTypeNode(pos, n.type()), nf.Id(pos, Name.make("apply")), Collections.EMPTY_LIST, Collections.EMPTY_LIST, nf.Block(pos, s));
        MethodDef mi = ts.methodDef(pos, Types.ref((StructType) anonDef.asType()), Flags.NONE, Types.ref(n.type()), Name.make("apply"), Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        anonDef.addMethod(mi);
        md = md.methodDef(mi);
        ClassBody body = nf.ClassBody(pos, Collections.<ClassMember>singletonList(md));
        New neu = nf.New(pos, nf.CanonicalTypeNode(pos, ts.Object()), ListUtil.list(), body);
        neu = neu.anonType(anonDef);
        ConstructorDef ci = ts.defaultConstructor(pos, Types.ref(anonDef.asType()));
        anonDef.addConstructor(ci);
        neu = neu.constructorInstance(ci.asInstance());
        neu = (New) neu.type(anonDef.asType());
        Call call = nf.Call(pos, neu, nf.Id(pos, Name.make("apply")));
        call = call.methodInstance(mi.asInstance());
        call = (Call) call.type(mi.asInstance().returnType());
        final Call call0 = call;
        
        Cont k = new Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                return seq(eval(e, e), cont.onSuccess(call0));
            }
            
            @Override
            Stmt onFailure() {
                return cont.onFailure();
            }
        };
        
        return        (Stmt) n.item().visit(cont(k));
    }

    public Stmt visit(final RhsBind n) {
//        final Stmt rest = (Stmt) n.rest().visit(this);
//        
//        Cont k = new Cont() {
//            @Override
//            Stmt onSuccess(Expr e) {
//                return seq(n.decl().init(e), rest);
//            }
//            
//            @Override
//            Stmt onFailure() {
//                return cont.onFailure();
//            }
//        };
//        
        
        Cont k = new Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                return seq(n.decl().init(e), cont.onSuccess(local(n.decl())));
            }
            
            @Override
            Stmt onFailure() {
                return cont.onFailure();
            }
        };
        return (Stmt) n.decl().init().visit(cont(k));
    }

    public Stmt visit(final RhsInvoke n) throws SemanticException {
        Call e = (Call) n.call().visit(this);

        // result = e(r)
        // result = rule_A(r);
        // if (result.matched)
        // result
        // else
        // alt

        // p.A
        // -->
        // context$.apply("C.A", new IAction() { Result apply(Context context$) { return p.A(context$) ) })

        String key = e.name().id().toString();
//        String key = e.methodInstance().container().toString() + "." + e.methodInstance().name().toString();

        Expr apply = applyRule(key, e);

        
//        Type MatchResult = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchResult"));
//        final LocalDecl resultDef = freshVar(n.position(), "result", true, MatchResult, apply);
        final LocalDecl resultDef = freshVar(n.position(), "result", true, n.call().type(), apply);
        final Expr result = local(resultDef);

//        final Stmt block = nf.Block(n.position(), resultDef,
//                                     nf.If(n.position(), matched(result),
//                                            cont.onSuccess(result), cont.onFailure()));
//
//        return block;
        return tryCatch(seq(resultDef, cont.onSuccess(result)), cont.onFailure());
    }

    public Stmt visit(final RhsOption n) {
        // [[ e ]] { };

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(final Expr e) {
                return cont.onSuccess(e);
            }

            @Override
            Stmt onFailure() {
                return cont.onSuccess(mkNull(n));
            }
        };

        return (Stmt) n.item().visit(this.cont(k));
    }

    public Stmt visit(final RhsStar n) throws SemanticException {

        // result = [];
        // while (true) {
        //    if (matched [[ e ]]) result.add(tmp) else break
        // }

        final Type ArrayList = (Type) ts.systemResolver().find(QName.make("java.util.ArrayList"));
        final MethodInstance mi = ts.findMethod(ArrayList, ts.MethodMatcher(ArrayList, Name.make("add"), Collections.singletonList(ts.Object()), ts.emptyContext()));

        Position pos = n.position();
        New initialList = nf.New(pos, nf.CanonicalTypeNode(pos, ArrayList), ListUtil.list());
        ConstructorInstance ci = ts.findConstructor(ArrayList, ts.ConstructorMatcher(ArrayList, Collections.EMPTY_LIST, ts.emptyContext()));
        initialList = initialList.constructorInstance(ci);
        initialList = (New) initialList.type(ArrayList);

        final LocalDecl listDef = freshVar(n.position(), "list", true, ArrayList, initialList);
        final Local list = local(listDef);
        final Expr array = listToArray(pos, list, n.item().type());

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                Position pos = e.position();
                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                addCall = (Call) addCall.type(mi.returnType());
                addCall = addCall.methodInstance(mi);
                return eval(e, addCall);
            }

            @Override
            Stmt onFailure() {
                return nf.Break(n.position(), null);
            }
        };

        final Stmt loopBody = (Stmt) n.item().visit(cont(k));
        
        return seq(listDef, nf.While(n.position(), nf.BooleanLit(n.position(), true).type(ts.Boolean()), loopBody), cont.onSuccess(array));
    }

    private Expr listToArray(Position pos, final Expr list, Type itemType) throws SemanticException {
        String convertName;
        
        if (itemType.isBoolean())
            convertName = "booleanArray";
        else if (itemType.isByte())
            convertName = "byteArray";
        else if (itemType.isChar())
            convertName = "charArray";
        else if (itemType.isShort())
            convertName = "shortArray";
        else if (itemType.isInt())
            convertName = "intArray";
        else if (itemType.isLong())
            convertName = "longArray";
        else if (itemType.isFloat())
            convertName = "floatArray";
        else if (itemType.isDouble())
            convertName = "doubleArray";
        else
            convertName = "objectArray";
        
        Type Arrays = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Arrays"));

        MethodInstance mi = ts.findMethod(Arrays, ts.MethodMatcher(Arrays, Name.make(convertName), Collections.singletonList(list.type()), ts.emptyContext()));
        Call c = nf.Call(pos, nf.CanonicalTypeNode(pos, Arrays), nf.Id(pos, Name.make(convertName)), list);
        c = c.methodInstance(mi);
        c = (Call) c.type(mi.returnType());
        return c;
        
    }
    
    public Stmt visit(final RhsPlus n) throws SemanticException {

        // matched = true;
        // tmp = [[ e ]] { matched = false };
        // result = [tmp];
        // if (matched)
        // while (true) {
        // tmp = [[ e ]] { break };
        // result := result + [tmp];
        // }
        // else
        // alt

        // A+ is implemented the same as A*, but evaluates A (and initializes
        // the list with the result) before entering the loop.

        Type ArrayList = (Type) ts.systemResolver().find(QName.make("java.util.ArrayList"));
        final MethodInstance mi = ts.findMethod(ArrayList, ts.MethodMatcher(ArrayList, Name.make("add"), Collections.singletonList(ts.Object()), ts.emptyContext()));
      
        Position pos = n.position();
        New initialList = nf.New(pos, nf.CanonicalTypeNode(pos, ArrayList), ListUtil.list());
        ConstructorInstance ci = ts.findConstructor(ArrayList, ts.ConstructorMatcher(ArrayList, Collections.EMPTY_LIST, ts.emptyContext()));
        initialList = initialList.constructorInstance(ci);
        initialList = (New) initialList.type(ArrayList);

        final LocalDecl listDef = freshVar(n.position(), "list", true, ArrayList, initialList);
        final Local list = local(listDef);
        final Expr array = listToArray(pos, list, n.item().type());

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                Position pos = e.position();
                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                addCall = (Call) addCall.type(mi.returnType());
                addCall = addCall.methodInstance(mi);
                return eval(e, addCall);
            }

            @Override
            Stmt onFailure() {
                return nf.Break(n.position(), null);
            }
        };

        final Stmt loopBody = (Stmt) n.item().visit(cont(k));

        RhsDesugarer.Cont k2 = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                Position pos = e.position();
                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                addCall = (Call) addCall.type(mi.returnType());
                addCall = addCall.methodInstance(mi);
                Stmt s = eval(e, addCall);
                return seq(s, nf.While(pos, nf.BooleanLit(pos, true).type(ts.Boolean()), loopBody), cont.onSuccess(array));
            }

            @Override
            Stmt onFailure() {
                return cont.onFailure();
            }
        };

        final Stmt ifwrapper = (Stmt) n.item().visit(cont(k2));
        return seq(listDef, ifwrapper);
    }

    public Stmt visit(final RhsStarList n) throws SemanticException {
        final Position pos = n.position();
        
        // [[ e ]] { };

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(final Expr e) {
                return cont.onSuccess(e);
            }

            @Override
            Stmt onFailure() {
                Type t = n.type().toArray().base();
                Expr zero = nf.IntLit(pos, IntLit.INT, 0).type(ts.Int());
                NewArray na = (NewArray) nf.NewArray(pos, nf.CanonicalTypeNode(pos, t), Collections.singletonList(zero)).type(n.type());
                return cont.onSuccess(na);
            }
        };

        RhsPlusList p = (RhsPlusList) nf.RhsPlusList(pos, n.item(), n.sep()).type(n.type());
        return (Stmt) p.visit(this.cont(k));
    }
    
    public Stmt visit(final RhsPlusList n) throws SemanticException {
        Type ArrayList = (Type) ts.systemResolver().find(QName.make("java.util.ArrayList"));
        final MethodInstance mi = ts.findMethod(ArrayList, ts.MethodMatcher(ArrayList, Name.make("add"), Collections.singletonList(ts.Object()), ts.emptyContext()));
        
        Position pos = n.position();
        New initialList = nf.New(pos, nf.CanonicalTypeNode(pos, ArrayList), ListUtil.list());
        ConstructorInstance ci = ts.findConstructor(ArrayList, ts.ConstructorMatcher(ArrayList, Collections.EMPTY_LIST, ts.emptyContext()));
        initialList = initialList.constructorInstance(ci);
        initialList = (New) initialList.type(ArrayList);
        
        final LocalDecl listDef = freshVar(n.position(), "list", true, ArrayList, initialList);
        final Local list = local(listDef);
        final Expr array = listToArray(pos, list, n.item().type());
        
        final RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                Position pos = e.position();
                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                addCall = (Call) addCall.type(mi.returnType());
                addCall = addCall.methodInstance(mi);
                return eval(e, addCall);
            }
            
            @Override
            Stmt onFailure() {
                return nf.Break(n.position(), null);
            }
        };
        
        final Stmt itemMatch = (Stmt) n.item().visit(cont(k));
        
        RhsDesugarer.Cont k1 = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                // Discard the sep.
                return seq(eval(e, e), itemMatch);
            }
            
            @Override
            Stmt onFailure() {
                return nf.Break(n.position(), null);
            }
        };
        
        final Stmt loopBody = (Stmt) n.sep().visit(cont(k1));
        
        RhsDesugarer.Cont k2 = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                Position pos = e.position();
                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                addCall = (Call) addCall.type(mi.returnType());
                addCall = addCall.methodInstance(mi);
                Stmt s = eval(e, addCall);
                return seq(s, nf.While(pos, nf.BooleanLit(pos, true).type(ts.Boolean()), loopBody), cont.onSuccess(array));
            }
            
            @Override
            Stmt onFailure() {
                return cont.onFailure();
            }
        };
        
        final Stmt matchFirst = (Stmt) n.item().visit(cont(k2));
        return seq(listDef, matchFirst);
    }

    public Stmt visit(final RhsPosLookahead n) {

        // r.saveForLookahead();
        // matched = true;
        // [[ e ]] { matched = false; }
        // r.restore();
        // if (matched)
        // null
        // else
        // alt

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                return seq(eval(e,e), restore(n), cont.onSuccess(mkNull(n)));
            }

            @Override
            Stmt onFailure() {
                return seq(restore(n), cont.onFailure());
            }
        };

        final Stmt e = (Stmt) n.item().visit(cont(k));
        return seq(save(n, true), e);
    }

    public Stmt visit(final RhsNegLookahead n) {
        // r.saveForLookahead();
        // matched = true;
        // [[ e ]] { matched = false; }
        // r.restore();
        // if (matched)
        // alt
        // else
        // null

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                return seq(eval(e,e), restore(n), cont.onFailure());
            }

            @Override
            Stmt onFailure() {
                return seq(restore(n), cont.onSuccess(mkNull(n)));
            }
        };

        Stmt e = (Stmt) n.item().visit(cont(k));
        return seq(save(n, true), e);
    }

    public Stmt visit(final RhsCase n) {
        // [[ x / y ]]
        // =
        // save(n);
        // matched = true;
        // result = [[ x ]] { matched = false }
        // if (matched)
        // accept(result);
        // else
        // restore();
        // [[ y ]] alt

        if (n.cases().size() == 0) {
            return cont.onSuccess(mkNull(n));
        }
        
        if (n.cases().size() == 1) {
            return (Stmt) n.cases().get(0).visit(this);
        }
        
        final Stmt opt = optimizeDisjointCases(n, cont);
        if (opt != null) {
            return opt;
        }

        final RhsExpr rn1 = n.cases().get(n.cases().size()-1);

        RhsDesugarer.Cont k = new RhsDesugarer.Cont() {
            @Override
            Stmt onSuccess(Expr e) {
                return cont.onSuccess(accept(e));
            }

            @Override
            Stmt onFailure() {
                return seq(restore(rn1), cont.onFailure());
            }
        };

        Stmt en1 = (Stmt) rn1.visit(cont(k));
        Stmt exp = seq(save(rn1, false), en1);
        
        for (int i = n.cases().size()-2; i >= 0; i--) {
            final RhsExpr ri = n.cases().get(i);
            
            final Stmt prev = exp;

            RhsDesugarer.Cont ki = new RhsDesugarer.Cont() {
                @Override
                Stmt onSuccess(Expr e) {
                    return cont.onSuccess(accept(e));
                }

                @Override
                Stmt onFailure() {
                    return seq(restore(ri), prev);
                }
            };

            final Stmt ei = (Stmt) ri.visit(cont(ki));
            exp = seq(save(ri, false), ei);
        }

        return exp;
    }

    // Expr evalElement(RhsExpr n, Expr matched, Expr result) {
    // return F(n).DoWhile(null, seq(F(n).Assign(result, n.accept(this,
    // F(n).Break(null))), F(n).Assign(matched, F(n).Bool(true))),
    // F(n).Bool(false));
    // }

    public Stmt visit(final RhsSequence n) throws SemanticException {
        // [[ x y ]]
        // ->
        // matched = true;
        // result = [];
        // tmp = [[ x ]] { matched = false; break; }
        // result := result + [tmp]
        // if (matched)
        // tmp = [[ y ]] { matched = false; break; }
        // result := result + [tmp]
        // else {}
        // if (matched)
        // result
        // else
        // alt

        if (n.terms().size() == 0) {
            return cont.onSuccess(mkNull(n));
        }
        else if (n.terms().size() == 1) {
            return (Stmt) n.terms().get(0).visit(this);
        }
        else {
            int count = 0;

            for (final RhsExpr r : n.terms()) {
                if (!(r instanceof RhsLookahead))
                    count++;
            }

            final LocalDecl matchedDef = freshVar(n.position(), "matched", false, ts.Boolean(), nf.BooleanLit(n.position(), true).type(ts.Boolean()));
            final Expr matched = local(matchedDef);

            if (count > 1) {
                assert n.type() != null : "null type for " + n;
                assert n.type().isArray():  "sequence of " + count + " but type is " + n.type();
                
                Position pos = n.position();
                final Type ArrayList = (Type) ts.systemResolver().find(QName.make("java.util.ArrayList"));
                New initialList = nf.New(pos, nf.CanonicalTypeNode(pos, ArrayList), ListUtil.list());
                ConstructorInstance ci = ts.findConstructor(ArrayList, ts.ConstructorMatcher(ArrayList, Collections.EMPTY_LIST, ts.emptyContext()));
                initialList = initialList.constructorInstance(ci);
                initialList = (New) initialList.type(ArrayList);

                final MethodInstance mi = ts.findMethod(ArrayList, ts.MethodMatcher(ArrayList, Name.make("add"), Collections.singletonList(ts.Object()), ts.emptyContext()));
                
                final LocalDecl listDef = freshVar(n.position(), "result", false, ArrayList, initialList);
                final Expr list = local(listDef);
                final Expr array = listToArray(pos, list, n.type().toArray().base());

                final List<Stmt> exps = new ArrayList<Stmt>();

                for (int i = 0; i < n.terms().size(); i++) {
                    final RhsExpr r = n.terms().get(i);
                    final int index = i;

                    final Stmt e = (Stmt) r.visit(cont(new RhsDesugarer.Cont() {
                        @Override
                        Stmt onSuccess(Expr e) {
                            Position pos = r.position();
                            if (r instanceof RhsLookahead) {
                                return nf.Empty(pos);
                            }
                            else
                            if (! (r instanceof RhsOption)
                                    && (r instanceof RhsSequence || r instanceof RhsIteration || r instanceof RhsIterationList)
                                    && r.type().isArray()) {
                                LocalDecl i0 = freshVar(pos, "i", false, ts.Int(), nf.IntLit(pos, IntLit.INT, 0L).type(ts.Int()));
                                Local i = local(i0);
                                LocalDecl tmp0 = freshVar(pos, "e", true, e.type(), e);
                                Local tmp = local(tmp0);
                                Field length = nf.Field(pos, tmp, nf.Id(pos, Name.make("length")));
                                length = length.fieldInstance(r.type().toArray().lengthField());
                                length = (Field) length.type(ts.Int());
                                Expr ipp = nf.Unary(pos, i, Unary.POST_INC).type(ts.Int());
                                Expr tmpi = nf.ArrayAccess(pos, tmp, i).type(r.type().toArray().base());
                                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), tmpi);
                                addCall = (Call) addCall.type(mi.returnType());
                                addCall = addCall.methodInstance(mi);
                                return seq(tmp0, nf.For(pos, Collections.<ForInit>singletonList(i0), nf.Binary(pos, i, Binary.LT, length).type(ts.Boolean()), Collections.<ForUpdate>singletonList((Eval) eval(e, ipp)), eval(e, addCall)));
                            }
                            else {
                                Call addCall = nf.Call(pos, list, nf.Id(pos, Name.make("add")), e);
                                addCall = (Call) addCall.type(mi.returnType());
                                addCall = addCall.methodInstance(mi);
                                return eval(e, addCall);
                            }
                        }

                        @Override
                        Stmt onFailure() {
                            Position pos = r.position();
                            return nf.Eval(pos, nf.Assign(pos, matched, Assign.ASSIGN, nf.BooleanLit(pos, false).type(ts.Boolean())).type(ts.Boolean()));
                        }
                    }));

                    if (index == 0)
                        exps.add(e);
                    else
                        exps.add(nf.If(r.position(), matched, e));
                }
                
                exps.add(nf.If(n.position(), matched, cont.onSuccess(accept(array)), seq(restore(n), cont.onFailure())));
                return seq(save(n, false), nf.Block(n.position(), matchedDef, listDef), seq(exps));
            }
            else if (false) {
                // FIXME: n.type might not be nullable, so should throw exception instead.
                final Expr init = mkNull(n);
                final LocalDecl resultDef = freshVar(n.position(), "result", false, n.type(), init);
                final Expr result = local(resultDef);

                final List<Stmt> exps = new ArrayList<Stmt>();

                for (int i = 0; i < n.terms().size(); i++) {
                    final RhsExpr r = n.terms().get(i);
                    final int index = i;

                    final Stmt e = (Stmt) r.visit(cont(new RhsDesugarer.Cont() {
                        @Override
                        Stmt onSuccess(Expr e) {
                            Position pos = r.position();
                            if (r instanceof RhsLookahead) {
                                return nf.Empty(pos);
                            }
                            else {
                                Assign a = (Assign) nf.Assign(pos, result, Assign.ASSIGN, e).type(e.type());
                                return nf.Eval(pos, a);
                            }
                        }

                        @Override
                        Stmt onFailure() {
                            Position pos = r.position();
                            return nf.Eval(pos, nf.Assign(pos, matched, Assign.ASSIGN, nf.BooleanLit(pos, false).type(ts.Boolean())).type(ts.Boolean()));
                        }
                    }));

                    if (index == 0)
                        exps.add(e);
                    else
                        exps.add(nf.If(r.position(), matched, e));
                }
                
                exps.add(nf.If(n.position(), matched, cont.onSuccess(accept(result)), seq(restore(n), cont.onFailure())));
                return seq(save(n, false), nf.Block(n.position(), matchedDef, resultDef), seq(exps));
            }
            else if (count == 1) {
                final LocalDecl resultDef = freshVar(n.position(), "result", true, n.type(), null);
                final Expr result = local(resultDef);
                
                final List<Stmt> exps = new ArrayList<Stmt>();
                
                for (int i = 0; i < n.terms().size(); i++) {
                    final RhsExpr r = n.terms().get(i);
                    
                    if (r instanceof RhsLookahead) {
                        final Stmt e = (Stmt) r.visit(cont(new RhsDesugarer.Cont() {
                            @Override
                            Stmt onSuccess(Expr e) {
                                Position pos = r.position();
                                return nf.Empty(pos);
                            }

                            @Override
                            Stmt onFailure() {
                                return seq(restore(n), cont.onFailure());
                            }
                        }));

                        exps.add(e);
                    }
                    else {
                        final Stmt e = (Stmt) r.visit(cont(new RhsDesugarer.Cont() {
                            @Override
                            Stmt onSuccess(Expr e) {
                                assert ts.isImplicitCastValid(e.type(), result.type(), ts.emptyContext()) : "bad assign of " + e + " for " + n;;
                                Position pos = r.position();
                                Assign a = (Assign) nf.Assign(pos, result, Assign.ASSIGN, e).type(e.type());
                                return nf.Eval(pos, a);
                            }

                            @Override
                            Stmt onFailure() {
                                return seq(restore(n), cont.onFailure());
                            }
                        }));
                        
                        exps.add(e);
                    }
                }
                
                exps.add(cont.onSuccess(accept(result)));
                return seq(save(n, false), resultDef, seq(exps));
            }
            else {
                assert count == 0;
                
                final List<Stmt> exps = new ArrayList<Stmt>();
                
                for (int i = 0; i < n.terms().size(); i++) {
                    final RhsExpr r = n.terms().get(i);
                    
                    final Stmt e = (Stmt) r.visit(cont(new RhsDesugarer.Cont() {
                        @Override
                        Stmt onSuccess(Expr e) {
                            Position pos = r.position();
                            if (r instanceof RhsLookahead) {
                                return nf.Empty(pos);
                            }
                            else {
                                return eval(r, e);
                            }
                        }
                        
                        @Override
                        Stmt onFailure() {
                            return seq(restore(n), cont.onFailure());
                        }
                    }));
                    
                    exps.add(e);
                }
                
                exps.add(cont.onSuccess(accept(mkNull(n))));
                return seq(save(n, false), seq(exps));
            }

        }
    }

    protected LocalDecl freshVar(final Position pos, final String nameBase, final boolean isVal, Type t) {
        if (t.isNull())
            t = ts.Object();
        LocalDecl def = nf.LocalDecl(pos, isVal ? nf.FlagsNode(pos, Flags.FINAL) : nf.FlagsNode(pos, Flags.NONE), nf.CanonicalTypeNode(pos, t),
                                               nf.Id(pos, Name.makeFresh(nameBase)), null);
        LocalDef ld = ts.localDef(pos, def.flags().flags(), Types.ref(t), def.name().id());
        def = def.localDef(ld);
        return def;
    }


    protected LocalDecl freshVar(final Position pos, final String nameBase, final boolean isVal, Type t, final Expr exp) {
        if (t == null)
            t = exp.type();
        return freshVar(pos, nameBase, isVal, t).init(exp);
    }

    private Stmt optimizeDisjointCases(final RhsCase n, final RhsDesugarer.Cont alt) {
        // TODO Auto-generated method stub
        return null;
    }
}
