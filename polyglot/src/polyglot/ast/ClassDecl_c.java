/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.*;

/**
 * A <code>ClassDecl</code> is the definition of a class, abstract class,
 * or interface. It may be a public or other top-level class, or an inner
 * named class, or an anonymous class.
 */
public class ClassDecl_c extends Term_c implements ClassDecl
{
    protected Flags flags;
    protected Id name;
    protected TypeNode superClass;
    protected List<TypeNode> interfaces;
    protected ClassBody body;
    protected ConstructorDef defaultCI;

    protected ClassDef type;

    public ClassDecl_c(Position pos, Flags flags, Id name,
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

    public Flags flags() {
        return this.flags;
    }

    public ClassDecl flags(Flags flags) {
        if (flags.equals(this.flags)) return this;
        ClassDecl_c n = (ClassDecl_c) copy();
        n.flags = flags;
        return n;
    }

    public Id id() {
        return this.name;
    }

    public ClassDecl id(Id name) {
        ClassDecl_c n = (ClassDecl_c) copy();
        n.name = name;
        return n;
    }

    public String name() {
        return this.name.id();
    }

    public ClassDecl name(String name) {
        return id(this.name.id(name));
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

    protected ClassDecl_c reconstruct(Id name, TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        if (name != this.name || superClass != this.superClass || ! CollectionUtil.equals(interfaces, this.interfaces) || body != this.body) {
            ClassDecl_c n = (ClassDecl_c) copy();
            n.name = name;
            n.superClass = superClass;
            n.interfaces = TypedList.copyAndCheck(interfaces, TypeNode.class, true);
            n.body = body;
            return n;
        }

        return this;
    }

    /**
     * Return the first (sub)term performed when evaluating this
     * term.
     */
    public Term firstChild() {
        return body();
    }

    /**
     * Visit this term in evaluation order.
     */
    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(this.body(), this, EXIT);
        return succs;
    }

    public Node visitChildren(NodeVisitor v) {
        Id name = (Id) visitChild(this.name, v);
        TypeNode superClass = (TypeNode) visitChild(this.superClass, v);
        List<TypeNode> interfaces = visitList(this.interfaces, v);
        ClassBody body = (ClassBody) visitChild(this.body, v);
        return reconstruct(name, superClass, interfaces, body);
    }

    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
        tb = tb.pushClass(position(), flags, name.id());

        ClassDef type = tb.currentClass();

        // Member classes of interfaces are implicitly public and static.
        if (type.isMember() && type.outer().get().flags().isInterface()) {
            type.flags(type.flags().Public().Static());
        }

        // Member interfaces are implicitly static. 
        if (type.isMember() && type.flags().isInterface()) {
            type.flags(type.flags().Static());
        }

        // Interfaces are implicitly abstract. 
        if (type.flags().isInterface()) {
            type.flags(type.flags().Abstract());
        }

        SymbolTable st = tb.typeSystem().symbolTable();
        Symbol<ClassDef> sym = st.<ClassDef>symbol(type);
        
        Goal tSup = Globals.Scheduler().SupertypeDef(tb.job(), type);
        Goal tSig = Globals.Scheduler().SignatureDef(tb.job(), type);
        Goal tChk = Globals.Scheduler().TypeCheckDef(tb.job(), type);

        TypeBuilder tbSup = tb.pushGoal(tSup);
        TypeBuilder tbSig = tb.pushGoal(tSig);
        TypeBuilder tbChk = tb.pushGoal(tChk);
        
        ClassDef outer = tb.pop().currentClass();
        if (outer != null) {
            Goal oSup = Globals.Scheduler().SupertypeDef(tb.job(), outer);
            Goal oSig = Globals.Scheduler().SignatureDef(tb.job(), outer);
            Goal oChk = Globals.Scheduler().TypeCheckDef(tb.job(), outer);
            
            tSup.addPrereq(oSup);
            tSig.addPrereq(oSig);
            
            if (tb.def() instanceof CodeDef) {
                // In a local class.  We will not visit the body when type-checking the enclosing method.
                // Add a dependency on the enclosing method.
                tSup.addPrereq(oChk);
            }
        }

        tSig.addPrereq(tSup);
        tChk.addPrereq(tSig);

        ClassDecl_c n = this;
        Id name = (Id) n.visitChild(n.name, tb);

        TypeNode superClass = (TypeNode) n.visitChild(n.superClass, tbSup);
        List<TypeNode> interfaces = n.visitList(n.interfaces, tbSup);

        n = n.reconstruct(name, superClass, interfaces, n.body);
        
        n.setSuperClass(tb.typeSystem(), type);
        n.setInterfaces(tb.typeSystem(), type);

        ClassBody body = (ClassBody) n.visitChild(n.body, tbChk);
        
        n = (ClassDecl_c) n.body(body);
        
        n = (ClassDecl_c) n.classDef(type).flags(type.flags());

        if (n.defaultConstructorNeeded()) {
            ConstructorDecl cd = n.createDefaultConstructor(type, tb.typeSystem(), tb.nodeFactory());
            cd = (ConstructorDecl) tbChk.visitEdge(this, cd);
            n = (ClassDecl_c) n.body(n.body().addMember(cd));
            n.defaultCI = cd.constructorDef();
        }

        return n;
    }

    public Context enterChildScope(Node child, Context c) {
        if (child == this.body) {
            TypeSystem ts = c.typeSystem();
            c = c.pushClass(type, type.asType());
        }
        else {
            // Add this class to the context, but don't push a class scope.
            // This allows us to detect loops in the inheritance
            // hierarchy, but avoids an infinite loop.
            c = c.pushBlock();
            c.addNamed(this.type.asType());
        }
        return super.enterChildScope(child, c);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (type == null) {
            throw new InternalCompilerError("Missing type.", position());
        }

        checkSupertypeCycles(ar.typeSystem());

        ClassDef type = classDef();

        // Make sure that the inStaticContext flag of the class is correct.
        Context ctxt = ar.context();
        type.inStaticContext(ctxt.inStaticContext());

        return this;
    }

    protected void checkSupertypeCycles(TypeSystem ts) throws SemanticException {
        Ref<? extends Type> stref = type.superType();
        if (stref != null) {
            Type t = stref.get();
            assert ! (t instanceof UnknownType);
            if (! t.isClass() || t.toClass().flags().isInterface()) {
                throw new SemanticException("Cannot extend type " +
                        t + "; not a class.",
                        superClass != null ? superClass.position() : position());
            }
            ts.checkCycles((ReferenceType) t);
        }

        for (Iterator<Ref<? extends Type>> i = type.interfaces().iterator(); i.hasNext(); ) {
            Ref<? extends Type> tref = i.next();
            Type t = tref.get();
            assert ! (t instanceof UnknownType);
            if (! t.isClass() || ! t.toClass().flags().isInterface()) {
                String s = type.flags().isInterface() ? "extend" : "implement";
                throw new SemanticException("Cannot " + s + " type " + t + "; not an interface.",
                        position());
            }
            ts.checkCycles((ReferenceType) t);
        }
    }

    protected void setSuperClass(TypeSystem ts, ClassDef thisType) throws SemanticException {
        TypeNode superClass = this.superClass;

        if (superClass != null) {
            Ref<? extends Type> t = superClass.typeRef();
            if (Report.should_report(Report.types, 3))
                Report.report(3, "setting superclass of " + this.type + " to " + t);
            thisType.superType(t);
        }
        else if (thisType.asType().typeEquals(ts.Object()) || thisType.fullName().equals(ts.Object().fullName())) {
            // the type is the same as ts.Object(), so it has no superclass.
            if (Report.should_report(Report.types, 3))
                Report.report(3, "setting superclass of " + thisType + " to " + null);
            thisType.superType(null);
        }
        else {
            // the superclass was not specified, and the type is not the same
            // as ts.Object() (which is typically java.lang.Object)
            // As such, the default superclass is ts.Object().
            if (Report.should_report(Report.types, 3))
                Report.report(3, "setting superclass of " + this.type + " to " + ts.Object());
            thisType.superType(Ref_c.<Type>ref(ts.Object()));
        }
    }

    protected void setInterfaces(TypeSystem ts, ClassDef thisType) throws SemanticException {
        List<TypeNode> interfaces = this.interfaces;
        for (Iterator<TypeNode> i = interfaces.iterator(); i.hasNext(); ) {
            TypeNode tn = (TypeNode) i.next();
            Ref<? extends Type> t = tn.typeRef();

            if (Report.should_report(Report.types, 3))
                Report.report(3, "adding interface of " + thisType + " to " + t);

            thisType.addInterface(t);
        }
    }

    protected boolean defaultConstructorNeeded() {
        if (flags.isInterface()) {
            return false;
        }
        for (Iterator<ClassMember> i = body().members().iterator(); i.hasNext(); ) {
            ClassMember cm = (ClassMember) i.next();
            if (cm instanceof ConstructorDecl) {
                return false;
            }
        }

        return true;
    }

    protected ConstructorDecl createDefaultConstructor(ClassDef thisType, TypeSystem ts, NodeFactory nf)
    throws SemanticException
    {
        Block block = null;

        Ref<? extends Type> superType = thisType.superType();

        if (superType != null) {
            ConstructorCall cc = nf.SuperCall(position().startOf(), Collections.EMPTY_LIST);
            block = nf.Block(position().startOf(), cc);
        }
        else {
            block = nf.Block(position().startOf());
        }

        ConstructorDecl cd = nf.ConstructorDecl(body().position().startOf(),
                Flags.PUBLIC,
                name, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                block);
        return cd;
    }

    @Override
    public Node typeCheckOverride(Node parent, TypeChecker tc) throws SemanticException {
        ClassDecl_c n = this;
        
        if (! tc.isCurrentFragmentRoot(this)) {
            return n;
        }
        
        NodeVisitor v = tc.enter(parent, n);
        if (v instanceof TypeChecker) {
            tc = (TypeChecker) v;
        }
        else if (v instanceof PruningVisitor) {
            return this;
        }
        else {
            assert false;
        }
        
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n);
        
        AmbiguityRemover ar = new AmbiguityRemover(tc.job(), tc.typeSystem(), tc.nodeFactory());
        ar = (AmbiguityRemover) ar.context(tc.context());
        
        Id name = (Id) visitChild(n.name, tc);
        
        // Disambiguate the super types.
        TypeNode superClass = n.superClass;
        List<TypeNode> interfaces = n.interfaces;
        
        if (tc.shouldVisitSupers()) {
            superClass = (TypeNode) n.visitChild(n.superClass, tc);
            interfaces = n.visitList(n.interfaces, tc);
        }


        ClassBody body = n.body;
        if (tc.shouldVisitSupers()) {
            body = (ClassBody) n.visitChild(body, tc.visitSupers());
            n = (ClassDecl_c) n.body(body);
        }
        if (tc.shouldVisitSignatures()) {
            body = (ClassBody) n.visitChild(body, tc.visitSignatures());
            n = (ClassDecl_c) n.body(body);
        }
        if (tc.shouldVisitBodies()) {
            body = (ClassBody) n.visitChild(body, tc.visitBodies());
            n = (ClassDecl_c) n.body(body);
        }    
        
        n = n.reconstruct(name, superClass, interfaces, body);

        if (tc.shouldVisitSupers()) {
            n = (ClassDecl_c) n.del().disambiguate(ar);
        }

        if (tc.shouldVisitBodies()) {
            n = (ClassDecl_c) n.del().typeCheck(tc);
            n = (ClassDecl_c) n.del().checkConstants(tc);
        }
        return n;
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        ClassType type = this.type.asType();
        String name = this.name.id();

        // The class cannot have the same simple name as any enclosing class.
        if (type.isNested()) {
            ClassType container = type.outer();

            while (container != null) {
                if (!container.isAnonymous()) {
                    String cname = container.name();

                    if (cname.equals(name)) {
                        throw new SemanticException("Cannot declare member " +
                                "class \"" + type.fullName() +
                                "\" inside class with the " +
                                "same name.", position());
                    }
                }
                if (container.isNested()) {
                    container = container.outer();
                }
                else {
                    break;
                }
            }
        }

        // A local class name cannot be redeclared within the same
        // method, constructor or initializer, and within its scope                
        if (type.isLocal()) {
            Context ctxt = tc.context();

            if (ctxt.isLocal(name)) {
                // Something with the same name was declared locally.
                // (but not in an enclosing class)                                    
                Named nm = ctxt.find(name);
                if (nm instanceof Type) {
                    Type another = (Type) nm;
                    if (another.isClass() && another.toClass().isLocal()) {
                        throw new SemanticException("Cannot declare local " +
                                "class \"" + this.type + "\" within the same " +
                                "method, constructor or initializer as another " +
                                "local class of the same name.", position());
                    }
                }
            }                
        }

        // check that inner classes do not declare member interfaces
        if (type.isMember() && flags().isInterface() &&
                type.outer().isInnerClass()) {
            // it's a member interface in an inner class.
            throw new SemanticException("Inner classes cannot declare " + 
                    "member interfaces.", this.position());             
        }

        // Make sure that static members are not declared inside inner classes
        if (type.isMember() && type.flags().isStatic() 
                && type.outer().isInnerClass()) {
            throw new SemanticException("Inner classes cannot declare static " 
                    + "member classes.", position());
        }

        if (type.superType() != null) {
            if (! type.superType().isClass() || type.superType().toClass().flags().isInterface()) {
                throw new SemanticException("Cannot extend non-class \"" +
                        type.superType() + "\".",
                        position());
            }

            if (type.superType().toClass().flags().isFinal()) {
                throw new SemanticException("Cannot extend final class \"" +
                        type.superType() + "\".",
                        position());
            }

            if (type.typeEquals(ts.Object())) {
                throw new SemanticException("Class \"" + this.type + "\" cannot have a superclass.",
                        superClass.position());
            }
        }

        for (Iterator<TypeNode> i = interfaces.iterator(); i.hasNext(); ) {
            TypeNode tn = (TypeNode) i.next();
            Type t = tn.type();

            if (! t.isClass() || ! t.toClass().flags().isInterface()) {
                throw new SemanticException("Superinterface " + t + " of " +
                        type + " is not an interface.", tn.position());
            }

            if (type.typeEquals(ts.Object())) {
                throw new SemanticException("Class " + this.type + " cannot have a superinterface.",
                        tn.position());
            }
        }

        try {
            if (type.isTopLevel()) {
                ts.checkTopLevelClassFlags(type.flags());
            }
            if (type.isMember()) {
                ts.checkMemberClassFlags(type.flags());
            }
            if (type.isLocal()) {
                ts.checkLocalClassFlags(type.flags());
            }
        }
        catch (SemanticException e) {
            throw new SemanticException(e.getMessage(), position());
        }

        // Check the class implements all abstract methods that it needs to.
        ts.checkClassConformance(type);

        return this;
    }

    public String toString() {
        return flags.clearInterface().translate() +
        (flags.isInterface() ? "interface " : "class ") + name + " " + body;
    }

    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        w.begin(0);
        if (flags.isInterface()) {
            w.write(flags.clearInterface().clearAbstract().translate());
        }
        else {
            w.write(flags.translate());
        }

        if (flags.isInterface()) {
            w.write("interface ");
        }
        else {
            w.write("class ");
        }

        tr.print(this, name, w);

        if (superClass() != null) {
            w.allowBreak(0);
            w.write("extends ");
            print(superClass(), w, tr);
        }

        if (! interfaces.isEmpty()) {
            w.allowBreak(2);
            if (flags.isInterface()) {
                w.write("extends ");
            }
            else {
                w.write("implements ");
            }

            w.begin(0);
            for (Iterator i = interfaces().iterator(); i.hasNext(); ) {
                TypeNode tn = (TypeNode) i.next();
                print(tn, w, tr);

                if (i.hasNext()) {
                    w.write(",");
                    w.allowBreak(0);
                }
            }
            w.end();
        }
        w.unifiedBreak(0);
        w.end();
        w.write("{");
    }

    public void prettyPrintFooter(CodeWriter w, PrettyPrinter tr) {
        w.write("}");
        w.newline(0);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        prettyPrintHeader(w, tr);
        print(body(), w, tr);
        prettyPrintFooter(w, tr);
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

    public Node copy(NodeFactory nf) {
        return nf.ClassDecl(this.position, this.flags, this.name, this.superClass, this.interfaces, this.body);
    }

}
