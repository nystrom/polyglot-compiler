package polyglot.ext.jl.ast;

import polyglot.ast.*;

import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import polyglot.frontend.*;
import polyglot.main.Report;
import java.util.*;

/**
 * A <code>FieldDecl</code> is an immutable representation of the declaration
 * of a field of a class.
 */
public class FieldDecl_c extends Node_c implements FieldDecl {
    Flags flags;
    TypeNode type;
    String name;
    Expr init;
    FieldInstance fi;
    InitializerInstance ii;

    public FieldDecl_c(JL del, Ext ext, Position pos, Flags flags, TypeNode type,
                       String name, Expr init)
    {
        super(del, ext, pos);
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.init = init;
    }

    /** Get the initializer instance of the initializer. */
    public InitializerInstance initializerInstance() {
        return ii;
    }

    /** Set the initializer instance of the initializer. */
    public FieldDecl initializerInstance(InitializerInstance ii) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.ii = ii;
        return n;
    }

    /** Get the type of the declaration. */
    public Type declType() {
        return type.type();
    }

    /** Get the flags of the declaration. */
    public Flags flags() {
        return flags;
    }

    /** Set the flags of the declaration. */
    public FieldDecl flags(Flags flags) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.flags = flags;
        return n;
    }

    /** Get the type node of the declaration. */
    public TypeNode type() {
        return type;
    }

    /** Set the type of the declaration. */
    public FieldDecl type(TypeNode type) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.type = type;
        return n;
    }

    /** Get the name of the declaration. */
    public String name() {
        return name;
    }

    /** Set the name of the declaration. */
    public FieldDecl name(String name) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.name = name;
        return n;
    }

    /** Get the initializer of the declaration. */
    public Expr init() {
        return init;
    }

    /** Set the initializer of the declaration. */
    public FieldDecl init(Expr init) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.init = init;
        return n;
    }

    /** Set the field instance of the declaration. */
    public FieldDecl fieldInstance(FieldInstance fi) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.fi = fi;
        return n;
    }

    /** Get the field instance of the declaration. */
    public FieldInstance fieldInstance() {
        return fi;
    }

    /** Reconstruct the declaration. */
    protected FieldDecl_c reconstruct(TypeNode type, Expr init) {
        if (this.type != type || this.init != init) {
            FieldDecl_c n = (FieldDecl_c) copy();
            n.type = type;
            n.init = init;
            return n;
        }

        return this;
    }

    /** Visit the children of the declaration. */
    public Node visitChildren(NodeVisitor v) {
        TypeNode type = (TypeNode) visitChild(this.type, v);
        Expr init = (Expr) visitChild(this.init, v);
        return reconstruct(type, init);
    }

    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        return tb.pushCode();
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();

        FieldDecl n;

        if (init != null) {
            ClassType ct = tb.currentClass();
            Flags f = (flags.isStatic()) ? Flags.STATIC : Flags.NONE;
            InitializerInstance ii = ts.initializerInstance(init.position(),
                                                            ct, f);
            n = initializerInstance(ii);
        }
        else {
            n = this;
        }

        FieldInstance fi = ts.fieldInstance(n.position(), ts.Object(),
                                            Flags.NONE,
                                            ts.unknownType(position()),
                                            n.name());

        return n.fieldInstance(fi);
    }

    /** Build type objects for the declaration. */
    public NodeVisitor disambiguateEnter(AmbiguityRemover ar)
        throws SemanticException
    {
        if (ar.kind() == AmbiguityRemover.SUPER) {
            return ar.bypassChildren(this);
        }
        else if (ar.kind() == AmbiguityRemover.SIGNATURES) {
            if (init != null) {
                return ar.bypass(init);
            }
        }

        return ar;
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (ar.kind() == AmbiguityRemover.SIGNATURES) {
            Context c = ar.context();
            TypeSystem ts = ar.typeSystem();

            ParsedClassType ct = c.currentClassScope();

            Flags f = flags;

            if (ct.flags().isInterface()) {
                f = f.Public().Static().Final();
            }

            FieldInstance fi = ts.fieldInstance(position(), ct, f,
                                                declType(), name);

            return flags(f).fieldInstance(fi);
        }

        if (ar.kind() == AmbiguityRemover.ALL) {
            FieldInstance fi = this.fi;

            if (init != null && fi.flags().isFinal() && init.isConstant()) {
                Object value = init.constantValue();
                fi.setConstantValue(value);
            }
        }

        return this;
    }

    public NodeVisitor addMembersEnter(AddMemberVisitor am) {
        ParsedClassType ct = am.context().currentClassScope();

        FieldInstance fi = this.fi;

        if (fi == null) {
            throw new InternalCompilerError("null field instance");
        }

        if (Report.should_report(Report.types, 5))
            Report.report(5, "adding " + fi + " to " + ct);

        ct.addField(fi);

        return am.bypassChildren(this);
    }

    public Context enterScope(Context c) {
        if (ii != null) {
            return c.pushCode(ii);
        }
        return c;
    }

    /** Type check the declaration. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        try {
            ts.checkFieldFlags(flags);
        }
        catch (SemanticException e) {
            throw new SemanticException(e.getMessage(), position());
        }

        if (tc.context().currentClass().flags().isInterface()) {
            if (flags.isProtected() || flags.isPrivate()) {
                throw new SemanticException("Interface members must be public.",
                                            position());
            }
        }

        if (init != null) {
            if (init instanceof ArrayInit) {
                ((ArrayInit) init).typeCheckElements(type.type());
            }
            else {
                boolean intConversion = false;

                if (init instanceof NumLit) {
                    long value = ((NumLit) init).longValue();
                    intConversion = ts.numericConversionValid(type.type(),
                                                              value);
                }

                if (! ts.isImplicitCastValid(init.type(), type.type()) &&
                    ! ts.equals(init.type(), type.type()) && ! intConversion) {

                    throw new SemanticException("The type of the variable " +
                                                "initializer \"" + init.type() +
                                                "\" does not match that of " +
                                                "the declaration \"" +
                                                type.type() + "\".",
                                                init.position());
                }
            }
        }

        return this;
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == init) {
            return type.type();
        }

        return child.type();
    }

    public String toString() {
        return flags.translate() + type + " " + name +
                (init != null ? " = " + init : "");
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        boolean isInterface = fi != null && fi.container() != null &&
                              fi.container().toClass().flags().isInterface();

        Flags f = flags;

        if (isInterface) {
            f = f.clearPublic();
            f = f.clearStatic();
            f = f.clearFinal();
        }

        w.write(f.translate());
        print(type, w, tr);
        w.write(" ");
        w.write(name);

        if (init != null) {
            w.write(" =");
            w.allowBreak(2, " ");
            print(init, w, tr);
        }

        w.write(";");
    }

    public void dump(CodeWriter w) {
        super.dump(w);

        if (fi != null) {
            w.allowBreak(4, " ");
            w.begin(0);
            w.write("(instance " + fi + ")");
            w.end();
        }
    }
}