package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.types.*;
import jltools.util.*;
import jltools.visit.*;
import jltools.frontend.*;
import java.util.*;

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
    protected List arguments;
    protected ClassBody body;
    protected ConstructorInstance ci;
    protected ParsedAnonClassType anonType;

    public New_c(Ext ext, Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body) {
	super(ext, pos);
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

    public ParsedAnonClassType anonType() {
	return this.anonType;
    }

    public New anonType(ParsedAnonClassType anonType) {
	New_c n = (New_c) copy();
	n.anonType = anonType;
	return n;
    }

    public ConstructorInstance constructorInstance() {
	return this.ci;
    }

    public New constructorInstance(ConstructorInstance ci) {
	New_c n = (New_c) copy();
	n.ci = ci;
	return n;
    }

    public List arguments() {
	return this.arguments;
    }

    public New arguments(List arguments) {
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
    protected New_c reconstruct(Expr qualifier, TypeNode tn, List arguments, ClassBody body) {
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

    /** Visit the children of the expression, except the body. */
    public Node visitChildren(NodeVisitor v) {
	Expr qualifier = (Expr) visitChild(this.qualifier, v);
	TypeNode tn = (TypeNode) visitChild(this.tn, v);
	List arguments = visitList(this.arguments, v);
	ClassBody body = (ClassBody) visitChild(this.body, v);
	return reconstruct(qualifier, tn, arguments, body);
    }

    public void enterScope(Context c) {
        if (anonType != null) {
            c.pushClass(anonType);
        }
    }

    public void leaveScope(Context c) {
        if (anonType != null) {
            c.popClass();
        }
    }

    public Node buildTypesEnter_(TypeBuilder tb) throws SemanticException {
        if (body != null) {
            tb.pushAnonClass(position());
        }

        return this;
    }

    public Node buildTypes_(TypeBuilder tb) throws SemanticException {
        New_c n = this;

        if (body != null) {
            ParsedAnonClassType type = (ParsedAnonClassType) tb.currentClass();
            n = (New_c) anonType(type);
            tb.popClass();
        }

        TypeSystem ts = tb.typeSystem();

        List l = new ArrayList(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            l.add(ts.unknownType(position()));
        }

        ConstructorInstance ci = ts.constructorInstance(position(), ts.Object(),
                                                        Flags.NONE, l,
                                                        Collections.EMPTY_LIST);
        n = (New_c) n.constructorInstance(ci);

        return n.type(ts.unknownType(position()));
    }

    public Node disambiguateEnter_(AmbiguityRemover ar)
        throws SemanticException
    {
        New n = this;

        // We can't disambiguate the type node if we have a qualifier.  The
        // type node represents an inner class of the qualifier, and we don't
        // know which outer class to look in until the qualifier is type
        // checked.
        if (n.qualifier() != null) {
            n = n.objectType((TypeNode) n.objectType().bypass(true));
        }

        if (n.body() != null) {
            n = n.body((ClassBody) n.body().bypass(true));
        }


        return n;
    }

    public Node disambiguate_(AmbiguityRemover ar) throws SemanticException {
        if (ar.kind() != AmbiguityRemover.ALL) {
            return this;
        }

        if (qualifier == null) {
            ClassType ct = tn.type().toClass();

            if (! ct.isMember() || ct.flags().isStatic()) {
                return this;
            }

            // If we're instantiating a non-static member class, add a "this"
            // qualifier.
            NodeFactory nf = ar.nodeFactory();
            TypeSystem ts = ar.typeSystem();
            Context c = ar.context();

            // Search for the outer class of the member.
            Type outer = null;

            String name = ct.toMember().name();
            ClassType t = c.currentClass();

            // We're in one scope too many.
            if (t == anonType) {
                t = t.toAnonymous().outer();
            }

            while (t != null) {
                try {
                    ClassType mt = ts.findMemberClass(t, name, c);

                    if (mt.isSame(ct)) {
                        outer = t;
                        break;
                    }
                }
                catch (SemanticException e) {
                }

                t = t.isMember() ? t.toMember().outer() : null;
            }

            if (outer == null) {
                throw new SemanticException("Could not find non-static member class \"" +
                                            name + "\".", position());
            }

            // Create the qualifier.
            Expr q;

            if (outer == c.currentClass()) {
                q = nf.This(position());
            }
            else {
                q = nf.This(position(),
                            nf.CanonicalTypeNode(position(),
                                                 ts.staticTarget(outer)));
            }

            return qualifier(q);
        }

        return this;
    }

    public Node typeCheckEnter_(TypeChecker tc) throws SemanticException {
        New n = this;

        if (n.qualifier() != null) {
            n = n.objectType((TypeNode) n.objectType().bypass(true));
        }

        if (n.body() != null) {
            n = n.body((ClassBody) n.body().bypass(true));
        }

        return n;
    }

    public Node typeCheck_(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        Context c = tc.context();

        New_c n = this;

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
            TypeNode tn = disambiguateTypeNode(tc, qt.toClass());
            ClassType ct = tn.type().toClass();

            /*
FIXME: check super types as well.
            if (! ct.isMember() || ! ts.isEnclosed(ct, qt.toClass())) {
                throw new SemanticException("Class \"" + qt +
                    "\" does not enclose \"" + ct + "\".",
                    qualifier.position());
            }
            */

            // ?: Is this really true?
            // MRC: yes, according to JLS2 15.9.1, the class type being
            //          instantiated must be inner, and static classes
            //      are by definition (8.1.2) not inner.
            if (ct.flags().isStatic()) {
                throw new SemanticException(
                    "Cannot specify a containing instance for static classes.",
                    qualifier.position());
            }


            n = (New_c) n.objectType(tn);
        }
        else {
            ClassType ct = tn.type().toClass();

            if (ct.isMember()) {
                for (ClassType t = ct; t.isMember(); t = t.toMember().outer()) {
                    if (! t.flags().isStatic()) {
                        throw new SemanticException(
                            "Cannot allocate non-static member class \"" +
                            t + "\".", position());
                    }
                }
            }
        }

        return n.typeCheckEpilogue(tc);
    }

    protected Node typeCheckEpilogue(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

	List argTypes = new ArrayList(arguments.size());

	for (Iterator i = this.arguments.iterator(); i.hasNext(); ) {
	    Expr e = (Expr) i.next();
	    argTypes.add(e.type());
	}

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
		    ((Expr) arguments.get(0)).position());
	    }
	}

        if (! ct.flags().isInterface()) {
            ci = ts.findConstructor(ct, argTypes, tc.context());
        }
        else {
            ci = ts.defaultConstructor(position(), ct);
        }

	New_c n = (New_c) this.constructorInstance(ci).type(ct);

	if (n.body == null) {
	    return n;
	}

	// Now, need to read symbols, clean, disambiguate, and type check
	// the body.

	if (! ct.flags().isInterface()) {
	    anonType.superType(ct);
	}
	else {
	    anonType.superType(ts.Object());
	    anonType.addInterface(ct);
	}

	// Now, run the four passes on the body.
	ClassBody body = n.typeCheckBody(tc, ct);

	return n.body(body);
    }

    protected TypeNode partialDisambTypeNode(TypeNode tn, TypeChecker tc, ClassType outer) throws SemanticException
    {
        // We have to disambiguate the type node as if it were a member of the
        // outer class.  For Java this is simple: outer is just a name and we
        // use that name to lookup a member of the outer class.  For some
        // extensions (e.g., PolyJ), the type node may be more complex than
        // just a name.  We'll just punt here and let the extensions handle
        // this complexity.

        if (tn instanceof CanonicalTypeNode) {
            return tn;
        }

        String name = null;

        if (tn instanceof AmbTypeNode && ((AmbTypeNode) tn).qual() == null) {
            name = ((AmbTypeNode) tn).name();
        }
        else {
            throw new SemanticException(
                "Cannot instantiate an member class.",
                tn.position());
        }

        TypeSystem ts = tc.typeSystem();
        NodeFactory nf = tc.nodeFactory();
        Context c = tc.context();

        MemberClassType ct = ts.findMemberClass(outer, name, c);
        return nf.CanonicalTypeNode(tn.position(), ct);
    }

    protected TypeNode disambiguateTypeNode(TypeChecker tc, ClassType ct)
        throws SemanticException
    {
        TypeNode tn = this.partialDisambTypeNode(this.tn, tc, ct);

        if (tn instanceof CanonicalTypeNode) {
            return tn;
        }

        // Run the disambiguation passes on the node.
        tn = (TypeNode) tc.job().spawn(tc.context(), tn,
                                       Pass.CLEAN_SUPER, Pass.DISAM_ALL);

        if (tn == null) {
            throw new SemanticException("Could not disambiguate type.",
                                        this.tn.position());
        }

        // Now, type-check the body.
        return (TypeNode) visitChild(tn, tc);
    }

    protected ClassBody typeCheckBody(TypeChecker tc, ClassType ct)
        throws SemanticException
    {
        ClassBody b = (ClassBody) tc.job().spawn(tc.context(), body,
                                                 Pass.CLEAN_SUPER,
                                                 Pass.DISAM_ALL);

        if (b == null) {
            throw new SemanticException("Could not disambiguate body of " +
                                        "anonymous " +
                                        (ct.flags().isInterface() ?
                                         "implementor" : "subclass") +
                                        " of \"" + ct + "\".");
        }

        // Now, type-check the body.
        b = (ClassBody) visitChild(b, tc);

        return b;
    }

    public Expr setExpectedType_(Expr child, ExpectedTypeVisitor tc)
      	throws SemanticException
    {
        if (child == qualifier) {
            ReferenceType t = ci.container();
                     
            if (t.isClass() && t.toClass().isMember()) {
                t = t.toClass().toMember().container();
                return child.expectedType(t);
            }

            return child;
        }

        Iterator i = this.arguments.iterator();
        Iterator j = ci.argumentTypes().iterator();

        while (i.hasNext() && j.hasNext()) {
	    Expr e = (Expr) i.next();
	    Type t = (Type) j.next();

            if (e == child) {
                return child.expectedType(t);
            }
        }

        return child;
    }

    public Node exceptionCheck_(ExceptionChecker ec) throws SemanticException {
	// something didn't work in the type check phase, so just ignore it.
	if (ci == null) {
	    throw new InternalCompilerError(position(),
		"Null constructor instance after type check.");
	}

	for (Iterator i = ci.exceptionTypes().iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();
	    ec.throwsException(t);
	}

	return this;
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
        return Precedence.LITERAL;
    }

    public String toString() {
	return (qualifier != null ? (qualifier.toString() + ".") : "") +
            "new " + tn + "(...)" + (body != null ? " " + body : "");
    }

    /** Write the expression to an output file. */
    public void translate_(CodeWriter w, Translator tr) {
        if (qualifier != null) {
            qualifier.translate(w, tr);
            w.write(".");
        }

	w.write("new ");

        if (qualifier != null) {
            ClassType ct = tn.type().toClass();

            if (! ct.isMember()) {
                throw new InternalCompilerError("Cannot qualify a non-member " +
                                                "class.", position());
            }

            tr.setOuterClass(ct.toMember().outer());
            tn.translate(w, tr);
            tr.setOuterClass(null);
        }
        else {
            tn.translate(w, tr);
        }

	w.write("(");
	w.begin(0);

	for (Iterator i = arguments.iterator(); i.hasNext();) {
	    Expr e = (Expr) i.next();

	    e.translate(w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();
	w.write(")");

	if (body != null) {
	    w.write(" ");
	    body.translate(w, tr);
	}
    }
}