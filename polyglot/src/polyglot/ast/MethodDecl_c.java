/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A method declaration.
 */
public class MethodDecl_c extends Term_c implements MethodDecl
{
    protected Flags flags;
    protected TypeNode returnType;
    protected Id name;
    protected List formals;
    protected List throwTypes;
    protected Block body;
    protected MethodInstance mi;

    public MethodDecl_c(Position pos, Flags flags, TypeNode returnType, Id name, List formals, List throwTypes, Block body) {
	super(pos);
	assert(flags != null && returnType != null && name != null && formals != null && throwTypes != null); // body may be null
	this.flags = flags;
	this.returnType = returnType;
	this.name = name;
	this.formals = TypedList.copyAndCheck(formals, Formal.class, true);
	this.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
	this.body = body;
    }

    public boolean isDisambiguated() {
        return mi != null && mi.isCanonical() && super.isDisambiguated();
    }

    public MemberInstance memberInstance() {
        return mi;
    }

    /** Get the flags of the method. */
    public Flags flags() {
	return this.flags;
    }

    /** Set the flags of the method. */
    public MethodDecl flags(Flags flags) {
        if (flags.equals(this.flags)) return this;
	MethodDecl_c n = (MethodDecl_c) copy();
	n.flags = flags;
	return n;
    }

    /** Get the return type of the method. */
    public TypeNode returnType() {
	return this.returnType;
    }

    /** Set the return type of the method. */
    public MethodDecl returnType(TypeNode returnType) {
	MethodDecl_c n = (MethodDecl_c) copy();
	n.returnType = returnType;
	return n;
    }

    /** Get the name of the method. */
    public Id id() {
        return this.name;
    }
    
    /** Set the name of the method. */
    public MethodDecl id(Id name) {
        MethodDecl_c n = (MethodDecl_c) copy();
        n.name = name;
        return n;
    }
    
    /** Get the name of the method. */
    public String name() {
        return this.name.id();
    }

    /** Set the name of the method. */
    public MethodDecl name(String name) {
        return id(this.name.id(name));
    }

    /** Get the formals of the method. */
    public List formals() {
	return Collections.unmodifiableList(this.formals);
    }

    /** Set the formals of the method. */
    public MethodDecl formals(List formals) {
	MethodDecl_c n = (MethodDecl_c) copy();
	n.formals = TypedList.copyAndCheck(formals, Formal.class, true);
	return n;
    }

    /** Get the exception types of the method. */
    public List throwTypes() {
	return Collections.unmodifiableList(this.throwTypes);
    }

    /** Set the exception types of the method. */
    public MethodDecl throwTypes(List throwTypes) {
	MethodDecl_c n = (MethodDecl_c) copy();
	n.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
	return n;
    }

    public Term codeBody() {
        return this.body;
    }
    
    /** Get the body of the method. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the method. */
    public CodeBlock body(Block body) {
	MethodDecl_c n = (MethodDecl_c) copy();
	n.body = body;
	return n;
    }

    /** Get the method instance of the method. */
    public MethodInstance methodInstance() {
	return mi;
    }

    /** Set the method instance of the method. */
    public MethodDecl methodInstance(MethodInstance mi) {
        if (mi == this.mi) return this;
	MethodDecl_c n = (MethodDecl_c) copy();
	n.mi = mi;
	return n;
    }

    public CodeInstance codeInstance() {
	return procedureInstance();
    }

    /** Get the procedure instance of the method. */
    public ProcedureInstance procedureInstance() {
	return mi;
    }

    /** Reconstruct the method. */
    protected MethodDecl_c reconstruct(TypeNode returnType, Id name, List formals, List throwTypes, Block body) {
	if (returnType != this.returnType || name != this.name || ! CollectionUtil.equals(formals, this.formals) || ! CollectionUtil.equals(throwTypes, this.throwTypes) || body != this.body) {
	    MethodDecl_c n = (MethodDecl_c) copy();
	    n.returnType = returnType;
            n.name = name;
	    n.formals = TypedList.copyAndCheck(formals, Formal.class, true);
	    n.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the method. */
    public Node visitChildren(NodeVisitor v) {
        Id name = (Id) visitChild(this.name, v);
        List formals = visitList(this.formals, v);
	TypeNode returnType = (TypeNode) visitChild(this.returnType, v);
	List throwTypes = visitList(this.throwTypes, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(returnType, name, formals, throwTypes, body);
    }

    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        return tb.pushCode();
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();

        ParsedClassType ct = tb.currentClass();

        if (ct == null) {
            return this;
        }

        List formalTypes = new ArrayList(formals.size());
        for (int i = 0; i < formals.size(); i++) {
            formalTypes.add(ts.unknownType(position()));
        }

        List throwTypes = new ArrayList(throwTypes().size());
        for (int i = 0; i < throwTypes().size(); i++) {
            throwTypes.add(ts.unknownType(position()));
        }

	Flags f = this.flags;

	if (ct.flags().isInterface()) {
	    f = f.Public().Abstract();
	}

        MethodInstance mi = ts.methodInstance(position(), ct, f,
                                              ts.unknownType(position()),
                                              name.id(), formalTypes, throwTypes);
        ct.addMethod(mi);
        return methodInstance(mi);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (this.mi.isCanonical()) {
            // already done
            return this;
        }

        if (! returnType.isDisambiguated()) {
            return this;
        }

        mi.setReturnType(returnType.type());

        List formalTypes = new LinkedList();
        List throwTypes = new LinkedList();

        for (Iterator i = formals.iterator(); i.hasNext(); ) {
            Formal f = (Formal) i.next();
            if (! f.isDisambiguated()) {
                return this;
            }
            formalTypes.add(f.declType());
        }

        mi.setFormalTypes(formalTypes);

        for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
            TypeNode tn = (TypeNode) i.next();
            if (! tn.isDisambiguated()) {
                return this;
            }
            throwTypes.add(tn.type());
        }

        mi.setThrowTypes(throwTypes);

        return this;
    }

    public Context enterScope(Context c) {
        if (Report.should_report(TOPICS, 5))
	    Report.report(5, "enter scope of method " + name);
        c = c.pushCode(mi);
        return c;
    }

    /** Type check the method. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	TypeSystem ts = tc.typeSystem();

        // Get the mi flags, not the node flags since the mi flags
        // account for being nested within an interface.
        Flags flags = mi.flags();
        
        if (tc.context().currentClass().flags().isInterface()) {
            if (flags.isProtected() || flags.isPrivate()) {
                throw new SemanticException("Interface methods must be public.",
                                            position());
            }
        }

        try {
            ts.checkMethodFlags(flags);
        }
        catch (SemanticException e) {
            throw new SemanticException(e.getMessage(), position());
        }

	if (body == null && ! (flags.isAbstract() || flags.isNative())) {
	    throw new SemanticException("Missing method body.", position());
	}

	if (body != null && (flags.isAbstract() || flags.isNative())) {
	    throw new SemanticException(
		"An abstract method cannot have a body.", position());
	}

	if (body != null && flags.isNative()) {
	    throw new SemanticException(
		"A native method cannot have a body.", position());
	}

        for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
            TypeNode tn = (TypeNode) i.next();
            Type t = tn.type();
            if (! t.isThrowable()) {
                throw new SemanticException("Type \"" + t +
                    "\" is not a subclass of \"" + ts.Throwable() + "\".",
                    tn.position());
            }
        }

        // check that inner classes do not declare static methods
        if (flags.isStatic() &&
              methodInstance().container().toClass().isInnerClass()) {
            // it's a static method in an inner class.
            throw new SemanticException("Inner classes cannot declare " + 
                    "static methods.", this.position());             
        }

        overrideMethodCheck(tc);

	return this;
    }

    protected void overrideMethodCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        for (Iterator j = mi.implemented().iterator(); j.hasNext(); ) {
            MethodInstance mj = (MethodInstance) j.next();

            if (! ts.isAccessible(mj, tc.context())) {
                continue;
            }

            ts.checkOverride(mi, mj);
        }
    }

    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec) throws SemanticException {
        return ec.push(methodInstance().throwTypes());
    }

    public String toString() {
	return flags.translate() + returnType + " " + name + "(...)";
    }

    /** Write the method to an output file. */
    public void prettyPrintHeader(Flags flags, CodeWriter w, PrettyPrinter tr) {
	w.begin(0);
	w.write(flags.translate());
	print(returnType, w, tr);
	w.allowBreak(2, 2, " ", 1);
	w.write(name + "(");

	w.allowBreak(2, 2, "", 0);
	w.begin(0);

	for (Iterator i = formals.iterator(); i.hasNext(); ) {
	    Formal f = (Formal) i.next();
	    print(f, w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0, " ");
	    }
	}

	w.end();
	w.write(")");

	if (! throwTypes().isEmpty()) {
	    w.allowBreak(6);
	    w.write("throws ");

	    for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
	        TypeNode tn = (TypeNode) i.next();
		print(tn, w, tr);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(4, " ");
		}
	    }
	}

	w.end();
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        prettyPrintHeader(flags(), w, tr);

	if (body != null) {
	    printSubStmt(body, w, tr);
	}
	else {
	    w.write(";");
	}
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (mi != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + mi + ")");
	    w.end();
	}

        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(name " + name + ")");
        w.end();
    }

    /**
     * Return the first (sub)term performed when evaluating this
     * term.
     */
    public Term entry() {
        return listEntry(formals(), returnType.entry());
    }

    /**
     * Visit this term in evaluation order.
     */
    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFGList(formals(), returnType.entry());
        if (body() == null) {
            v.visitCFG(returnType, this);
        }
        else {
            v.visitCFG(returnType, body().entry());
            v.visitCFG(body(), this);
        }
        return succs;
    }

    private static final Collection TOPICS = 
            CollectionUtil.list(Report.types, Report.context);
    public Node copy(NodeFactory nf) {
        return nf.MethodDecl(this.position, this.flags, this.returnType, this.name, this.formals, this.throwTypes, this.body);
    }

}
