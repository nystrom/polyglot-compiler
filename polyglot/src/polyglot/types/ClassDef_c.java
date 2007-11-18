/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.io.*;
import java.util.*;

import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.util.*;

/**
 * ParsedClassType
 *
 * Overview: 
 * A ParsedClassType represents a information that has been parsed (but not
 * necessarily type checked) from a .java file.
 **/
public class ClassDef_c extends Def_c implements ClassDef
{
    protected transient LazyClassInitializer init;
    protected transient Source fromSource;

    protected Ref<? extends Type> superType;
    protected List<Ref<? extends Type>> interfaces;
    protected List<MethodDef> methods;
    protected List<FieldDef> fields;
    protected List<ConstructorDef> constructors;
    protected Ref<? extends Package> package_;
    protected Flags flags;
    protected Kind kind;
    protected String name;
    protected Ref<ClassDef> outer;

    // ### When writing this out, convert back to uncached refs; can follow the pointers to reload the member types.
    // ### Maybe, in general, convert all refs to uncached.
    protected transient List<Ref<? extends Type>> memberClasses;

    public ClassType asType() {
        return new ParsedClassType_c(this);
    }
    
    public LazyClassInitializer init() {
        return init;
    }

    public void setInit(LazyClassInitializer init) {
        this.init = init;
    }
    
    /** Was the class declared in a static context? */
    protected boolean inStaticContext = false;
    
    /** Whether we need to serialize this class. */
    protected boolean needSerialization = true;

    public boolean isMember() {
        return kind == MEMBER;
    }

    public boolean isTopLevel() {
        return kind == TOP_LEVEL;
    }

    public boolean isLocal() {
        return kind == LOCAL;
    }

    public boolean isAnonymous() {
        return kind == ANONYMOUS;
    }

    public boolean isNested() {
        return !isTopLevel();
    }

    public boolean isInnerClass() {
        return !flags().isInterface() && isNested() && !flags().isStatic()
                && !inStaticContext();
    }
    
    protected ClassDef_c() {
	super();
    }

    public ClassDef_c(TypeSystem ts, LazyClassInitializer init, 
                             Source fromSource) {
        super(ts);
        this.fromSource = fromSource;

        setInitializer(init);
        
        this.interfaces = new LinkedList();
        this.methods = new LinkedList();
        this.fields = new LinkedList();
        this.constructors = new LinkedList();
        this.memberClasses = new LinkedList();
    }
     
    public LazyInitializer initializer() {
        return this.init;
    }

    public void setInitializer(LazyInitializer init) {
        this.init = (LazyClassInitializer) init;
        ((LazyClassInitializer) init).setClass(this);
    }
        
    public Source fromSource() {
        return fromSource;
    }
    
    Job job;
    
    /** @deprecated */
    public Job job() {
        return job;
    }
    
    public void setJob(Job job) {
        this.job = job;
    }
    
    public Kind kind() {
        return kind;
    }


    public void inStaticContext(boolean inStaticContext) {
        this.inStaticContext = inStaticContext;
    }

    public boolean inStaticContext() {
        return inStaticContext;
    }

    public Ref<? extends ClassDef> outer() {
        if (kind() == TOP_LEVEL)
            return null;
        if (outer == null)
            throw new InternalCompilerError("Nested classes must have outer classes.");
        return outer;
    }

    public String name() {
        return name;
    }
    
    public void setContainer(Ref<? extends ReferenceType> container) {
        throw new InternalCompilerError("Call outer(container.def()) instead.");
    }

    /** Get the class's super type. */
    public Ref<? extends Type> superType() {
        return this.superType;
    }

    /** Get the class's package. */
    public Ref<? extends Package> package_() {
        return package_;
    }

    /** Get the class's flags. */
    public Flags flags() {
        if (kind() == ANONYMOUS)
            return Flags.NONE;
        return flags;
    }
    
    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public void flags(Flags flags) {
	this.flags = flags;
    }

    public void kind(Kind kind) {
        this.kind = kind;
    }

    public void outer(Ref<ClassDef> outer) {
        if (kind() == TOP_LEVEL)
            throw new InternalCompilerError("Top-level classes cannot have outer classes.");
        this.outer = outer;
    }
    
    public Ref<? extends ReferenceType> container() {
        return Ref_c.<ClassType>ref(new ParsedClassType_c(ts, position(), this.outer));
    }
    
    public void name(String name) {
        if (kind() == ANONYMOUS)
            throw new InternalCompilerError("Anonymous classes cannot have names.");
        this.name = name;
    }

    public void position(Position pos) {
	this.position = pos;
    }

    public void package_(Ref<? extends Package> p) {
	this.package_ = p;
    }

    public void superType(Ref<? extends Type> t) {
	this.superType = t;
    }

    public void addInterface(Ref<? extends Type> t) {
	interfaces.add(t);
    }

    public void addMethod(MethodDef mi) {
	methods.add(mi);
    }

    public void addConstructor(ConstructorDef ci) {
	constructors.add(ci);
    }

    public void addField(FieldDef fi) {
	fields.add(fi);
    }

    public void addMemberClass(Ref<? extends Type> t) {
	memberClasses.add(t);
    }
    

    public void setConstructors(List<? extends ConstructorDef> l) {
        this.constructors = new ArrayList(l);
    }

    public void setFields(List<? extends FieldDef> l) {
        this.fields = new ArrayList(l);
    }

    public void setInterfaces(List<Ref<? extends Type>> l) {
        this.interfaces = new ArrayList(l);
    }

    public void setMemberClasses(List<Ref<? extends Type>> l) {
        this.memberClasses = new ArrayList(l);
    }

    public void setMethods(List<? extends MethodDef> l) {
        this.methods = new ArrayList(l);
    }
    
                                          
    public boolean defaultConstructorNeeded() {
        init.initConstructors();
        if (flags().isInterface()) {
            return false;
        }
        return this.constructors.isEmpty();
    }
    
    /** Return an immutable list of constructors */
    public List constructors() {
        init.initConstructors();
        return Collections.unmodifiableList(constructors);
    }

    /** Return an immutable list of member classes */
    public List memberClasses() {
        init.initMemberClasses();
        return Collections.unmodifiableList(memberClasses);
    }

    /** Return an immutable list of methods. */
    public List methods() {
        init.initMethods();
        return Collections.unmodifiableList(methods);
    }

    /** Return an immutable list of fields */
    public List fields() {
        init.initFields();
        return Collections.unmodifiableList(fields);
    }
    
    /** Return an immutable list of interfaces */
    public List interfaces() {
        init.initInterfaces();
        return Collections.unmodifiableList(interfaces);
    }
    
    protected boolean membersAdded;
    protected boolean supertypesResolved;
    protected boolean signaturesResolved;

    /**
     * @return Returns the membersAdded.
     */
    public boolean membersAdded() {
        return membersAdded;
    }
    /**
     * @param membersAdded The membersAdded to set.
     */
    public void setMembersAdded(boolean membersAdded) {
        this.membersAdded = membersAdded;
    }
    /**
     * @param signaturesDisambiguated The signaturesDisambiguated to set.
     */
    public void setSignaturesResolved(boolean signaturesDisambiguated) {
        this.signaturesResolved = signaturesDisambiguated;
    }
    /**
     * @return Returns the supertypesResolved.
     */
    public boolean supertypesResolved() {
        return supertypesResolved;
    }
    /**
     * @param supertypesResolved The supertypesResolved to set.
     */
    public void setSupertypesResolved(boolean supertypesResolved) {
        this.supertypesResolved = supertypesResolved;
    }

    public boolean signaturesResolved() {        
        return signaturesResolved;
    }

    public String toString() {
        if (kind() == null) {
            return "<unknown class " + name + ">";
        }
        if (kind() == ANONYMOUS) {
            if (interfaces != null && ! interfaces.isEmpty()) {
                return "<anonymous subtype of " + interfaces.get(0) + ">";
            }
            if (superType != null) {
                return "<anonymous subclass of " + superType + ">";
            }
        }
        String name = name();
        if (kind() == TOP_LEVEL && package_() != null) {
            return get(package_()).fullName() + "." + name;
        }
        else if (kind() == MEMBER && get(outer()) instanceof Named) {
            return ((Named) get(outer())).fullName() + "." + name;
        }
        else {
            return name;
        }
    }

    /**
     * When serailizing, write out the place holder as well as the object itself.
     * This should be done in TypeOutputStream, not here, but I couldn't get it working.
     * --Nate
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        Object o = ts.placeHolder(this);
        if (o instanceof PlaceHolder && o != this) {
            out.writeBoolean(true);
            out.writeObject(o);
        }
        else {
            out.writeBoolean(false);
        }
        out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (in instanceof TypeInputStream) {
            TypeInputStream tin = (TypeInputStream) in;

            boolean b = tin.readBoolean();
            
            if (b) {
                tin.enableReplace(false);
                PlaceHolder p = (PlaceHolder) tin.readObject();
                tin.installInPlaceHolderCache(p, this);
                tin.enableReplace(true);
            }

            fromSource = null;
           
            init = tin.getTypeSystem().deserializedClassInitializer();
            init.setClass(this);
            
            membersAdded = true;
            supertypesResolved = true;
            signaturesResolved = true;
            memberClasses = new ArrayList();
        }

        in.defaultReadObject();
    }

    public void needSerialization(boolean b) {
        needSerialization = b;
    }
    
    public boolean needSerialization() {
        return needSerialization;
    }

    /** Get the full name of the class, if possible. */
    public String fullName() {
        if (kind() == ANONYMOUS) {
            return toString();
        }
        String name = name();
        if (kind() == TOP_LEVEL && package_() != null) {
            return get(package_()).fullName() + "." + name;
        }
        else if (kind() == MEMBER && get(outer()) instanceof Named) {
            return ((Named) get(outer())).fullName() + "." + name;
        }
        else {
            return name;
        }
    }
}
