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
import polyglot.visit.*;

/**
 * A <code>ConstructorDecl</code> is an immutable representation of a
 * constructor declaration as part of a class body.
 */
public class ConstructorDecl_c extends Term_c implements ConstructorDecl
{
    protected FlagsNode flags;
    protected Id name;
    protected List<Formal> formals;
    protected List<TypeNode> throwTypes;
    protected Block body;
    protected ConstructorDef ci;

    public ConstructorDecl_c(Position pos, FlagsNode flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        super(pos);
        assert(flags != null && name != null && formals != null && throwTypes != null); // body may be null
        this.flags = flags;
        this.name = name;
        this.formals = TypedList.copyAndCheck(formals, Formal.class, true);
        this.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
        this.body = body;
    }

    public List<Def> defs() {
        return Collections.<Def>singletonList(ci);
    }

    public MemberDef memberDef() {
        return ci;
    }

    /** Get the flags of the constructor. */
    public FlagsNode flags() {
        return this.flags;
    }

    /** Set the flags of the constructor. */
    public ConstructorDecl flags(FlagsNode flags) {
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.flags = flags;
        return n;
    }

    /** Get the name of the constructor. */
    public Id name() {
        return this.name;
    }

    /** Set the name of the constructor. */
    public ConstructorDecl name(Id name) {
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.name = name;
        return n;
    }

    /** Get the formals of the constructor. */
    public List<Formal> formals() {
        return Collections.unmodifiableList(this.formals);
    }

    /** Set the formals of the constructor. */
    public ConstructorDecl formals(List<Formal> formals) {
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.formals = TypedList.copyAndCheck(formals, Formal.class, true);
        return n;
    }

    /** Get the throwTypes of the constructor. */
    public List<TypeNode> throwTypes() {
        return Collections.unmodifiableList(this.throwTypes);
    }

    /** Set the throwTypes of the constructor. */
    public ConstructorDecl throwTypes(List<TypeNode> throwTypes) {
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
        return n;
    }

    public Term codeBody() {
        return this.body;
    }

    /** Get the body of the constructor. */
    public Block body() {
        return this.body;
    }

    /** Set the body of the constructor. */
    public CodeBlock body(Block body) {
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.body = body;
        return n;
    }

    /** Get the constructorInstance of the constructor. */
    public ConstructorDef constructorDef() {
        return ci;
    }


    /** Get the procedureInstance of the constructor. */
    public ProcedureDef procedureInstance() {
        return ci;
    }

    public CodeDef codeDef() {
        return procedureInstance();
    }

    /** Set the constructorInstance of the constructor. */
    public ConstructorDecl constructorDef(ConstructorDef ci) {
        if (ci == this.ci) return this;
        ConstructorDecl_c n = (ConstructorDecl_c) copy();
        n.ci = ci;
        return n;
    }

    /** Reconstruct the constructor. */
    protected ConstructorDecl_c reconstruct(FlagsNode flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        if (flags != this.flags || name != this.name || ! CollectionUtil.allEqual(formals, this.formals) || ! CollectionUtil.allEqual(throwTypes, this.throwTypes) || body != this.body) {
            ConstructorDecl_c n = (ConstructorDecl_c) copy();
            n.flags = flags;
            n.name = name;
            n.formals = TypedList.copyAndCheck(formals, Formal.class, true);
            n.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
            n.body = body;
            return n;
        }

        return this;
    }

    /** Visit the children of the constructor. */
    public Node visitChildren(NodeVisitor v) {
        ConstructorDecl_c n = (ConstructorDecl_c) visitSignature(v);
        Block body = (Block) n.visitChild(n.body, v);
        return body == n.body ? n : n.body(body);
    }

    public ConstructorDef createConstructorDef(TypeSystem ts, ClassDef ct, Flags flags) {
	ConstructorDef ci = ts.constructorDef(position(), Types.ref(ct.asType()), flags,
                                              Collections.<Ref<? extends Type>>emptyList(), Collections.<Ref<? extends Type>>emptyList());
	return ci;
    }

    public Context enterScope(Context c) {
        return c.pushCode(ci);
    }

    public Node visitSignature(NodeVisitor v) {
	FlagsNode flags = (FlagsNode) this.visitChild(this.flags, v);
        Id name = (Id) this.visitChild(this.name, v);
        List<Formal> formals = this.visitList(this.formals, v);
        List<TypeNode> throwTypes = this.visitList(this.throwTypes, v);
        return reconstruct(flags, name, formals, throwTypes, this.body);
    }
    
    public String toString() {
        return flags.flags().translate() + name + "(...)";
    }

    public void dump(CodeWriter w) {
        super.dump(w);

        if (ci != null) {
            w.allowBreak(4, " ");
            w.begin(0);
            w.write("(instance " + ci + ")");
            w.end();
        }
    }
}
