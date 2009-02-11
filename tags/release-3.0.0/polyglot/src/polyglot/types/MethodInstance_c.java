package polyglot.types;

import java.util.*;

import polyglot.main.Report;
import polyglot.util.Position;

public class MethodInstance_c extends FunctionInstance_c<MethodDef> implements MethodInstance {

    public MethodInstance_c(TypeSystem ts, Position pos, Ref<? extends MethodDef> def) {
        super(ts, pos, def);
    }
    
    protected Name name;
    protected Flags flags;
    protected StructType container;
    
    public MethodInstance container(StructType container) {
        MethodInstance_c p = (MethodInstance_c) copy();
        p.container = container;
        return p;
    }

    public StructType container() {
        if (this.container == null) {
            return Types.get(def().container());
        }
        return this.container;
    }
    
    public MethodInstance flags(Flags flags) {
        MethodInstance_c p = (MethodInstance_c) copy();
        p.flags = flags;
        return p;
    }
    
    public Flags flags() {
        if (this.flags == null) { 
            return def().flags();
        }
        return this.flags;
    }
    
    public MethodInstance name(Name name) {
        MethodInstance_c p = (MethodInstance_c) copy();
        p.name = name;
        return p;
    }

    public Name name() {
        if (this.name == null) { 
            return def().name();
        }
        return this.name;
    }
    
    public MethodInstance returnType(Type returnType) {
        return (MethodInstance) super.returnType(returnType);
    }
    public MethodInstance returnTypeRef(Ref<? extends Type> returnType) {
	return (MethodInstance) super.returnTypeRef(returnType);
    }

    public MethodInstance formalTypes(List<Type> formalTypes) {
        return (MethodInstance) super.formalTypes(formalTypes);
    }
    
    public MethodInstance throwTypes(List<Type> throwTypes) {
        return (MethodInstance) super.throwTypes(throwTypes);
    }
    
    /** Returns true iff <this> is the same method as <m> */
    public boolean isSameMethod(MethodInstance m) {
        return this.name().equals(m.name()) && hasFormals(m.formalTypes());
    }

    public List<MethodInstance> overrides() {
        List<MethodInstance> l = new ArrayList<MethodInstance>();
        StructType rt = container();

        while (rt != null) {
            // add any method with the same name and formalTypes from rt
            l.addAll(rt.methods(name(), formalTypes()));

            StructType sup = null;
            
            if (rt instanceof ObjectType) {
		ObjectType ot = (ObjectType) rt;
		if (ot.superClass() instanceof StructType) {
		    sup = (StructType) ot.superClass();
		}
	    }
            
            rt = sup;
        };

        return l;
    }
    
    @Override
    public String signature() {
	return name + super.signature();
    }

    /**
     * Leave this method in for historic reasons, to make sure that extensions
     * modify their code correctly.
     */
    public boolean canOverride(MethodInstance mj) {
        try {
            checkOverride(mj);
            return true;
        }
        catch (SemanticException e) {
            return false;
        }
    }

    public void checkOverride(MethodInstance mj) throws SemanticException {
	MethodInstance mi = this;

	// HACK: Java5 allows return types to be covariant.  We'll allow covariant
	// return if we mj is defined in a class file.
	boolean allowCovariantReturn = false;

	if (mj.container() instanceof ClassType) {
	    ClassType ct = (ClassType) mj.container();
	    if (ct.def().fromJavaClassFile()) {
		allowCovariantReturn = true;
	    }
	}

	checkOverride(mj, allowCovariantReturn);
    }

    protected void checkOverride(MethodInstance mj, boolean allowCovariantReturn) throws SemanticException {
        MethodInstance mi = this;
        
        if (mi == mj)
            return;

        if (!(mi.name().equals(mj.name()) && mi.hasFormals(mj.formalTypes()))) {
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; incompatible " +
                                        "parameter types",
                                        mi.position());
        }
        
        if (allowCovariantReturn
                ? ! ts.isSubtype(mi.returnType(), mj.returnType())
                : ! ts.typeEquals(mi.returnType(), mj.returnType())) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, "return type " + mi.returnType() +
                              " != " + mj.returnType());
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; attempting to use incompatible " +
                                        "return type\n" +                                        
                                        "found: " + mi.returnType() + "\n" +
                                        "required: " + mj.returnType(), 
                                        mi.position());
        } 

        if (! ts.throwsSubset(mi, mj)) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.throwTypes() + " not subset of " +
                              mj.throwTypes());
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; the throw set " + mi.throwTypes() + " is not a subset of the " +
                                        "overridden method's throw set " + mj.throwTypes() + ".", 
                                        mi.position());
        }   

        if (mi.flags().moreRestrictiveThan(mj.flags())) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.flags() + " more restrictive than " +
                              mj.flags());
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; attempting to assign weaker " + 
                                        "access privileges", 
                                        mi.position());
        }

        if (mi.flags().isStatic() != mj.flags().isStatic()) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, mi.signature() + " is " + 
                              (mi.flags().isStatic() ? "" : "not") + 
                              " static but " + mj.signature() + " is " +
                              (mj.flags().isStatic() ? "" : "not") + " static");
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is " + 
                                        (mj.flags().isStatic() ? "" : "not") +
                                        "static", 
                                        mi.position());
        }

        if (! mi.def().equals(mj.def()) && mj.flags().isFinal()) {
            // mi can "override" a final method mj if mi and mj are the same method instance.
            if (Report.should_report(Report.types, 3))
                Report.report(3, mj.flags() + " final");
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is final", 
                                        mi.position());
        }
    }
    
    public List<MethodInstance> implemented() {
        return implemented(container());
    }

    public List<MethodInstance> implemented(StructType st) {
	    if (st == null) {
		    return Collections.<MethodInstance>emptyList();
	    }
	    
	    List<MethodInstance> l = new LinkedList<MethodInstance>();
	    l.addAll(st.methods(name(), formalTypes()));

	    if (st instanceof ObjectType) {
		ObjectType rt = (ObjectType) st;
		
		Type superType = rt.superClass();
		
		if (superType instanceof StructType) {
		    l.addAll(implemented((StructType) superType)); 
		}

		List<Type> ints = rt.interfaces();
		for (Type t : ints) {
		    if (t instanceof StructType) {
			StructType rt2 = (StructType) t;
			l.addAll(implemented(rt2));
		    }
		}
	    }

	    return l;
    }
}
