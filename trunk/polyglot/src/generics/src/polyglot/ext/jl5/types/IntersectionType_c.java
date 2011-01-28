package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import polyglot.ext.jl.types.ClassType_c;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.Package;
import polyglot.types.ParsedClassType;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class IntersectionType_c extends ClassType_c implements IntersectionType {

    protected List<ReferenceType> bounds;

    protected List<ReferenceType> concreteBounds;
    
    protected TypeVariable boundOf_;

    public IntersectionType_c(TypeSystem ts, List<ReferenceType> bounds) {
        super(ts, null);
        this.bounds = bounds;
    }

    public List<ReferenceType> bounds() {
        if (bounds == null || bounds.size() == 0) {
            bounds = new ArrayList<ReferenceType>();
            bounds.add(ts.Object());
        }
        return bounds;
    }

    public String translate(Resolver c) {
        return "Intersection Type";
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();//("intersection[ ");
        for (Iterator<ReferenceType> iter = bounds.iterator(); iter.hasNext();) {
            ReferenceType b = iter.next();
            sb.append(b);
            if (iter.hasNext())
                sb.append(" & ");
        }
        //sb.append(" ]");
        return sb.toString();
    }

    protected List<ReferenceType> getConcreteBounds() {
        if (concreteBounds == null) {
            concreteBounds = ((JL5TypeSystem) typeSystem()).concreteBounds(this.bounds());
        }
        return concreteBounds;
    }

    public Type superType() {
        return getSyntheticClass().superType();
    }

    public ReferenceType toReference() {
        return this;
    }

    public boolean isReference() {
        return true;
    }

    @Override
    public List constructors() {
        return Collections.emptyList();
    }

    protected ParsedClassType syntheticClass = null;

    protected ClassType getSyntheticClass() {
        if (syntheticClass == null) {
            syntheticClass = typeSystem().createClassType();
            ArrayList<ReferenceType> onlyClasses = new ArrayList<ReferenceType>();
            for (ReferenceType t : getConcreteBounds()) {
                if (t.isClass() && ((ClassType)t).flags().isInterface())
                    syntheticClass.addInterface(t);
                else
                    onlyClasses.add(t);
            }
            if (onlyClasses.size() > 0) {
                Collections.sort(onlyClasses, new Comparator<ReferenceType>() {
                    public int compare(ReferenceType o1, ReferenceType o2) {
                        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
                        if (ts.equals(o1, o2))
                            return 0;
                        if (ts.isSubtype(o1, o2))
                            return -1;
                        return 1;
                    }
                });
                syntheticClass.superType(onlyClasses.get(0));
            }
            syntheticClass.package_(this.package_());
        }
        return syntheticClass;
    }

    @Override
    public List fields() {
        return getSyntheticClass().fields();
    }

    @Override
    public Flags flags() {
        return getSyntheticClass().flags();
    }

    @Override
    public List interfaces() {
        return getSyntheticClass().interfaces();
    }

    @Override
    public Kind kind() {
        return INTERSECTION;
    }

    @Override
    public List memberClasses() {
        return Collections.emptyList();
    }

    @Override
    public List methods() {
        return getSyntheticClass().methods();
    }

    @Override
    public String name() {
        return this.toString();
    }

    @Override
    public ClassType outer() {
        return null;
    }

    @Override
    public Package package_() {
        if (boundOf() != null) 
            return boundOf().package_();
        return null;
    }

    public boolean inStaticContext() {
        return false;
    }

    @Override
    public boolean isImplicitCastValidImpl(Type toType) {
        for (Type b : bounds()) {
            if (typeSystem().isImplicitCastValid(b, toType)) return true;
        }
        //or just isImplicitCastValid(getSyntaticClass(), toType());
        return false;
    }

    @Override
    public boolean isSubtypeImpl(Type ancestor) {
        for (Type b : bounds()) {
            if (typeSystem().isSubtype(b, ancestor)) return true;
        }
        return false;
    }

    @Override
    public boolean isCastValidImpl(Type toType) {
        for (Type b : bounds()) {
            if (typeSystem().isCastValid(b, toType)) return true;
        }
        return false;
    }

    public void boundOf(TypeVariable tv) {
        boundOf_ = tv;
    }

    public TypeVariable boundOf() {
        return boundOf_;
    }
    
    public boolean equalsImpl(TypeObject other) {
        if (!super.equalsImpl(other)) {
            if (other instanceof ReferenceType) {
                return typeSystem().isSubtype(this, (Type) other) && typeSystem().isSubtype((Type) other, this);
            }
        }
        return true;
    }
    
 

}
