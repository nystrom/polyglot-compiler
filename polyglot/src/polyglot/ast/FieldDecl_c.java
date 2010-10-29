/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * A <code>FieldDecl</code> is an immutable representation of the declaration
 * of a field of a class.
 */
public class FieldDecl_c extends Term_c implements FieldDecl {
    protected FlagsNode flags;
    protected TypeNode type;
    protected Id name;
    protected Expr init;
    protected FieldDef fi;
    protected InitializerDef ii;

    public FieldDecl_c(Position pos, FlagsNode flags, TypeNode type,
            Id name, Expr init)
    {
        super(pos);
        assert(flags != null && type != null && name != null); // init may be null
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.init = init;
    }

    public List<Def> defs() {
        if (init == null)
            return Collections.<Def>singletonList(fi);
        else {
            return CollectionUtil.<Def>list(fi, ii);
        }
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
    public FlagsNode flags() {
        return flags;
    }

    /** Set the flags of the declaration. */
    public FieldDecl flags(FlagsNode flags) {
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
    public Id name() {
        return name;
    }

    /** Set the name of the declaration. */
    public FieldDecl name(Id name) {
        FieldDecl_c n = (FieldDecl_c) copy();
        n.name = name;
        return n;
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
    protected FieldDecl_c reconstruct(FlagsNode flags, TypeNode type, Id name, Expr init) {
        if (this.flags != flags || this.type != type || this.name != name || this.init != init) {
            FieldDecl_c n = (FieldDecl_c) copy();
            n.flags = flags;
            n.type = type;
            n.name = name;
            n.init = init;
            return n;
        }

        return this;
    }

    /** Visit the children of the declaration. */
    public Node visitChildren(NodeVisitor v) {
        FieldDecl_c n = (FieldDecl_c) visitSignature(v);
        Expr init = (Expr) n.visitChild(n.init, v);
        return init == n.init ? n : n.init(init);
    }

    public InitializerDef createInitializerDef(TypeSystem ts, ClassDef ct, Flags iflags) {
	InitializerDef ii;
	ii = ts.initializerDef(init.position(), Types.<ClassType>ref(ct.asType()), iflags);
	return ii;
    }

    public FieldDef createFieldDef(TypeSystem ts, ClassDef ct, Flags flags) {
	FieldDef fi = ts.fieldDef(position(), Types.ref(ct.asType()), flags, type.typeRef(), name.id());
	return fi;
    }

    public Context enterScope(Context c) {
        if (ii != null) {
            return c.pushCode(ii);
        }
        return c;
    }

    public Node visitSignature(NodeVisitor v) {
	FlagsNode flags = (FlagsNode) this.visitChild(this.flags, v);
        TypeNode type = (TypeNode) this.visitChild(this.type, v);
        Id name = (Id) this.visitChild(this.name, v);
        return reconstruct(flags, type, name, this.init);
    }

    public String toString() {
        return flags.flags().translate() + type + " " + name +
        (init != null ? " = " + init : "");
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
    

}
