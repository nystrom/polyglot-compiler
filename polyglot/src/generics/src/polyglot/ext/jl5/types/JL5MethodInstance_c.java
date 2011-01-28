package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.ProcedureInstance;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

public class JL5MethodInstance_c extends JL5ProcedureInstance_c implements JL5MethodInstance{
    
    protected String name;

    protected Type returnType;

    /** Used for deserializing types. */
    protected JL5MethodInstance_c() {
    }

    public JL5MethodInstance_c(TypeSystem ts, Position pos, ReferenceType container, Flags flags,
            Type returnType, String name, List formalTypes, List excTypes) {
        super(ts, pos, container, flags, formalTypes, excTypes);
        this.returnType = returnType;
        this.name = name;
    }
    protected boolean compilerGenerated;
    
    
    public boolean isCompilerGenerated(){
        return compilerGenerated;
    }
        
    public JL5MethodInstance setCompilerGenerated(boolean val){
        if (compilerGenerated != val) {
            JL5MethodInstance_c mi = (JL5MethodInstance_c)copy();
            mi.compilerGenerated = val;
        
            return mi;
        }
        return this;
    }
    
    public boolean canOverrideImpl(MethodInstance mj, boolean quiet) throws SemanticException{

        MethodInstance mi = this;

        if (!(mi.name().equals(mj.name()) && ((JL5MethodInstance)mi).hasSameFormals((JL5ProcedureInstance) mj))) {
            if (quiet) return false;
            throw new SemanticException("Arguments or type variables are different", mi.position());
        }

	    // substitute the type parameters of this method for those of the other method,
	    // so that they can be properly compared to one another
	mj = (MethodInstance) ((JL5MethodInstance)mj).typeArguments(this.typeVariables());
	
        // changed to isSubtype may need to add bridge methods - even when no generics used - covariant return types 
        // equals part handles two void return types
        if (! ts.isImplicitCastValid(mi.returnType(), mj.returnType()) && !ts.equals(mi.returnType() , mj.returnType())){
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
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; the throw set is not a subset of the " +
                                        "overridden method's throw set", 
                                        mi.position());
        }   

        if (mi.flags().moreRestrictiveThan(mj.flags())) {
            if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; attempting to assign weaker " + 
                                        "access privileges", 
                                        mi.position());
        }

        if (mi.flags().isStatic() != mj.flags().isStatic()) {
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
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                                        " cannot override " + 
                                        mj.signature() + " in " + mj.container() + 
                                        "; overridden method is final", 
                                        mi.position());
        }

        return true;
    }


    public JL5MethodInstance flags(Flags flags) {
        if (!flags.equals(this.flags)) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.flags = flags;
            return n;
        }
        return this;
    }

    public String name() {
        return name;
    }

    public JL5MethodInstance name(String name) {
        if ((name != null && !name.equals(this.name)) || (name == null && name != this.name)) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.name = name;
            return n;
        }
        return this;
    }

    public Type returnType() {
        if (!isGeneric() || typeArguments == null)
            return returnType;
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        return ts.applySubstitution(returnType, typeVariables(), knownTypeArguments());
    }

    public JL5MethodInstance returnType(Type returnType) {
        if (this.returnType != returnType) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.returnType = returnType;
            return n;
        }
        return this;
    }

    public JL5MethodInstance formalTypes(List l) {
        if (!CollectionUtil.equals(this.formalTypes, l)) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.formalTypes = new ArrayList(l);
            return n;
        }
        return this;
    }

    public JL5MethodInstance throwTypes(List l) {
        if (!CollectionUtil.equals(this.excTypes, l)) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.excTypes = new ArrayList(l);
            return n;
        }
        return this;
    }

    public JL5MethodInstance container(ReferenceType container) {
        if (this.container != container) {
            JL5MethodInstance_c n = (JL5MethodInstance_c) copy();
            n.container = container;
            return n;
        }
        return this;
    }

    public int hashCode() {
        // return container.hashCode() + flags.hashCode() +
        // returnType.hashCode() + name.hashCode();
        return flags.hashCode() + name.hashCode();
    }

    public boolean equalsImpl(TypeObject o) {
        if (o instanceof JL5MethodInstance) {
            JL5MethodInstance i = (JL5MethodInstance) o;
            return ts.equals(returnType, i.returnType()) && name.equals(i.name())
                    && hasSameFormals(i);
        }

        return false;
    }
    
    
    @Override
    public JL5MethodInstance erasure() {
        JL5MethodInstance e = (JL5MethodInstance) super.erasure();
        return (JL5MethodInstance) e.returnType(((JL5TypeSystem)typeSystem()).erasure(returnType()));
    }

    public String toString() {
        String s = designator() + " " + flags.translate() + returnType + " " + signature();

        if (!excTypes.isEmpty()) {
            s += " throws " + JL5TypeSystem_c.listToString(excTypes);
        }

        return s;
    }

    public String signature() {
        return name + "(" + JL5TypeSystem_c.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "method";
    }

    /** Returns true iff <this> is the same method as <m> */
    public final boolean isSameMethod(MethodInstance m) {
        return ts.isSameMethod(this, m);
    }

    /** Returns true iff <this> is the same method as <m> */
    public boolean isSameMethodImpl(MethodInstance m) {
        return this.name().equals(m.name()) && hasSameFormals((JL5ProcedureInstance) m);
    }

    public boolean isCanonical() {
        return container.isCanonical() && returnType.isCanonical() && listIsCanonical(formalTypes)
                && listIsCanonical(excTypes);
    }

    public final boolean methodCallValid(String name, List argTypes) {
        return ts.methodCallValid(this, name, argTypes);
    }

    public boolean methodCallValidImpl(String name, List argTypes) {
        return name().equals(name) && ts.callValid(this, argTypes);
    }

    public List overrides() {
        return ts.overrides(this);
    }

    public List<JL5MethodInstance> overridesImpl() {
        List<JL5MethodInstance> l = new ArrayList<JL5MethodInstance>();
        ReferenceType rt = container();

        while (rt != null) {
            // add any method with the same name and formalTypes from
            // rt
            for (JL5MethodInstance mi : (List<JL5MethodInstance>)rt.methods()) {
                if (isSameMethodImpl(mi) || isSameMethod(mi.erasure())) l.add(mi);
            }

            ReferenceType sup = null;
            if (rt.superType() != null && rt.superType().isReference()) {
                sup = (ReferenceType) rt.superType();
            }

            rt = sup;
        }

        return l;
    }

    public final boolean canOverride(MethodInstance mj) {
        return ts.canOverride(this, mj);
    }

    public final void checkOverride(MethodInstance mj) throws SemanticException {
        ts.checkOverride(this, mj);
    }

 
    public List implemented() {
        return ts.implemented(this);
    }

    public List<JL5MethodInstance> implementedImpl(ReferenceType rt) {
        if (rt == null) {
            return (List<JL5MethodInstance>)Collections.EMPTY_LIST;
        }

        List<JL5MethodInstance> l = new LinkedList<JL5MethodInstance>();
        for (JL5MethodInstance mi : (List<JL5MethodInstance>)rt.methods()) {
            if (isSameMethodImpl(mi) || isSameMethod(mi.erasure())) l.add(mi);
        }

        Type superType = rt.superType();
        if (superType != null) {
            l.addAll(implementedImpl(superType.toReference()));
        }

        List ints = rt.interfaces();
        for (Iterator i = ints.iterator(); i.hasNext();) {
            ReferenceType rt2 = (ReferenceType) i.next();
            l.addAll(implementedImpl(rt2));
        }

        return l;
    }
    

}
