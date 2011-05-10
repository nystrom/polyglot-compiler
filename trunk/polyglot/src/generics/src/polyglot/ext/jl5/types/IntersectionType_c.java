package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Package;
import polyglot.types.ParsedClassType;
import polyglot.types.Ref;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;

/**
 * JLS 4.9 Intersection Types
 * An intersection type takes the form T1 & ... & Tn, n>0, where Ti, 1<=i<=n, are type expressions.
 * Intersection types arise in the processes of capture conversion (¤5.1.10) and type inference (¤15.12.2.7)
 */
public class IntersectionType_c extends SyntheticClassType_c implements IntersectionType {
    protected List<ClassType> bounds;
    protected List<ClassType> concreteBounds;
    protected TypeVariable boundOf_;

    public IntersectionType_c(TypeSystem ts, Position pos, Ref<? extends ClassDef> def, List<ClassType> bounds) {
    	// we copy the def 
        super(ts, pos, Types.ref((ClassDef)def.get().copy()));
        this.bounds = bounds;    	
    }

    public List<ClassType> bounds() {
        if (bounds == null || bounds.size() == 0) {
            bounds = new ArrayList<ClassType>();
            bounds.add((ClassType)ts.Object());
        }
        return bounds;
    }

    public String translate(Resolver c) {
        return "Intersection Type";
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();//("intersection[ ");
        for (Iterator<ClassType> iter = bounds.iterator(); iter.hasNext();) {
            ReferenceType b = iter.next();
            sb.append(b);
            if (iter.hasNext())
                sb.append(" & ");
        }
        //sb.append(" ]");
        return sb.toString();
    }

    protected List<ClassType> getConcreteBounds() {
        if (concreteBounds == null) {
            concreteBounds = ((JL5TypeSystem) typeSystem()).concreteBounds(this.bounds());
        }
        return concreteBounds;
    }

    @Override
    public Type superClass() {
        return getSyntheticClass().superClass();
    }

    @Override
    public List constructors() {
        return Collections.emptyList();
    }

    protected ParsedClassType syntheticClass = null;

    /**
     * Updates the def according to the concrete bounds 
     * and return a type instance of the def. 
     * @return
     */
    protected ClassType getSyntheticClass() {
        if (syntheticClass == null) {
        	ClassDef syntheticDef = def.get();
            List<ClassType> onlyClasses = new ArrayList<ClassType>();
            for (ClassType t : getConcreteBounds()) {
                if (t.isClass() && t.flags().isInterface()) {
                	syntheticDef.addInterface(Types.ref(t));
                } else {
                    onlyClasses.add(t);
                }
            }
            if (onlyClasses.size() > 0) {
                Collections.sort(onlyClasses, new Comparator<ClassType>() {
                    public int compare(ClassType o1, ClassType o2) {
                        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
                        if (ts.typeEquals(o1, o2, null))
                            return 0;
                        if (ts.isSubtype(o1, o2, null))
                            return -1;
                        return 1;
                    }
                });
                syntheticDef.superType(Types.ref(onlyClasses.get(0)));
            }
            syntheticDef.setPackage(Types.ref(this.package_()));
        }
    	// Getting the type from the copied definition
        syntheticClass = (ParsedClassType) def.get().asType();

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
    public Name name() {
        return Name.make(this.toString());
    }

    @Override
    public Package package_() {
        if (boundOf() != null) 
            return boundOf().package_();
        return null;
    }

    @Override
    public boolean inStaticContext() {
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
                return ts.isSubtype(this, (Type) other, null) && ts.isSubtype((Type) other, this, null);
            }
        }
        return true;
    }
}
