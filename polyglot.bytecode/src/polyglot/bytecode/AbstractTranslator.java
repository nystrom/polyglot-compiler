package polyglot.bytecode;

import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.NonEmpty;
import polyglot.bytecode.types.Type;
import polyglot.bytecode.types.Unreachable;
import polyglot.dispatch.Dispatch;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Copy;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public abstract class AbstractTranslator implements Copy {

    public AbstractTranslator(Job job, TypeSystem ts, NodeFactory nf) {
        super();
        this.job = job;
        this.ts = ts;
        this.nf = nf;
    }
    
    public StmtTranslator newStmtTranslator(BytecodeTranslator bc, MethodContext context) {
        return new StmtTranslator(job, ts, nf, bc, context);
    }
    public ExprTranslator newExprTranslator(BytecodeTranslator bc, MethodContext context) {
        return new ExprTranslator(job, ts, nf, bc, context);
    }
    public BranchTranslator newBranchTranslator(BytecodeTranslator bc, MethodContext context, ILabel target, boolean branchOnTrue) {
        return new BranchTranslator(job, ts, nf, bc, context, target, branchOnTrue);
    }
    public ClassTranslator newClassTranslator(BytecodeTranslator bc, ClassDef cd, MethodContext context) {
        return new ClassTranslator(job, ts, nf, bc, cd, context);
    }
    public ClassTranslator newClassTranslator(BytecodeTranslator bc, ClassDef cd) {
        return new ClassTranslator(job, ts, nf, bc, cd);
    }
    
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;

    public AbstractExpTranslator copy() {
        try {
            return (AbstractExpTranslator) super.clone();
        }
        catch (CloneNotSupportedException e) {
            assert false;
            return null;
        }
    }

    protected Type typeFromPolyglotTypeV(polyglot.types.Type t) {
        if (t.isArray())
            return Type.array(typeFromPolyglotTypeV(t.toArray().base()));
        return Type.typeFromPolyglotType(t);
    }

    protected Type typeof(polyglot.types.Type t) {
        return typeFromPolyglotTypeV(t);
    }

    protected Type typeof(Ref<? extends polyglot.types.Type> t) {
        return typeFromPolyglotTypeV(Types.get(t));
    }

    protected Type typeof(TypeNode t) {
        return typeof(t.type());
    }

    public Type[] typeof(List<Expr> es) {
        Type[] ts = new Type[es.size()];
        for (int i = 0; i < es.size(); i++) {
            ts[i] = typeof(es.get(i));
        }
        return ts;
    }

    public Type[] typeofTypes(List<polyglot.types.Type> es) {
        Type[] ts = new Type[es.size()];
        for (int i = 0; i < es.size(); i++) {
            ts[i] = typeof(es.get(i));
        }
        return ts;
    }

    protected Type typeof(Expr e) {
        polyglot.types.Type t = e.type();
        return typeof(t);
    }

    protected static void box(IOpcodes il, final Type top, final Position pos) {
        if (top.isRef())
            return;
        else if (top.isBoolean()) {
            Type Boolean = Type.typeFromDescriptor("Ljava/lang/Boolean;");
            il.INVOKESTATIC(Boolean, "valueOf", new Type[] { top }, Boolean, pos);
        }
        else if (top.isChar()) {
            Type Char = Type.typeFromDescriptor("Ljava/lang/Character;");
            il.INVOKESTATIC(Char, "valueOf", new Type[] { top }, Char, pos);
        }
        else if (top.isShort()) {
            Type Short = Type.typeFromDescriptor("Ljava/lang/Short;");
            il.INVOKESTATIC(Short, "valueOf", new Type[] { top }, Short, pos);
        }
        else if (top.isByte()) {
            Type Byte = Type.typeFromDescriptor("Ljava/lang/Byte;");
            il.INVOKESTATIC(Byte, "valueOf", new Type[] { top }, Byte, pos);
        }
        else if (top.isLong()) {
            Type Long = Type.typeFromDescriptor("Ljava/lang/Long;");
            il.INVOKESTATIC(Long, "valueOf", new Type[] { top }, Long, pos);
        }
        else if (top.isFloat()) {
            Type Float = Type.typeFromDescriptor("Ljava/lang/Float;");
            il.INVOKESTATIC(Float, "valueOf", new Type[] { top }, Float, pos);
        }
        else if (top.isDouble()) {
            Type Double = Type.typeFromDescriptor("Ljava/lang/Double;");
            il.INVOKESTATIC(Double, "valueOf", new Type[] { top }, Double, pos);
        }
        else if (top.isInt()) {
            Type Int = Type.typeFromDescriptor("Ljava/lang/Integer;");
            il.INVOKESTATIC(Int, "valueOf", new Type[] { top }, Int, pos);
        }
        else
            assert false;
    }

    protected static void unbox(final IOpcodes il, final Type top, final Position pos) {
        if (top.isRef())
            return;
        else if (top.isBoolean()) {
            Type Boolean = Type.typeFromDescriptor("Ljava/lang/Boolean;");
            il.CHECKCAST(Boolean, pos);
            il.INVOKEVIRTUAL(Boolean, "booleanValue", new Type[0], top, pos);
        }
        else if (top.isChar()) {
            Type Char = Type.typeFromDescriptor("Ljava/lang/Character;");
            il.CHECKCAST(Char, pos);
            il.INVOKEVIRTUAL(Char, "charValue", new Type[0], top, pos);
        }
        else if (top.isByte()) {
            Type Byte = Type.typeFromDescriptor("Ljava/lang/Byte;");
            il.CHECKCAST(Byte, pos);
            il.INVOKEVIRTUAL(Byte, "byteValue", new Type[0], top, pos);
        }
        else if (top.isShort()) {
            Type Short = Type.typeFromDescriptor("Ljava/lang/Short;");
            il.CHECKCAST(Short, pos);
            il.INVOKEVIRTUAL(Short, "shortValue", new Type[0], top, pos);
        }
        else if (top.isInt()) {
            Type Int = Type.typeFromDescriptor("Ljava/lang/Integer;");
            il.CHECKCAST(Int, pos);
            il.INVOKEVIRTUAL(Int, "intValue", new Type[0], top, pos);
        }
        else if (top.isLong()) {
            Type Long = Type.typeFromDescriptor("Ljava/lang/Long;");
            il.CHECKCAST(Long, pos);
            il.INVOKEVIRTUAL(Long, "longValue", new Type[0], top, pos);
        }
        else if (top.isFloat()) {
            Type Float = Type.typeFromDescriptor("Ljava/lang/Float;");
            il.CHECKCAST(Float, pos);
            il.INVOKEVIRTUAL(Float, "floatValue", new Type[0], top, pos);
        }
        else if (top.isDouble()) {
            Type Double = Type.typeFromDescriptor("Ljava/lang/Double;");
            il.CHECKCAST(Double, pos);
            il.INVOKEVIRTUAL(Double, "doubleValue", new Type[0], top, pos);
        }
        else
            assert false;
    }

    protected static void coerce(IOpcodes il, final Type currentTop, final Type newTop, final Position pos) {
        if (currentTop.equals(newTop))
            return;
        if (currentTop.isRef() && newTop.isRef()) {
            il.CHECKCAST(newTop, pos);
            return;
        }
        if (currentTop.isRef() && !newTop.isRef()) {
            unbox(il, newTop, pos);
            return;
        }
        if (!currentTop.isRef() && newTop.isRef()) {
            box(il, currentTop, pos);
            if (! currentTop.equals(newTop))
                il.CHECKCAST(newTop, pos);
            return;
        }
        if (currentTop.isIType() && newTop.isBoolean()) {
            il.uncheckedCoerce(currentTop, newTop);
            return;
        }
        if (currentTop.isIType() && newTop.isByte()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2B(pos);
            return;
        }
        if (currentTop.isIType() && newTop.isShort()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2S(pos);
            return;
        }
        if (currentTop.isIType() && newTop.isChar()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2C(pos);
            return;
        }
        if (currentTop.isIType() && newTop.isInt()) {
            il.uncheckedCoerce(currentTop, Type.INT);
            return;
        }
        if (currentTop.isIType() && newTop.isLong()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2L(pos);
            return;
        }
        if (currentTop.isIType() && newTop.isFloat()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2F(pos);
            return;
        }
        if (currentTop.isIType() && newTop.isDouble()) {
            coerce(il, currentTop, Type.INT, pos);
            il.I2D(pos);
            return;
        }
        if (currentTop.isLong() && newTop.isIType()) {
            il.L2I(pos);
            coerce(il, Type.INT, newTop, pos);
            return;
        }
        if (currentTop.isLong() && newTop.isLong()) {
            return;
        }
        if (currentTop.isLong() && newTop.isFloat()) {
            il.L2F(pos);
            return;
        }
        if (currentTop.isLong() && newTop.isDouble()) {
            il.L2D(pos);
            return;
        }
        if (currentTop.isFloat() && newTop.isIType()) {
            il.F2I(pos);
            coerce(il, Type.INT, newTop, pos);
            return;
        }
        if (currentTop.isFloat() && newTop.isLong()) {
            il.F2L(pos);
            return;
        }
        if (currentTop.isFloat() && newTop.isFloat()) {
            return;
        }
        if (currentTop.isFloat() && newTop.isDouble()) {
            il.F2D(pos);
            return;
        }
        if (currentTop.isDouble() && newTop.isIType()) {
            il.D2I(pos);
            coerce(il, Type.INT, newTop, pos);
            return;
        }
        if (currentTop.isDouble() && newTop.isLong()) {
            il.D2L(pos);
            return;
        }
        if (currentTop.isDouble() && newTop.isFloat()) {
            il.D2F(pos);
            return;
        }
        if (currentTop.isDouble() && newTop.isDouble()) {
            return;
        }
        assert false : "cannot coerce " + currentTop + " to " + newTop;
    }

    public void visitChild(Node n) {
        visitChild(n, this);
    }
    public void visitChild(Node s, AbstractTranslator t) {
        new Dispatch.Dispatcher("visit").invoke(t, s);
    }
    static
    boolean isI(Type t) {
        if (t.isBoolean())
            return false;
        return t.isIType();
    }
    static
    boolean isJ(Type t) {
        if (t.isBoolean())
            return false;
        return t.isIType() || t.isLong();
    }
    static
    boolean isF(Type t) {
        if (t.isBoolean())
            return false;
        return t.isIType() || t.isLong() || t.isFloat();
    }
    static
    boolean isD(Type t) {
        if (t.isBoolean())
            return false;
        return t.isIType() || t.isLong() || t.isFloat() || t.isDouble();
    }


}
