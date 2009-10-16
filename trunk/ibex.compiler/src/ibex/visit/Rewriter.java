package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.RhsAction;
import ibex.ast.RhsBind;
import ibex.ast.RhsExpr;
import ibex.ast.RhsOr;
import ibex.ast.RhsSequence;
import ibex.ast.RuleDecl;
import ibex.ast.RuleDecl_c;
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
import ibex.types.RuleInstance;
import ibex.types.Symbol;
import ibex.types.Terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayInit;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Case;
import polyglot.ast.Cast;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Special;
import polyglot.ast.Stmt;
import polyglot.ast.StringLit;
import polyglot.ast.Switch;
import polyglot.ast.SwitchBlock;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which traverses the AST constructing type objects. */
public class Rewriter extends ContextVisitor
{
    GLR glr;

    public Rewriter(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        glr = null;
    }

    public IbexTypeSystem glrTypeSystem() {
        return (IbexTypeSystem) typeSystem();
    }

    public IbexNodeFactory glrNodeFactory() {
        return (IbexNodeFactory) nodeFactory();
    }

    public GLR glr() {
        return glr;
    }

    ArrayList<TypeNode> orphanInterfaces;

    protected NodeVisitor enterCall(Node parent, Node n) throws SemanticException {
        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            IbexClassDef def = (IbexClassDef) cd.classDef();
            this.glr = def.glr();
            
            orphanInterfaces = new ArrayList<TypeNode>();
        }

        return super.enterCall(parent, n);
    }
    
    @Override
    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        // Use the context used to visit children to get the current class.
        ContextVisitor v_ = (ContextVisitor) v;
        Context c = v_.context();
        IbexTypeSystem ts = (IbexTypeSystem) this.ts;

//        if (n instanceof RuleDecl) {
//            IbexClassDef pt = (IbexClassDef) c.currentClassDef();
//            List l = ((RuleDecl_c) n).rewrite(this);
//            members(pt).addAll(l);
//        }
//        else if (n instanceof ClassMember) {
//            IbexClassDef pt = (IbexClassDef) c.currentClassDef();
//            members(pt).add((ClassMember) n);
//        }

        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            IbexClassDef def = (IbexClassDef) cd.classDef();
            IbexClassType pt = (IbexClassType) def.asType();

            // Ensure the class implements ibex.runtime.ParserImpl
            // and not just ibex.runtime.Parser
            ClassType impl = ts.runtimeParserImplType();

            if (pt.isParser()) {
                List<TypeNode> interfaces = new ArrayList<TypeNode>(cd.interfaces());
                interfaces.addAll(orphanInterfaces);
                return cd.interfaces(interfaces);
            }
        }
        
        if (n instanceof ClassBody) {
            ClassBody cb = (ClassBody) n;

            IbexClassDef def = (IbexClassDef) c.currentClassDef();
            {
                IbexClassType pt = (IbexClassType) def.asType();

                // Ensure the class implements ibex.runtime.ParserImpl
                // and not just ibex.runtime.Parser
                ClassType impl = ts.runtimeParserImplType();

                if (pt.isParser() && ! ts.isSubtype(pt, impl, context)) {
                    def.addInterface(Types.ref(impl));
                    orphanInterfaces.add(nf.CanonicalTypeNode(n.position(), impl));
                }
            }
            
            List<ClassMember> members = new ArrayList<ClassMember>();
            
            // Add semantic action methods.
            for (ClassMember m : cb.members()) {
                if (!(m instanceof RuleDecl)) {
                    members.add(m);
                }
            }

            // Add semantic action methods.
            for (ClassMember m : cb.members()) {
                if (m instanceof RuleDecl) {
                    RuleDecl d = (RuleDecl) m;
                    members.addAll(semanticActions(def, d.rule(), d.rhs()));
                }
            }

            // Add rule methods.
            for (ClassMember m : cb.members()) {
                if (m instanceof RuleDecl) {
                    RuleDecl d = (RuleDecl) m;
                    if (glr.isStartSymbol(d.rule().asNonterminal()))
                        members.add(ruleMethod(def, d.rule()));
                }
            }

            members.addAll(addGlrTableMembers(def, glr));
//            addGlrSemanticActions(def, glr);
            
            members.add(scanMethod(def));
            members.add(semanticActionDispatcher(def));
            
            return cb.members(members);
        }

        return super.leaveCall(old, n, v);
    }
    
    MethodDecl semanticActionDispatcher(IbexClassDef def) throws SemanticException {
        Position pos = def.position();
        Name name = Name.make("semanticAction");

        List<Formal> formals = new ArrayList<Formal>();
        
        LocalDef ruleLd = ts.localDef(pos, Flags.FINAL, Types.ref(ts.Int()), Name.make("rule"));
        Formal ruleFormal = nf.Formal(pos, nf.FlagsNode(pos, ruleLd.flags()), nf.CanonicalTypeNode(pos, ruleLd.type()), nf.Id(pos, ruleLd.name()));
        ruleFormal = ruleFormal.localDef(ruleLd);
        formals.add(ruleFormal);
        
        LocalDef argsLd = ts.localDef(pos, Flags.FINAL, Types.ref(ts.arrayOf(ts.Object())), Name.make("args"));
        Formal argsFormal = nf.Formal(pos, nf.FlagsNode(pos, argsLd.flags()), nf.CanonicalTypeNode(pos, argsLd.type()), nf.Id(pos, argsLd.name()));
        argsFormal = argsFormal.localDef(argsLd);
        formals.add(argsFormal);
        
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        
//        Type Terminal = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Terminal"));
//        Type IOE = (Type) ts.systemResolver().find(QName.make("java.io.IOException"));
//        Type Util = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Util"));
//        
//        Special this_ = (Special) nf.This(pos).type(def.asType());
//    
//        MethodInstance mi = ts.findMethod(Util, ts.MethodMatcher(Util, Name.make("scanChar"), Collections.<Type>singletonList(def.asType()), context));
//        Call scan = nf.Call(pos, nf.CanonicalTypeNode(pos, Util), nf.Id(pos, mi.name()), this_);
//        scan = scan.methodInstance(mi);
//        scan = (Call) scan.type(mi.returnType());
        
        Block body = nf.Block(pos, nf.Return(pos, nf.NullLit(pos).type(ts.Null())));
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(ts.Object()), name, argTypes, Collections.EMPTY_LIST);
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    MethodDecl scanMethod(IbexClassDef def) throws SemanticException {
        Position pos = def.position();
        Name name = Name.make("scanTerminal");

        List<Formal> formals = Collections.EMPTY_LIST;
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        
        Type Terminal = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Terminal"));
        Type IOE = (Type) ts.systemResolver().find(QName.make("java.io.IOException"));
        Type Util = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Util"));
        
        Special this_ = (Special) nf.This(pos).type(def.asType());
        
        MethodInstance mi = ts.findMethod(Util, ts.MethodMatcher(Util, Name.make("scanChar"), Collections.<Type>singletonList(def.asType()), context));
        Call scan = nf.Call(pos, nf.CanonicalTypeNode(pos, Util), nf.Id(pos, mi.name()), this_);
        scan = scan.methodInstance(mi);
        scan = (Call) scan.type(mi.returnType());
        
        Block body = nf.Block(pos, nf.Return(pos, scan));
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(Terminal), name, argTypes, Collections.<Ref<? extends Type>>singletonList(Types.ref(IOE)));
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.<TypeNode>singletonList(nf.CanonicalTypeNode(pos, IOE)), body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    Type boxedType(Type t) throws SemanticException {
        if (t.isBoolean())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Boolean"));
        if (t.isChar())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Character"));
        if (t.isByte())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Byte"));
        if (t.isShort())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Short"));
        if (t.isInt())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Integer"));
        if (t.isLong())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Long"));
        if (t.isFloat())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Float"));
        if (t.isDouble())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Double"));
        assert t.isReference();
        return t;
    }

    MethodInstance unboxMethod(Type t) throws SemanticException {
        Type T = boxedType(t);
        if (t.isBoolean())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("booleanValue"), Collections.EMPTY_LIST, context));
        if (t.isChar())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("charValue"), Collections.EMPTY_LIST, context));
        if (t.isByte())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("byteValue"), Collections.EMPTY_LIST, context));
        if (t.isShort())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("shortValue"), Collections.EMPTY_LIST, context));
        if (t.isInt())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("intValue"), Collections.EMPTY_LIST, context));
        if (t.isLong())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("longValue"), Collections.EMPTY_LIST, context));
        if (t.isFloat())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("floatValue"), Collections.EMPTY_LIST, context));
        if (t.isDouble())
            return ts.findMethod(T, ts.MethodMatcher(T, Name.make("doubleValue"), Collections.EMPTY_LIST, context));
        assert false;
        return null;
    }
    
    Expr unbox(Type t, Expr e) throws SemanticException {
        Position pos = e.position();
        Cast         cast = nf.Cast(pos, nf.CanonicalTypeNode(pos, boxedType(t)), e);
        cast = (Cast) cast.type(boxedType(t));
        if (t.isReference())
            return cast;
        
        MethodInstance mi = unboxMethod(t);
        Call unboxed = nf.Call(pos, cast, nf.Id(pos, mi.name()));
        unboxed = unboxed.methodInstance(mi);
        unboxed = (Call) unboxed.type(mi.returnType());
        return unboxed;
    }
    
    MethodDecl ruleMethod(IbexClassDef def, RuleDef rule) throws SemanticException {
        Position pos = def.position();
        Name name = rule.name();
        
        List<Formal> formals = Collections.EMPTY_LIST;
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        
        // T A() throws IOE { return (T) new GLRDriver(this).parse(i, j); }
        
        Type Driver = (Type) ts.systemResolver().find(QName.make("ibex.runtime.GLRDriver"));
        Type IOE = (Type) ts.systemResolver().find(QName.make("java.io.IOException"));
        
        Special this_ = (Special) nf.This(pos).type(def.asType());
        
        ConstructorInstance ci = ts.findConstructor(Driver, ts.ConstructorMatcher(Driver, Collections.<Type>singletonList(def.asType()), context));
   
        New neu = nf.New(pos, nf.CanonicalTypeNode(pos, Driver), Collections.<Expr>singletonList(this_));
        neu = neu.constructorInstance(ci);
        neu = (New) neu.type(ci.container());
        
        MethodInstance mi = ts.findMethod(Driver, ts.MethodMatcher(Driver, Name.make("parse"), Arrays.<Type>asList(ts.Int(), ts.Int()), context));
        Nonterminal s = rule.asNonterminal();
        Expr startState = nf.IntLit(pos, IntLit.INT, glr.startStateNumber(s)).type(ts.Int());
        Expr startSym = nf.IntLit(pos, IntLit.INT, glr.startSymbolNumber(s)).type(ts.Int());
        Call parse = nf.Call(pos, neu, nf.Id(pos, mi.name()), startState, startSym);
        parse = parse.methodInstance(mi);
        parse = (Call) parse.type(mi.returnType());
        
        Stmt stmt;
        if (! rule.type().get().isVoid())
            stmt = nf.Return(pos, unbox(rule.type().get(), parse));
        else
            stmt = nf.Eval(pos, parse);
        
        Block body = nf.Block(pos, stmt);
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), rule.flags(), rule.type(), name, argTypes, Collections.<Ref<? extends Type>>singletonList(Types.ref(IOE)));
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.<TypeNode>singletonList(nf.CanonicalTypeNode(pos, IOE)), body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }

    private List<Ref<? extends Type>> argTypesOfFormals(List<Formal> formals) {
        List<Ref<? extends Type>> argTypes = new ArrayList<Ref<? extends Type>>();
        for (Formal f : formals) {
            argTypes.add(f.type().typeRef());
        }
        return argTypes;
    }
    MethodDecl action(IbexClassDef def, RuleDef rule, RhsAction e, int index) {
        RhsExpr item = e.item();
        List<Formal> formals = formals(item);
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        Position pos = e.position();
        Name name = Name.make("action$" + rule.name() + "$" + index);
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PROTECTED, Types.ref(e.type()), name, argTypes, Collections.EMPTY_LIST);
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, e.body());
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    private List<Formal> formals(RhsExpr item) {
        if (item instanceof RhsBind) {
          RhsBind e = (RhsBind) item;
          return Collections.singletonList(formal(e.decl().localDef()));
        }
        else if (item.localDef() != null) {
            return Collections.singletonList(formal(item.localDef()));
        }
        else if (item instanceof RhsSequence) {
            RhsSequence            seq = (RhsSequence) item;
            List<Formal> l = new ArrayList<Formal>(seq.terms().size());
            for (RhsExpr e : seq.terms()) {
                l.addAll(formals(e));
            }
            return l;
        }
        else {
            return Collections.EMPTY_LIST;
        }
    }

    private Formal formal(LocalDef ld) {
        Position pos = ld.position();
        Formal f = nf.Formal(pos, nf.FlagsNode(pos, ld.flags()), nf.CanonicalTypeNode(pos, ld.type()), nf.Id(pos, ld.name()));
        f = f.localDef(ld);
        return f;
    }

    List<ClassMember> semanticActions(IbexClassDef def, RuleDef rule, RhsExpr e) {
        List<ClassMember> l = new ArrayList<ClassMember>();
        List<RhsExpr> q = new ArrayList<RhsExpr>();
        q.add(e);
        while (! q.isEmpty()) {
            RhsExpr ei = q.remove(q.size()-1);
            if (ei instanceof RhsOr) {
                RhsOr r = (RhsOr) ei;
                q.add(r.right());
                q.add(r.left());
            }
            else if (ei instanceof RhsAction) {
                l.add(action(def, rule, (RhsAction) ei, l.size()));
            }
        }
        return l;
    }

//    protected void addGlrMergeFunctions(IbexClassDef pt, GLR glr)
//        throws SemanticException
//    {
//        IbexTypeSystem ts = (IbexTypeSystem) pt.typeSystem();
//
//        // add method:
//        // Object merge(int symbol, Object o1, Object o2) {
//        //     switch (symbol) {
//        //         case 0:
//        //             return marshal(merge$N0(unmarshal(o1), unmarshal(o1)));
//        //         ...
//        //     }
//        //     return ((Mergeable) o1).merge(o2);
//        // }
//
//        Position pos = Position.COMPILER_GENERATED;
//
//        Type objectArray = ts.arrayOf(pos, ts.Object());
//
//        ClassType mergeable = ts.runtimeMergeableType();
//        TypeNode mtn = nf.CanonicalTypeNode(pos, mergeable);
//        TypeNode otn = nf.CanonicalTypeNode(pos, ts.Object());
//        TypeNode atn = nf.CanonicalTypeNode(pos, objectArray);
//        TypeNode itn = nf.CanonicalTypeNode(pos, ts.Int());
//
//        MethodInstance mmi;
//
//        try {
//            mmi = ts.findMethod(mergeable, ts.MethodMatcher(mergeable, Name.make("merge"),
//                                                            Collections.<Type>singletonList(mergeable), context));
//        }
//        catch (SemanticException e) {
//            throw new InternalCompilerError(e);
//        }
//
//        LocalDef rule1LI = ts.localDef(pos, Flags.NONE, Types.ref(ts.Int()), Name.make("rule1"));
//        LocalDef rule2LI = ts.localDef(pos, Flags.NONE, Types.ref(ts.Int()), Name.make("rule2"));
//        LocalDef sval1LI = ts.localDef(pos, Flags.NONE, Types.ref(ts.arrayOf(pos, ts.Object())), Name.make("sval1"));
//        LocalDef sval2LI = ts.localDef(pos, Flags.NONE, Types.ref(ts.arrayOf(pos, ts.Object())), Name.make("sval2"));
//
//        Local rule1 = nf.Local(pos, nf.Id(pos, rule1LI.name()));
//        rule1 = rule1.localInstance(rule1LI.asInstance());
//        rule1 = (Local) rule1.type(rule1LI.asInstance().type());
//
//        Local rule2 = nf.Local(pos, nf.Id(pos, rule2LI.name()));
//        rule2 = rule2.localInstance(rule2LI.asInstance());
//        rule2 = (Local) rule2.type(rule2LI.asInstance().type());
//
//        Local sval1 = nf.Local(pos, nf.Id(pos, sval1LI.name()));
//        sval1 = sval1.localInstance(sval1LI.asInstance());
//        sval1 = (Local) sval1.type(sval1LI.asInstance().type());
//
//        Local sval2 = nf.Local(pos, nf.Id(pos, sval2LI.name()));
//        sval2 = sval2.localInstance(sval2LI.asInstance());
//        sval2 = (Local) sval2.type(sval2LI.asInstance().type());
//
//        List elements = new ArrayList(2*glr.numRules());
//
//        // build the case arms for nonterminals with explicit merge actions.
//        for (Iterator<RuleDef> i = pt.rules().iterator(); i.hasNext(); ) {
//            RuleDef def = i.next();
//            RuleInstance lhs = def.asInstance();
//            Nonterminal nt = new Nonterminal_c(ts, pos, lhs);
//            
//            for (Iterator<Rhs> j = lhs.choices().iterator(); j.hasNext(); ) {
//                Rhs choice = j.next();
//
//                if (! (choice instanceof RAnd)) {
//                    continue;
//                }
//
//                RAnd merge = (RAnd) choice;
//                Rhs rhs1 = merge.choice1();
//                Rhs rhs2 = merge.choice2();
//
//                int ruleNumber1 = glr.ruleNumber(nt, rhs1);
//                int ruleNumber2 = glr.ruleNumber(nt, rhs2);
//                boolean flipped = false;
//
//                // The lower numbered rule should be first.
//                if (ruleNumber2 <= ruleNumber1) {
//                    int tmp = ruleNumber1;
//                    ruleNumber1 = ruleNumber2;
//                    ruleNumber2 = tmp;
//
//                    Rhs tmpRhs = rhs1;
//                    rhs1 = rhs2;
//                    rhs2 = tmpRhs;
//
//                    flipped = true;
//                }
//
//                int rule = (ruleNumber1 << 16) | ruleNumber2;
//
//                // case i:
//                IntLit label = nf.IntLit(pos, IntLit.INT, rule);
//                label = (IntLit) label.type(ts.Int());
//                Case case_ = nf.Case(pos, label);
//                case_ = case_.value(rule);
//                elements.add(case_);
//
//                // build the call:
//                // merge$lhs$rhs1$rhs2(unmarshal(rhs1), unmarshal(rhs2))
//                List code = new ArrayList();
//
//                Name methodName = merge.actionMethodName(lhs);
//                List<Ref<? extends Type>> argTypes = merge.actionMethodFormalTypes();
//
//                MethodDef mi = ts.methodDef(
//                    pos, Types.ref(pt.asType()), Flags.PROTECTED, Types.ref(lhs.type()), methodName,
//                    argTypes, Collections.EMPTY_LIST);
//
//                List args = new ArrayList();
//
//                // Unmarshal in the same order as the rules are declared
//                // in the source file, since the action method takes
//                // its arguments in that order.
//                if (flipped) {
//                    unmarshalSvals(rhs2, sval2, code, args, pt);
//                    unmarshalSvals(rhs1, sval1, code, args, pt);
//                }
//                else {
//                    unmarshalSvals(rhs1, sval1, code, args, pt);
//                    unmarshalSvals(rhs2, sval2, code, args, pt);
//                }
//
//                if (args.size() != argTypes.size()) {
//                    throw new InternalCompilerError("Argument count mismatch between " + mi + " and " + args);
//                }
//
//                Special this_ = nf.This(pos);
//                this_ = (Special) this_.type(pt.asType());
//                Call call = nf.Call(pos, this_, nf.Id(pos, mi.name()), args);
//                call = call.methodInstance(mi.asInstance());
//                call = (Call) call.type(mi.asInstance().returnType());
//
//                // marshal the result and return it
//                Return ret = nf.Return(pos, marshal(call, mi.asInstance().returnType(), pt));
//                code.add(ret);
//
//                Block b = nf.Block(pos, code);
//                SwitchBlock sb = nf.SwitchBlock(pos, Collections.<Stmt>singletonList(b));
//                elements.add(sb);
//            }
//        }
//
//        // build the switch statement.
//        List stmts = new ArrayList(2);
//
//        if (! elements.isEmpty()) {
//            Expr sixteen = nf.IntLit(pos, IntLit.INT, 16);
//            sixteen = sixteen.type(ts.Int());
//            Expr shift = nf.Binary(pos, rule1, Binary.SHL, sixteen);
//            shift = shift.type(ts.Int());
//            Expr index = nf.Binary(pos, shift, Binary.BIT_OR, rule2);
//            index = index.type(ts.Int());
//
//            Switch s = nf.Switch(pos, index, elements);
//            stmts.add(s);
//        }
//
//        // build the default fall-through case:
//        //         return null;
//        Expr null_ = nf.NullLit(pos);
//        null_ = null_.type(ts.Null());
//        Return retN = nf.Return(pos, null_);
//        stmts.add(retN);
//
///*
//        // build the default fall-through case:
//        //     if (sval1 != null)
//        //         return ((Mergeable) sval1).merge((Mergeable) sval2);
//        //     else if (sval2 != null)
//        //         return ((Mergeable) sval2).merge((Mergeable) sval1);
//        //     else
//        //         return null;
//        Cast cast1 = nf.Cast(pos, mtn, o1);
//        Cast cast2 = nf.Cast(pos, mtn, o2);
//
//        Call call1 = nf.Call(pos, cast1, mmi.name(),
//                            Collections.singletonList(cast2));
//        call1 = call1.methodInstance(mmi);
//        call1 = (Call) call1.type(mmi.returnType());
//
//        Call call2 = nf.Call(pos, cast2, mmi.name(),
//                            Collections.singletonList(cast1));
//        call2 = call2.methodInstance(mmi);
//        call2 = (Call) call2.type(mmi.returnType());
//
//        Expr null_ = nf.NullLit(pos);
//        null_ = null_.type(ts.Null());
//
//        Return ret1 = nf.Return(pos, call1);
//        Return ret2 = nf.Return(pos, call2);
//        Return retN = nf.Return(pos, null_);
//
//        Expr eqNull1 = nf.Binary(pos, o1, Binary.NE, null_);
//        eqNull1 = eqNull1.type(ts.Boolean());
//        Expr eqNull2 = nf.Binary(pos, o2, Binary.NE, null_);
//        eqNull2 = eqNull2.type(ts.Boolean());
//
//        If if2 = nf.If(pos, eqNull2, ret2, retN);
//        If if1 = nf.If(pos, eqNull1, ret1, if2);
//
//        stmts.add(if1);
//*/
//
//        // build the method body.
//        Block b = nf.Block(pos, stmts);
//
//        // now build the method declaration.
//        Formal rule1f = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), itn, nf.Id(pos, rule1LI.name()));
//        rule1f = rule1f.localDef(rule1LI);
//        Formal rule2f = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), itn, nf.Id(pos, rule2LI.name()));
//        rule2f = rule2f.localDef(rule2LI);
//        Formal sval1f = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), atn, nf.Id(pos, sval1LI.name()));
//        sval1f = sval1f.localDef(sval1LI);
//        Formal sval2f = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), atn, nf.Id(pos, sval2LI.name()));
//        sval2f = sval2f.localDef(sval2LI);
//
//        List formals = new ArrayList(2);
//        formals.add(rule1f);
//        formals.add(rule2f);
//        formals.add(sval1f);
//        formals.add(sval2f);
//
//        MethodDef md;
//
//        List formalTypes = new ArrayList(4);
//        formalTypes.add(rule1LI.type());
//        formalTypes.add(rule2LI.type());
//        formalTypes.add(sval1LI.type());
//        formalTypes.add(sval2LI.type());
//
//        try {
//            MethodInstance mi;
//            mi = ts.findMethod(ts.runtimeParserImplType(), 
//                               ts.MethodMatcher(ts.runtimeParserImplType(), Name.make("mergeAction"),
//                                                formalTypes, context));
//            mi = (MethodInstance) mi.copy();
//            md = (MethodDef) mi.def().copy();
//            md.setFlags(Flags.PUBLIC);
//            md.setContainer(Types.ref(pt.asType()));
//      }
//        catch (SemanticException e) {
//            throw new InternalCompilerError(e);
//        }
//
//        MethodDecl m = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), otn, nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, b);
//        m = m.methodDef(md);
//
//        pt.addMethod(md);
//
//        members(pt).add(m);
//    }

//    protected void unmarshalSvals(RSeq rhs, Expr sval, List code, List args, IbexClassDef pt) {
//        Position pos = Position.COMPILER_GENERATED;
//
//        int k = 0;
//
//        Iterator<Rhs> ei = rhs.items().iterator();
//        Iterator<Name> ti = rhs.tags().iterator();
//
//        while (ei.hasNext() && ti.hasNext()) {
//            Rhs s = (Rhs) ei.next();
//            Name tag = (Name) ti.next();
//
//            if (tag != null) {
//                  // k: the index into the sval array
//                  IntLit index = nf.IntLit(pos, IntLit.INT, k);
//                  index = (IntLit) index.type(ts.Int());
//
//                  // sval[k]
//                  ArrayAccess access = nf.ArrayAccess(pos, sval, index);
//                  access = (ArrayAccess) access.type(ts.Object());
//
//                  LocalDef li = ts.localDef(pos, Flags.NONE, Types.ref(s.type()), tag);
//
//                  // T tag = (T) sval[k]
//                  LocalDecl d = nf.LocalDecl(pos, nf.FlagsNode(pos, li.flags()),
//                      nf.CanonicalTypeNode(pos, li.type()), nf.Id(pos, li.name()),
//                      unmarshal(access, li.type().get(), (IbexClassType) pt.asType()));
//                  d = d.localDef(li);
//
//                  code.add(d);
//
//                  Local l = nf.Local(pos, d.name());
//                  l = l.localInstance(li.asInstance());
//                  l = (Local) l.type(li.asInstance().type());
//
//                  args.add(l);
//            }
//
//            k++;
//        }
//    }

    protected Expr unmarshal(Expr e, Type t, IbexClassType pt) {
        TypeSystem ts = pt.typeSystem();
        Position pos = Position.COMPILER_GENERATED;

        try {
            if (t.isReference()) {
                TypeNode tn = nf.CanonicalTypeNode(pos, t);
                Cast cast = nf.Cast(pos, tn, e);
                cast = (Cast) cast.type(t);
                return cast;
            }
            else if (t.isPrimitive()) {
                // ((Integer) e).intValue();

                // Get Integer
                String wrapperName = ts.wrapperTypeString(t.toPrimitive());
                ClassType wrapperType = (ClassType) ts.systemResolver().find(QName.make(wrapperName));
                TypeNode tn = nf.CanonicalTypeNode(pos, wrapperType);

                // (Integer) e
                Cast cast = nf.Cast(pos, tn, e);
                cast = (Cast) cast.type(wrapperType);

                // "intValue"
                Name methodName = Name.make(t.toPrimitive().name() + "Value");

                MethodInstance mi = ts.findMethod(wrapperType,
                                                  ts.MethodMatcher(wrapperType, methodName, Collections.EMPTY_LIST, context));

                // ((Integer) e).intValue()
                Call call = nf.Call(pos, cast, nf.Id(pos, methodName), Collections.EMPTY_LIST);
                call = call.methodInstance(mi);
                call = (Call) call.type(mi.returnType());

                return call;
            }
        }
        catch (SemanticException ex) {
            throw new InternalCompilerError(ex);
        }

        throw new InternalCompilerError("Cannot unmarshal type " + t);
    }

    protected Expr marshal(Expr e, Type t, IbexClassDef pt) {
        TypeSystem ts = pt.typeSystem();
        Position pos = Position.COMPILER_GENERATED;

        try {
            if (t.isReference()) {
                return e;
            }
            else if (t.isPrimitive()) {
                // new Integer(e);

                // Integer
                String wrapperName = ts.wrapperTypeString(t.toPrimitive());
                ClassType wrapperType = (ClassType) ts.systemResolver().find(QName.make(wrapperName));
                TypeNode tn = nf.CanonicalTypeNode(pos, wrapperType);

                ConstructorInstance ci = ts.findConstructor(wrapperType,
                                                            ts.ConstructorMatcher(wrapperType, Collections.singletonList(t), context));

                // new Integer(e)
                New new_ = nf.New(pos, tn, Collections.singletonList(e));
                new_ = (New) new_.constructorInstance(ci);
                new_ = (New) new_.type(wrapperType);

                return new_;
            }
        }
        catch (SemanticException ex) {
            throw new InternalCompilerError(ex);
        }

        throw new InternalCompilerError("Cannot marshal type " + t);
    }

//    protected void addGlrSemanticActions(IbexClassDef pt, GLR glr)
//        throws SemanticException
//    {
//        IbexTypeSystem ts = (IbexTypeSystem) pt.typeSystem();
//
//        // add method:
//        // Object semanticAction(int rule, Object[] sval) {
//        //    switch (rule) {
//        //        case 0:
//        //            // Int ::= LP ID:x RP Int:y { return 0; }
//        //            return new Integer(actions$Int$LP$ID$RP$Int(
//        //                (Identifier) sval[1],
//        //                ((Integer) sval[3]).intValue()));
//        //        ...
//        //        default:
//        //            return null;
//        //    }
//        // }
//
//        Position pos = Position.COMPILER_GENERATED;
//
//        TypeNode otn = nf.CanonicalTypeNode(pos, ts.Object());
//        TypeNode itn = nf.CanonicalTypeNode(pos, ts.Int());
//        TypeNode atn = nf.CanonicalTypeNode(pos, ts.arrayOf(pos, ts.Object()));
//
//        ////////////////////////////////////////////////////
//        // Introduce locals for the formal params
//        ////////////////////////////////////////////////////
//
//        LocalDef ruleLI = ts.localDef(pos, Flags.NONE, Types.ref(ts.Int()), Name.make("rule"));
//        LocalDef svalLI = ts.localDef(pos, Flags.NONE, Types.ref(ts.arrayOf(pos, ts.Object())), Name.make("sval"));
//        Local rule = nf.Local(pos, nf.Id(pos, ruleLI.name()));
//        rule = rule.localInstance(ruleLI.asInstance());
//        rule = (Local) rule.type(ruleLI.asInstance().type());
//
//        Local sval = nf.Local(pos, nf.Id(pos, svalLI.name()));
//        sval = sval.localInstance(svalLI.asInstance());
//        sval = (Local) sval.type(svalLI.asInstance().type());
//
//        ////////////////////////////////////////////////////
//        // Generate the method body
//        ////////////////////////////////////////////////////
//
//        Expr null_ = nf.NullLit(pos);
//        null_ = null_.type(ts.Null());
//        Return retNull = nf.Return(pos, null_);
//
//        List elements = new ArrayList(2*glr.numRules());
//
//        for (int i = 0; i < glr.numRules(); i++) {
//            Nonterminal lhs = glr.ruleLhs(i);
//            RSeq rhs = glr.ruleRhs(i);
//
//            if (lhs == null ||rhs == null) {
//                continue;
//            }
//
//System.out.println("generating code for " + i + ": " + lhs + " ::= " + rhs);
//
//            // case i:
//            IntLit label = nf.IntLit(pos, IntLit.INT, i);
//            label = (IntLit) label.type(ts.Int());
//            Case case_ = nf.Case(pos, label);
//            case_ = case_.value(i);
//            elements.add(case_);
//
//            List code = new ArrayList();
//
//            // For rule,
//            //     lhs ::= rhs0 rhs1:x1 rhs2 rhs3:x3
//            //
//            // Unmarshal the semantic values x1 and x3:
//            //     T1 x1 = unmarshalT1(sval[1]);
//            //     T3 x3 = unmarshalT3(sval[3]);
//            //
//            // Call the action method:
//            //     T x = action$lhs$rhs0$rhs1$rhs2$rhs3(x1, x3);
//            // or, if void:
//            //     action$lhs$rhs0$rhs1$rhs2$rhs3(x1, x3);
//            //
//            // Marshal and return result:
//            //     return marshalT(x);
//            // or, if void:
//            //     return null;
//
//            Name methodName = rhs.actionMethodName(lhs);
//            List<Ref<? extends Type>> formalTypes = rhs.actionMethodFormalTypes();
//
//            MethodDef mi = ts.methodDef(pos,
//                                        Types.ref(pt.asType()), lhs.rule().flags(), Types.ref(lhs.type()), methodName,
//                                        formalTypes, Collections.EMPTY_LIST);
//
//            List<Expr> args = new ArrayList();
//            unmarshalSvals(rhs, sval, code, args, pt);
//
//            if (args.size() != formalTypes.size()) {
//                throw new InternalCompilerError("Argument count mismatch between " + mi + " and " + args);
//            }
//
//            Special this_ = nf.This(pos);
//            this_ = (Special) this_.type(pt.asType());
//            Call call = nf.Call(pos, this_, nf.Id(pos,  methodName), args);
//            call = call.methodInstance(mi.asInstance());
//            call = (Call) call.type(mi.asInstance().returnType());
//
//            if (mi.asInstance().returnType().isVoid()) {
//                // action(x, y)
//                // return null;
//                Eval eval = nf.Eval(pos, call);
//                code.add(eval);
//                code.add(retNull);
//            }
//            else {
//                // return (T) action(x, y);
//                Return ret = nf.Return(pos, marshal(call, mi.asInstance().returnType(), pt));
//                code.add(ret);
//            }
//
//            Block b = nf.Block(pos, code);
//            SwitchBlock sb = nf.SwitchBlock(pos, Collections.<Stmt>singletonList(b));
//            elements.add(sb);
//        }
//
//        Switch s = nf.Switch(pos, rule, elements);
//
//        List stmts = new ArrayList(2);
//        stmts.add(s);
//        stmts.add(retNull);
//
//        Block b = nf.Block(pos, stmts);
//
//        ////////////////////////////////////////////////////
//        // Generate the method declaration
//        ////////////////////////////////////////////////////
//
//        Formal ruleFormal = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), itn, nf.Id(pos, ruleLI.name()));
//        ruleFormal = ruleFormal.localDef(ruleLI);
//        Formal svalFormal = nf.Formal(pos, nf.FlagsNode(pos, Flags.NONE), atn, nf.Id(pos, svalLI.name()));
//        svalFormal = svalFormal.localDef(svalLI);
//
//        List formals = new ArrayList(2);
//        formals.add(ruleFormal);
//        formals.add(svalFormal);
//
//        MethodDef md;
//
//        List formalTypes = new ArrayList(2);
//        formalTypes.add(ruleLI.type());
//        formalTypes.add(svalLI.type());
//
//        try {
//            MethodInstance mi;
//            mi = ts.findMethod(ts.runtimeParserImplType(), 
//                               ts.MethodMatcher(ts.runtimeParserImplType(), Name.make("semanticAction"), formalTypes, context));
//            md = (MethodDef) mi.def().copy();
//            md.setFlags(Flags.PUBLIC);
//            md.setContainer(Types.ref(pt.asType()));
//       }
//        catch (SemanticException e) {
//            throw new InternalCompilerError(e);
//        }
//
//        MethodDecl m = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), otn,
//                                     nf.Id(pos, md.name()),
//                formals, Collections.EMPTY_LIST, b);
//        m = m.methodDef(md);
//
//        pt.addMethod(md);
//
//        members(pt).add(m);
//    }

    protected List<ClassMember> addGlrTableMembers(IbexClassDef pt, GLR glr)
        throws SemanticException
    {
        NodeFactory nf = nodeFactory();
        TypeSystem ts = typeSystem();

        Position pos = Position.COMPILER_GENERATED;

        Type t = ts.arrayOf(pos, ts.String());

        String[] s = new String[] { "encodedActionTable",
                                    "encodedOverflowTable",
                                    "encodedGotoTable",
                                    "encodedRuleTable",
                                    "encodedMergeTable" };

        List<String[]> tables = new ArrayList<String[]>();

        tables.add(glr.encodedActionTable());
        tables.add(glr.encodedOverflowTable());
        tables.add(glr.encodedGotoTable());
        tables.add(glr.encodedRuleTable());
        tables.add(glr.encodedMergeTable());
        
        List<ClassMember> ms = new ArrayList<ClassMember>();

        for (int i = 0; i < s.length; i++) {
            Name name = Name.make(s[i]);
            FieldDef fi = ts.fieldDef(pos, Types.ref(pt.asType()), Flags.STATIC, Types.ref(t), name);

            // Generate the field declaration:
            // private static String[] table = new String[] { ... };

            List<Expr> strings = new ArrayList<Expr>();
            String[] table = tables.get(i);

            for (int j = 0; j < table.length; j++) {
                StringLit x = nf.StringLit(pos, table[j]);
                x = (StringLit) x.type(ts.String());
                strings.add(x);
            }

            TypeNode tn;

            tn = nf.CanonicalTypeNode(pos, ts.String());
            ArrayInit init = nf.ArrayInit(pos, strings);
            init = (ArrayInit) init.type(t);
            Expr e = nf.NewArray(pos, tn, Collections.EMPTY_LIST, 1, init);
            e = e.type(t);

            tn = nf.CanonicalTypeNode(pos, t);
            FieldDecl fd = nf.FieldDecl(pos, nf.FlagsNode(pos, fi.flags()), tn, nf.Id(pos, name), e);
            fd = fd.fieldDef(fi);

            pt.addField(fi);
            ms.add(fd);

            // Generate the block b:
            // { return MyType.table; }
            tn = nf.CanonicalTypeNode(pos, pt.asType());
            Field f = nf.Field(pos, tn, nf.Id(pos, name));
            f = (Field) f.type(t);
            f = f.fieldInstance(fi.asInstance());

            Return ret = nf.Return(pos, f);
            Block b = nf.Block(pos, Collections.<Stmt>singletonList(ret));

            // Generate the method:
            // protected String[] table() { return MyType.table; }
            MethodDef mi = ts.methodDef(
                pos, Types.ref(pt.asType()), Flags.PUBLIC, Types.ref(t), name,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST);

            tn = nf.CanonicalTypeNode(pos, t);
            MethodDecl m =
                nf.MethodDecl(pos,  nf.FlagsNode(pos, mi.flags()), tn, nf.Id(pos, mi.name()),
                    Collections.EMPTY_LIST, Collections.EMPTY_LIST, b);
            m = m.methodDef(mi);

            pt.addMethod(mi);

            ms.add(m);
        }
        
        return ms;
    }
}
