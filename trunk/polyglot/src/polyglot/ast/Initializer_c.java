/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 *
 */

package polyglot.ast;

import java.util.*;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.visit.*;

/**
 * An <code>Initializer</code> is an immutable representation of an
 * initializer block in a Java class (which appears outside of any
 * method).  Such a block is executed before the code for any of the
 * constructors.  Such a block can optionally be static, in which case
 * it is executed when the class is loaded.
 */
public class Initializer_c extends Term_c implements Initializer
{
    protected Flags flags;
    protected Block body;
    protected InitializerDef ii;

    public Initializer_c(Position pos, Flags flags, Block body) {
	super(pos);
	assert(flags != null && body != null);
	this.flags = flags;
	this.body = body;
    }
    
    public List<Def> defs() {
        return Collections.<Def>singletonList(ii);
    }

    public MemberDef memberDef() {
        return ii;
    }

    /** Get the flags of the initializer. */
    public Flags flags() {
	return this.flags;
    }

    /** Set the flags of the initializer. */
    public Initializer flags(Flags flags) {
        if (flags.equals(this.flags)) return this;
	Initializer_c n = (Initializer_c) copy();
	n.flags = flags;
	return n;
    }

    /** Get the initializer instance of the initializer. */
    public InitializerDef initializerDef() {
        return ii;
    }

    public CodeDef codeDef() {
	return initializerDef();
    }

    /** Set the initializer instance of the initializer. */
    public Initializer initializerDef(InitializerDef ii) {
        if (ii == this.ii) return this;
	Initializer_c n = (Initializer_c) copy();
	n.ii = ii;
	return n;
    }

    public Term codeBody() {
        return this.body;
    }
    
    /** Get the body of the initializer. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the initializer. */
    public CodeBlock body(Block body) {
	Initializer_c n = (Initializer_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the initializer. */
    protected Initializer_c reconstruct(Block body) {
	if (body != this.body) {
	    Initializer_c n = (Initializer_c) copy();
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the initializer. */
    public Node visitChildren(NodeVisitor v) {
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(body);
    }

    public Context enterScope(Context c) {
	return c.pushCode(ii);
    }

    /**
     * Return the first (sub)term performed when evaluating this
     * term.
     */
    public Term firstChild() {
        return body();
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(body(), this, EXIT);
        return succs;
    }

    /** Build type objects for the method. */
    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();
        ClassDef ct = tb.currentClass();
        InitializerDef ii = ts.initializerDef(position(), Types.ref(ct.asType()), flags);
        
        Goal g = Globals.Scheduler().TypeCheckDef(tb.job(), ii);
        if (false)
        g.addPrereq(Globals.Scheduler().SupertypeDef(tb.job(), ct));
        return tb.pushCode(ii, g);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        InitializerDef ii = (InitializerDef) tb.def();
        return initializerDef(ii);
    }

    /** Type check the declaration. */
    @Override
    public Node typeCheckOverride(Node parent, TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
  
        TypeChecker childtc;
        NodeVisitor childv = tc.enter(parent, this);
        if (childv instanceof TypeChecker) {
            childtc = (TypeChecker) childv;
        }
        else {
            return this;
        }

        Block body = this.body;

        switch (tc.scope()) {
        case SUPER: {
            return this;
        }
        case SIGNATURES: {
            return this;
        }
        case BODY: {
            Initializer_c n = this;
            
            switch (tc.mode(this)) {
            case NON_ROOT:
                assert false;
                break;
            case INNER_ROOT:
                break;
            case CURRENT_ROOT:
                body = (Block) this.visitChild(this.body, childtc);
                n = reconstruct(body);
                n = (Initializer_c) tc.leave(parent, this, n, childtc);
            }
            
            return n;
        }
        }
        
        return this;
    }

    /** Type check the initializer. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	TypeSystem ts = tc.typeSystem();

	try {
	    ts.checkInitializerFlags(flags());
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), position());
	}

        // check that inner classes do not declare static initializers
        if (flags().isStatic() &&
              initializerDef().container().get().toClass().isInnerClass()) {
            // it's a static initializer in an inner class.
            throw new SemanticException("Inner classes cannot declare " + 
                    "static initializers.", this.position());             
        }

	return this;
    }

    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec) throws SemanticException {
        if (initializerDef().flags().isStatic()) {
            return ec.push(new ExceptionChecker.CodeTypeReporter("static initializer block"));
        }
        
        if (!initializerDef().container().get().toClass().isAnonymous()) {
            ec = ec.push(new ExceptionChecker.CodeTypeReporter("instance initializer block"));

            // An instance initializer of a named class may not throw
            // a checked exception unless that exception or one of its 
            // superclasses is explicitly declared in the throws clause
            // of each contructor or its class, and the class has at least
            // one explicitly declared constructor.
            SubtypeSet allowed = null;
            Type throwable = ec.typeSystem().Throwable();
            ClassType container = initializerDef().container().get().toClass();
            for (Iterator<ConstructorInstance> iter = container.constructors().iterator(); iter.hasNext(); ) {
                ConstructorInstance ci = (ConstructorInstance)iter.next();
                if (allowed == null) {
                    allowed = new SubtypeSet(throwable);
                    allowed.addAll(ci.throwTypes());
                }
                else {
                    // intersect allowed with ci.throwTypes()
                    SubtypeSet other = new SubtypeSet(throwable);
                    other.addAll(ci.throwTypes());
                    SubtypeSet inter = new SubtypeSet(throwable);
                    for (Iterator<Type> i = allowed.iterator(); i.hasNext(); ) {
                        Type t = (Type)i.next();
                        if (other.contains(t)) {
                            // t or a supertype is thrown by other.
                            inter.add(t);
                        }
                    }
                    for (Iterator<Type> i = other.iterator(); i.hasNext(); ) {
                        Type t = (Type)i.next();
                        if (allowed.contains(t)) {
                            // t or a supertype is thrown by the allowed.
                            inter.add(t);
                        }
                    }
                    allowed = inter;
                }
            }
            // allowed is now an intersection of the throw types of all
            // constructors
            
            ec = ec.push(allowed);
            
            
            return ec;
        }

        return ec.push();
    }


    /** Write the initializer to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.begin(0);
	w.write(flags.translate());
	print(body, w, tr);
	w.end();
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (ii != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + ii + ")");
	    w.end();
	}
    }

    public String toString() {
	return flags.translate() + "{ ... }";
    }
    
    public Node copy(NodeFactory nf) {
        return nf.Initializer(this.position, this.flags, this.body);
    }

}
