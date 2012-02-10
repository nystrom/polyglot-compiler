package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;

/** 
 * A reference to a raw type:
 * Used whenever a generic class type is used without type parameters
 * 
 * This is somehow a proxy to a JL5ParsedClassType that erases all Generic-related info
 * 
 */
public class RawType_c extends GenericTypeRef_c implements RawType {

    public RawType_c(JL5ParsedClassType t) {
        super(t);
    }

    public RawType_c(JL5ParsedClassType type, Ref<ClassDef> defRef) {
        super(type.typeSystem(), type.position(), type, defRef);
    }

    // GenericTypeRef_c applies type substitution everywhere.
    // For a raw type, we should keep things raw when we talk
    // about types, for example its supertype, rather than
    // treating it as a substitution with Object, as we do
    // when we consider the raw type's methods and fields.

    @Override
    public Type superClass() {
    	Type superType = Types.get(def().superType());
        return ((JL5TypeSystem) typeSystem()).erasure(superType);
    }

    /** Return an immutable list of erased interfaces */
    @Override
    public List<Type> interfaces() {
        return new TransformingList<Ref<? extends Type>, Type>(
                                                    def().interfaces(),
                                                    new DerefEraseTransform<Type>(ts));
    }

    /** Return an immutable list of constructors */
    @Override
    public List<ConstructorInstance> constructors() {
        return new TransformingList<ConstructorDef,ConstructorInstance>(
                                    def().constructors(),
                                    new ConstructorAsErasedTypeTransform(ts, this));
    }

    /** Return an immutable list of fields */
    @Override
    public List<FieldInstance> fields() {
        return new TransformingList<FieldDef, FieldInstance>(def().fields(),
                                                         new FieldAsErasedTypeTransform(ts, this));
    }

    /** Return an immutable list of methods. */
    @Override
    public List<MethodInstance> methods() {
        return new TransformingList<MethodDef,MethodInstance>(
                                    def().methods(),
                                    new MethodAsErasedTypeTransform(ts, this));
    }

    @Override
    public ClassType outer() {
        ClassDef outer = Types.get(def().outer());
        if (outer == null) return null;
        return (ClassType) ((JL5TypeSystem) ts).erasure(outer.asType());
    }

    /** Return an immutable list of member classes */
    public List<ClassType> memberClasses() {
        return new TransformingList<Ref<? extends ClassType>,ClassType>(def().memberClasses(),
                                    new DerefEraseTransform<ClassType>(ts));
    }
    
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof RawType) {
        	TypeObject thisRT = (TypeObject) this.baseType(); 
        	TypeObject rt = (TypeObject) ((RawType) t).baseType(); 
            return thisRT.equalsImpl(rt);
        }
        return false;
    }

    public boolean equivalentImpl(TypeObject t) {
        return equalsImpl(t);
    }

    public String translate(Resolver c) {
        return baseType().translate(c);
    }

    public GenericTypeRef capture() {
        return this;
    }
}

class DerefEraseTransform<T extends TypeObject> implements
        Transformation<Ref<? extends T>, T> {
	private final JL5TypeSystem ts;
	public DerefEraseTransform(TypeSystem ts) {
		this.ts = (JL5TypeSystem) ts;
	}
    public T transform(Ref<? extends T> ref) {
        return (T) ts.erasure((Type)Types.get(ref));
    }
}

class ConstructorAsErasedTypeTransform implements
Transformation<ConstructorDef, ConstructorInstance> {
	private final JL5TypeSystem ts;
	private final StructType rt;
	public ConstructorAsErasedTypeTransform(TypeSystem ts, StructType rt) {
		this.ts = (JL5TypeSystem) ts;
		this.rt = rt;
	}
	public ConstructorInstance transform(ConstructorDef def) {
        ConstructorInstance ci = def.asInstance();
        ci = ci.container(rt);
        ci = (ConstructorInstance) ((JL5ConstructorInstance) ci).erasure();
		return ci;
	}
}

class FieldAsErasedTypeTransform implements
Transformation<FieldDef, FieldInstance>  {
	private final JL5TypeSystem ts;
	private final StructType rt;
	public FieldAsErasedTypeTransform(TypeSystem ts, StructType rt) {
		this.ts = (JL5TypeSystem) ts;
		this.rt = rt;
	}
    public FieldInstance transform(FieldDef def) {
        FieldInstance fi = def.asInstance();
        Type er = ts.erasure(fi.type());
        fi = fi.type(er);
        fi = fi.container(rt);
		return fi;
	}
}

class MethodAsErasedTypeTransform implements
        Transformation<MethodDef, MethodInstance> {
	private final JL5TypeSystem ts;
	private final StructType rt;
	public MethodAsErasedTypeTransform(TypeSystem ts, StructType rt) {
		this.ts = (JL5TypeSystem) ts;
		this.rt = rt;
	}
    public MethodInstance transform(MethodDef def) {
        MethodInstance mi = def.asInstance();
        mi = ((JL5MethodInstance) mi).erasure();
        mi = (MethodInstance) mi.container(rt);
        return mi;
    }
}

