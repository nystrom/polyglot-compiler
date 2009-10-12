package polyglot.bytecode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.ArrayInit;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassMember;
import polyglot.ast.CodeNode;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.Initializer;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Term;
import polyglot.ast.TypeNode;
import polyglot.bytecode.MethodContext.InstructionSequence;
import polyglot.bytecode.rep.AsmFactory;
import polyglot.bytecode.rep.IClassGen;
import polyglot.bytecode.rep.IFieldGen;
import polyglot.bytecode.rep.IMethodGen;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.Empty;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.ProcedureDef;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * This class produces a Java source file (in a string) for a single Thorn
 * compilation unit.
 */
public class ClassTranslator extends AbstractTranslator implements BytecodeConstants {
    final MethodContext containingExp;
    ClassDef currentClass;
    IClassGen cg;
    BytecodeTranslator bc;
    Map<MethodContext, String> frameFields;

    public ClassTranslator(Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, ClassDef currentClass, MethodContext context) {
        super(job, ts, nf);
        this.containingExp = context;
        this.currentClass = currentClass;
        this.bc = bc;
    }

    public ClassTranslator(Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, ClassDef currentClass) {
        this(job, ts, nf, bc, currentClass, null);
    }

    public void visit(final ClassBody n) {
        for (final ClassMember def : n.members()) {
            visitChild(def, this);
        }
    }

    public void visit(final Node n) {
        assert false : n.getClass().getName();
    }
    
    public void visit(final Initializer n) {
        // Already done
    }

    public void visit(final ClassDecl n) {
        final ClassDef sym = n.classDef();
        ClassBody body = n.body();
        translateClass(n, sym, body);
    }

    public String fileName(final Position pos) {
        if (pos == null)
            return null;
        if (pos.file() == null)
            return null;
        String s = new File(pos.file()).getName();
        return s;
    }

    protected String translateClass(final Node n, final ClassDef sym, final ClassBody body) {
        String generatedClassName = classNameOfSymbol(sym);

        final Type superklass = getSuperklass(sym);
        final Type[] interfaces = getInterfaces(sym);

        int flags = bitsFromFlags(sym.flags());

        // Now create the class.
        final IClassGen cg = AsmFactory.makeClass(generatedClassName, flags | ACC_SUPER, superklass, interfaces, fileName(n.position()));
        bc.addClassForOutput(sym, cg);
        this.cg = cg;

        final Map<MethodContext, String> frameFields1 = new HashMap<MethodContext, String>();

        // Create frame fields and constructor.
        final List<Type> argTypes = new ArrayList<Type>();
        final List<String> argNames = new ArrayList<String>();

        for (MethodContext et = containingExp; et != null; et = et.containingExp) {
            final Type frameClass = et.frameClass;

            if (frameClass != null) {
                // Only inner classes can have frame fields.
                assert sym.isInnerClass();

                final String fieldName = Name.makeFresh("frame").toString();
                final IFieldGen fg = AsmFactory.makeField(ACC_PUBLIC | ACC_FINAL, frameClass, fieldName, null);
                cg.addField(fg);
                frameFields1.put(et, fieldName);

                argTypes.add(frameClass);
                argNames.add(fieldName);
            }
        }

        final IMethodGen staticInit = AsmFactory.makeMethod(ACC_STATIC, "<clinit>", Type.VOID, new Type[0], new String[0], new Type[0]);
        final IOpcodes staticInitIL = AsmFactory.makeOpcodes(staticInit);

        // Init static fields, run initializers
        for (ClassMember member : body.members()) {
            if (member instanceof FieldDecl) {
                FieldDecl fd = (FieldDecl) member;
                if (fd.fieldDef().flags().isStatic() && fd.init() != null) {
                    MethodContext context = new MethodContext(fd.initializerDef(), staticInit, this, Empty.it);
                    if (fd.init() instanceof ArrayInit)
                        visitChild(fd.init().type(Types.get(fd.fieldDef().type())), new ExprTranslator(job, ts, nf, bc, context));
                    else
                        visitChild(fd.init(), new ExprTranslator(job, ts, nf, bc, context));
                    coerce(staticInitIL, typeof(fd.init()), typeof(fd.fieldDef().type()), fd.position());
                    staticInitIL.PUTSTATIC(typeof(fd.fieldDef().container()), fd.fieldDef().name().toString(), typeof(fd.fieldDef().type()), fd.position());
                    if (staticInitIL.isUnreachable())
                        break;
                }
            }
            if (member instanceof Initializer) {
                Initializer i = (Initializer) member;
                if (i.initializerDef().flags().isStatic()) {
                    MethodContext context = new MethodContext(i.initializerDef(), staticInit, this, Empty.it);
                    visitChild(i.body(), new StmtTranslator(job, ts, nf, bc, context));
                    if (staticInitIL.isUnreachable())
                        break;
                }
            }
        }

        if (staticInitIL.isReachable())
            staticInitIL.RETURN(n.position());
        cg.addMethod(staticInit);

        fieldInits = new MethodContext.InstructionSequence() {
            public void appendInstructions(IOpcodes il, MethodContext context) {
                for (ClassMember member : body.members()) {
                    if (member instanceof FieldDecl) {
                        FieldDecl fd = (FieldDecl) member;
                        if (! fd.fieldDef().flags().isStatic() && fd.init() != null) {
                            il.ALOAD(context.getThisIndex(), typeof(currentClass.asType()), fd.position());
                            if (fd.init() instanceof ArrayInit)
                                visitChild(fd.init().type(Types.get(fd.fieldDef().type())), new ExprTranslator(job, ts, nf, bc, context));
                            else
                                visitChild(fd.init(), new ExprTranslator(job, ts, nf, bc, context));
                            coerce(il, typeof(fd.init()), typeof(fd.fieldDef().type()), fd.position());
                            il.PUTFIELD(typeof(fd.fieldDef().container()), fd.fieldDef().name().toString(), typeof(fd.fieldDef().type()), fd.position());
                        }
                    }
                    if (member instanceof Initializer) {
                        Initializer i = (Initializer) member;
                        if (! i.initializerDef().flags().isStatic()) {
                            visitChild(i.body(), new StmtTranslator(job, ts, nf, bc, context));
                        }
                    }
                }
            }
        };
        
        visitChild(body);

        return generatedClassName;
    }
    
    InstructionSequence fieldInits;

    private static int count = 1;

    private String classNameOfSymbol(ClassDef def) {
        if (def.isTopLevel()) {
            return def.fullName().toString();
        }
        else if (def.isMember()) {
            polyglot.types.Type outer = Types.get(def.container());
            if (outer instanceof ClassType) {
                ClassDef outerDef = ((ClassType) outer).def();
                return classNameOfSymbol(outerDef) + "$" + def.name().toString();
            }
            else {
                throw new InternalCompilerError("Member class container is not a class.");
            }
        }
        else {
            ClassDef outerDef = Types.get(def.outer());
            return classNameOfSymbol(outerDef) + "$" + count++;
        }
    }

    // static void debug(final MethodGen mg, final Bytecodes il, final String
    // msg, final Position pos) {
    // final Bytecodes j = il;
    // j.INVOKESTATIC(ClassType.mk("thorn.Globals"), "reporter", new
    // JavaType[0], ClassType.mk("thorn.errors.reporter.Reporter"), pos);
    // j.LDC(pos + ": " + msg, pos);
    // j.INVOKEVIRTUAL_void(ClassType.mk("thorn.errors.reporter.Reporter"),
    // "println", new JavaType[] { ClassType.mk("java.lang.String") }, pos);
    // }

    protected Type getSuperklass(final ClassDef sym) {
        polyglot.types.ClassType t = (ClassType) Types.get(sym.superType());
        assert ! t.flags().isInterface();
        return Type.typeFromPolyglotType(t);
    }

    protected Type[] getInterfaces(final ClassDef sym) {
        Type[] ifaces = new Type[sym.interfaces().size()];
        for (int i = 0; i < sym.interfaces().size(); i++) {
            Ref<? extends polyglot.types.Type> ref = sym.interfaces().get(i);
            polyglot.types.ClassType t = (ClassType) Types.get(ref);
            assert t.flags().isInterface();
            ifaces[i] = Type.typeFromPolyglotType(t);
        }
        return ifaces;
    }

    public void visit(final FieldDecl n) {
        final FieldDef sym = n.fieldDef();
        final Name name = sym.name();

        int flags = bitsFromFlags(sym.flags());

        Type type = typeof(n.type().type());
        final IFieldGen fg = AsmFactory.makeField(flags, type, name.toString(), n.fieldDef().constantValue());
        cg.addField(fg);
    }

    private int bitsFromFlags(final Flags flags) {
        TypeSystem ts = Globals.TS();
        int result = 0;
        for (int bit = 1; bit != 0; bit <<= 1) {
            Flags b = ts.flagsForBits(bit);
            if (! b.equals(Flags.NONE) && flags.contains(b))
                result |= bit;
        }
        if (flags.isInterface())
            result |= ACC_INTERFACE;
        return result;
    }

    public void visit(final ConstructorDecl n) {
        TypeSystem ts = Globals.TS();
        genMethod(n.constructorDef().flags(), n.constructorDef(), "<init>", n.formals(), ts.Void(), n.throwTypes(), n.body());
    }

    public void visit(final MethodDecl n) {
        genMethod(n.methodDef().flags(), n.methodDef(), n.methodDef().name().toString(), n.formals(), Types.get(n.methodDef().returnType()), n.throwTypes(),
                  n.body());
    }

    private void genMethod(Flags flags, ProcedureDef def, String name, List<Formal> formals, polyglot.types.Type returnType, List<TypeNode> throwTypes,
            Block body) {

        final Name[] names = new Name[formals.size()];
        final String[] nameStrings = new String[formals.size()];
        final Type[] types = new Type[formals.size()];
        final Type[] ttypes = new Type[throwTypes.size()];

        for (int i = 0; i < names.length; i++)
            names[i] = formals.get(i).name().id();
        for (int i = 0; i < names.length; i++)
            nameStrings[i] = names[i].toString();

        for (int i = 0; i < types.length; i++)
            types[i] = ExprTranslator.typeof(formals.get(i).type());
        for (int i = 0; i < ttypes.length; i++) {
            ttypes[i] = ExprTranslator.typeof(throwTypes.get(i));
        }

        // Generate the actual implementation as a static method.
        // This can be called by super calls.

        final IMethodGen mg = AsmFactory.makeMethod(bitsFromFlags(flags), name, typeof(returnType), types, nameStrings, ttypes);
        final IOpcodes methodIL = AsmFactory.makeOpcodes(mg);
        
        final MethodContext t = new MethodContext(def, mg, this, Empty.it);
        
        t.fieldInits = fieldInits;
        
        // /if (currentClass.toString().contains("Iterator\\$"))
        // System.err.println(n.getBody());

        if (body != null) {
            translateFunctionBody(t, flags, formals, body);
        }

        cg.addMethod(mg);
    }

    IClassGen makeFrameClass(final List<LocalDef> needToBox, final CodeDef funsym, final Map<LocalDef, String> boxingMap) {
        for (final LocalDef s : needToBox) {
            if (boxingMap.containsKey(s)) {
                continue;
            }

            String name;

            final String oldName = s.name().toString();
            String newName = oldName;
            while (boxingMap.values().contains(newName)) {
                newName = Name.makeFresh(oldName).toString();
            }
            name = newName;
            boxingMap.put(s, name);
        }

        final String frameClass = classNameOfSymbol(currentClass) + "$$$frame$" + count++;

        final IClassGen frameCG = AsmFactory.makeClass(frameClass, ACC_PUBLIC | ACC_SUPER, Type.OBJECT, new Type[0], fileName(funsym.position()));
        bc.addClassForOutput(currentClass, frameCG);
        
        // Create a field for each local.
        for (final LocalDef v : boxingMap.keySet()) {
            final IFieldGen fg = AsmFactory.makeField(ACC_PUBLIC, typeof(v.type()), boxingMap.get(v), v.constantValue());
            frameCG.addField(fg);
        }

        return frameCG;
    }

    public MethodContext translateFunctionBody(final MethodContext c, Flags flags, final List<Formal> formals, final Term body) {
        if (body != null) {
            int offset = 0;

            if (! flags.isStatic()) {
                // Make sure this is in the var map.
                c.putThisIndex(0);
                offset++;
            }

            // if (Trace.translate>=2) Trace.t(2,"Checking boxing of locals in "
            // + funsym);

            // Collect any variables that need to be boxed.
            final List<LocalDef> needToBox = new ArrayList<LocalDef>();
            final List<LocalDef> declared = new ArrayList<LocalDef>();
            
            // Create local variables for the formals.
            for (int i = 0, j = offset; i < formals.size(); i++) {
                final Formal v = formals.get(i);

                final LocalDef sym = v.localDef();
                declared.add(sym);

                final int index = c.addFormal(sym, j);
                assert index == j;
                j++;
                if (typeof(sym.type()).isWide())
                    j++;
            }
            
            body.visit(new NodeVisitor() {
                @Override
                public Node override(Node n) {
                    if (n instanceof CodeNode) {
                        return n;
                    }
                    if (n instanceof Formal) {
                        Formal f = (Formal) n;
                        declared.add(f.localDef());
                    }
                    if (n instanceof LocalDecl) {
                        LocalDecl f = (LocalDecl) n;
                        declared.add(f.localDef());
                    }
                    return null;
                }
            });
            
            body.visit(new NodeVisitor() {
                @Override
                public Node override(Node n) {
                    if (n instanceof CodeNode) {
                        n.visit(new NodeVisitor() {
                            @Override
                            public Node leave(Node old, Node n, NodeVisitor v) {
                                if (n instanceof Local) {
                                    Local l = (Local) n;
                                    LocalDef sym = l.localInstance().def();
                                    if (declared.contains(sym) && ! sym.flags().isFinal())
                                        needToBox.add(sym);
                                }
                                return n;
                            }

                        });
                        return n;
                    }
                    return null;
                }
            });

            // Create a frame object.
            if (!needToBox.isEmpty()) {
                // First create a frame class.

                final Map<LocalDef, String> boxingMap = new HashMap<LocalDef, String>();
                final IClassGen frameClass = makeFrameClass(needToBox, c.theMethod, boxingMap);
                final Name frameLocal = Name.makeFresh("frame");

                c.frameClass = frameClass.type();
                c.frameLocal = frameLocal;
                c.boxingMap = boxingMap;

                // Instantiate the frame class.
                c.il.NEW(frameClass.type(), body.position());
                c.il.DUP(body.position());
                c.il.INVOKESPECIAL(frameClass.type(), "<init>", new Type[0], Type.VOID, body.position());

                // // And save in a local.
                // final int frameIndex = t.addFrameLocal(frameLocal);
                // t.il.ASTORE(frameIndex, body.position());
                //
                // // Initialize the formals in the frame.
                // for (int i = 0; i < formals.size(); i++) {
                // final Formal v = formals.get(i);
                //
                // final ILocalSymbol sym = (ILocalSymbol) v.getSymbol();
                // final boolean boxed = sym.escapesFunction();
                //
                // final int index = t.getLocalIndex(sym);
                //
                // if (boxed) {
                // final AstFactory F = new AstScopeFactory(v);
                // final INode var = F.Var(v.getName()).newSymbol(sym);
                //
                // var.accept(new LhsBeforeTranslator(compiler, t));
                //
                // t.il.ALOAD(index, IObject, v.getPosition());
                //
                // var.accept(new LhsAfterTranslator(compiler, t, null));
                // }
                // }
            }

            final StmtTranslator v = new StmtTranslator(job, ts, nf, bc, c);

            visitChild(body, v);

            final StackType s = c.il.currentStack();
            if (!s.isUnreachable() && c.mg.getReturnType().isVoid())
                c.il.RETURN(body.position());
        }

        return c;
    }
}
