/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.Context;
import polyglot.types.ErrorRef_c;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.InitializerDef;
import polyglot.types.MemberDef;
import polyglot.types.ParsedClassType_c;
import polyglot.types.Ref_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.VarDef;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/**
 * A <code>FieldDecl</code> is an immutable representation of the declaration
 * of a field of a class.
 */
public class FieldDecl_c extends Term_c implements FieldDecl {
    protected Flags flags;
    protected TypeNode type;
    protected Id name;
    protected Expr init;
    protected FieldDef fi;
    protected InitializerDef ii;

    public FieldDecl_c(Position pos, Flags flags, TypeNode type,
                       Id name, Expr init)
    {
        super(pos);
        assert(flags != null && type != null && name != null); // init may be null
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.init = init;
    }

    public MemberDef memberDef() {
        return fi;
    }

    public VarDef varDef() {
        return fi;
    }
    
    public CodeDef codeDef() {
        return ii;
    }

    /** Get the initializer instance of the initializer. */
    public InitializerDef initializerDef() {
        return ii;
    }

    /** Set the initializer instance of the initializer. */
    public FieldDecl initializerDef(InitializerDef ii) {
        if (ii == this.ii) return this;
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
        if (flags.equals(this.flags)) return this;
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
    public Id id() {
        return name;
    }
    
    /** Set the name of the declaration. */
    public FieldDecl id(Id name) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.name = name;
        return n;
    }

    /** Get the name of the declaration. */
    public String name() {
        return name.id();
    }

    /** Set the name of the declaration. */
    public FieldDecl name(String name) {
        return id(this.name.id(name));
    }
    
    public Term codeBody() {
        return init;
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
    public FieldDecl fieldDef(FieldDef fi) {
        if (fi == this.fi) return this;
        FieldDecl_c n = (FieldDecl_c) copy();
        n.fi = fi;
        return n;
    }

    /** Get the field instance of the declaration. */
    public FieldDef fieldDef() {
        return fi;
    }

    /** Reconstruct the declaration. */
    protected FieldDecl_c reconstruct(TypeNode type, Id name, Expr init) {
        if (this.type != type || this.name != name || this.init != init) {
            FieldDecl_c n = (FieldDecl_c) copy();
            n.type = type;
            n.name = name;
            n.init = init;
            return n;
        }

        return this;
    }

    /** Visit the children of the declaration. */
    public Node visitChildren(NodeVisitor v) {
        TypeNode type = (TypeNode) visitChild(this.type, v);
        Id name = (Id) visitChild(this.name, v);
        Expr init = (Expr) visitChild(this.init, v);
        return reconstruct(type, name, init);
    }

    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
        TypeSystem ts = tb.typeSystem();

        ClassDef cd = tb.currentClass();
        ClassType ct = cd.asType();

        Flags flags = this.flags;

        if (cd.flags().isInterface()) {
            flags = flags.Public().Static().Final();
        }
        
        if (init != null) {
            Flags iflags = flags.isStatic() ? Flags.STATIC : Flags.NONE;
            InitializerDef ii = ts.initializerInstance(init.position(),
                                                            Ref_c.<ClassType>ref(ct), iflags);
            tb = tb.pushCode(ii, Globals.Scheduler().TypeCheckDef(tb.job(), ii));
        }

        FieldDef fi = ts.fieldInstance(position(), Ref_c.<ClassType>ref(ct), flags, type.typeRef(), name.id());
        ts.symbolTable().<FieldDef>symbol(fi);
        
        cd.addField(fi);

        Goal g = Globals.Scheduler().TypeCheckDef(tb.job(), fi);
        g.addPrereq(Globals.Scheduler().SupertypeDef(tb.job(), cd));
        return tb.pushDef(fi).pushGoal(g);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        FieldDecl n = this;
        

        InitializerDef ii = null;
        if (init != null) {
            for (TypeBuilder tb2 = tb; tb2 != null; tb2 = tb2.pop()) {
                if (tb2.def() instanceof InitializerDef) {
                    ii = (InitializerDef) tb2.def();                   
                    break;
                }
            }
            n = n.initializerDef(ii);
        }

        FieldDef fi = null;
        for (TypeBuilder tb2 = tb; tb2 != null; tb2 = tb2.pop()) {
            if (tb2.def() instanceof FieldDef) {
                fi = (FieldDef) tb2.def();                   
                break;
            }
        }

        return n.flags(fi.flags()).fieldDef(fi);
    }

    public Context enterScope(Context c) {
        if (ii != null) {
            return c.pushCode(ii);
        }
        return c;
    }
   
    public Node checkConstants(TypeChecker tc) throws SemanticException {
        if (init == null || ! init.isConstant() || ! fi.flags().isFinal()) {
            fi.setNotConstant();
        }
        else {
            fi.setConstantValue(init.constantValue());
        }

        return this;
    }
    
    public boolean constantValueSet() {
        return fi != null && fi.constantValueSet();
    }

    /** Type check the declaration. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        // Get the fi flags, not the node flags since the fi flags
        // account for being nested within an interface.
        Flags flags = fi.flags();
        
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
                if (! ts.isImplicitCastValid(init.type(), type.type()) &&
                    ! ts.typeEquals(init.type(), type.type()) &&
                    ! ts.numericConversionValid(type.type(),
                                                init.constantValue())) {

                    throw new SemanticException("The type of the variable " +
                                                "initializer \"" + init.type() +
                                                "\" does not match that of " +
                                                "the declaration \"" +
                                                type.type() + "\".",
                                                init.position());
                }
            }
        }

        // check that inner classes do not declare static fields, unless they
        // are compile-time constants
        if (flags().isStatic() &&
              fieldDef().container().get().toClass().isInnerClass()) {
            // it's a static field in an inner class.
            if (!flags().isFinal() || init == null || !init.isConstant()) {
                throw new SemanticException("Inner classes cannot declare " +
                        "static fields, unless they are compile-time " +
                        "constant fields.", this.position());
            }

        }

        return this;
    }

    public NodeVisitor exceptionCheckEnter(ExceptionChecker ec) throws SemanticException {
        return ec.push(new ExceptionChecker.CodeTypeReporter("field initializer"));
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == init) {
            TypeSystem ts = av.typeSystem();

            // If the RHS is an integral constant, we can relax the expected
            // type to the type of the constant.
            if (ts.numericConversionValid(type.type(), child.constantValue())) {
                return child.type();
            }
            else {
                return type.type();
            }
        }

        return child.type();
    }

    public Term firstChild() {
        return type;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        if (init != null) {
            v.visitCFG(type, init, ENTRY);
            v.visitCFG(init, this, EXIT);
        } else {
            v.visitCFG(type, this, EXIT);
        }
        
        return succs;
    }


    public String toString() {
        return flags.translate() + type + " " + name +
                (init != null ? " = " + init : "");
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        boolean isInterface = fi != null && fi.container() != null &&
                              fi.container().get().toClass().flags().isInterface();

        Flags f = flags;

        if (isInterface) {
            f = f.clearPublic();
            f = f.clearStatic();
            f = f.clearFinal();
        }

        w.write(f.translate());
        print(type, w, tr);
	w.allowBreak(2, 2, " ", 1);
        tr.print(this, name, w);

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

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(name " + name + ")");
	w.end();
    }
    public Node copy(NodeFactory nf) {
        return nf.FieldDecl(this.position, this.flags, this.type, this.name, this.init);
    }

}
