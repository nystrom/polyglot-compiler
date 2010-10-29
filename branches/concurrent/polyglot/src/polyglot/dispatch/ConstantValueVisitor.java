package polyglot.dispatch;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;

public class ConstantValueVisitor {
    Job job;
    private TypeSystem ts;
    private NodeFactory nf;
    
    public ConstantValueVisitor(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }
    
    public static final Object NOT_CONSTANT = new Object();

    public Object visit(Expr n) {
	return NOT_CONSTANT;
    }

    public Object visit(IntLit n) {
	if (n.kind() == IntLit.INT)
	    return (int) n.value();
	else
	    return n.value();
    }

    public Object visit(FloatLit n) {
	if (n.kind() == FloatLit.FLOAT)
	    return (float) n.value();
	else
	    return n.value();
    }

    public Object visit(CharLit n) {
	return n.value();
    }

    public Object visit(BooleanLit n) {
	return n.value();
    }

    public Object visit(StringLit n) {
	return n.value();
    }

    public Object visit(NullLit n) {
	return null;
    }

    public Object visit(ClassLit n) {
	return NOT_CONSTANT;
    }
    
    public Object visit(Lit n) {
	return NOT_CONSTANT;
    }

    public Object visit(Local n) {
	LocalInstance li = n.localInstance();
	if (li.isConstant())
	    return li.constantValue();
	return NOT_CONSTANT;
    }
    
    public Object visit(Field n) {
	FieldInstance fi = n.fieldInstance();
	if (fi.isConstant())
	    return fi.constantValue();
	return NOT_CONSTANT;
    }

    public Object visit(Binary n) {
	Binary.Operator op = n.operator();
	Expr left = n.left();
	Expr right = n.right();

	Object lv = left.accept(this);
	Object rv = right.accept(this);

	if (op == Binary.ADD && (lv instanceof String || rv instanceof String)) {
	    // toString() does what we want for String, Number, and Boolean
	    return String.valueOf(lv) + String.valueOf(rv);
	}

	if (op == Binary.EQ && (lv instanceof String && rv instanceof String)) {
	    return Boolean.valueOf(((String) lv).intern() == ((String) rv).intern());
	}

	if (op == Binary.NE && (lv instanceof String && rv instanceof String)) {
	    return Boolean.valueOf(((String) lv).intern() != ((String) rv).intern());
	}

	// promote chars to ints.
	if (lv instanceof Character) {
	    lv = Integer.valueOf(((Character) lv).charValue());
	}

	if (rv instanceof Character) {
	    rv = Integer.valueOf(((Character) rv).charValue());
	}

	try {
	    if (lv instanceof Number && rv instanceof Number) {
		if (lv instanceof Double || rv instanceof Double) {
		    double l = ((Number) lv).doubleValue();
		    double r = ((Number) rv).doubleValue();
		    if (op == Binary.ADD) return Double.valueOf(l + r);
		    if (op == Binary.SUB) return Double.valueOf(l - r);
		    if (op == Binary.MUL) return Double.valueOf(l * r);
		    if (op == Binary.DIV) return Double.valueOf(l / r);
		    if (op == Binary.MOD) return Double.valueOf(l % r);
		    if (op == Binary.EQ) return Boolean.valueOf(l == r);
		    if (op == Binary.NE) return Boolean.valueOf(l != r);
		    if (op == Binary.LT) return Boolean.valueOf(l < r);
		    if (op == Binary.LE) return Boolean.valueOf(l <= r);
		    if (op == Binary.GE) return Boolean.valueOf(l >= r);
		    if (op == Binary.GT) return Boolean.valueOf(l > r);
		    return NOT_CONSTANT;
		}

		if (lv instanceof Float || rv instanceof Float) {
		    float l = ((Number) lv).floatValue();
		    float r = ((Number) rv).floatValue();
		    if (op == Binary.ADD) return Float.valueOf(l + r);
		    if (op == Binary.SUB) return Float.valueOf(l - r);
		    if (op == Binary.MUL) return Float.valueOf(l * r);
		    if (op == Binary.DIV) return Float.valueOf(l / r);
		    if (op == Binary.MOD) return Float.valueOf(l % r);
		    if (op == Binary.EQ) return Boolean.valueOf(l == r);
		    if (op == Binary.NE) return Boolean.valueOf(l != r);
		    if (op == Binary.LT) return Boolean.valueOf(l < r);
		    if (op == Binary.LE) return Boolean.valueOf(l <= r);
		    if (op == Binary.GE) return Boolean.valueOf(l >= r);
		    if (op == Binary.GT) return Boolean.valueOf(l > r);
		    return NOT_CONSTANT;
		}

		if (lv instanceof Long && rv instanceof Number) {
		    long l = ((Long) lv).longValue();
		    long r = ((Number) rv).longValue();
		    if (op == Binary.SHL) return Long.valueOf(l << r);
		    if (op == Binary.SHR) return Long.valueOf(l >> r);
		    if (op == Binary.USHR) return Long.valueOf(l >>> r);
		}

		if (lv instanceof Long || rv instanceof Long) {
		    long l = ((Number) lv).longValue();
		    long r = ((Number) rv).longValue();
		    if (op == Binary.ADD) return Long.valueOf(l + r);
		    if (op == Binary.SUB) return Long.valueOf(l - r);
		    if (op == Binary.MUL) return Long.valueOf(l * r);
		    if (op == Binary.DIV) return Long.valueOf(l / r);
		    if (op == Binary.MOD) return Long.valueOf(l % r);
		    if (op == Binary.EQ) return Boolean.valueOf(l == r);
		    if (op == Binary.NE) return Boolean.valueOf(l != r);
		    if (op == Binary.LT) return Boolean.valueOf(l < r);
		    if (op == Binary.LE) return Boolean.valueOf(l <= r);
		    if (op == Binary.GE) return Boolean.valueOf(l >= r);
		    if (op == Binary.GT) return Boolean.valueOf(l > r);
		    if (op == Binary.BIT_AND) return Long.valueOf(l & r);
		    if (op == Binary.BIT_OR) return Long.valueOf(l | r);
		    if (op == Binary.BIT_XOR) return Long.valueOf(l ^ r);
		    return NOT_CONSTANT;
		}

		// At this point, both lv and rv must be ints.
		int l = ((Number) lv).intValue();
		int r = ((Number) rv).intValue();

		if (op == Binary.ADD) return Integer.valueOf(l + r);
		if (op == Binary.SUB) return Integer.valueOf(l - r);
		if (op == Binary.MUL) return Integer.valueOf(l * r);
		if (op == Binary.DIV) return Integer.valueOf(l / r);
		if (op == Binary.MOD) return Integer.valueOf(l % r);
		if (op == Binary.EQ) return Boolean.valueOf(l == r);
		if (op == Binary.NE) return Boolean.valueOf(l != r);
		if (op == Binary.LT) return Boolean.valueOf(l < r);
		if (op == Binary.LE) return Boolean.valueOf(l <= r);
		if (op == Binary.GE) return Boolean.valueOf(l >= r);
		if (op == Binary.GT) return Boolean.valueOf(l > r);
		if (op == Binary.BIT_AND) return Integer.valueOf(l & r);
		if (op == Binary.BIT_OR) return Integer.valueOf(l | r);
		if (op == Binary.BIT_XOR) return Integer.valueOf(l ^ r);
		if (op == Binary.SHL) return Integer.valueOf(l << r);
		if (op == Binary.SHR) return Integer.valueOf(l >> r);
		if (op == Binary.USHR) return Integer.valueOf(l >>> r);
		return NOT_CONSTANT;
	    }
	}
	catch (ArithmeticException e) {
	    // ignore div by 0
	    return NOT_CONSTANT;
	}

	if (lv instanceof Boolean && rv instanceof Boolean) {
	    boolean l = (Boolean) lv;
	    boolean r = (Boolean) rv;

	    if (op == Binary.EQ) return Boolean.valueOf(l == r);
	    if (op == Binary.NE) return Boolean.valueOf(l != r);
	    if (op == Binary.BIT_AND) return Boolean.valueOf(l & r);
	    if (op == Binary.BIT_OR) return Boolean.valueOf(l | r);
	    if (op == Binary.BIT_XOR) return Boolean.valueOf(l ^ r);
	    if (op == Binary.COND_AND) return Boolean.valueOf(l && r);
	    if (op == Binary.COND_OR) return Boolean.valueOf(l || r);
	}

	return NOT_CONSTANT;
    }

    public Object visit(Unary n) {
	Unary.Operator op = n.operator();

	Object v = n.expr().accept(this);

	if (v instanceof Boolean) {
	    boolean vv = (Boolean) v;
	    if (op == Unary.NOT) return Boolean.valueOf(!vv);
	}
	if (v instanceof Double) {
	    double vv = (Double) v;
	    if (op == Unary.POS) return Double.valueOf(+vv);
	    if (op == Unary.NEG) return Double.valueOf(-vv);
	}
	if (v instanceof Float) {
	    float vv = (Float) v;
	    if (op == Unary.POS) return Float.valueOf(+vv);
	    if (op == Unary.NEG) return Float.valueOf(-vv);
	}
	if (v instanceof Long) {
	    long vv = (Long) v;
	    if (op == Unary.BIT_NOT) return Long.valueOf(~vv);
	    if (op == Unary.POS) return Long.valueOf(+vv);
	    if (op == Unary.NEG) return Long.valueOf(-vv);
	}
	if (v instanceof Integer) {
	    int vv = (Integer) v;
	    if (op == Unary.BIT_NOT) return Integer.valueOf(~vv);
	    if (op == Unary.POS) return Integer.valueOf(+vv);
	    if (op == Unary.NEG) return Integer.valueOf(-vv);
	}
	if (v instanceof Character) {
	    char vv = (Character) v;
	    if (op == Unary.BIT_NOT) return Integer.valueOf(~vv);
	    if (op == Unary.POS) return Integer.valueOf(+vv);
	    if (op == Unary.NEG) return Integer.valueOf(-vv);
	}
	if (v instanceof Short) {
	    short vv = (Short) v;
	    if (op == Unary.BIT_NOT) return Integer.valueOf(~vv);
	    if (op == Unary.POS) return Integer.valueOf(+vv);
	    if (op == Unary.NEG) return Integer.valueOf(-vv);
	}
	if (v instanceof Byte) {
	    byte vv = (Byte) v;
	    if (op == Unary.BIT_NOT) return Integer.valueOf(~vv);
	    if (op == Unary.POS) return Integer.valueOf(+vv);
	    if (op == Unary.NEG) return Integer.valueOf(-vv);
	}

	// not a constant
	return NOT_CONSTANT;
    }

    public Object visit(Conditional n) {
	Object cond = n.cond().accept(this);

	if (cond instanceof Boolean) {
	    boolean c = (Boolean) cond;
	    if (c) {
		return n.consequent().<Object>accept(this);
	    }
	    else {
		return n.alternative().<Object>accept(this);
	    }
	}

	return NOT_CONSTANT;
    }

    public Object visit(Cast n) {
	Type ct = n.castType().type();
	Context context = ts.emptyContext(); // new ContextCache(job, ts, nf).context(n);

	Object v = n.expr().accept(this);
	
	if (v == null) {
	    return null;
	}
	
	if (v instanceof Boolean) {
	    if (ct.isBoolean()) return v;
	}

	if (v instanceof String) {
	    if (ct.typeEquals(ts.String(), context)) return v;
	}

	if (v instanceof Double) {
	    double vv = (Double) v;

	    if (ct.isDouble()) return (double) vv;
	    if (ct.isFloat()) return (float) vv;
	    if (ct.isLong()) return (long) vv;
	    if (ct.isInt()) return (int) vv;
	    if (ct.isChar()) return (char) vv;
	    if (ct.isShort()) return (short) vv;
	    if (ct.isByte()) return (byte) vv;
	}

	if (v instanceof Float) {
	    float vv = ((Float) v).floatValue();

	    if (ct.isDouble()) return (double) vv;
	    if (ct.isFloat()) return (float) vv;
	    if (ct.isLong()) return (long) vv;
	    if (ct.isInt()) return (int) vv;
	    if (ct.isChar()) return (char) vv;
	    if (ct.isShort()) return (short) vv;
	    if (ct.isByte()) return (byte) vv;
	}

	if (v instanceof Number) {
	    long vv = ((Number) v).longValue();

	    if (ct.isDouble()) return (double) vv;
	    if (ct.isFloat()) return (float) vv;
	    if (ct.isLong()) return (long) vv;
	    if (ct.isInt()) return (int) vv;
	    if (ct.isChar()) return (char) vv;
	    if (ct.isShort()) return (short) vv;
	    if (ct.isByte()) return (byte) vv;
	}

	if (v instanceof Character) {
	    char vv = ((Character) v).charValue();

	    if (ct.isDouble()) return (double) vv;
	    if (ct.isFloat()) return (float) vv;
	    if (ct.isLong()) return (long) vv;
	    if (ct.isInt()) return (int) vv;
	    if (ct.isChar()) return (char) vv;
	    if (ct.isShort()) return (short) vv;
	    if (ct.isByte()) return (byte) vv;
	}

	// not a constant
	return NOT_CONSTANT;
    }
}
