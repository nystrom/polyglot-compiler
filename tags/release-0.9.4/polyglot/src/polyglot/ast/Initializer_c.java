package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.frontend.*;
import java.util.*;

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
    protected InitializerInstance ii;

    public Initializer_c(JL del, Ext ext, Position pos, Flags flags, Block body) {
	super(del, ext, pos);
	this.flags = flags;
	this.body = body;
    }

    /** Get the flags of the initializer. */
    public Flags flags() {
	return this.flags;
    }

    /** Set the flags of the initializer. */
    public Initializer flags(Flags flags) {
	Initializer_c n = (Initializer_c) copy();
	n.flags = flags;
	return n;
    }

    /** Get the initializer instance of the initializer. */
    public InitializerInstance initializerInstance() {
        return ii;
    }

    public CodeInstance codeInstance() {
	return initializerInstance();
    }

    /** Set the initializer instance of the initializer. */
    public Initializer initializerInstance(InitializerInstance ii) {
	Initializer_c n = (Initializer_c) copy();
	n.ii = ii;
	return n;
    }

    /** Get the body of the initializer. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the initializer. */
    public CodeDecl body(Block body) {
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

    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        return tb.pushCode();
    }

    /** Build type objects for the method. */
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();
        ClassType ct = tb.currentClass();
        InitializerInstance ii = ts.initializerInstance(position(), ct, flags);
        return initializerInstance(ii);
    }

    public NodeVisitor disambiguateEnter(AmbiguityRemover ar) throws SemanticException {
        // Do not visit body on the clean-super and clean-signatures passes.
        if (ar.kind() == AmbiguityRemover.SUPER ||
            ar.kind() == AmbiguityRemover.SIGNATURES) {
            return ar.bypassChildren(this);
        }
        return ar;
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

	return this;
    }

    /** Check exceptions thrown by the initializer. */
    public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
      	TypeSystem ts = ec.typeSystem();

	SubtypeSet s = (SubtypeSet) ec.throwsSet();

	for (Iterator i = s.iterator(); i.hasNext(); ) {
	    Type t = (Type) i.next();

	    if (! t.isUncheckedException()) {
		throw new SemanticException(
		    "An initializer block may not throw" +
		    " a " + t + ".", position());
	    }
	}

	return this;
    }

    /** Write the initializer to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write(flags.translate());
	printBlock(body, w, tr);
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
}
