package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.types.*;
import jltools.util.*;
import jltools.visit.*;
import java.util.*;

/**
 * A <code>ClassDecl</code> is the definition of a class, abstract class,
 * or interface. It may be a public or other top-level class, or an inner
 * named class, or an anonymous class.
 */
public class ClassDecl_c extends Node_c implements ClassDecl
{
	protected Flags flags;
	protected String name;
	protected TypeNode superClass;
	protected List interfaces;
	protected ClassBody body;

	protected ParsedClassType type;

	public ClassDecl_c(Ext ext, Position pos, Flags flags, String name, TypeNode superClass, List interfaces, ClassBody body) {
		super(ext, pos);
		this.flags = flags;
		this.name = name;
		this.superClass = superClass;
		this.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
		this.body = body;
	}

	public ParsedClassType type() {
		return type;
	}

	public Flags flags() {
		return this.flags;
	}

	public ClassDecl flags(Flags flags) {
		ClassDecl_c n = (ClassDecl_c) copy();
		n.flags = flags;
		return n;
	}

	public String name() {
		return this.name;
	}

	public ClassDecl name(String name) {
		ClassDecl_c n = (ClassDecl_c) copy();
		n.name = name;
		return n;
	}

	public TypeNode superClass() {
		return this.superClass;
	}

	public ClassDecl superClass(TypeNode superClass) {
		ClassDecl_c n = (ClassDecl_c) copy();
		n.superClass = superClass;
		return n;
	}

	public List interfaces() {
		return this.interfaces;
	}

	public ClassDecl interfaces(List interfaces) {
		ClassDecl_c n = (ClassDecl_c) copy();
		n.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
		return n;
	}

	public ClassBody body() {
		return this.body;
	}

	public ClassDecl body(ClassBody body) {
		ClassDecl_c n = (ClassDecl_c) copy();
		n.body = body;
		return n;
	}

	protected ClassDecl_c reconstruct(TypeNode superClass, List interfaces, ClassBody body) {
		if (superClass != this.superClass || ! CollectionUtil.equals(interfaces, this.interfaces) || body != this.body) {
			ClassDecl_c n = (ClassDecl_c) copy();
			n.superClass = superClass;
			n.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
			n.body = body;
			return n;
		}

		return this;
	}

	public Node visitChildren(NodeVisitor v) {
		TypeNode superClass = null;

		if (this.superClass != null) {
			superClass = (TypeNode) this.superClass.visit(v);
		}

		List interfaces = new ArrayList(this.interfaces.size());
		for (Iterator i = this.interfaces.iterator(); i.hasNext(); ) {
			TypeNode n = (TypeNode) i.next();
			n = (TypeNode) n.visit(v);
			interfaces.add(n);
		}

		ClassBody body = (ClassBody) this.body.visit(v);

		return reconstruct(superClass, interfaces, body);
	}

	public Node buildTypesOverride_(TypeBuilder tb) throws SemanticException {
		TypeSystem ts = tb.typeSystem();

		ParsedClassType type = tb.pushClass(position(), flags, name);

		// Set the super type to unknown and don't include any interfaces.
		// We'll figure it out on the next pass.
		if (superClass != null) {
			type.superType(ts.unknownType(superClass.position()));
		}
		else {
			type.superType(ts.Object());
		}

		for (Iterator i = interfaces.iterator(); i.hasNext(); ) {
			TypeNode tn = (TypeNode) i.next();
			type.addInterface(ts.unknownType(tn.position()));
		}

		// Plonk in the type before recursing to add members.
		// FIXME: This should be done non-destructively.
		this.type = type;

		ClassDecl n = (ClassDecl) this.visitChildren(tb);

        n = addDefaultConstructorIfNeeded(tb, n);

		tb.popClass();

		return n;
	}

    /**
     * Add a default constructor to n, if it is needed.
     */
    protected ClassDecl addDefaultConstructorIfNeeded(TypeBuilder tb, ClassDecl n) {
        if (defaultConstructorNeeded(tb, n)) {
            return addDefaultConstructor(tb, n);
		} else {
            return n;
        }
    }

    /**
     * Return whether n needs a default constructor
     * added to it.
     */
    protected boolean defaultConstructorNeeded(TypeBuilder tb, ClassDecl n) {
        if (n.flags().isInterface()) {
            return false;
        }
        return n.type().constructors().isEmpty();
    }

    /**
     * Add a default constructor to n.  The base
     * JL implementation only adds a constructor to the
     * type representing the class, not the AST itself.
     */
    protected ClassDecl addDefaultConstructor(TypeBuilder tb, ClassDecl n) {
		n.type().addConstructor(
			tb.typeSystem().constructorInstance(position(), n.type(),
			Flags.PUBLIC, Collections.EMPTY_LIST, Collections.EMPTY_LIST));
        return n;
    }

	public void enterScope(Context c) {
		c.pushClass(type);
	}

	public void leaveScope(Context c) {
		c.popClass();
	}

	public Node disambiguateTypesOverride_(TypeAmbiguityRemover sc) throws SemanticException {
		TypeSystem ts = sc.typeSystem();
		Context c = sc.context();
		boolean repeat = false;

		Types.report(2, "Cleaning " + type + ".");

		enterScope(sc.context());

		// Visit the super class and interfaces.  Reconstruct the type,
		// then visit the body.
		TypeNode superClass = null;

		if (this.superClass != null) {
			superClass = (TypeNode) this.superClass.visit(sc);

			Type t = superClass.type();

			if (! t.isCanonical()) {
				throw new SemanticException("Could not disambiguate super " +
					"class of " + type + ".", superClass.position());
			}

			if (! t.isClass() || t.toClass().flags().isInterface()) {
				throw new SemanticException("Super class " + t + " of " +
					type + " is not a class.", superClass.position());
			}

			this.type.superType(t);

			ts.checkCycles(t.toReference());

			if (t instanceof ParsedType) {
				ParsedType pt = (ParsedType) t;

				if (! pt.isClean()) {
					Types.report(2, "Delaying until " + pt + " is clean.");
					sc.job().disambTypesPass().runAfter(pt.job().disambTypesPass());
					repeat = true;
				}
			}
		}

		this.type.interfaces().clear();

		List interfaces = new ArrayList(this.interfaces.size());

		for (Iterator i = this.interfaces.iterator(); i.hasNext(); ) {
			TypeNode tn = (TypeNode) i.next();
			tn = (TypeNode) tn.visit(sc);
			interfaces.add(tn);

			Type t = tn.type();

			if (! t.isCanonical()) {
				throw new SemanticException("Could not disambiguate super " +
					"class of " + type + ".", tn.position());
			}

			if (! t.isClass() || ! t.toClass().flags().isInterface()) {
				throw new SemanticException("Interface " + t + " of " +
					type + " is not an interface.", tn.position());
			}

			this.type.addInterface(t);

			ts.checkCycles(t.toReference());

			if (t instanceof ParsedType) {
				ParsedType pt = (ParsedType) t;

				if (! pt.isClean()) {
					Types.report(2, "Delaying until " + pt + " is clean.");
					sc.job().disambTypesPass().runAfter(pt.job().disambTypesPass());
					repeat = true;
				}
			}
		}

		if (! repeat) {
			type.setClean(true);

			// Visit children.
			Node n = this.visitChildren(sc);
			n.leaveScope(sc.context());

			return n;
		}

		leaveScope(sc.context());

		return this;
	}

	protected void duplicateFieldCheck(TypeChecker tc) throws SemanticException {
	    TypeSystem ts = tc.typeSystem();

	    ArrayList l = new ArrayList(type.fields());

	    for (int i = 0; i < l.size(); i++) {
		FieldInstance fi = (FieldInstance) l.get(i);

	      	for (int j = i+1; j < l.size(); j++) {
		    FieldInstance fj = (FieldInstance) l.get(j);

		    if (fi.name().equals(fj.name())) {
			throw new SemanticException("Duplicate field \"" + fj + "\".", fj.position());
		    }
		}
	    }
	}

	protected void duplicateConstructorCheck(TypeChecker tc) throws SemanticException {
	    TypeSystem ts = tc.typeSystem();

	    ArrayList l = new ArrayList(type.constructors());

	    for (int i = 0; i < l.size(); i++) {
		ConstructorInstance ci = (ConstructorInstance) l.get(i);

	      	for (int j = i+1; j < l.size(); j++) {
		    ConstructorInstance cj = (ConstructorInstance) l.get(j);

		    if (hasSameArguments(ts, ci, cj)) {
			throw new SemanticException("Duplicate constructor \"" + cj.signature() + "\".", cj.position());
		    }
		}
	    }
	}

	protected void duplicateMethodCheck(TypeChecker tc) throws SemanticException {
	    TypeSystem ts = tc.typeSystem();

	    ArrayList l = new ArrayList(type.methods());

	    for (int i = 0; i < l.size(); i++) {
		MethodInstance mi = (MethodInstance) l.get(i);

	      	for (int j = i+1; j < l.size(); j++) {
		    MethodInstance mj = (MethodInstance) l.get(j);

		    if (mi.name().equals(mj.name()) &&
		        hasSameArguments(ts, mi, mj)) {

			throw new SemanticException("Duplicate method \"" + mj.signature() + "\".", mj.position());
		    }
		}
	    }
	}

	protected void overrideMethodCheck(TypeChecker tc) throws SemanticException {
	    TypeSystem ts = tc.typeSystem();

	    for (Iterator i = type.methods().iterator(); i.hasNext(); ) {
		MethodInstance mi = (MethodInstance) i.next();

		Type t = type.superType();

		while (t instanceof ReferenceType) {
		    ReferenceType rt = (ReferenceType) t;
		    t = rt.superType();

		    for (Iterator j = rt.methods().iterator(); j.hasNext(); ) {
			MethodInstance mj = (MethodInstance) j.next();

			if (! mi.name().equals(mj.name()) ||
			    ! hasSameArguments(ts, mi, mj) ||
			    ! ts.isAccessible(mj, tc.context())) {

			  continue;
			}

			if (! ts.isSame(mi.returnType(), mj.returnType())) {
			    throw new SemanticException("Cannot override method \"" + mj.signature() + "\" with different return type.", mi.position());
			}

			int ai = 2;
			if (mi.flags().isPrivate()) ai = 0;
			if (mi.flags().isProtected()) ai = 1;
			if (mi.flags().isPublic()) ai = 3;

			int aj = 2;
			if (mj.flags().isPrivate()) aj = 0;
			if (mj.flags().isProtected()) aj = 1;
			if (mj.flags().isPublic()) aj = 3;

			if (ai < aj) {
			    throw new SemanticException("Cannot override method \"" + mi.signature() + "\" with more restrictive access flags.", mi.position());
			}

			if (! mi.flags().isStatic() && mj.flags().isStatic()) {
			    throw new SemanticException("Cannot override static method with instance method \"" + mi + "\".", mi.position());
			}

			if (mj.flags().isFinal()) {
			    throw new SemanticException("Cannot override final method \"" + mi.signature() + "\".", mi.position());
			}
		    }
		}
	    }
	}

    /**
     * Return whether p1 and p2 have arguments that are all of the same
     * type in the context of overriding/overloading.
     */
    protected boolean hasSameArguments(TypeSystem ts, ProcedureInstance p1, ProcedureInstance p2) {
        return ts.hasSameArguments(p1, p2);
    }

	public Node typeCheck_(TypeChecker tc) throws SemanticException {
	    duplicateFieldCheck(tc);
	    duplicateConstructorCheck(tc);
	    duplicateMethodCheck(tc);
	    overrideMethodCheck(tc);

	    return this;
	}

	public String toString() {
		return flags.clearInterface().translate() +
			   (flags.isInterface() ? "interface " : "class ") + name + " " + body;
	}

	public void translate_(CodeWriter w, Translator tr) {
		enterScope(tr.context());

		w.write(flags.clearInterface().translate());

		if (flags.isInterface()) {
			w.write("interface ");
		}
		else {
			w.write("class ");
		}

		w.write(name);

		if (superClass() != null) {
			w.write(" extends ");
			superClass().ext().translate(w, tr);
		}

		if (! interfaces.isEmpty()) {
			if (flags.isInterface()) {
				w.write(" extends ");
			}
			else {
				w.write(" implements ");
			}

			for (Iterator i = interfaces().iterator(); i.hasNext(); ) {   
				TypeNode tn = (TypeNode) i.next();
				tn.ext().translate(w, tr);

				if (i.hasNext()) {
					w.write (", ");
				}
			}
		}

		w.write(" ");
		body().ext().translate(w, tr);

		leaveScope(tr.context());
	}

	/*
	public Node reconstructTypes_(NodeFactory nf, TypeSystem ts, Context c) {
	// Nothing to do here.  The type is reconstructed by the members.
	return this;
	}
	*/

	public void dump(CodeWriter w) {
		super.dump(w);

		w.allowBreak(4, " ");
		w.begin(0);
		w.write("(name " + name + ")");
		w.end();

		if (type != null) {
			w.allowBreak(4, " ");
			w.begin(0);
			w.write("(type " + type + ")");
			w.end();
		}
	}
}