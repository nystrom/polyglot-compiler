package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;

/* A reference to an instantiation of a parameterized type */

public class ParameterizedType_c extends GenericTypeRef_c implements ParameterizedType,
        SignatureType {

    protected List<Type> typeArguments;
    protected List<Ref<?extends Type>> typeArgumentsRefs;
 
    public ParameterizedType_c(JL5ParsedClassType t) {
        super(t);
    }

    public ParameterizedType_c(JL5ParsedClassType type, Ref<ClassDef> defRef, List<Ref<?extends Type>> typeArgumentsRefs) {
        super(type.typeSystem(), type.position(), type, defRef);
        this.typeArgumentsRefs = typeArgumentsRefs;
    }

    public List<Type> typeArguments() {
        // deref typeArguments 
        if (typeArgumentsRefs != null) {
            List<Type> newTypeArguments = new LinkedList<Type>();
            for (Ref<?extends Type> ref : typeArgumentsRefs) {
                newTypeArguments.add((Type) Types.get(ref));
            }
            this.typeArguments = newTypeArguments;
            this.typeArgumentsRefs = null;
        }
        return typeArguments;
    }

    public void typeArguments(List<Type> args) {
        assert this.typeArgumentsRefs == null;
        this.typeArguments = args;
    }

    public String translate(Resolver c) {
        StringBuffer sb = new StringBuffer(baseType.translate(c));
        sb.append("<");
        for (Iterator<Type> it = typeArguments().iterator(); it.hasNext();) {
            sb.append(it.next().translate(c));
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    public String toString() {
        Type b = ((JL5TypeSystem)typeSystem()).erasure(baseType());
        StringBuffer sb = new StringBuffer(b.toString());
        sb.append("<");
        for (Iterator<Type> it = typeArguments().iterator(); it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    /*
     private boolean argsEquivalent(ParameterizedType ancestor){
     for (int i = 0; i < ancestor.typeArguments().size(); i++){
     
     Type arg1 = ancestor.typeArguments().get(i);
     Type arg2 = typeArguments().get(i);
     TypeVariable cap1 = (TypeVariable)ancestor.typeVariables().get(i);
     TypeVariable cap2 = (TypeVariable)baseType().typeVariables().get(i);
     // if both are AnySubType then arg2 bound must be subtype 
     // of arg1 bound
     if (arg1 instanceof AnySubType){
     if (arg2 instanceof AnySubType){
     if (!typeSystem().equals(((AnySubType)arg2).bound(), ((AnySubType)arg1).bound())) return false;
     }
     else if (arg2 instanceof AnySuperType){
     if (!typeSystem().equals(((AnySubType)arg1).bound(), ((AnySuperType)arg2).bound())) return false;
     }
     else if (arg2 instanceof TypeVariable){
     // need to break out here or will recurse for ever
     if (((TypeVariable)arg2).name().equals(((TypeVariable)((AnySubType)arg1).bound()).name())) return true;
     }
     // if only ancestor(arg1) is AnySubType then arg2 is not
     // wildcard must be subtype of bound of arg1
     else {
     if (!typeSystem().equals(arg2, arg1)) return false;
     }
     }
     // if both are AnySuperType then arg1 bound must be a subtype
     // of arg2 bound
     else if (arg1 instanceof AnySuperType){
     if (arg2 instanceof AnySuperType){
     if (!typeSystem().equals(((AnySuperType)arg1).bound(), ((AnySuperType)arg2).bound())) return false;
     }
     // if only arg1 instanceof AnySuperType then arg1 bounds 
     // must be a subtype of arg2
     else {
     if (!typeSystem().equals(arg1, arg2)) return false;
     }
     }
     else if (arg1 instanceof AnyType){
     if (arg2 instanceof AnyType){
     if (!typeSystem().equals(((AnyType)arg1).upperBound(), ((AnyType)arg2).upperBound())) return false;
     }
     else {
     if (!typeSystem().equals(arg1, arg2)) return false;
     }
     }
     else if (arg1 instanceof ParameterizedType && arg2 instanceof ParameterizedType){
     //if (arg1.equals(arg2)) return true;
     if (!typeSystem().equals(arg1, arg2)) return false;
     }
     else if (arg1 instanceof TypeVariable && arg2 instanceof TypeVariable){
     if (!typeSystem().equals(arg1, arg2) && !((JL5TypeSystem)typeSystem()).isEquivalent(arg1, arg2)) return false;
     }
     else {
     if (!typeSystem().equals(arg1, arg2)) return false;
     }
     }
     return true;
     }
     */
    public boolean equivalentImpl(TypeObject t) {
    	Context ctx = ts.emptyContext();
        if (!(t instanceof ParameterizedType))
            return false;
        if (ts.typeEquals(((ParameterizedType) t).baseType(), this.baseType(), ctx)) {
            int i = 0;
            for (i = 0; i < ((ParameterizedType) t).typeArguments().size()
                    && i < this.typeArguments().size(); i++) {
                Type t1 = ((ParameterizedType) t).typeArguments().get(i);
                Type t2 = this.typeArguments().get(i);
                if (t1 instanceof AnyType && t2 instanceof AnyType) {
                    continue;
                }
                if (t1 instanceof AnySubType && t2 instanceof AnySubType) {
                    Type bound1 = ((AnySubType) t1).bound();
                    if (bound1 instanceof TypeVariable) {
                        bound1 = ((TypeVariable) bound1).erasureType();
                    }
                    Type bound2 = ((AnySubType) t2).bound();
                    if (bound2 instanceof TypeVariable) {
                        bound2 = ((TypeVariable) bound2).erasureType();
                    }
                    if (bound1 instanceof ParameterizedType && bound2 instanceof ParameterizedType) {
                        if (!((JL5TypeSystem) typeSystem()).equivalent(bound1, bound2))
                            return false;
                    }
                    else {
                        if (!ts.typeEquals(bound1, bound2, ctx))
                            return false;
                    }
                    continue;
                }
                if (t1 instanceof AnySuperType && t2 instanceof AnySuperType) {
                    Type bound1 = ((AnySuperType) t1).bound();
                    if (bound1 instanceof TypeVariable) {
                        bound1 = ((TypeVariable) bound1).erasureType();
                    }
                    Type bound2 = ((AnySuperType) t2).bound();
                    if (bound2 instanceof TypeVariable) {
                        bound2 = ((TypeVariable) bound2).erasureType();
                    }
                    if (bound1 instanceof ParameterizedType && bound2 instanceof ParameterizedType) {
                        if (!((JL5TypeSystem) typeSystem()).equivalent(bound1, bound2))
                            return false;
                    }
                    else {
                        if (!ts.typeEquals(bound1, bound2, ctx))
                            return false;
                    }
                    continue;
                }
                if (t1 instanceof ParameterizedType && t2 instanceof ParameterizedType) {
                    if (!((JL5TypeSystem) typeSystem()).equivalent(t1, t2))
                        return false;
                    continue;
                }
                else {
                    if (!ts.typeEquals(t1, t2, ctx))
                        return false;
                    continue;
                }
            }
            if (i < ((ParameterizedType) t).typeArguments().size()
                    || i < this.typeArguments().size())
                return false;
            return true;
        }
        return false;
    }

    public boolean equalsImpl(TypeObject t) {
        if (t instanceof ParameterizedType) {
            ParameterizedType other = (ParameterizedType) t;
            if (ts.equals((TypeObject)baseType(), (TypeObject)other.baseType())
                    && (typeArguments().size() == other.typeArguments().size())) {
                for (int i = 0; i < typeArguments().size(); i++) {
                    Type arg1 = typeArguments().get(i);
                    Type arg2 = other.typeArguments().get(i);
                    if (!ts.equals((TypeObject)arg1, (TypeObject)arg2))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    public String signature() {
        StringBuffer signature = new StringBuffer();
        // no trailing ; for base type before the type args
        signature.append("L" + ((Named) baseType).fullName().toString().replaceAll("\\.", "/") + "<");
        for (Iterator<Type> it = typeArguments().iterator(); it.hasNext();) {
            SignatureType next = (SignatureType) it.next();
            signature.append(next.signature());
            if (it.hasNext()) {
                signature.append(",");
            }
        }
        signature.append(">;");
        return signature.toString();
    }


    protected ParameterizedType capturedType;

    private static int captureCount = 0;

    /*
     * (non-Javadoc)
     * @see polyglot.ext.jl5.types.GenericTypeRef#capture()
     */
    public ParameterizedType capture() {
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        //if (capturedType != null) return capturedType; //to cache or not to cache ? 
        List<Type> capturedArgs = new ArrayList<Type>();
        boolean anyWildCard = false;
        for (int i = 0; i < typeArguments().size(); i++) {
            Type arg = typeArguments().get(i);
            if (arg instanceof Wildcard) {
                anyWildCard = true;
                //just put null bounds now
                capturedArgs.add(ts.typeVariable(position(), "capture of ?_" + captureCount++, null));
            }
            else {
                capturedArgs.add(arg);
            }
        }
        if (anyWildCard) {
            List<TypeVariable> baseTypeVars = baseType().typeVariables();
            for (int i = 0; i < typeArguments().size(); i++) {
                Type arg = typeArguments().get(i);
                if (arg instanceof Wildcard) {
                    TypeVariable capArg = (TypeVariable) capturedArgs.get(i);
                    if (arg instanceof AnyType) {
                        capArg.bounds(
                                ts.toRefTypes(ts.applySubstitution(baseTypeVars.get(i).bounds(), baseTypeVars, capturedArgs)));
                    }
                    else if (arg instanceof AnySubType) {
                        AnySubType argcast = (AnySubType) arg;
                        List<Type> newBounds = new ArrayList<Type>();
                        newBounds.add(argcast.bound());
                        newBounds.addAll(ts.applySubstitution(baseTypeVars.get(i).bounds(), baseTypeVars, capturedArgs));
                        capArg.bounds(ts.toRefTypes(newBounds));
                    }
                    else if (arg instanceof AnySuperType) {
                        AnySuperType argcast = (AnySuperType) arg;
                        capArg.lowerBound(argcast.bound());
                        TypeVariable tv = baseTypeVars.get(i);
                        List<Type> toBeSubed = tv.bounds();
                        List<TypeVariable> orig = baseTypeVars; 
                        List<Type> sub = capturedArgs;
                        capArg.bounds(ts.toRefTypes(ts.applySubstitution(toBeSubed, orig, sub)));
                        anyWildCard = true;
                    }
                }

            }
            capturedType = ts.parameterizedType(baseType());
            capturedType.typeArguments(capturedArgs);
        }
        else {
            capturedType = this;
        }
        return capturedType;
    }

    @Override
    public ClassType outer() {
        ClassDef outer = Types.get(def().outer());
        if (outer == null) return null;
        return (ClassType) ((JL5TypeSystem) typeSystem()).getSubstitution(this, outer.asType());
    }

    @Override
    public Type superClass() {
    	Type superType = Types.get(def().superType());
        return ((JL5TypeSystem) typeSystem()).getSubstitution(this, superType);
    }

    @Override
    public List<ConstructorInstance> constructors() {
        return new TransformingList<ConstructorDef,ConstructorInstance>(
                                    def().constructors(),
                                    new ConstructorAsSubstitutionTypeTransform(ts, this));
    }

    /** Return an immutable list of member classes */
    public List<ClassType> memberClasses() {
        return new TransformingList<Ref<? extends ClassType>,ClassType>(def().memberClasses(),
                                    new DerefSubstitutionTransform<ClassType>(ts, this));
    }

    /** Return an immutable list of methods. */
    @Override
    public List<MethodInstance> methods() {
        return new TransformingList<MethodDef,MethodInstance>(
                                    def().methods(),
                                    new MethodAsSubstitutionTypeTransform(ts, this));
    }

    /** Return an immutable list of interfaces */
    public List<Type> interfaces() {
        return new TransformingList<Ref<? extends Type>, Type>(
                                                    def().interfaces(),
                                                    new DerefSubstitutionTransform(ts, this));
    }

    @Override
    public List<FieldInstance> fields() {
        return new TransformingList<FieldDef,FieldInstance>(
                def().fields(),
                new FieldAsSubstitutionTypeTransform(ts, this));
    }

    protected List<TypeVariable> substTypeVars = null;

    public List<TypeVariable> typeVariables() {
/*        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        if (substTypeVars == null) {
            substTypeVars = new ArrayList<TypeVariable>();
            for (TypeVariable origTv : super.typeVariables()) {
                TypeVariable newTv = (TypeVariable) origTv.copy();
                newTv.bounds(ts.applySubstitution(origTv.bounds(), super.typeVariables(), typeArguments()));
                substTypeVars.add(newTv);
            }
        }
        return substTypeVars;*/
        return baseType().typeVariables();
    }
    
    class DerefSubstitutionTransform<T extends TypeObject> implements
    Transformation<Ref<? extends T>, T> {
    	private final JL5TypeSystem ts;
    	private final GenericTypeRef pt;
    	public DerefSubstitutionTransform(TypeSystem ts, GenericTypeRef pt) {
    		this.ts = (JL5TypeSystem) ts;
    		this.pt = pt;
    	}
    	public T transform(Ref<? extends T> ref) {
    		return (T) ts.getSubstitution(pt, ((Type)Types.get(ref)));
    	}
    }

    class ConstructorAsSubstitutionTypeTransform implements
    Transformation<ConstructorDef, ConstructorInstance> {
    	private final JL5TypeSystem ts;
    	private final GenericTypeRef rt;
    	public ConstructorAsSubstitutionTypeTransform(TypeSystem ts, GenericTypeRef rt) {
    		this.ts = (JL5TypeSystem) ts;
    		this.rt = rt;
    	}
    	public ConstructorInstance transform(ConstructorDef def) {
    		ConstructorInstance ci = def.asInstance();
    		ci = ci.container(rt);
    		List<Type> substFormalTypes = 
    			new TransformingList<Ref <? extends Type>,Type>(def.formalTypes(),
    				new DerefSubstitutionTransform<Type>(ts, rt));
    		ci = ci.formalTypes(substFormalTypes);
    		return ci;
    	}
    }

    class MethodAsSubstitutionTypeTransform implements
    Transformation<MethodDef, MethodInstance> {
    	private final JL5TypeSystem ts;
    	private final GenericTypeRef rt;
    	public MethodAsSubstitutionTypeTransform(TypeSystem ts, GenericTypeRef rt) {
    		this.ts = (JL5TypeSystem) ts;
    		this.rt = rt;
    	}
    	public MethodInstance transform(MethodDef def) {
    		MethodInstance mi = def.asInstance();
    		mi = (MethodInstance) mi.container(rt);
            mi = mi.returnType(ts.getSubstitution(rt, mi.returnType()));
    		List<Type> substFormalTypes = 
    			// CHECK is ok to deRef from def rather than use mi.formalTypes() ?
    			new TransformingList<Ref <? extends Type>,Type>(def.formalTypes(),
    				new DerefSubstitutionTransform<Type>(ts, rt));
    		mi = mi.formalTypes(substFormalTypes);
    		return mi;
    	}
    }
    
    class FieldAsSubstitutionTypeTransform implements
    Transformation<FieldDef, FieldInstance>  {
    	private final JL5TypeSystem ts;
    	private final GenericTypeRef rt;
    	public FieldAsSubstitutionTypeTransform(TypeSystem ts, GenericTypeRef rt) {
    		this.ts = (JL5TypeSystem) ts;
    		this.rt = rt;
    	}
        public FieldInstance transform(FieldDef def) {
            FieldInstance fi = def.asInstance();
            Type er = ts.getSubstitution(rt, fi.type());
            fi = fi.type(er);
            fi = fi.container(rt);
    		return fi;
    	}
    }

}