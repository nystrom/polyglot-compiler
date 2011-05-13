package polyglot.ext.jl5.types;

import java.util.Collections;
import java.util.List;

import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.ClassType_c;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

/**
 * 
 * A TypeVariable as 'T' in the type declaration Collection<T>
 *
 */
public class TypeVariable_c extends ClassType_c implements TypeVariable, SignatureType {

    protected Name name;
    protected Flags flags;
    protected Type lowerBound;
    protected IntersectionType upperBound;
    protected TVarDecl declaredIn;
    protected ClassType declaringClass;
    protected JL5ProcedureInstance declaringProcedure;

    public TypeVariable_c(TypeSystem ts, Position pos, Name id,
    		Ref<? extends ClassDef> def, List<ClassType> bounds) {
    	super(ts, pos, def);
        this.name = id;
        this.upperBound = ((JL5TypeSystem)ts).intersectionType(def, bounds);
        upperBound.boundOf(this);
        flags = Flags.NONE;
    }
    
    public void declaringProcedure(JL5ProcedureInstance pi) {
        declaredIn = TVarDecl.PROCEDURETV;
        declaringProcedure = pi;
        declaringClass = null;
    }
    
    public void declaringClass(ClassType ct) {
        declaredIn = TVarDecl.CLASSTV;
        declaringProcedure = null;
        declaringClass = ct;
    }
    
    public TVarDecl declaredIn() {
        if (declaredIn == null) {
            declaredIn = TVarDecl.SYNTHETICTV;
        }
        return declaredIn;
    }

    public ClassType declaringClass() {
        if (declaredIn.equals(TVarDecl.CLASSTV)) return declaringClass;
        return null;
    }
    
    public JL5ProcedureInstance declaringProcedure() {
        if (declaredIn.equals(TVarDecl.PROCEDURETV)) return declaringProcedure;
        return null;
    }

    public List<ClassType> bounds() {
        return upperBound().bounds();
    }

    public void bounds(List<ClassType> b) {
        upperBound = ((JL5TypeSystem)typeSystem()).intersectionType(def, b);
        upperBound.boundOf(this);
    }

    public Kind kind() {
        return TYPEVARIABLE;
    }

    public ClassType outer() {
        return null;
    }

    public Name name() {
        return name;
    }

    public void name(Name name) {
        this.name = name;
    }

    public polyglot.types.Package package_() {
        if (TVarDecl.CLASSTV.equals(declaredIn)) {
            return declaringClass().package_();
        }
        if (TVarDecl.PROCEDURETV.equals(declaredIn)) {
            return ((ClassDef)declaringProcedure().def()).asType().package_();
        }
        return null;
    }

    public Flags flags() {
        return flags;
    }

    public List constructors() {
        return Collections.emptyList();
    }

    public List memberClasses() {
        return Collections.emptyList();
    }

    public List methods() {
        return Collections.emptyList();
    }

    public List fields() {
        return Collections.emptyList();
    }

    public List interfaces() {
        return Collections.emptyList();
    }

    public Type superType() {
        return upperBound();
    }

    public boolean inStaticContext() {
        return false; // not sure
    }

    public String translate(Resolver c) {
        return name().toString();
    }

    public String toString() {
        return name.toString();// +":"+bounds;
    }

    public IntersectionType upperBound() {
        return upperBound;
    }
    
    public void upperBound(IntersectionType b) {
        upperBound = b;
        upperBound.boundOf(this);
    }

//     public boolean isImplicitCastValidImpl(Type toType) {
//         return ts.isImplicitCastValid(upperBound(), toType);
//     }
    
    public boolean equalsImpl(TypeObject other) {
        if (!(other instanceof TypeVariable))
            return super.equalsImpl(other);
        TypeVariable arg2 = (TypeVariable) other;
        if (this.name.equals(arg2.name())) {
            if (declaredIn().equals(TVarDecl.SYNTHETICTV)) {
                return arg2.declaredIn().equals(TVarDecl.SYNTHETICTV);
            }
            else if (declaredIn().equals(TVarDecl.PROCEDURETV)) {
                return (arg2.declaredIn().equals(TVarDecl.PROCEDURETV)) && 
                    declaringProcedure() == arg2.declaringProcedure();
                //(ts.equals(declaringMethod(), arg2.declaringMethod())); 
            }
            else if (declaredIn().equals(TVarDecl.CLASSTV)) {
            	return (arg2.declaredIn().equals(TVarDecl.CLASSTV)) &&
            		ts.equals((TypeObject)declaringClass(), (TypeObject)arg2.declaringClass());
            }
            return true;
        }
        return false;
    }

    public boolean equivalentImpl(TypeObject other) {
        if (!(other instanceof TypeVariable))
            return super.equalsImpl(other);
        TypeVariable arg2 = (TypeVariable) other;
        if (this.name.equals(arg2.name()))
            return true;// && allBoundsEqual(arg2)) return true;
        return false;
    }
    public boolean isEquivalent(TypeObject arg2) {
    	if (arg2 instanceof TypeVariable) {
    		if (this.erasureType() instanceof ParameterizedType
    				&& ((TypeVariable) arg2).erasureType() instanceof ParameterizedType) {
    			return typeSystem().equals((TypeObject)((ParameterizedType) this.erasureType()).baseType(), 
    					(TypeObject) ((ParameterizedType) ((TypeVariable) arg2).erasureType()).baseType());
    		} else {
    			return typeSystem().equals((TypeObject) this.erasureType(), 
    					(TypeObject) ((TypeVariable) arg2).erasureType());
    		}
    	}
    	return false;
    }
    /*
    private boolean allBoundsEqual(TypeVariable arg2) {
        if ((bounds == null || bounds.isEmpty())
                && (arg2.bounds() == null || arg2.bounds().isEmpty()))
            return true;
        Iterator<ClassType> it = bounds.iterator();
        Iterator<ClassType> jt = arg2.bounds().iterator();
        while (it.hasNext() && jt.hasNext()) {
            /*
             * Type t1 = (type)it.next(); Type t2 = (type)jt.next();
             *//*
            if (!ts.equals(it.next(), jt.next())) {
                return false;
            }
        }
        if (it.hasNext() || jt.hasNext())
            return false;
        return true;
    }
*/


    public Type erasureType() {
        return ((JL5TypeSystem)typeSystem()).erasure(bounds().get(0));
    }

    public ClassType toClass() {
        return this;
    }

    public String signature() {
        return "T" + name + ";";
    }

    public Type lowerBound() {
        if (lowerBound == null) return typeSystem().Null();
        return lowerBound;
    }

    public void lowerBound(Type lowerBound) {
        this.lowerBound = lowerBound;
    }

	@Override
	public ClassType flags(Flags flags) {
        TypeVariable_c t = (TypeVariable_c) copy();
        t.flags = flags;
        return t;
    }

	@Override
	public ClassType container(StructType container) {
//		TypeVariable_c t = (TypeVariable_c) copy();
//        t.container = container;
//        return t;
		//CHECK What's the container of a TypeVariable ?
		assert false;
		return null;
	}

	@Override
	public Job job() {
		//CHECK There is no job associated with a type variable ?
		assert false;
		return null;
	}

	@Override
	public Type superClass() {
		//CHECK What's the super class of a typeVariable ?
		assert false;
		return null;
	}
}
