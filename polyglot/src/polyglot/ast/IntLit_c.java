package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.types.*;
import jltools.visit.*;
import jltools.util.*;

public class IntLit_c extends NumLit_c implements IntLit
{
    protected Kind kind;

    public IntLit_c(Ext ext, Position pos, long value) {
	super(ext, pos, value);

        if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
	    kind = BYTE;
	}
	else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
	    kind = SHORT;
	}
	else if (Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE) {
	    kind = INT;
	}
	else {
	    kind = LONG;
	}
    }

    public long value() {
        return (int) longValue();
    }

    public IntLit value(long value) {
        IntLit_c n = (IntLit_c) copy();
	n.value = value;
	return n;
    }

    public IntLit.Kind kind() {
        return kind;
    }

    public IntLit kind(IntLit.Kind kind) {
	IntLit_c n = (IntLit_c) copy();
	n.kind = kind;
	return n;
    }

    public Node typeCheck_(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

	Kind kind = kind();

        if (kind == BYTE) {
	    return type(ts.Byte());
	}
	else if (kind == SHORT) {
	    return type(ts.Short());
	}
	else if (kind == INT) {
	    return type(ts.Int());
	}
	else if (kind == LONG) {
	    return type(ts.Long());
	}
	else {
	    throw new InternalCompilerError("Unrecognized IntLit kind " + kind);
	}
    }

    public String toString() {
	return Long.toString(value);
    }

    public void translate_(CodeWriter w, Translator tr) {
	if (kind() == LONG) {
	    w.write(Long.toString(value) + "L");
	}
	else {
	    w.write(Long.toString(value));
	}
    }
}
