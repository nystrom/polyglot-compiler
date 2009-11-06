package polyglot.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Expr;
import polyglot.ast.FloatLit;
import polyglot.ast.IntLit;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.Binary.Operator;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.Empty;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.NonEmpty;
import polyglot.bytecode.types.Type;
import polyglot.bytecode.types.Unreachable;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.LocalDef;
import polyglot.types.LocalInstance;
import polyglot.types.TypeSystem;
import polyglot.util.Copy;
import polyglot.util.InternalCompilerError;
import polyglot.util.Pair;
import polyglot.util.Position;

/**
 * This class produces bytecode for a Thorn expression. It leaves a single
 * IObject on the operand stack.
 */
public class AbstractExpTranslator extends AbstractTranslator {
    protected MethodContext context;
    protected IOpcodes il;
    protected BytecodeTranslator bc;
    
    public MethodContext context() {
        return context;
    }

    public AbstractTranslator context(MethodContext c) {
        AbstractExpTranslator t = copy();
        t.context = c;
        t.il = c.il;
        return t;
    }

    public AbstractExpTranslator(final Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, MethodContext context) {
        super(job, ts, nf);
        this.bc = bc;
        this.context = context;
        this.il = context.il;
    }
    
    public void visitBranch(Expr e, ILabel target, boolean branchOnTrue) {
        visitChild(e, new BranchTranslator(job, ts, nf, bc, context, target, branchOnTrue));
    }

    public void visitExpr(Expr n) {
        visitChild(n, new ExprTranslator(job, ts, nf, bc, context));
    }

    public void visitChild(final Expr e, final Type newTop) {
        final StackType oldStack = il.currentStack();
        visitExpr(e);
    
        if (il.currentStack() instanceof Unreachable)
            return;
    
        if (newTop != null) {
            assert il.currentStack() instanceof NonEmpty : "expected non-empty, found " + il.currentStack();
            NonEmpty stack = (NonEmpty) il.currentStack();
            if (!stack.pop().equals(oldStack))
                throw new InternalCompilerError("expected ?" + oldStack + " found " + stack + " at " + e + " " + e);
            assert stack.pop().equals(oldStack) : "expected ?" + oldStack + " found " + stack + " at " + e;
            coerce(stack.top(), newTop, e.position());
            stack = (NonEmpty) il.currentStack();
            // This will assert if the stack top is not newTop.
            stack.merge(oldStack.push(newTop));
        }
    }
    
    void debug(final String msg, final Position pos) {
        Type System = Type.typeFromClassName("java.lang.System");
        Type PrintStream = Type.typeFromClassName("java.io.PrintStream");
        il.GETSTATIC(System, "out", PrintStream, pos);
        il.LDC(msg, pos);
        il.INVOKEVIRTUAL(PrintStream, "println", new Type[] { Type.STRING }, Type.VOID, pos);
    }

    
    void box(final Type top, final Position pos) {
        box(il, top, pos);
    }

    void unbox(final Type top, final Position pos) {
        unbox(il, top, pos);
    }

    List<MethodContext.Var> spillStack(final Position pos) {
        // If the stack is not empty at a try-catch, we need to spill the stack
        // to local
        // variables
        // This is because the stack at an exception handler is empty except for
        // the
        // exception object itself.
        // When the try path and catch path join, we need the stacks to be the
        // same
        // type.
        final List<MethodContext.Var> locals = new ArrayList<MethodContext.Var>();
        while (il.currentStack() instanceof NonEmpty) {
            final NonEmpty t = (NonEmpty) il.currentStack();
            Type type;
            MethodContext.Var v = new MethodContext.DummyVar("spill", t.top());
            int i = context.addLocal(new MethodContext.DummyVar("spill", t.top()));
            locals.add(v);
            il.store(i, t.top(), pos);
        }

        il.assertStack(polyglot.bytecode.types.Empty.it);

        return locals;
    }

    void restoreStack(final List<MethodContext.Var> locals, final Type keepOnTop, final Position pos) {
        if (keepOnTop != null)
            il.assertStack(Empty.it.push(keepOnTop));

        if (keepOnTop == null) {
            for (int i = locals.size() - 1; i >= 0; i--) {
                final MethodContext.Var l = locals.get(i);
                final Type top = l.type;
                final int index = context.getIndex(l);
                il.load(index, top, pos);
            }
        }
        else {
            assert keepOnTop.isNarrow();
            for (int i = locals.size() - 1; i >= 0; i--) {
                final MethodContext.Var l = locals.get(i);
                final Type top = l.type;
                final int index = context.getIndex(l);
                il.load(index, top, pos);

                if (top.isNarrow())
                    // 21 -> 12
                    il.SWAP(pos); // swap the object back up
                else {
                    // 3(21) -> (21)3(21) -> (21)3
                    il.DUP2_X1(pos);
                    il.POP2(pos);
                }
            }
        }
    }

    protected int createLocalDef(final LocalDef v) {
        LocalDef sym = v;
        final MethodContext t = context;
        final boolean boxed = t.isBoxedHere(v);
        int index = -1;
        if (boxed)
            context.addFormal(sym, -1);
        else
            index = context.addLocal(sym);
        return index;
    }

    protected void initLocalDef(final LocalDecl v) {
        final LocalDef sym = v.localDef();
        final MethodContext t = context;
        final MethodContext ti = context;

        final boolean boxed = t.isBoxedHere(sym);
        final Type type = ExprTranslator.typeof(sym.type());
        assert type != null;
        Expr init = v.init();

        // Ensure local variables are initialized. This should be checked for
        // statically
        // during CheckPhase.
        if (init == null) {
            TypeSystem ts = Globals.TS();
            NodeFactory nf = Globals.NF();
            if (type.isRef())
                init = nf.NullLit(v.position()).type(ts.Null());
            else if (type.isBoolean())
                init = nf.BooleanLit(v.position(), false).type(ts.Boolean());
            else if (type.isIType())
                init = nf.IntLit(v.position(), IntLit.INT, 0L).type(ts.Int());
            else if (type.isLong())
                init = nf.IntLit(v.position(), IntLit.LONG, 0L).type(ts.Long());
            else if (type.isFloat())
                init = nf.FloatLit(v.position(), FloatLit.FLOAT, 0.0).type(ts.Float());
            else if (type.isDouble())
                init = nf.FloatLit(v.position(), FloatLit.DOUBLE, 0.0).type(ts.Double());
            else
                assert false;
        }

        if (boxed) {
            // // Get a pointer to the frame.
            // final int frameIndex = t.getFrameIndex(t.frameLocal);
            // t.il.ALOAD(frameIndex, IObject, v.getPosition());
            // visitChild(init, type);
            // if (il.currentStack() instanceof Unreachable) return;
            //
            // t.il.PUTFIELD(ClassType.mk(ti.frameClass), ti.boxedField(sym),
            // type, v.getPosition());
            assert false;
        }
        else {
            final int index = context.getLocalIndex(sym);
            visitChild(init, type);
            if (il.currentStack() instanceof Unreachable)
                return;
            il.store(index, type, v.position());
        }
    }

    /** Pop the stack until it's type agrees with t. */
    void popToStack(final StackType t, final Position pos) {
        while (il.currentStack() instanceof NonEmpty && !il.currentStack().equals(t))
            il.pop(pos);
        il.assertStack(t);
    }

    public String toString() {
        return il.toString();
    }

    public void binaryOp(Assign.Operator op, Type t, Position pos) {
        Map<Assign.Operator, Binary.Operator> m = new HashMap<Assign.Operator, Binary.Operator>();
        m.put(Assign.ADD_ASSIGN, Binary.ADD);
        m.put(Assign.BIT_AND_ASSIGN, Binary.BIT_AND);
        m.put(Assign.BIT_OR_ASSIGN, Binary.BIT_OR);
        m.put(Assign.BIT_XOR_ASSIGN, Binary.BIT_XOR);
        m.put(Assign.DIV_ASSIGN, Binary.DIV);
        m.put(Assign.MOD_ASSIGN, Binary.MOD);
        m.put(Assign.MUL_ASSIGN, Binary.MUL);
        m.put(Assign.SHL_ASSIGN, Binary.SHL);
        m.put(Assign.SHR_ASSIGN, Binary.SHR);
        m.put(Assign.SUB_ASSIGN, Binary.SUB);
        m.put(Assign.USHR_ASSIGN, Binary.USHR);
        binaryOp(m.get(op), t, pos);
    }

    public void binaryOp(Assign.Operator op, polyglot.types.Type t, final Position pos) {
        binaryOp(op, typeof(t), pos);
    }

    public void binaryOp(Binary.Operator op, polyglot.types.Type t, final Position pos) {
        binaryOp(op, typeof(t), pos);
    }

    public void binaryOp(Binary.Operator op, Type t, final Position pos) {
        abstract class Apply {
            abstract void apply();
        }

        Map<Pair<Operator, Type>, Apply> m = new HashMap<Pair<Binary.Operator, Type>, Apply>();
        m.put(new Pair(Binary.ADD, Type.BYTE), new Apply() {
            void apply() {
                il.IADD(pos);
            }
        });
        m.put(new Pair(Binary.ADD, Type.INT), new Apply() {
            void apply() {
                il.IADD(pos);
            }
        });
        m.put(new Pair(Binary.ADD, Type.LONG), new Apply() {
            void apply() {
                il.LADD(pos);
            }
        });
        m.put(new Pair(Binary.ADD, Type.FLOAT), new Apply() {
            void apply() {
                il.FADD(pos);
            }
        });
        m.put(new Pair(Binary.ADD, Type.DOUBLE), new Apply() {
            void apply() {
                il.DADD(pos);
            }
        });

        m.put(new Pair(Binary.SUB, Type.INT), new Apply() {
            void apply() {
                il.ISUB(pos);
            }
        });
        m.put(new Pair(Binary.SUB, Type.LONG), new Apply() {
            void apply() {
                il.LSUB(pos);
            }
        });
        m.put(new Pair(Binary.SUB, Type.FLOAT), new Apply() {
            void apply() {
                il.FSUB(pos);
            }
        });
        m.put(new Pair(Binary.SUB, Type.DOUBLE), new Apply() {
            void apply() {
                il.DSUB(pos);
            }
        });

        m.put(new Pair(Binary.MUL, Type.INT), new Apply() {
            void apply() {
                il.IMUL(pos);
            }
        });
        m.put(new Pair(Binary.MUL, Type.LONG), new Apply() {
            void apply() {
                il.LMUL(pos);
            }
        });
        m.put(new Pair(Binary.MUL, Type.FLOAT), new Apply() {
            void apply() {
                il.FMUL(pos);
            }
        });
        m.put(new Pair(Binary.MUL, Type.DOUBLE), new Apply() {
            void apply() {
                il.DMUL(pos);
            }
        });

        m.put(new Pair(Binary.DIV, Type.INT), new Apply() {
            void apply() {
                il.IDIV(pos);
            }
        });
        m.put(new Pair(Binary.DIV, Type.LONG), new Apply() {
            void apply() {
                il.LDIV(pos);
            }
        });
        m.put(new Pair(Binary.DIV, Type.FLOAT), new Apply() {
            void apply() {
                il.FDIV(pos);
            }
        });
        m.put(new Pair(Binary.DIV, Type.DOUBLE), new Apply() {
            void apply() {
                il.DDIV(pos);
            }
        });

        m.put(new Pair(Binary.MOD, Type.INT), new Apply() {
            void apply() {
                il.IREM(pos);
            }
        });
        m.put(new Pair(Binary.MOD, Type.LONG), new Apply() {
            void apply() {
                il.LREM(pos);
            }
        });
        m.put(new Pair(Binary.MOD, Type.FLOAT), new Apply() {
            void apply() {
                il.FREM(pos);
            }
        });
        m.put(new Pair(Binary.MOD, Type.DOUBLE), new Apply() {
            void apply() {
                il.DREM(pos);
            }
        });

        m.put(new Pair(Binary.BIT_AND, Type.INT), new Apply() {
            void apply() {
                il.IAND(pos);
            }
        });
        m.put(new Pair(Binary.BIT_AND, Type.LONG), new Apply() {
            void apply() {
                il.LAND(pos);
            }
        });
        m.put(new Pair(Binary.BIT_AND, Type.BOOLEAN), new Apply() {
            void apply() {
                il.setStack(il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                il.IAND(pos);
                il.setStack(il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
            }
        });

        m.put(new Pair(Binary.BIT_OR, Type.INT), new Apply() {
            void apply() {
                il.IOR(pos);
            }
        });
        m.put(new Pair(Binary.BIT_OR, Type.LONG), new Apply() {
            void apply() {
                il.LOR(pos);
            }
        });
        m.put(new Pair(Binary.BIT_OR, Type.BOOLEAN), new Apply() {
            void apply() {
                il.setStack(il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                il.IOR(pos);
                il.setStack(il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
            }
        });

        m.put(new Pair(Binary.BIT_XOR, Type.INT), new Apply() {
            void apply() {
                il.IXOR(pos);
            }
        });
        m.put(new Pair(Binary.BIT_XOR, Type.LONG), new Apply() {
            void apply() {
                il.LXOR(pos);
            }
        });
        m.put(new Pair(Binary.BIT_XOR, Type.BOOLEAN), new Apply() {
            void apply() {
                il.setStack(il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                il.IXOR(pos);
                il.setStack(il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
            }
        });

        m.put(new Pair(Binary.SHL, Type.INT), new Apply() {
            void apply() {
                il.ISHL(pos);
            }
        });
        m.put(new Pair(Binary.SHL, Type.LONG), new Apply() {
            void apply() {
                il.LSHL(pos);
            }
        });

        m.put(new Pair(Binary.SHR, Type.INT), new Apply() {
            void apply() {
                il.ISHR(pos);
            }
        });
        m.put(new Pair(Binary.SHR, Type.LONG), new Apply() {
            void apply() {
                il.LSHR(pos);
            }
        });

        m.put(new Pair(Binary.USHR, Type.INT), new Apply() {
            void apply() {
                il.IUSHR(pos);
            }
        });
        m.put(new Pair(Binary.USHR, Type.LONG), new Apply() {
            void apply() {
                il.LUSHR(pos);
            }
        });

        Apply a = m.get(new Pair(op, t));

        if (a != null) {
            a.apply();
            return;
        }

        assert false;
    }

    protected void store(LocalInstance li, Position pos) {
        int index = context.getLocalIndex(li.def());
        il.store(index, typeof(li.type()), pos);
    }

    protected void arrayLoad(polyglot.types.Type t, Position pos) {
        if (t.isBoolean()) {
            il.IALOAD(pos);
            il.uncheckedCoerce(Type.INT, Type.BOOLEAN);
        }
        if (t.isByte())
            il.BALOAD(pos);
        if (t.isShort())
            il.SALOAD(pos);
        if (t.isChar())
            il.CALOAD(pos);
        if (t.isInt())
            il.IALOAD(pos);
        if (t.isLong())
            il.LALOAD(pos);
        if (t.isFloat())
            il.FALOAD(pos);
        if (t.isDouble())
            il.DALOAD(pos);
        if (t.isReference())
            il.AALOAD(pos, typeof(t));
    }

    protected void arrayStore(polyglot.types.Type t, Position pos) {
        if (t.isBoolean()) {
            il.uncheckedCoerce(Type.BOOLEAN, Type.INT);
            il.IASTORE(pos);
        }
        if (t.isByte())
            il.BASTORE(pos);
        if (t.isShort())
            il.SASTORE(pos);
        if (t.isChar())
            il.CASTORE(pos);
        if (t.isInt())
            il.IASTORE(pos);
        if (t.isLong())
            il.LASTORE(pos);
        if (t.isFloat())
            il.FASTORE(pos);
        if (t.isDouble())
            il.DASTORE(pos);
        if (t.isReference())
            il.AASTORE(pos, typeof(t));
    }

    public void True(final Position pos) {
        il.LDC(1, pos);
        il.uncheckedCoerce(Type.INT, Type.BOOLEAN);
    }

    public void False(final Position pos) {
        il.LDC(0, pos);
        il.uncheckedCoerce(Type.INT, Type.BOOLEAN);
    }


    void promote(Expr n, polyglot.types.Type t) {
        visitExpr(n);
        coerce(typeof(n), typeof(t), n.position());
    }

    void pushArguments(List<polyglot.types.Type> formalTypes, List<Expr> args) {
        assert formalTypes.size() == args.size();
        for (int i = 0; i < args.size(); i++) {
            polyglot.types.Type t = formalTypes.get(i);
            Expr arg = args.get(i);
            promote(arg, t);
        }
    }

    public void alloc(final ClassType type, final List<polyglot.types.Type> formalTypes, final List<Expr> args, final Position pos) {
        il.NEW(typeof(type), pos);
        il.DUP(pos);

        pushArguments(formalTypes, args);

        final Type[] tys = new Type[args.size()];
        for (int i = 0; i < args.size(); i++)
            tys[i] = typeof(args.get(i));

        il.INVOKESPECIAL(typeof(type), "<init>", tys, Type.VOID, pos);
    }

    void coerce(final Type currentTop, final Type newTop, final Position pos) {
        coerce(il, currentTop, newTop, pos);
    }
}
