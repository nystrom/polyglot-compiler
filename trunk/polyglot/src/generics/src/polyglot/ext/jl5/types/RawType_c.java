package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;

/* A reference to a raw type */

public class RawType_c extends GenericTypeRef_c implements RawType {

    public RawType_c(JL5ParsedClassType t) {
        super(t);
    }

    // GenericTypeRef_c applies type substitution everywhere.
    // For a raw type, we should keep things raw when we talk
    // about types, for example its supertype, rather than
    // treating it as a substitution with Object, as we do
    // when we consider the raw type's methods and fields.
    // should we do this for outer() and memberClasses() as well?
    public Type superType() {
        if (superType == null) {
            superType = ((JL5TypeSystem) typeSystem()).erasure(baseType().superType());
        }
        return superType;
    }

    public List interfaces() {
        if (interfaces == null) {
            List<Type> t = new ArrayList<Type>();
            List orig = baseType().interfaces();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                JL5ParsedClassType i = (JL5ParsedClassType) it.next();
                Type rt = ((JL5TypeSystem) typeSystem()).erasure(i);
                t.add(rt);
            }
            interfaces = t;
        }
        return interfaces;
    }
    
    @Override
    public List constructors() {
        if (constructors == null) {
            List<ConstructorInstance> t = new ArrayList<ConstructorInstance>();
            List<ConstructorInstance> old = baseType().constructors();
            for (ConstructorInstance oldconstr : old) {
                ConstructorInstance newconst = (ConstructorInstance) oldconstr.copy();
                newconst = newconst.container(this);
                if (newconst instanceof JL5ConstructorInstance) {
                    JL5ConstructorInstance nc = (JL5ConstructorInstance) newconst;
                    newconst = (ConstructorInstance) nc.erasure();
                    newconst = newconst.container(this);
                }
                t.add(newconst);
            }
            constructors = t;
        }
        return constructors;
    }

    @Override
    public List fields() {
        if (fields == null) {
            List<FieldInstance> t = new ArrayList<FieldInstance>();
            List orig = baseType.fields();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                FieldInstance f = (FieldInstance) it.next();
                Type er = ((JL5TypeSystem) typeSystem()).erasure(f.type());
                f = f.type(er);
                f = f.container(this);
                t.add(f);
            }
            fields = t;
        }
        return fields;
    }

    @Override
    public List methods() {
        if (methods == null) {
            List<MethodInstance> t = new ArrayList<MethodInstance>();
            List orig = baseType.methods();
            for (Iterator it = orig.iterator(); it.hasNext();) {
                MethodInstance mi = (MethodInstance) it.next();
                MethodInstance n = (MethodInstance) mi.copy();
                if (n instanceof JL5MethodInstance) {
                    JL5MethodInstance nm = (JL5MethodInstance) n;
                    n = (MethodInstance) nm.erasure();
                    n = n.container(this);
                }
                t.add(n);
            }
            methods = t;
        }
        return methods;
    }

    @Override
    public ClassType outer() {
        if (outer == null) {
            outer = (ClassType) ((JL5TypeSystem)typeSystem()).erasure(baseType.outer());
        }
        return outer;
    }

    public List memberClasses() {
        if (memberClasses == null) {
            List<Type> old = baseType.memberClasses();
            List<Type> nmembers = new ArrayList<Type>();
            for (Type member : old) {
                nmembers.add(((JL5TypeSystem)typeSystem()).erasure(member));
            }
            memberClasses = nmembers;
        }
        return memberClasses;
    }
    
    public boolean equalsImpl(TypeObject t) {
        if (t instanceof RawType) {
            RawType rt = (RawType) t;
            return ts.equals((TypeObject)this.baseType(), (TypeObject)rt.baseType());
        }
        return false;
    }

    public boolean equivalentImpl(TypeObject t) {
        return equalsImpl(t);
    }

    public String translate(Resolver c) {
        return baseType().translate(c);
    }

    @Override
    public boolean descendsFromImpl(Type ancestor) {
        if (super.descendsFromImpl(ancestor))
            return true;
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        // if the ancestor's associated raw type is in the set
        // then we allow it
        if (ancestor instanceof ParameterizedType
	    || (ancestor instanceof JL5ParsedClassType && !(ancestor instanceof RawType) &&
		((JL5ParsedClassType)ancestor).isGeneric())) {
            return ts.isSubtype(this, ts.rawify(ancestor));
        }
        return false;
    }

    public GenericTypeRef capture() {
        return this;
    }

}
