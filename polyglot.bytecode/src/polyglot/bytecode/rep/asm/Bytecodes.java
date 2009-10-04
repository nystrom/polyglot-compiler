package polyglot.bytecode.rep.asm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import polyglot.bytecode.rep.IExceptionHandler;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.Empty;
import polyglot.bytecode.types.NonEmpty;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.bytecode.types.Unreachable;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class Bytecodes implements IOpcodes {
    InsnList instructions;
    Bytecodes mn;
    int maxStack;
    int maxLocals;
    
    static LabelNode asmLabel(ILabel L) {
        assert L != null;
        return new LabelNode(((MyLabel) L).L);
    }

    private StackType currentStack;

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#currentStack()
     */
    public StackType currentStack() {
        return currentStack;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#isUnreachable()
     */
    public boolean isUnreachable() {
        return currentStack.isUnreachable();
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#setStack(polyglot.bytecode.types.StackType)
     */
    public void setStack(StackType st) {
        currentStack = st;
        maxStack = Math.max(maxStack, st.size());
    }
    
    public void useLocal(Type t, int index) {
        maxLocals = Math.max(maxLocals, index + (t.isWide() ? 1 : 0));
    }

    public static String methodSignature(final Type[] argTypes, final Type returnType) {
        final StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (final Type argType : argTypes) {
            sb.append(argType.desc());
        }
        sb.append(")");
        sb.append(returnType.desc());
        if (sb.toString().indexOf(";;") >= 0)
            throw new InternalCompilerError("bad sig" + sb.toString());
        return sb.toString();
    }

    public Bytecodes(final MethodGen mg, StackType st) {
        assert mg != null;
        this.setStack(st);
        this.maxStack = st.size();
        this.instructions = new InsnList();
        this.mn = this;
    }

    public Bytecodes(final MethodGen mg) {
        this(mg, Empty.it);
    }

    @Override
    public String toString() {
        if (mn == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("null MethodNode");
            return sb.toString();
        }
        return insnListToString(mn.instructions);
    }

    public static String insnListToString(InsnList instructions) {
        StringBuilder sb = new StringBuilder();
        for (AbstractInsnNode i = instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getType() == InsnNode.LABEL)
                sb.append("L" + i.hashCode());
            else                if (i.getType() == InsnNode.LINE)
                sb.append("Line");
            else
            try {
                Class c = Opcodes.class;
                Field[] f = c.getFields();
                boolean on = false;
                for (int k = 0; k < f.length; k++) {
                    if (! on)
                        if (f[k].getName().equals("NOP"))
                            on = true;
                    if (! on)
                        continue;
                    if ((f[k].getModifiers() & Modifier.STATIC) != 0)
                            try {
                                if (f[k].getInt(null) == i.getOpcode()) {
                                    sb.append(f[k].getName());
                                    break;
                                }
                            }
                            catch (IllegalArgumentException e) {
                            }
                            catch (IllegalAccessException e) {
                            }
                }
            }
            catch (NullPointerException e) {
                sb.append("NPE");
            }
            sb.append("\n");
        }
      return sb.toString();
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#postAppend(polyglot.util.Position, polyglot.bytecode.rep.ILabel)
     */
    public ILabel postAppend(final Position pos, final ILabel h) {
        return h;
    }

    List<CodeExceptionGen> handlers;

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#addExceptionHandler(polyglot.bytecode.rep.ILabel, polyglot.bytecode.rep.ILabel, polyglot.bytecode.rep.ILabel, polyglot.bytecode.types.Type)
     */
    public IExceptionHandler addExceptionHandler(final ILabel start_pc, final ILabel end_pc, final ILabel handler_pc, final Type catch_type) {
        CodeExceptionGen e = new CodeExceptionGen(start_pc, end_pc, handler_pc, catch_type);
        if (handlers == null)
            handlers = new ArrayList<CodeExceptionGen>();
        handlers.add(e);
        return e;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#makeLabel(polyglot.util.Position)
     */
    public ILabel makeLabel(final Position pos) {
        ILabel L = new MyLabel(new Label());
        if (pos != null)
            mn.instructions.add(new LineNumberNode(pos.line(), asmLabel(L)));
        return L;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#addLabel(polyglot.bytecode.rep.ILabel)
     */
    public void addLabel(ILabel L) {
        mn.instructions.add(asmLabel(L));
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#dup(polyglot.util.Position)
     */
    public ILabel dup(final Position pos) {
        assert currentStack instanceof NonEmpty;
        if (top().isNarrow())
            return DUP(pos);
        else
            return DUP2(pos);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#pop(polyglot.util.Position)
     */
    public ILabel pop(final Position pos) {
        assert currentStack instanceof NonEmpty : currentStack;
        if (top().isNarrow())
            return POP(pos);
        else
            return POP2(pos);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#returnTop(polyglot.util.Position)
     */
    public ILabel returnTop(Position pos) {
        if (top().isRef())
            return ARETURN(pos);
        if (top().isIType()) {
            uncheckedCoerce(Type.INT);
            return IRETURN(pos);
        }
        if (top().isLong())
            return LRETURN(pos);
        if (top().isFloat())
            return FRETURN(pos);
        if (top().isDouble())
            return DRETURN(pos);
        throw new InternalCompilerError("bad top");
    }

    Type top() {
        return currentStack.top();
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#coerce(polyglot.bytecode.types.Type)
     */
    public void uncheckedCoerce(Type neu) {
        uncheckedCoerce(top(), neu);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#coerce(polyglot.bytecode.types.Type, polyglot.bytecode.types.Type)
     */
    public void uncheckedCoerce(Type old, Type neu) {
        if (old.equals(neu))
            return;
        if (currentStack.isUnreachable())
            return;
        this.setStack(currentStack.pop(old).push(neu));
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#load(int, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public void load(final int index, final Type t, final Position pos) {
        if (t.isIType()) {
            this.ILOAD(index, pos);
            assert top().isInt();
            uncheckedCoerce(t);
        }
        else if (t.isLong()) {
            this.LLOAD(index, pos);
        }
        else if (t.isFloat()) {
            this.FLOAD(index, pos);
        }
        else if (t.isDouble()) {
            this.DLOAD(index, pos);
        }
        else {
            assert t.isRef();
            this.ALOAD(index, t, pos);
        }
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#store(int, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public void store(final int index, Type t, final Position pos) {
        if (t.isIType()) {
            uncheckedCoerce(Type.INT);
            this.ISTORE(index, pos);
        }
        else if (t.isLong()) {
            this.LSTORE(index, pos);
        }
        else if (t.isFloat()) {
            this.FSTORE(index, pos);
        }
        else if (t.isDouble()) {
            this.DSTORE(index, pos);
        }
        else {
            assert t.isRef();
            this.ASTORE(index, pos);
        }
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#isReachable()
     */
    public boolean isReachable() {
        return !isUnreachable();
    }

    // One method for each bytecode instruction follows. Each method computes
    // the new
    // operand stack after executing the instruction.
    // A run-time error (usually a cast error) should occur if the stack type is
    // not as
    // expected. This should be regularized.
    

    ILabel preAppend(Position pos) {
        ILabel L = makeLabel(pos);
        mn.instructions.add(asmLabel(L));
        return L;
    }
    
    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#AALOAD(polyglot.util.Position, polyglot.bytecode.types.Type)
     */
    public ILabel AALOAD(final Position pos, Type elementType) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.AALOAD));
        assert isReachable();
        setStack(currentStack.pop(Type.INT).pop(Type.array(elementType)).push(elementType));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#AASTORE(polyglot.util.Position, polyglot.bytecode.types.Type)
     */
    public ILabel AASTORE(final Position pos, Type elementType) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.AASTORE));
        assert isReachable();

        setStack(currentStack.pop(elementType).pop(Type.INT).pop(Type.array(elementType)));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ACONST_NULL(polyglot.util.Position)
     */
    public ILabel ACONST_NULL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        assert isReachable();
        setStack(currentStack.push(Type.NULL));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ALOAD(int, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel ALOAD(final int index, final Type it, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, index));
        assert isReachable();

        setStack(currentStack.push(it));
        useLocal(it, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ANEWARRAY(polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel ANEWARRAY(final Type elementType, final Position pos) {
        final ILabel h = preAppend(pos);
        assert elementType.isRef();
        mn.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, elementType.desc()));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.array(elementType)));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ARETURN(polyglot.util.Position)
     */
    public ILabel ARETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT));
        setStack(Unreachable.it);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ARRAYLENGTH(polyglot.util.Position)
     */
    public ILabel ARRAYLENGTH(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        assert isReachable();

        assert top().isArray();
        setStack(currentStack.pop(Type.OBJECT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ASTORE(int, polyglot.util.Position)
     */
    public ILabel ASTORE(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.ASTORE, index));
        assert isReachable();
        setStack(currentStack.pop(Type.OBJECT));
        useLocal(Type.OBJECT, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ATHROW(polyglot.util.Position)
     */
    public ILabel ATHROW(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ATHROW));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#BALOAD(polyglot.util.Position)
     */
    public ILabel BALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.BALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.BYTE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#BASTORE(polyglot.util.Position)
     */
    public ILabel BASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.BASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.BYTE).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#BIPUSH(byte, polyglot.util.Position)
     */
    public ILabel BIPUSH(final byte b, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(new Byte(b)));
        assert isReachable();
        setStack(currentStack.push(Type.BYTE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#CALOAD(polyglot.util.Position)
     */
    public ILabel CALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.CALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.CHAR));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#CASTORE(polyglot.util.Position)
     */
    public ILabel CASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.CASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.CHAR).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#CHECKCAST(polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel CHECKCAST(final Type type, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, type.className()));
        assert isReachable();
        uncheckedCoerce(type);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#D2F(polyglot.util.Position)
     */
    public ILabel D2F(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.D2F));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#D2I(polyglot.util.Position)
     */
    public ILabel D2I(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.D2I));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#D2L(polyglot.util.Position)
     */
    public ILabel D2L(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.D2L));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DADD(polyglot.util.Position)
     */
    public ILabel DADD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DADD));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DALOAD(polyglot.util.Position)
     */
    public ILabel DALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DASTORE(polyglot.util.Position)
     */
    public ILabel DASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DCMPG(polyglot.util.Position)
     */
    public ILabel DCMPG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DCMPG));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DCMPL(polyglot.util.Position)
     */
    public ILabel DCMPL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DCMPL));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DCONST(double, polyglot.util.Position)
     */
    public ILabel DCONST(final double d, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(d));
        assert isReachable();

        setStack(currentStack.push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DDIV(polyglot.util.Position)
     */
    public ILabel DDIV(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DDIV));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DLOAD(int, polyglot.util.Position)
     */
    public ILabel DLOAD(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.DLOAD, index));
        assert isReachable();

        setStack(currentStack.push(Type.DOUBLE));
        useLocal(Type.DOUBLE, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DMUL(polyglot.util.Position)
     */
    public ILabel DMUL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DMUL));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DNEG(polyglot.util.Position)
     */
    public ILabel DNEG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DNEG));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DREM(polyglot.util.Position)
     */
    public ILabel DREM(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DREM));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DRETURN(polyglot.util.Position)
     */
    public ILabel DRETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DRETURN));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DSTORE(int, polyglot.util.Position)
     */
    public ILabel DSTORE(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.DSTORE, index));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE));
        useLocal(Type.DOUBLE, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DSUB(polyglot.util.Position)
     */
    public ILabel DSUB(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DSUB));
        assert isReachable();

        setStack(currentStack.pop(Type.DOUBLE).pop(Type.DOUBLE).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP(polyglot.util.Position)
     */
    public ILabel DUP(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP));
        assert isReachable();

        assert top().isNarrow();
        setStack(currentStack.push(top()));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP2(polyglot.util.Position)
     */
    public ILabel DUP2(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP2));
        assert isReachable();

        Type t1 = currentStack.top();
        if (t1.isNarrow()) {
            Type t2 = currentStack.pop().top();
            setStack(currentStack.push(t2).push(t1));
        }
        else {
            setStack(currentStack.push(t1));
        }
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP2_X1(polyglot.util.Position)
     */
    public ILabel DUP2_X1(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP2_X1));
        assert isReachable();

        Type t1 = currentStack.top();
        if (t1.isNarrow()) {
            // 321 -> 21321
            Type t2 = currentStack.pop().top();
            Type t3 = currentStack.pop().pop().top();
            assert t2.isNarrow();
            assert t3.isNarrow();
            setStack(currentStack.pop().pop().pop().push(t2).push(t1).push(t3).push(t2).push(t1));
        }
        else {
            Type t21 = t1;
            Type t3 = currentStack.pop().top();
            assert t3.isNarrow();
            setStack(currentStack.pop().pop().push(t21).push(t3).push(t21));
        }
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP2_X2(polyglot.util.Position)
     */
    public ILabel DUP2_X2(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP2_X1));
        assert isReachable();

        Type t1 = currentStack.top();
        if (t1.isNarrow()) {
            Type t2 = currentStack.pop().top();
            assert t2.isNarrow();
            Type t3 = currentStack.pop().pop().top();
            if (t3.isNarrow()) {
                // 4321 -> 214321
                Type t4 = currentStack.pop().pop().pop().top();
                assert t3.isNarrow();
                assert t4.isNarrow();
                setStack(currentStack.pop().pop().pop().pop().push(t2).push(t1).push(t4).push(t3).push(t2).push(t1));
            }
            else {
                // (43)21 -> 21(43)21
                Type t43 = t3;
                setStack(currentStack.pop().pop().pop().push(t2).push(t1).push(t43).push(t2).push(t1));
            }
        }
        else {
            Type t21 = t1;
            Type t3 = currentStack.pop().top();
            if (t3.isNarrow()) {
                // 43(21) -> (21)43(21)
                Type t4 = currentStack.pop().pop().top();
                assert t4.isNarrow();
                setStack(currentStack.pop().pop().pop().push(t21).push(t4).push(t3).push(t21));
            }
            else {
                // (43)(21) -> (21)(43)(21)
                Type t43 = t3;
                setStack(currentStack.pop().pop().push(t21).push(t43).push(t21));
            }
        }
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP_X1(polyglot.util.Position)
     */
    public ILabel DUP_X1(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP_X1));
        assert isReachable();

        // 21 -> 121
        Type t1 = currentStack.top();
        Type t2 = currentStack.pop().top();
        assert t1.isNarrow();
        assert t2.isNarrow();
        setStack(currentStack.pop().pop().push(t1).push(t2).push(t1));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#DUP_X2(polyglot.util.Position)
     */
    public ILabel DUP_X2(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.DUP_X2));
        assert isReachable();

        // 321 -> 1321
        Type t1 = currentStack.top();
        Type t2 = currentStack.pop().top();
        assert t1.isNarrow();
        if (t2.isNarrow()) {
            // 321 -> 1321
            Type t3 = currentStack.pop().pop().top();
            assert t3.isNarrow();
            setStack(currentStack.pop().pop().pop().push(t1).push(t3).push(t2).push(t1));
        }
        else {
            // (32)1 -> 1(32)1
            Type t32 = t2;
            setStack(currentStack.pop().pop().push(t1).push(t32).push(t1));
        }
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#F2D(polyglot.util.Position)
     */
    public ILabel F2D(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.F2D));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#F2I(polyglot.util.Position)
     */
    public ILabel F2I(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.F2I));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#F2L(polyglot.util.Position)
     */
    public ILabel F2L(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.F2L));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FADD(polyglot.util.Position)
     */
    public ILabel FADD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FADD));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FALOAD(polyglot.util.Position)
     */
    public ILabel FALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FASTORE(polyglot.util.Position)
     */
    public ILabel FASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FCMPG(polyglot.util.Position)
     */
    public ILabel FCMPG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FCMPG));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FCMPL(polyglot.util.Position)
     */
    public ILabel FCMPL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FCMPL));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FCONST(float, polyglot.util.Position)
     */
    public ILabel FCONST(final float f, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(f));
        assert isReachable();

        setStack(currentStack.push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FDIV(polyglot.util.Position)
     */
    public ILabel FDIV(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FDIV));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FLOAD(int, polyglot.util.Position)
     */
    public ILabel FLOAD(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.FLOAD, index));
        assert isReachable();

        setStack(currentStack.push(Type.FLOAT));
        useLocal(Type.FLOAT, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FMUL(polyglot.util.Position)
     */
    public ILabel FMUL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FMUL));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FNEG(polyglot.util.Position)
     */
    public ILabel FNEG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FNEG));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FREM(polyglot.util.Position)
     */
    public ILabel FREM(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FREM));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FRETURN(polyglot.util.Position)
     */
    public ILabel FRETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FRETURN));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FSTORE(int, polyglot.util.Position)
     */
    public ILabel FSTORE(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.FSTORE, index));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT));
        useLocal(Type.FLOAT, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#FSUB(polyglot.util.Position)
     */
    public ILabel FSUB(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.FSUB));
        assert isReachable();

        setStack(currentStack.pop(Type.FLOAT).pop(Type.FLOAT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#GETFIELD(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel GETFIELD(final Type container, final String name, final Type it, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, container.className(), name, it.desc()));
        assert isReachable();

        setStack(currentStack.pop(container).push(it));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#GETSTATIC(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel GETSTATIC(final Type container, final String name, final Type it, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, container.className(), name, it.desc()));
        assert isReachable();

        setStack(currentStack.push(it));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#GOTO(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel GOTO(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.GOTO, asmLabel(target)));
        assert isReachable();

        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    // public ILabel GOTO_W(final Label target, final Position pos) {
    // final ILabel h = preAppend(pos);
    // mg.instructions.add(new InsnNode(Opcodes.GOTO));
    // assert isReachable();
    //
    // setStack(Unreachable.it);
    // return postAppend(pos, h);
    // }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2B(polyglot.util.Position)
     */
    public ILabel I2B(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2B));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.BYTE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2C(polyglot.util.Position)
     */
    public ILabel I2C(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2C));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.CHAR));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2D(polyglot.util.Position)
     */
    public ILabel I2D(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2D));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2F(polyglot.util.Position)
     */
    public ILabel I2F(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2F));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2L(polyglot.util.Position)
     */
    public ILabel I2L(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2L));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#I2S(polyglot.util.Position)
     */
    public ILabel I2S(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.I2S));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.SHORT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IADD(polyglot.util.Position)
     */
    public ILabel IADD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IADD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IALOAD(polyglot.util.Position)
     */
    public ILabel IALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IAND(polyglot.util.Position)
     */
    public ILabel IAND(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IAND));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IASTORE(polyglot.util.Position)
     */
    public ILabel IASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ICONST(int, polyglot.util.Position)
     */
    public ILabel ICONST(final int v, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(v));
        assert isReachable();

        setStack(currentStack.push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IDIV(polyglot.util.Position)
     */
    public ILabel IDIV(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IDIV));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFEQ(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFEQ(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFEQ, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFGE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFGE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFGE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFGT(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFGT(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFGT, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFLE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFLE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFLE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFLT(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFLT(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFLT, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFNE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFNE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFNONNULL(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFNONNULL(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IFNULL(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IFNULL(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IFNULL, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ACMPEQ(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ACMPEQ(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ACMPNE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ACMPNE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPNE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPEQ(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPEQ(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPGE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPGE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPGT(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPGT(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGT, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPLE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPLE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPLE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPLT(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPLT(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPLT, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IF_ICMPNE(polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel IF_ICMPNE(final ILabel target, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPNE, asmLabel(target)));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IINC(int, int, polyglot.util.Position)
     */
    public ILabel IINC(final int index, final int incr, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new IincInsnNode(index, incr));
        assert isReachable();

        setStack(currentStack);
        useLocal(Type.INT, index);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ILOAD(int, polyglot.util.Position)
     */
    public ILabel ILOAD(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.ILOAD, index));
        assert isReachable();

        setStack(currentStack.push(Type.INT));
        useLocal(Type.INT, index);
        
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IMUL(polyglot.util.Position)
     */
    public ILabel IMUL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IMUL));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INEG(polyglot.util.Position)
     */
    public ILabel INEG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.INEG));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INSTANCEOF(polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel INSTANCEOF(final Type type, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, type.desc()));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INVOKEINTERFACE(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type[], polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel INVOKEINTERFACE(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, type.className(), name, methodSignature(args, returnIt)));
        assert isReachable();

        setStack(currentStack.pop(args).pop(type).push(returnIt));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INVOKESPECIAL(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type[], polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel INVOKESPECIAL(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type.className(), name, methodSignature(args, returnIt)));
        assert isReachable();

        setStack(currentStack.pop(args).pop(type).push(returnIt));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INVOKESTATIC(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type[], polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel INVOKESTATIC(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, type.className(), name, methodSignature(args, returnIt)));
        assert isReachable();

        setStack(currentStack.pop(args).push(returnIt));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#INVOKEVIRTUAL(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type[], polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel INVOKEVIRTUAL(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, type.className(), name, methodSignature(args, returnIt)));
        assert isReachable();

        setStack(currentStack.pop(args).pop(type).push(returnIt));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IOR(polyglot.util.Position)
     */
    public ILabel IOR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IOR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IREM(polyglot.util.Position)
     */
    public ILabel IREM(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IREM));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IRETURN(polyglot.util.Position)
     */
    public ILabel IRETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IRETURN));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ISHL(polyglot.util.Position)
     */
    public ILabel ISHL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ISHL));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ISHR(polyglot.util.Position)
     */
    public ILabel ISHR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ISHR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ISTORE(int, polyglot.util.Position)
     */
    public ILabel ISTORE(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.ISTORE, index));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        useLocal(Type.INT, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#ISUB(polyglot.util.Position)
     */
    public ILabel ISUB(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.ISUB));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IUSHR(polyglot.util.Position)
     */
    public ILabel IUSHR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IUSHR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#IXOR(polyglot.util.Position)
     */
    public ILabel IXOR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.IXOR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.INT).push(Type.INT));
        return postAppend(pos, h);
    }

    // Label JSR(Label target, Position pos) {
    // JSR i = new JSR(target);
    // assert ! (currentStack instanceof Unreachable); Label h = il.append(i);
    // setStack(OperandStack.add(i, currentStack));
    // return postAppend(pos, h);
    // }
    //
    // Label JSR_W(Label target, Position pos) {
    // JSR_W i = new JSR_W(target);
    // assert ! (currentStack instanceof Unreachable); Label h = il.append(i);
    // setStack(OperandStack.add(i, currentStack));
    // return postAppend(pos, h);
    // }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#L2D(polyglot.util.Position)
     */
    public ILabel L2D(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.L2D));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#L2F(polyglot.util.Position)
     */
    public ILabel L2F(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.L2F));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#L2I(polyglot.util.Position)
     */
    public ILabel L2I(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.L2I));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LADD(polyglot.util.Position)
     */
    public ILabel LADD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LADD));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LALOAD(polyglot.util.Position)
     */
    public ILabel LALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LAND(polyglot.util.Position)
     */
    public ILabel LAND(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LAND));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LASTORE(polyglot.util.Position)
     */
    public ILabel LASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LCMP(polyglot.util.Position)
     */
    public ILabel LCMP(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LCMP));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LCONST(long, polyglot.util.Position)
     */
    public ILabel LCONST(final long l, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(l));
        assert isReachable();
        setStack(currentStack.push(Type.LONG));
        return postAppend(pos, h);
    }
    

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(java.lang.String, polyglot.util.Position)
     */
    public ILabel LDC(final String value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(value));
        assert isReachable();
        setStack(currentStack.push(Type.STRING));
        return postAppend(pos, h);
    }
    
    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(java.lang.String, polyglot.util.Position)
     */
    public ILabel LDC(final Type value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(org.objectweb.asm.Type.getType(value.desc())));

        assert isReachable();
        setStack(currentStack.push(Type.CLASS));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(int, polyglot.util.Position)
     */
    public ILabel LDC(final int value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(value));
        assert isReachable();
        setStack(currentStack.push(Type.INT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(float, polyglot.util.Position)
     */
    public ILabel LDC(final float value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(value));
        assert isReachable();
        setStack(currentStack.push(Type.FLOAT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(double, polyglot.util.Position)
     */
    public ILabel LDC(final double value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(value));
        assert isReachable();
        setStack(currentStack.push(Type.DOUBLE));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDC(long, polyglot.util.Position)
     */
    public ILabel LDC(final long value, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(value));
        assert isReachable();
        setStack(currentStack.push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LDIV(polyglot.util.Position)
     */
    public ILabel LDIV(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LDIV));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LLOAD(int, polyglot.util.Position)
     */
    public ILabel LLOAD(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.LLOAD, index));
        assert isReachable();

        setStack(currentStack.push(Type.LONG));
        useLocal(Type.LONG, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LMUL(polyglot.util.Position)
     */
    public ILabel LMUL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LMUL));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LNEG(polyglot.util.Position)
     */
    public ILabel LNEG(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LNEG));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LOR(polyglot.util.Position)
     */
    public ILabel LOR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LOR));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LREM(polyglot.util.Position)
     */
    public ILabel LREM(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LREM));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LRETURN(polyglot.util.Position)
     */
    public ILabel LRETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LRETURN));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LSHL(polyglot.util.Position)
     */
    public ILabel LSHL(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LSHL));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LSHR(polyglot.util.Position)
     */
    public ILabel LSHR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LSHR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LSTORE(int, polyglot.util.Position)
     */
    public ILabel LSTORE(final int index, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new VarInsnNode(Opcodes.LSTORE, index));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG));
        useLocal(Type.LONG, index);

        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LSUB(polyglot.util.Position)
     */
    public ILabel LSUB(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LSUB));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LUSHR(polyglot.util.Position)
     */
    public ILabel LUSHR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LUSHR));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LXOR(polyglot.util.Position)
     */
    public ILabel LXOR(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.LXOR));
        assert isReachable();

        setStack(currentStack.pop(Type.LONG).pop(Type.LONG).push(Type.LONG));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#MONITORENTER(polyglot.util.Position)
     */
    public ILabel MONITORENTER(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.MONITORENTER));
        assert isReachable();

        setStack(currentStack.pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#MONITOREXIT(polyglot.util.Position)
     */
    public ILabel MONITOREXIT(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.MONITOREXIT));
        assert isReachable();
        setStack(currentStack.pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#MULTIANEWARRAY(polyglot.bytecode.types.Type, int, polyglot.util.Position)
     */
    public ILabel MULTIANEWARRAY(final Type elementType, final int dimensions, final Position pos) {
        final ILabel h = preAppend(pos);
        assert elementType.isRef();
        mn.instructions.add(new MultiANewArrayInsnNode(elementType.desc(), dimensions));
        assert isReachable();
        for (int i = 0; i < dimensions; i++) {
            setStack(currentStack.pop(Type.INT));
        }
        setStack(currentStack.push(Type.array(elementType, dimensions)));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#NEW(polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel NEW(final Type type, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new TypeInsnNode(Opcodes.NEW, type.className()));
        assert isReachable();
        setStack(currentStack.push(type));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#NEWARRAY(polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel NEWARRAY(final Type elementType, final Position pos) {
        final ILabel h = preAppend(pos);
        assert ! elementType.isRef();
        int t = 0;
        if (elementType.isBoolean())
            t = Opcodes.T_BOOLEAN;
        else if (elementType.isChar())
            t = Opcodes.T_CHAR;
        else if (elementType.isByte())
            t = Opcodes.T_BYTE;
        else if (elementType.isShort())
            t = Opcodes.T_SHORT;
        else if (elementType.isInt())
            t = Opcodes.T_INT;
        else if (elementType.isLong())
            t = Opcodes.T_LONG;
        else if (elementType.isFloat())
            t = Opcodes.T_FLOAT;
        else if (elementType.isDouble())
            t = Opcodes.T_DOUBLE;
        else
            assert false;
        mn.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, t));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).push(Type.array(elementType)));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#NOP(polyglot.util.Position)
     */
    public ILabel NOP(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.NOP));
        assert isReachable();

        setStack(currentStack);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#POP(polyglot.util.Position)
     */
    public ILabel POP(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.POP));
        assert isReachable();

        assert top().isNarrow();
        setStack(currentStack.pop());
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#POP2(polyglot.util.Position)
     */
    public ILabel POP2(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.POP2));
        assert isReachable();

        if (top().isNarrow()) {
            assert currentStack.pop().top().isNarrow();
            setStack(currentStack.pop().pop());
        }
        else {
            setStack(currentStack.pop());
        }
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#PUTFIELD(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel PUTFIELD(final Type type, final String name, final Type it, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, type.className(), name, it.desc()));
        assert isReachable();

        setStack(currentStack.pop(it).pop(type));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#PUTSTATIC(polyglot.bytecode.types.Type, java.lang.String, polyglot.bytecode.types.Type, polyglot.util.Position)
     */
    public ILabel PUTSTATIC(final Type type, final String name, final Type it, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, type.className(), name, it.desc()));
        assert isReachable();

        setStack(currentStack.pop(it));
        return postAppend(pos, h);
    }

    // Label RET(Position pos) {
    // RET i = new RET();
    // assert ! (currentStack instanceof Unreachable); Label h = il.append(i);
    // setStack(OperandStack.add(i, (T<O,StackType>) currentStack));
    // return postAppend(pos, h);
    // }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#RETURN(polyglot.util.Position)
     */
    public ILabel RETURN(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        assert isReachable();

        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#SALOAD(polyglot.util.Position)
     */
    public ILabel SALOAD(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.SALOAD));
        assert isReachable();

        setStack(currentStack.pop(Type.INT).pop(Type.OBJECT).push(Type.SHORT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#SASTORE(polyglot.util.Position)
     */
    public ILabel SASTORE(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.SASTORE));
        assert isReachable();

        setStack(currentStack.pop(Type.SHORT).pop(Type.INT).pop(Type.OBJECT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#SIPUSH(short, polyglot.util.Position)
     */
    public ILabel SIPUSH(final short s, final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new LdcInsnNode(s));
        assert isReachable();

        setStack(currentStack.push(Type.SHORT));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#SWAP(polyglot.util.Position)
     */
    public ILabel SWAP(final Position pos) {
        final ILabel h = preAppend(pos);
        mn.instructions.add(new InsnNode(Opcodes.SWAP));
        assert isReachable();

        Type t1 = currentStack.top();
        Type t2 = currentStack.pop().top();
        assert t1.isNarrow();
        assert t2.isNarrow();
        setStack(currentStack.pop(t1).pop(t2).push(t1).push(t2));
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#LOOKUPSWITCH(int[], polyglot.bytecode.rep.ILabel[], polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel LOOKUPSWITCH(final int[] match, final ILabel[] targets, final ILabel defaultTarget, final Position pos) {
        final ILabel h = preAppend(pos);
        LabelNode[] L = new LabelNode[targets.length];
        for (int i = 0; i < targets.length; i++) {
            L[i] = asmLabel(targets[i]);
        }
        mn.instructions.add(new LookupSwitchInsnNode(asmLabel(defaultTarget), match, L));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#TABLESWITCH(int[], polyglot.bytecode.rep.ILabel[], polyglot.bytecode.rep.ILabel, polyglot.util.Position)
     */
    public ILabel TABLESWITCH(final int[] match, final ILabel[] targets, final ILabel defaultTarget, final Position pos) {
        final ILabel h = preAppend(pos);
        LabelNode[] L = new LabelNode[targets.length];
        for (int i = 0; i < targets.length; i++) {
            L[i] = asmLabel(targets[i]);
        }
        mn.instructions.add(new TableSwitchInsnNode(match[0], match[match.length - 1], asmLabel(defaultTarget), L));
        assert isReachable();

        setStack(currentStack.pop(Type.INT));
        setStack(Unreachable.it);
        return postAppend(pos, h);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.Opcodes#assertStack(polyglot.bytecode.types.StackType)
     */
    public void assertStack(final StackType t) {
        // just merge and discard the resulting stack: this will assert if a
        // merge is not
        // possible.
        currentStack.merge(t);
        // assert currentStack.equals(t) : "found " + currentStack +
        // " expected " + t;
    }
}