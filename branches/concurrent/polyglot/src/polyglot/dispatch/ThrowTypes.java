package polyglot.dispatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccess_c;
import polyglot.ast.Assign;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Call_c;
import polyglot.ast.Cast_c;
import polyglot.ast.ConstructorCall_c;
import polyglot.ast.Expr;
import polyglot.ast.Field_c;
import polyglot.ast.NewArray_c;
import polyglot.ast.New_c;
import polyglot.ast.Node_c;
import polyglot.ast.Special;
import polyglot.ast.Throw_c;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;

public class ThrowTypes {
    TypeSystem ts;

    public ThrowTypes(TypeSystem ts) {
	this.ts = ts;
    }

    public List<Type> visit(Node_c n) throws SemanticException {
	return Collections.<Type> emptyList();
    }

    public List<Type> visit(Call_c n) {
	List<Type> l = new ArrayList<Type>();

	l.addAll(n.methodInstance().throwTypes());
	l.addAll(ts.uncheckedExceptions());

	if (n.target() instanceof Expr && !(n.target() instanceof Special)) {
	    l.add(ts.NullPointerException());
	}

	return l;
    }

    public List<Type> visit(New_c n) {
	List<Type> l = new ArrayList<Type>();
	l.addAll(n.constructorInstance().throwTypes());
	l.addAll(ts.uncheckedExceptions());
	return l;
    }

    public List<Type> visit(ArrayAccess_c n) {
	return Arrays.asList(ts.OutOfBoundsException(), ts.NullPointerException());
    }

    public List<Type> visit(Assign_c n) {
	if (n.left() instanceof ArrayAccess) {
	    List<Type> l = new ArrayList<Type>();

	    if (n.operator() == Assign.ASSIGN && n.type().isReference()) {
		l.add(ts.ArrayStoreException());
	    }
	    
	    if (n.operator() == Assign.DIV_ASSIGN || n.operator() == Assign.MOD_ASSIGN) {
		l.add(ts.ArithmeticException());
	    }

	    l.add(ts.NullPointerException());
	    l.add(ts.OutOfBoundsException());

	    return l;
	}

	return Collections.<Type> emptyList();
    }

    public List<Type> visit(Field_c n) {
	if (n.target() instanceof Expr && !(n.target() instanceof Special)) {
	    return Collections.<Type> singletonList(ts.NullPointerException());
	}

	return Collections.<Type> emptyList();
    }

    public List<Type> visit(Binary_c n) {
	if (n.operator() == Binary.DIV || n.operator() == Binary.MOD) {
	    return Collections.<Type> singletonList(ts.ArithmeticException());
	}

	return Collections.<Type> emptyList();
    }

    public List<Type> visit(Cast_c n) {
	if (n.expr().type().isReference()) {
	    return Collections.<Type> singletonList(ts.ClassCastException());
	}

	return Collections.<Type> emptyList();
    }

    public List<Type> visit(ConstructorCall_c n) {
	List<Type> l = new ArrayList<Type>();
	l.addAll(n.constructorInstance().throwTypes());
	l.addAll(ts.uncheckedExceptions());
	return l;
    }

    public List<Type> visit(NewArray_c n) {
	if (n.dims() != null && !n.dims().isEmpty()) {
	    // if dimension expressions are given, then
	    // a NegativeArraySizeException may be thrown.
	    try {
		return Arrays.asList(ts.typeForName(QName.make("java.lang.NegativeArraySizeException")));
	    }
	    catch (SemanticException e) {
		throw new InternalCompilerError("Cannot find class java.lang.NegativeArraySizeException", e);
	    }
	}
	return Collections.<Type> emptyList();
    }

    public List<Type> visit(Throw_c n) {
	// if the exception that a throw statement is given to throw is null,
	// then a NullPointerException will be thrown.
	return Arrays.asList(n.expr().type(), ts.NullPointerException());
    }
}
