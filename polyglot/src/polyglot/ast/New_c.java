/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.dispatch.TypeChecker;
import polyglot.frontend.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>New</code> is an immutable representation of the use of the
 * <code>new</code> operator to create a new instance of a class.  In
 * addition to the type of the class being created, a <code>New</code> has a
 * list of arguments to be passed to the constructor of the object and an
 * optional <code>ClassBody</code> used to support anonymous classes.
 */
public class New_c extends Expr_c implements New
{
    protected Expr qualifier;
    protected TypeNode tn;
    protected List<Expr> arguments;
    protected ClassBody body;
    protected Ref<ConstructorInstance> ci;
    protected ClassDef anonType;

    
    public New_c(Position pos, Expr qualifier, TypeNode tn, List<Expr> arguments, ClassBody body) {
	super(pos);
        assert(tn != null && arguments != null); // qualifier and body may be null
        this.qualifier = qualifier;
        this.tn = tn;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	this.body = body;
	
	TypeSystem ts = Globals.TS();
	ConstructorInstance ci = ts.createConstructorInstance(position(), new ErrorRef_c<ConstructorDef>(ts, position(), "Cannot get ConstructorDef before type-checking constructor call."));
	this.setCi(Types.<ConstructorInstance>lazyRef(ci));
    }

    public List<Def> defs() {
        if (body != null) {
            return Collections.<Def>singletonList(anonType);
        }
        return Collections.<Def>emptyList();
    }

    /** Get the qualifier expression of the allocation. */
    public Expr qualifier() {
        return this.qualifier;
    }

    /** Set the qualifier expression of the allocation. */
    public New qualifier(Expr qualifier) {
        New_c n = (New_c) copy();
        n.qualifier = qualifier;
        return n;
    }

    /** Get the type we are instantiating. */
    public TypeNode objectType() {
        return this.tn;
    }

    /** Set the type we are instantiating. */
    public New objectType(TypeNode tn) {
        New_c n = (New_c) copy();
	n.tn = tn;
	return n;
    }

    public ClassDef anonType() {
	return this.anonType;
    }

    public New anonType(ClassDef anonType) {
        if (anonType == this.anonType) return this;
	New_c n = (New_c) copy();
	n.anonType = anonType;
	return n;
    }

    public ProcedureInstance procedureInstance() {
	return constructorInstance();
    }

    public ConstructorInstance constructorInstance() {
	return Types.get(this.getCi());
    }

    public New constructorInstance(ConstructorInstance ci) {
        if (ci == this.getCi()) return this;
	New_c n = (New_c) copy();
	n.getCi().update(ci);
	return n;
    }

    public List<Expr> arguments() {
	return this.arguments;
    }

    public ProcedureCall arguments(List<Expr> arguments) {
	New_c n = (New_c) copy();
	n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	return n;
    }

    public ClassBody body() {
	return this.body;
    }

    public New body(ClassBody body) {
	New_c n = (New_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the expression. */
    protected New_c reconstruct(Expr qualifier, TypeNode tn, List<Expr> arguments, ClassBody body) {
	if (qualifier != this.qualifier || tn != this.tn || ! CollectionUtil.allEqual(arguments, this.arguments) || body != this.body) {
	    New_c n = (New_c) copy();
	    n.tn = tn;
	    n.qualifier = qualifier;
	    n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr qualifier = (Expr) visitChild(this.qualifier, v);
	TypeNode tn = (TypeNode) visitChild(this.tn, v);
	List<Expr> arguments = visitList(this.arguments, v);
	ClassBody body = (ClassBody) visitChild(this.body, v);
	return reconstruct(qualifier, tn, arguments, body);
    }

    public Context enterChildScope(Node child, Context c) {
        if (child == body && anonType != null && body != null) {
            c = c.pushClass(anonType, anonType.asType());
        }
        return super.enterChildScope(child, c);
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == qualifier) {
            StructType t = constructorInstance().container();
                     
            if (t.isClass() && t.toClass().isMember()) {
                t = t.toClass().container();
                return t;
            }

            return child.type();
        }

        Iterator<Expr> i = this.arguments.iterator();
        Iterator<Type> j = constructorInstance().formalTypes().iterator();

        while (i.hasNext() && j.hasNext()) {
	    Expr e = i.next();
	    Type t = j.next();

            if (e == child) {
                return t;
            }
        }

        return child.type();
    }

    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
	// something didn't work in the type check phase, so just ignore it.
	if (getCi() == null) {
	    throw new InternalCompilerError(position(),
		"Null constructor instance after type check.");
	}

	for (Iterator i = constructorInstance().throwTypes().iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    ec.throwsException(t, position());
	}

	return super.exceptionCheck(ec);
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
        return Precedence.LITERAL;
    }

    public String toString() {
	return (qualifier != null ? (qualifier.toString() + ".") : "") +
            "new " + tn + "(...)" + (body != null ? " " + body : "");
    }

    protected void printQualifier(CodeWriter w, PrettyPrinter tr) {
        if (qualifier != null) {
            print(qualifier, w, tr);
            w.write(".");
        }
    }

    protected void printArgs(CodeWriter w, PrettyPrinter tr) {
	w.write("(");
	w.allowBreak(2, 2, "", 0);
	w.begin(0);

	for (Iterator<Expr> i = arguments.iterator(); i.hasNext();) {
	    Expr e = i.next();

	    print(e, w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();
	w.write(")");
    }

    protected void printBody(CodeWriter w, PrettyPrinter tr) {
	if (body != null) {
	    w.write(" {");
	    print(body, w, tr);
            w.write("}");
	}
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printQualifier(w, tr);
	w.write("new ");
       
	// We need to be careful when pretty printing "new" expressions for
        // member classes.  For the expression "e.new C()" where "e" has
        // static type "T", the TypeNode for "C" is actually the type "T.C".
        // But, if we print "T.C", the post compiler will try to lookup "T"
        // in "T".  Instead, we print just "C".
        if (qualifier != null) {
            w.write(tn.nameString());
        }
        else {
            print(tn, w, tr);
        }
        
        printArgs(w, tr);
        printBody(w, tr);
    }
    
    public Term firstChild() {
        return qualifier != null ? (Term) qualifier : tn;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        if (qualifier != null) {
            v.visitCFG(qualifier, tn, ENTRY);
        }
        
        if (body() != null) {
            v.visitCFG(tn, listChild(arguments, body()), ENTRY);
            v.visitCFGList(arguments, body(), ENTRY);
            v.visitCFG(body(), this, EXIT);
        } else {
            if (!arguments.isEmpty()) {
                v.visitCFG(tn, listChild(arguments, null), ENTRY);
                v.visitCFGList(arguments, this, EXIT);
            } else {
                v.visitCFG(tn, this, EXIT);
            }
        }

        return succs;
    }

    public List<Type> throwTypes(TypeSystem ts) {
      List<Type> l = new ArrayList<Type>();
      l.addAll(constructorInstance().throwTypes());
      l.addAll(ts.uncheckedExceptions());
      return l;
    }

    public void setCi(Ref<ConstructorInstance> ci) {
	this.ci = ci;
    }

    public Ref<ConstructorInstance> getCi() {
	return ci;
    }

}
