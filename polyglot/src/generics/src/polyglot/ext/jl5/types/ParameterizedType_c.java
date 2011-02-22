package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;

/* A reference to an instantiation of a parameterized type */

public class ParameterizedType_c extends GenericTypeRef_c implements ParameterizedType,
        SignatureType {

    protected List<Type> typeArguments;

    public ParameterizedType_c(JL5ParsedClassType t) {
        super(t);
    }

    public List<Type> typeArguments() {
        return typeArguments;
    }

    public void typeArguments(List<Type> args) {
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
        signature.append("L" + ((Named) baseType).fullName().replaceAll("\\.", "/") + "<");
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

    public boolean descendsFromImpl(Type ancestor) {
        if (super.descendsFromImpl(ancestor))
            return true;

        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        // if the ancestor is a raw type and some corresponding
        // parameterized type is in the set then we allow it
        if (ancestor instanceof RawType) {
            if (ts.isSubtype(ts.rawify(this), ancestor)) {

                return true;
            }
        }
        else if ((ancestor instanceof ParameterizedType) && (!equals(ancestor))) {
            return ts.checkContains(capture(), (ParameterizedType) ancestor);
        }
        return false;
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
                        capArg.bounds(ts.applySubstitution(baseTypeVars.get(i).bounds(), baseTypeVars, capturedArgs));
                    }
                    else if (arg instanceof AnySubType) {
                        AnySubType argcast = (AnySubType) arg;
                        List<ReferenceType> newBounds = new ArrayList<ReferenceType>();
                        newBounds.add(argcast.bound());
                        newBounds.addAll(ts.applySubstitution(baseTypeVars.get(i).bounds(), baseTypeVars, capturedArgs));
                        capArg.bounds(newBounds);
                    }
                    else if (arg instanceof AnySuperType) {
                        AnySuperType argcast = (AnySuperType) arg;
                        capArg.lowerBound(argcast.bound());
                        capArg.bounds(ts.applySubstitution(baseTypeVars.get(i).bounds(), baseTypeVars, capturedArgs));
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

    public ClassType outer() {
        if (outer == null) {
            outer = (ClassType) ((JL5TypeSystem) typeSystem()).getSubstitution(this, baseType.outer());
        }
        return outer;
    }

    public Type superType() {
        if (superType == null) {
            superType = ((JL5TypeSystem) typeSystem()).getSubstitution(this, baseType.superType());
        }
        return superType;
    }

    public List constructors() {
        if (constructors == null) {
            List<ConstructorInstance> t = new ArrayList<ConstructorInstance>();
            List orig = baseType.constructors();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                ConstructorInstance ci = (ConstructorInstance) it.next();
                ConstructorInstance n = (ConstructorInstance) ci.copy();
                n = n.container(this);
                List<Type> formals = new ArrayList<Type>();
                for (Iterator it2 = ci.formalTypes().iterator(); it2.hasNext();) {
                    Type formalType = (Type) it2.next();
                    Type nf = ((JL5TypeSystem) ts).getSubstitution(this, formalType);
                    formals.add(nf);
                }
                n = n.formalTypes(formals);
                t.add(n);
            }
            constructors = t;
        }
        return constructors;
    }

    public List memberClasses() {
        if (memberClasses == null) {
            List<Type> t = new ArrayList<Type>();
            List orig = baseType.memberClasses();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                Type type = (Type) it.next();
                Type ntype = ((JL5TypeSystem) ts).getSubstitution(this, type);
                t.add(ntype);
            }
            memberClasses = t;
        }
        return memberClasses;
    }

    public List methods() {
        if (methods == null) {
            List<MethodInstance> t = new ArrayList<MethodInstance>();
            List orig = baseType.methods();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                MethodInstance mi = (MethodInstance) it.next();
                MethodInstance n = (MethodInstance) mi.copy();
                n = n.returnType(((JL5TypeSystem) ts).getSubstitution(this, mi.returnType()));
                n = n.container(this);
                List formals = new ArrayList();
                for (Iterator it2 = mi.formalTypes().iterator(); it2.hasNext();) {
                    Type formalType = (Type) it2.next();
                    Type nf = ((JL5TypeSystem) ts).getSubstitution(this, formalType);
                    formals.add(nf);
                }
                n = n.formalTypes(formals);
                t.add(n);
            }
            methods = t;
        }
        return methods;
    }

    public List fields() {
        if (fields == null) {
            List<FieldInstance> t = new ArrayList<FieldInstance>();
            List orig = baseType.fields();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                FieldInstance f = (FieldInstance) it.next();
                Type i = f.type();
                Type subst = ((JL5TypeSystem) typeSystem()).getSubstitution(this, i);
                f = f.type(subst);
                f = f.container(this);
                t.add(f);
            }
            fields = t;
        }
        return fields;
    }

    public List interfaces() {
        if (interfaces == null) {
            // System.out.println("interfaces is null!");
            List<Type> t = new ArrayList<Type>();
            List orig = baseType.interfaces();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                Type i = (Type) it.next();
                Type subst = ((JL5TypeSystem) typeSystem()).getSubstitution(this, i);
                t.add(subst);
            }
            interfaces = t;
        }
        return interfaces;
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

}
