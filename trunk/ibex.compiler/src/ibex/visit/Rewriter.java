package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.RhsAction;
import ibex.ast.RhsBind;
import ibex.ast.RhsExpr;
import ibex.ast.RhsOr;
import ibex.ast.RhsSequence;
import ibex.ast.RuleDecl;
import ibex.lr.GLR;
import ibex.types.ByteTerminal;
import ibex.types.CharTerminal;
import ibex.types.IbexClassDef;
import ibex.types.IbexClassType;
import ibex.types.IbexTypeSystem;
import ibex.types.Nonterminal;
import ibex.types.RAnd;
import ibex.types.RSeq;
import ibex.types.RSub;
import ibex.types.Rhs;
import ibex.types.RuleDef;
import ibex.types.Terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import polyglot.ast.SwitchElement;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
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
            
//          Add non-rule members.
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
            
            members.add(eofMethod(def));
            members.add(mapField(def));
            members.add(scanMethod(def));
            members.add(semanticActionDispatcher(def, cb));
            
            return cb.members(members);
        }

        return super.leaveCall(old, n, v);
    }
    
    MethodDecl semanticActionDispatcher(IbexClassDef def, ClassBody cb) throws SemanticException {
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

        Return retNull = nf.Return(pos, nf.NullLit(pos).type(ts.Null()));

        // Build a switch statement.
        List<SwitchElement> elements = new ArrayList<SwitchElement>(2*glr.numRules());

        for (int i = 0; i < glr.numRules(); i++) {
            Nonterminal lhs = glr.ruleLhs(i);
            Rhs rhs = glr.ruleRhs(i);

            if (lhs == null || rhs == null) {
                continue;
            }

            // case i:
            IntLit label = nf.IntLit(pos, IntLit.INT, i);
            label = (IntLit) label.type(ts.Int());
            Case case_ = nf.Case(pos, label);
            case_ = case_.value(i);
            elements.add(case_);
            
            List<Stmt> code = new ArrayList<Stmt>();
            
            Name methodName = actionMethodName(lhs, rhs);
        
            if (! def.asType().methodsNamed(methodName).isEmpty()) {
                List<Ref<? extends Type>> formalTypes = (List) Collections.singletonList(Types.ref(ts.Object().arrayOf()));

                MethodDef mi = ts.methodDef(pos,
                                            Types.ref(def.asType()), lhs.rule().flags(), Types.ref(lhs.type()), methodName,
                                            formalTypes, Collections.EMPTY_LIST);

                List<Expr> args = new ArrayList<Expr>();

                Expr arg = nf.Local(pos, nf.Id(pos, argsLd.name())).localInstance(argsLd.asInstance()).type(argsLd.asInstance().type());
                args.add(arg);

                if (args.size() != formalTypes.size()) {
                    throw new InternalCompilerError("Argument count mismatch between " + mi + " and " + args);
                }

                Special this_ = nf.This(pos);
                this_ = (Special) this_.type(def.asType());
                Call call = nf.Call(pos, this_, nf.Id(pos,  methodName), args);
                call = call.methodInstance(mi.asInstance());
                call = (Call) call.type(mi.asInstance().returnType());

                if (mi.asInstance().returnType().isVoid()) {
                    // action(x, y)
                    // return null;
                    Eval eval = nf.Eval(pos, call);
                    code.add(eval);
                    code.add(retNull);
                }
                else {
                    // return (T) action(x, y);
                    Return ret = nf.Return(pos, call);
                    code.add(ret);
                }
            }
            else {
                Expr arg = nf.Local(pos, nf.Id(pos, argsLd.name())).localInstance(argsLd.asInstance()).type(argsLd.asInstance().type());
                Return ret = nf.Return(pos, unbox(rhs, arg));
                code.add(ret);
            }

            SwitchBlock sb = nf.SwitchBlock(pos, code);
            elements.add(sb);
        }

        Switch s = nf.Switch(pos, nf.Local(pos, nf.Id(pos, ruleLd.name())).localInstance(ruleLd.asInstance()).type(ruleLd.asInstance().type()), elements);
        
        Block body = nf.Block(pos, s, retNull);
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(ts.Object()), name, argTypes, Collections.EMPTY_LIST);
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    Expr unbox(Rhs rhs, Expr e) throws SemanticException {
        if (rhs instanceof RSeq && ((RSeq) rhs).items().size() == 0) {
            return nf.NullLit(e.position()).type(ts.Null());
        }
        if (rhs instanceof RSeq && ((RSeq) rhs).items().size() == 1) {
            return unbox(((RSeq) rhs).items().get(0), e);
        }
        if (rhs instanceof Nonterminal || rhs instanceof Terminal) {
            Rhs ri = rhs;
            Expr l = e;
            int i = 0;
            Expr ai = nf.ArrayAccess(ri.position(), l, nf.IntLit(ri.position(), IntLit.INT, i).type(ts.Int())).type(l.type().toArray().base());
            ai = nf.Conditional(ri.position(), nf.Binary(ri.position(), l, Binary.NE, nf.NullLit(ri.position()).type(ts.Null())).type(ts.Boolean()), ai, nf.NullLit(ri.position()).type(ts.Null())).type(ai.type());
            ai = Rewriter.unbox(ri.type(), ai, nf);
            return ai;
        }
        if (rhs instanceof RSub) {
            RSub r = (RSub) rhs;
            return unbox(r.choice1(), e);
        }
        return e;
    }

    
    MethodDecl eofMethod(IbexClassDef def) throws SemanticException {
        Position pos = def.position();
        Name name = Name.make("eofSymbol");
        
        List<Formal> formals = Collections.EMPTY_LIST;
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        
        Block body = nf.Block(pos, nf.Return(pos, nf.IntLit(pos, IntLit.INT, glr.eofSymbolNumber()).type(ts.Int())));
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(ts.Int()), name, argTypes, Collections.EMPTY_LIST);
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    FieldDecl mapField(IbexClassDef def) throws SemanticException {
        Position pos = def.position();
        Name name = Name.make("terminalTable");

        Type Util = (Type) ts.systemResolver().find(QName.make("ibex.runtime.Util"));

        MethodInstance
        mi = ts.findMethod(Util, ts.MethodMatcher(Util, Name.make("decodeTerminalTable"), Arrays.<Type> asList(def.asType()), context));

        Special this_ = (Special) nf.This(pos).type(def.asType());

        Call map = nf.Call(pos, nf.CanonicalTypeNode(pos, Util), nf.Id(pos, mi.name()), this_);
        map = map.methodInstance(mi);
        map = (Call) map.type(mi.returnType());
        
        FieldDef md = ts.fieldDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(ts.Int().arrayOf()), name);
        FieldDecl mdl = nf.FieldDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.type()), nf.Id(pos, md.name()), map);
        mdl = mdl.fieldDef(md);
        def.addField(md);
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
        Type ByteParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IByteParser"));
        Type CharParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.ICharParser"));
        
        Special this_ = (Special) nf.This(pos).type(def.asType());

        FieldInstance fi = ts.findField(def.asType(), ts.FieldMatcher(def.asType(), Name.make("terminalTable"), context));
        
        Field map = nf.Field(pos, this_, nf.Id(pos, fi.name()));
        map = map.fieldInstance(fi);
        map = (Field) map.type(fi.type());

        MethodInstance mi;
        
        if (ts.isSubtype(def.asType(), ByteParser, context)) {
            mi = ts.findMethod(Util, ts.MethodMatcher(Util, Name.make("scanByte"), Arrays.<Type> asList(def.asType(), ts.Int().arrayOf()), context));
        }
        else if (ts.isSubtype(def.asType(), CharParser, context)) {
            mi = ts.findMethod(Util, ts.MethodMatcher(Util, Name.make("scanChar"), Arrays.<Type> asList(def.asType(), ts.Int().arrayOf()), context));
        }
        else {
            throw new SemanticException("Parser class must implement either IByteParser or ICharParser", pos);
        }

        Call scan = nf.Call(pos, nf.CanonicalTypeNode(pos, Util), nf.Id(pos, mi.name()), this_, map);
        scan = scan.methodInstance(mi);
        scan = (Call) scan.type(mi.returnType());
        Block body = nf.Block(pos, nf.Return(pos, scan));
        
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PUBLIC, Types.ref(Terminal), name, argTypes, Collections.<Ref<? extends Type>>singletonList(Types.ref(IOE)));
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.<TypeNode>singletonList(nf.CanonicalTypeNode(pos, IOE)), body);
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    static
    Type boxedType(Type t) throws SemanticException {
        TypeSystem ts = t.typeSystem();
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
    
    static

    MethodInstance unboxMethod(Type t) throws SemanticException {
        Type T = boxedType(t);
        TypeSystem ts = t.typeSystem();
        Context context = ts.emptyContext();
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
    
    public static
    Expr unbox(Type t, Expr e, NodeFactory nf) throws SemanticException {
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
            stmt = nf.Return(pos, unbox(rule.type().get(), parse, nf));
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
    
    MethodDecl action(IbexClassDef def, RuleDef rule, RhsAction e, Rhs rhs) {
        RhsExpr item = e.item();
        List<Formal> formals = e.formal() != null ? Collections.singletonList(e.formal()) : Collections.EMPTY_LIST;
        List<Ref<? extends Type>> argTypes = argTypesOfFormals(formals);
        Position pos = e.position();
        Name name = actionMethodName(rule.asNonterminal(), rhs);
        MethodDef md = ts.methodDef(pos, Types.ref(def.asType()), Flags.PROTECTED, Types.ref(e.type()), name, argTypes, Collections.EMPTY_LIST);
        MethodDecl mdl = nf.MethodDecl(pos, nf.FlagsNode(pos, md.flags()), nf.CanonicalTypeNode(pos, md.returnType()), nf.Id(pos, md.name()), formals, Collections.EMPTY_LIST, e.body());
        mdl = mdl.methodDef(md);
        def.addMethod(md);
        return mdl;
    }
    
    Name actionMethodName(Nonterminal lhs, Rhs rhs) {
        Name name = Name.make("action$" + lhs.name() + "$" + mangle(rhs));
        return name;
    }
    
    String mangle(Rhs rhs) {
        if (rhs instanceof Nonterminal) {
            return ((Nonterminal) rhs).name().toString();
        }
        if (rhs instanceof CharTerminal) {
            return "" + (int) ((CharTerminal) rhs).value();
        }
        if (rhs instanceof ByteTerminal) {
            return "" + (int) ((ByteTerminal) rhs).value();
        }
        if (rhs instanceof RSeq) {
            StringBuilder sb = new StringBuilder();
            String sep = "";
            for (Rhs r : ((RSeq) rhs).items()) {
                sb.append(sep);
                sep = "$";
                sb.append(mangle(r));
            }
            return sb.toString();
        }
        if (rhs instanceof RAnd) {
            return mangle(((RAnd) rhs).choice1())
            + "$and$"            
            + mangle(((RAnd) rhs).choice1());
        }
        if (rhs instanceof RSub) {
            return mangle(((RSub) rhs).choice1())
            + "$minus$"            
            + mangle(((RSub) rhs).choice1());
        }
        return "";
    }
    
    private List<Formal> formals(RhsExpr item) {
        if (item instanceof RhsBind) {
          RhsBind e = (RhsBind) item;
          return Collections.singletonList(formal(e.decl().localDef()));
        }
        else if (item instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) item;
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
                l.add(action(def, rule, (RhsAction) ei, ei.rhs()));
            }
        }
        return l;
    }

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
                                    "encodedMergeTable",
                                    "encodedTerminalTable"};

        List<String[]> tables = new ArrayList<String[]>();

        tables.add(glr.encodedActionTable());
        tables.add(glr.encodedOverflowTable());
        tables.add(glr.encodedGotoTable());
        tables.add(glr.encodedRuleTable());
        tables.add(glr.encodedMergeTable());
        tables.add(glr.encodedTerminalTable());
        
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
