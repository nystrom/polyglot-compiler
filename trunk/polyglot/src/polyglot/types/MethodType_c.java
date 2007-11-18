package polyglot.types;

import java.util.*;

import polyglot.main.Report;
import polyglot.util.Position;

public class MethodType_c extends FunctionType_c<MethodDef> implements MethodType {

    public MethodType_c(TypeSystem ts, Position pos, Ref<MethodDef> def) {
        super(ts, pos, def);
    }
    
    public ReferenceType container() {
        return get(def().container());
    }
    
    public Flags flags() {
        return def().flags();
    }
    
    public String name() {
        return def().name();
    }
    
    /** Returns true iff <this> is the same method as <m> */
    public boolean isSameMethod(MethodType m) {
        return this.name().equals(m.name()) && hasFormals(m.formalTypes());
    }

    public boolean methodCallValid(String name, List<Type> argTypes) {
        return name().equals(name) && this.callValid(argTypes);
    }

    public List<MethodType> overrides() {
        List<MethodType> l = new LinkedList();
        ReferenceType rt = container();

        while (rt != null) {
            // add any method with the same name and formalTypes from rt
            l.addAll(rt.methods(name(), formalTypes()));

            ReferenceType sup = null;
            
            if (rt.superType() != null && rt.superType().isReference()) {
                sup = (ReferenceType) rt.superType();    
            }
            
            rt = sup;
        };

        return l;
    }

    public final void checkOverride(MethodType mj) throws SemanticException {
        canOverride(mj, false);
    }

    /**
     * Leave this method in for historic reasons, to make sure that extensions
     * modify their code correctly.
     */
    public boolean canOverride(MethodType mj) {
        try {
            return canOverride(mj, true);
        }
        catch (SemanticException e) {
            return false;
        }
    }
    
    public boolean canOverride(MethodType mj, boolean quiet) throws SemanticException {
        MethodType mi = this;

        if (!(mi.name().equals(mj.name()) && mi.hasFormals(mj.formalTypes()))) {
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; incompatible " +
                                        "parameter types",
                                        mi.position());
        }
        
        // HACK: Java5 allows return types to be covariant.  We'll allow covariant
        // return if we mj is defined in a class file.
        boolean allowCovariantReturn = false;
        
        if (mj.container() instanceof ClassType) {
            ClassType ct = (ClassType) mj.container();
            if (ct.def().initializer() instanceof LazyClassInitializer) {
                LazyClassInitializer init = (LazyClassInitializer) ct.def().initializer();
                if (init.fromClassFile()) {
                    allowCovariantReturn = true;
                }
            }
        }
        
        if ((allowCovariantReturn && ! ts.isSubtype(mi.returnType(), mj.returnType())) ||
            (! allowCovariantReturn && ! ts.typeEquals(mi.returnType(), mj.returnType()))) {
            if (Report.should_report(Report.types, 3))
                Report.report(3, "return type " + mi.returnType() +
                              " != " + mj.returnType());
            if (quiet) return false;
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
            if (quiet) return false;
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
            if (quiet) return false;
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
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is " + 
                                        (mj.flags().isStatic() ? "" : "not") +
                                        "static", 
                                        mi.position());
        }

        if (mi != mj && !mi.equals(mj) && mj.flags().isFinal()) {
            // mi can "override" a final method mj if mi and mj are the same method instance.
            if (Report.should_report(Report.types, 3))
                Report.report(3, mj.flags() + " final");
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is final", 
                                        mi.position());
        }

        return true;
    }
    
    public List<MethodType> implemented() {
        return implemented(container());
    }

    public List<MethodType> implemented(ReferenceType rt) {
        if (rt == null) {
            return Collections.EMPTY_LIST;
        }

        List<MethodType> l = new LinkedList();
        l.addAll(rt.methods(name(), formalTypes()));

        Type superType = rt.superType();
        if (superType != null) {
            l.addAll(implemented(superType.toReference())); 
        }
        
        List<Type> ints = rt.interfaces();
        for (Iterator<Type> i = ints.iterator(); i.hasNext(); ) {
            ReferenceType rt2 = (ReferenceType) i.next();
            l.addAll(implemented(rt2));
        }
        
        return l;
    }
}
