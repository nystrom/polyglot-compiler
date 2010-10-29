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
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/**
 * A <code>ClassDecl</code> is the definition of a class, abstract class,
 * or interface. It may be a public or other top-level class, or an inner
 * named class, or an anonymous class.
 */
public class ClassDecl_c extends Term_c implements ClassDecl
{
    protected FlagsNode flags;
    protected Id name;
    protected TypeNode superClass;
    protected List<TypeNode> interfaces;
    protected ClassBody body;
    protected ConstructorDef defaultCI;

    protected ClassDef type;

    public ClassDecl_c(Position pos, FlagsNode flags, Id name,
            TypeNode superClass, List interfaces, ClassBody body) {
        super(pos);
        // superClass may be null, interfaces may be empty
        assert(flags != null && name != null && interfaces != null && body != null); 
        this.flags = flags;
        this.name = name;
        this.superClass = superClass;
        this.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
        this.body = body;
    }
    
    public List<Def> defs() {
        return Collections.<Def>singletonList(type);
    }

    public MemberDef memberDef() {
        return type;
    }

    public ClassDef classDef() {
        return type;
    }

    public ClassDecl classDef(ClassDef type) {
        if (type == this.type) return this;
        ClassDecl_c n = (ClassDecl_c) copy();
        n.type = type;
        return n;
    }

    public FlagsNode flags() {
        return this.flags;
    }

    public ClassDecl flags(FlagsNode flags) {
        ClassDecl_c n = (ClassDecl_c) copy();
        n.flags = flags;
        return n;
    }

    public Id name() {
        return this.name;
    }

    public ClassDecl name(Id name) {
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

    public List<TypeNode> interfaces() {
        return this.interfaces;
    }

    public ClassDecl interfaces(List<TypeNode> interfaces) {
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

    protected ClassDecl_c reconstruct(FlagsNode flags, Id name, TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        if (flags != this.flags || name != this.name || superClass != this.superClass || ! CollectionUtil.allEqual(interfaces, this.interfaces) || body != this.body) {
            ClassDecl_c n = (ClassDecl_c) copy();
            n.flags = flags;
            n.name = name;
            n.superClass = superClass;
            n.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
            n.body = body;
            return n;
        }

        return this;
    }
    
    public Node visitSignature(NodeVisitor v) {
	FlagsNode flags = (FlagsNode) visitChild(this.flags, v);
        Id name = (Id) visitChild(this.name, v);
        TypeNode superClass = (TypeNode) visitChild(this.superClass, v);
        List<TypeNode> interfaces = visitList(this.interfaces, v);
        ClassBody body = this.body;
        return reconstruct(flags, name, superClass, interfaces, body);
    }

    public Node visitChildren(NodeVisitor v) {
        ClassDecl_c n = (ClassDecl_c) visitSignature(v);
        ClassBody body = (ClassBody) n.visitChild(n.body, v);
        return body == n.body ? n : n.body(body);
    }

    public Context enterChildScope(Node child, Context c) {
        if (child == this.body) {
            TypeSystem ts = c.typeSystem();
            c = c.pushClass(type, type.asType());
        }
        else if (child == this.superClass || this.interfaces.contains(child)) {
            // Add this class to the context, but don't push a class scope.
            // This allows us to detect loops in the inheritance
            // hierarchy, but avoids an infinite loop.
            c = c.pushBlock();
            c.addNamed(this.type.asType());
        }
        return super.enterChildScope(child, c);
    }

    public String toString() {
	Flags flags = this.flags.flags();
        return flags.clearInterface().translate() +
        (flags.isInterface() ? "interface " : "class ") + name + " " + body;
    }

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
