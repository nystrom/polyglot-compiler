package polyglot.bytecode.rep;

import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.util.Position;

public interface IOpcodes {

    StackType currentStack();

    boolean isUnreachable();

    void setStack(StackType st);

    void addExceptionHandler(final ILabel start_pc, final ILabel end_pc, final ILabel handler_pc, final Type catch_type);

    ILabel makeLabel(final Position pos);

    void addLabel(ILabel L);

    ILabel dup(final Position pos);

    ILabel pop(final Position pos);

    ILabel returnTop(Position pos);

    void uncheckedCoerce(Type neu);

    void uncheckedCoerce(Type old, Type neu);

    void load(final int index, final Type t, final Position pos);

    void store(final int index, Type t, final Position pos);

    boolean isReachable();

    ILabel AALOAD(final Position pos, Type elementType);

    ILabel AASTORE(final Position pos, Type elementType);

    ILabel ACONST_NULL(final Position pos);

    ILabel ALOAD(final int index, final Type it, final Position pos);

    ILabel ANEWARRAY(final Type elementType, final Position pos);

    ILabel ARETURN(final Position pos);

    ILabel ARRAYLENGTH(final Position pos);

    ILabel ASTORE(final int index, final Position pos);

    ILabel ATHROW(final Position pos);

    ILabel BALOAD(final Position pos);

    ILabel BASTORE(final Position pos);

    ILabel BIPUSH(final byte b, final Position pos);

    ILabel CALOAD(final Position pos);

    ILabel CASTORE(final Position pos);

    ILabel CHECKCAST(final Type type, final Position pos);

    ILabel D2F(final Position pos);

    ILabel D2I(final Position pos);

    ILabel D2L(final Position pos);

    ILabel DADD(final Position pos);

    ILabel DALOAD(final Position pos);

    ILabel DASTORE(final Position pos);

    ILabel DCMPG(final Position pos);

    ILabel DCMPL(final Position pos);

    ILabel DCONST(final double d, final Position pos);

    ILabel DDIV(final Position pos);

    ILabel DLOAD(final int index, final Position pos);

    ILabel DMUL(final Position pos);

    ILabel DNEG(final Position pos);

    ILabel DREM(final Position pos);

    ILabel DRETURN(final Position pos);

    ILabel DSTORE(final int index, final Position pos);

    ILabel DSUB(final Position pos);

    ILabel DUP(final Position pos);

    ILabel DUP2(final Position pos);

    ILabel DUP2_X1(final Position pos);

    ILabel DUP2_X2(final Position pos);

    ILabel DUP_X1(final Position pos);

    ILabel DUP_X2(final Position pos);

    ILabel F2D(final Position pos);

    ILabel F2I(final Position pos);

    ILabel F2L(final Position pos);

    ILabel FADD(final Position pos);

    ILabel FALOAD(final Position pos);

    ILabel FASTORE(final Position pos);

    ILabel FCMPG(final Position pos);

    ILabel FCMPL(final Position pos);

    ILabel FCONST(final float f, final Position pos);

    ILabel FDIV(final Position pos);

    ILabel FLOAD(final int index, final Position pos);

    ILabel FMUL(final Position pos);

    ILabel FNEG(final Position pos);

    ILabel FREM(final Position pos);

    ILabel FRETURN(final Position pos);

    ILabel FSTORE(final int index, final Position pos);

    ILabel FSUB(final Position pos);

    ILabel GETFIELD(final Type container, final String name, final Type it, final Position pos);

    ILabel GETSTATIC(final Type container, final String name, final Type it, final Position pos);

    ILabel GOTO(final ILabel target, final Position pos);

    ILabel I2B(final Position pos);

    ILabel I2C(final Position pos);

    ILabel I2D(final Position pos);

    ILabel I2F(final Position pos);

    ILabel I2L(final Position pos);

    ILabel I2S(final Position pos);

    ILabel IADD(final Position pos);

    ILabel IALOAD(final Position pos);

    ILabel IAND(final Position pos);

    ILabel IASTORE(final Position pos);

    ILabel ICONST(final int v, final Position pos);

    ILabel IDIV(final Position pos);

    ILabel IFEQ(final ILabel target, final Position pos);

    ILabel IFGE(final ILabel target, final Position pos);

    ILabel IFGT(final ILabel target, final Position pos);

    ILabel IFLE(final ILabel target, final Position pos);

    ILabel IFLT(final ILabel target, final Position pos);

    ILabel IFNE(final ILabel target, final Position pos);

    ILabel IFNONNULL(final ILabel target, final Position pos);

    ILabel IFNULL(final ILabel target, final Position pos);

    ILabel IF_ACMPEQ(final ILabel target, final Position pos);

    ILabel IF_ACMPNE(final ILabel target, final Position pos);

    ILabel IF_ICMPEQ(final ILabel target, final Position pos);

    ILabel IF_ICMPGE(final ILabel target, final Position pos);

    ILabel IF_ICMPGT(final ILabel target, final Position pos);

    ILabel IF_ICMPLE(final ILabel target, final Position pos);

    ILabel IF_ICMPLT(final ILabel target, final Position pos);

    ILabel IF_ICMPNE(final ILabel target, final Position pos);

    ILabel IINC(final int index, final int incr, final Position pos);

    ILabel ILOAD(final int index, final Position pos);

    ILabel IMUL(final Position pos);

    ILabel INEG(final Position pos);

    ILabel INSTANCEOF(final Type type, final Position pos);

    ILabel INVOKEINTERFACE(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos);

    ILabel INVOKESPECIAL(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos);

    ILabel INVOKESTATIC(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos);

    ILabel INVOKEVIRTUAL(final Type type, final String name, final Type[] args, final Type returnIt, final Position pos);

    ILabel IOR(final Position pos);

    ILabel IREM(final Position pos);

    ILabel IRETURN(final Position pos);

    ILabel ISHL(final Position pos);

    ILabel ISHR(final Position pos);

    ILabel ISTORE(final int index, final Position pos);

    ILabel ISUB(final Position pos);

    ILabel IUSHR(final Position pos);

    ILabel IXOR(final Position pos);

    ILabel L2D(final Position pos);

    ILabel L2F(final Position pos);

    ILabel L2I(final Position pos);

    ILabel LADD(final Position pos);

    ILabel LALOAD(final Position pos);

    ILabel LAND(final Position pos);

    ILabel LASTORE(final Position pos);

    ILabel LCMP(final Position pos);

    ILabel LCONST(final long l, final Position pos);

    ILabel LDC(final String value, final Position pos);

    ILabel LDC(final int value, final Position pos);

    ILabel LDC(final float value, final Position pos);

    ILabel LDC(final double value, final Position pos);

    ILabel LDC(final long value, final Position pos);

    ILabel LDIV(final Position pos);

    ILabel LLOAD(final int index, final Position pos);

    ILabel LMUL(final Position pos);

    ILabel LNEG(final Position pos);

    ILabel LOR(final Position pos);

    ILabel LREM(final Position pos);

    ILabel LRETURN(final Position pos);

    ILabel LSHL(final Position pos);

    ILabel LSHR(final Position pos);

    ILabel LSTORE(final int index, final Position pos);

    ILabel LSUB(final Position pos);

    ILabel LUSHR(final Position pos);

    ILabel LXOR(final Position pos);

    ILabel MONITORENTER(final Position pos);

    ILabel MONITOREXIT(final Position pos);

    ILabel MULTIANEWARRAY(final Type elementType, final int dimensions, final Position pos);

    ILabel NEW(final Type type, final Position pos);

    ILabel NEWARRAY(final Type type, final Position pos);

    ILabel NOP(final Position pos);

    ILabel POP(final Position pos);

    ILabel POP2(final Position pos);

    ILabel PUTFIELD(final Type type, final String name, final Type it, final Position pos);

    ILabel PUTSTATIC(final Type type, final String name, final Type it, final Position pos);

    ILabel RETURN(final Position pos);

    ILabel SALOAD(final Position pos);

    ILabel SASTORE(final Position pos);

    ILabel SIPUSH(final short s, final Position pos);

    ILabel SWAP(final Position pos);

    ILabel LOOKUPSWITCH(final int[] match, final ILabel[] targets, final ILabel defaultTarget, final Position pos);

    ILabel TABLESWITCH(final int[] match, final ILabel[] targets, final ILabel defaultTarget, final Position pos);

    void assertStack(final StackType t);

    ILabel LDC(Type type, Position pos);
}