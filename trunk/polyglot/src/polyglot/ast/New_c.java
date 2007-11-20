/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

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
    protected ConstructorInstance ci;
    protected ClassDef anonType;

    public New_c(Position pos, Expr qualifier, TypeNode tn, List<Expr> arguments, ClassBody body) {
	super(pos);
        assert(tn != null && arguments != null); // qualifier and body may be null
        this.qualifier = qualifier;
        this.tn = tn;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	this.body = body;
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
	return this.ci;
    }

    public New constructorInstance(ConstructorInstance ci) {
        if (ci == this.ci) return this;
	New_c n = (New_c) copy();
	n.ci = ci;
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
	if (qualifier != this.qualifier || tn != this.tn || ! CollectionUtil.equals(arguments, this.arguments) || body != this.body) {
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

    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        if (body != null) {
            return tb.pushAnonClass(position());
        }
        
        return tb;
    }

    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
        New_c n = this;
        TypeSystem ts = tb.typeSystem();

        ConstructorInstance ci = new ConstructorInstance_c(ts, position(), new ErrorRef_c<ConstructorDef>(ts, position()));
        n = (New_c) n.constructorInstance(ci);
        
        Expr qual = (Expr) n.visitChild(n.qualifier(), tb);
        TypeNode objectType = (TypeNode) n.visitChild(n.objectType(), tb);
        List<Expr> arguments = (List<Expr>) n.visitList(n.arguments(), tb);
        
        ClassBody body = null;
        
        if (n.body() != null) {
            TypeBuilder tb2 = tb.pushAnonClass(position());
            ClassDef type = tb2.currentClass();

            Goal sup = Globals.Scheduler().SupertypeDef(tb.job(), type);
            Goal sig = Globals.Scheduler().SignatureDef(tb.job(), type);
            Goal chk = Globals.Scheduler().TypeCheckDef(tb.job(), type);

            // To figure out the supertype, we need to type, check the enclosing method.
            sup.addPrereq(tb.goal());
            sig.addPrereq(sup);
            chk.addPrereq(sig);
            
            n = (New_c) n.anonType(type);
            
            body = (ClassBody) n.visitChild(n.body(), tb2.pushGoal(chk));
        }
        
        n = n.reconstruct(qual, objectType, arguments, body);
        
        return n.type(ts.unknownType(position()));
    }
    
    public Node typeCheckOverride(Node parent, TypeChecker ar) throws SemanticException {
        TypeSystem ts = ar.typeSystem();
        NodeFactory nf = ar.nodeFactory();
        Context c = ar.context();
        
        NodeVisitor childv = ar.enter(parent, this);

        New nn = this;
        New old = nn;
        
        // If we're checking the body and not the enclosing method, visit the body only.
        if (nn.body() != null && anonType != null) {
            Goal sup = Globals.Scheduler().SupertypeDef(ar.job(), anonType);
            Goal sig = Globals.Scheduler().SignatureDef(ar.job(), anonType);
            Goal chk = Globals.Scheduler().TypeCheckDef(ar.job(), anonType);
            Goal g = Globals.Scheduler().currentGoal();

            if (g == sup || g == sig || g == chk) {
                if (ar.shouldVisitBodies()) {
                    // Now visit the body.
                    nn = nn.body((ClassBody) nn.visitChild(nn.body(), childv));
                    ts.checkClassConformance(anonType.asType());
                }
                
                return nn;
            }
        }

        // Disambiguate the qualifier and object type, if possible.
        if (nn.qualifier() == null) {
            nn = nn.objectType((TypeNode) nn.visitChild(nn.objectType(), childv));
            
            ClassType ct = nn.objectType().type().toClass();
            
            if (ct.isMember() && ! ct.flags().isStatic()) {
                nn = ((New_c) nn).findQualifier(ar, ct);

                nn = nn.qualifier((Expr) nn.visitChild(nn.qualifier(), childv));
            }
        }
        else {
            nn = nn.qualifier((Expr) nn.visitChild(nn.qualifier(), childv));
            
            if (nn.objectType() instanceof AmbTypeNode &&
                    ((AmbTypeNode) nn.objectType()).qual() == null) {

                // We have to disambiguate the type node as if it were a member of the
                // static type, outer, of the qualifier.  For Java this is simple: type
                // nested type is just a name and we
                // use that name to lookup a member of the outer class.  For some
                // extensions (e.g., PolyJ), the type node may be more complex than
                // just a name.  We'll just punt here and let the extensions handle
                // this complexity.
                
                String name = ((AmbTypeNode) nn.objectType()).name();

                if (! nn.qualifier().type().isClass()) {
                    throw new SemanticException("Cannot instantiate member class of non-class type.", nn.position());
                }

                ClassType outer = nn.qualifier().type().toClass();
                ClassType ct = ts.findMemberClass(outer, name, c.currentClassScope());
                TypeNode tn = nf.CanonicalTypeNode(nn.objectType().position(), ct);
                nn = nn.objectType(tn);
            }
            else {
                throw new SemanticException("Only simply-named member classes may be instantiated by a qualified new expression.",
                        nn.objectType().position());
            }
        }
        
        // Now disambiguate the actuals.
        nn = (New) nn.arguments(nn.visitList(nn.arguments(), childv));
        
        if (nn.body() != null) {
            Ref<? extends Type> ct = nn.objectType().typeRef();
            ClassDef anonType = nn.anonType();
            
            if (anonType != null) {
                if (! ct.get().toClass().flags().isInterface()) {
                    anonType.superType(ct);
                }
                else {
                    anonType.superType(Ref_c.<Type>ref(ts.Object()));
                    anonType.addInterface(ct);
                }
            }

            // Don't visit the body!  This will be done when type-checking the body.
        }
        
        nn = (New) ar.leave(parent, old, nn, childv);

        return nn;
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        // Everything is done in disambiguateOverride.
        return this;
    }
    
    /**
     * @param ar
     * @param ct
     * @throws SemanticException
     */
    protected New findQualifier(TypeChecker ar, ClassType ct) throws SemanticException {
        // If we're instantiating a non-static member class, add a "this"
        // qualifier.
        NodeFactory nf = ar.nodeFactory();
        TypeSystem ts = ar.typeSystem();
        Context c = ar.context();
        
        // Search for the outer class of the member.  The outer class is
        // not just ct.outer(); it may be a subclass of ct.outer().
        Type outer = null;
        
        String name = ct.name();
        ClassType t = c.currentClass();
        
        // We're in one scope too many.
        if (t == anonType) {
            t = t.outer();
        }
        
        // Search all enclosing classes for the type.
        while (t != null) {
            try {
                ClassType mt = ts.findMemberClass(t, name, c.currentClassScope());
                
                if (ts.typeEquals(mt, ct)) {
                    outer = t;
                    break;
                }
            }
            catch (SemanticException e) {
            }
            
            t = t.outer();
        }
        
        if (outer == null) {
            throw new SemanticException("Could not find non-static member class \"" +
                                        name + "\".", position());
        }
        
        // Create the qualifier.
        Expr q;

        if (outer.equals(c.currentClass())) {
            q = nf.This(position().startOf());
        }
        else {
            q = nf.This(position().startOf(),
                        nf.CanonicalTypeNode(position(), outer));
        }
        
        q = q.type(outer);
        return qualifier(q);
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        
        List<Type> argTypes = new ArrayList<Type>(arguments.size());
        
        for (Iterator<Expr> i = this.arguments.iterator(); i.hasNext(); ) {
            Expr e = i.next();
            argTypes.add(e.type());
        }
        
        typeCheckFlags(tc);
        typeCheckNested(tc);
        
        ClassType ct = tn.type().toClass();
        ConstructorInstance ci;
        
        if (! ct.flags().isInterface()) {
            Context c = tc.context();
            if (anonType != null) {
                c = c.pushClass(anonType, anonType.asType());
            }
            ci = ts.findConstructor(ct, argTypes, c.currentClassScope());
        }
        else {
            ConstructorDef dci = ts.defaultConstructor(this.position(), Ref_c.<ClassType>ref(ct));
            ci = dci.asReference();
        }
        
        New n = this.constructorInstance(ci);
        
        if (anonType != null) {
            // The type of the new expression is the anonymous type, not the base type.
            ct = anonType.asType();
        }

        return n.type(ct);
    }

    protected void typeCheckNested(TypeChecker tc) throws SemanticException {
        if (qualifier != null) {
            // We have not disambiguated the type node yet.

            // Get the qualifier type first.
            Type qt = qualifier.type();

            if (! qt.isClass()) {
                throw new SemanticException(
                    "Cannot instantiate member class of a non-class type.",
                    qualifier.position());
            }
            
            // Disambiguate the type node as a member of the qualifier type.
            ClassType ct = tn.type().toClass();

            // According to JLS2 15.9.1, the class type being
            // instantiated must be inner.
	    if (! ct.isInnerClass()) {
                throw new SemanticException(
                    "Cannot provide a containing instance for non-inner class " +
		    ct.fullName() + ".", qualifier.position());
            }
        }
        else {
            ClassType ct = tn.type().toClass();

            if (ct.isMember()) {
                for (ClassType t = ct; t.isMember(); t = t.outer()) {
                    if (! t.flags().isStatic()) {
                        throw new SemanticException(
                            "Cannot allocate non-static member class \"" +
                            t + "\".", position());
                    }
                }
            }
        }
    }

    protected void typeCheckFlags(TypeChecker tc) throws SemanticException {
        ClassType ct = tn.type().toClass();

	if (this.body == null) {
	    if (ct.flags().isInterface()) {
		throw new SemanticException(
		    "Cannot instantiate an interface.", position());
	    }

	    if (ct.flags().isAbstract()) {
		throw new SemanticException(
		    "Cannot instantiate an abstract class.", position());
	    }
	}
	else {
	    if (ct.flags().isFinal()) {
		throw new SemanticException(
		    "Cannot create an anonymous subclass of a final class.",
                    position());
            }

	    if (ct.flags().isInterface() && ! arguments.isEmpty()) {
	        throw new SemanticException(
		    "Cannot pass arguments to an anonymous class that " +
		    "implements an interface.",
		    arguments.get(0).position());
	    }
	}
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == qualifier) {
            ReferenceType t = ci.container();
                     
            if (t.isClass() && t.toClass().isMember()) {
                t = t.toClass().container();
                return t;
            }

            return child.type();
        }

        Iterator<Expr> i = this.arguments.iterator();
        Iterator j = ci.formalTypes().iterator();

        while (i.hasNext() && j.hasNext()) {
	    Expr e = i.next();
	    Type t = (Type) j.next();

            if (e == child) {
                return t;
            }
        }

        return child.type();
    }

    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
	// something didn't work in the type check phase, so just ignore it.
	if (ci == null) {
	    throw new InternalCompilerError(position(),
		"Null constructor instance after type check.");
	}

	for (Iterator i = ci.throwTypes().iterator(); i.hasNext(); ) {
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
            w.write(tn.name());
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
      l.addAll(ci.throwTypes());
      l.addAll(ts.uncheckedExceptions());
      return l;
    }

    public Node copy(NodeFactory nf) {
        return nf.New(this.position, this.qualifier, this.tn, this.arguments, this.body);
    }

}
