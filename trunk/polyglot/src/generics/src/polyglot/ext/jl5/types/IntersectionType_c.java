package polyglot.ext.jl5.types;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.types.Ref;
import polyglot.types.ReferenceType;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.Type_c;
import polyglot.types.Types;
import polyglot.util.TransformingList;

/**
 * JLS 4.9 Intersection Types
 * An intersection type takes the form T1 & ... & Tn, n>0, where Ti, 1<=i<=n, are type expressions.
 * Intersection types arise in the processes of capture conversion (¤5.1.10) and type inference (¤15.12.2.7)
 */
public class IntersectionType_c extends Type_c implements IntersectionType {
    protected List<Ref<? extends Type>> bounds;
    protected List<Type> concreteBounds;
    protected TypeVariable boundOf_;

    public IntersectionType_c(TypeSystem ts, List<Ref<? extends Type>> bounds) { 
        super(ts);
        this.bounds = bounds;    	
    }

    public List<Ref<? extends Type>> bounds() {
        if (bounds.isEmpty()) {
            bounds =  (List) Collections.singletonList(Types.ref(ts.Object()));
        }
        return bounds;
    }

    public String translate(Resolver c) {
        return "Intersection Type";
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();//("intersection[ ");
        for (Iterator<Ref<? extends Type>> iter = bounds.iterator(); iter.hasNext();) {
        	Ref<? extends Type> b = iter.next();
            sb.append(b.get());
            if (iter.hasNext())
                sb.append(" & ");
        }
        //sb.append(" ]");
        return sb.toString();
    }

    protected List<Type> getConcreteBounds() {
        if (concreteBounds == null) {
            concreteBounds = ((JL5TypeSystem) typeSystem()).concreteBounds(this.boundsTypes());
        }
        return concreteBounds;
    }

    /**
     * Updates the def according to the concrete bounds 
     * and return a type instance of the def. 
     * @return
     */
//    protected ClassType getSyntheticClass() {
//        if (syntheticClass == null) {
//        	ClassDef syntheticDef = def.get();
//            List<ClassType> onlyClasses = new ArrayList<ClassType>();
//            for (ClassType t : getConcreteBounds()) {
//                if (t.isClass() && t.flags().isInterface()) {
//                	syntheticDef.addInterface(Types.ref(t));
//                } else {
//                    onlyClasses.add(t);
//                }
//            }
//            if (onlyClasses.size() > 0) {
//                Collections.sort(onlyClasses, new Comparator<ClassType>() {
//                    public int compare(ClassType o1, ClassType o2) {
//                        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
//                        if (ts.typeEquals(o1, o2, null))
//                            return 0;
//                        if (ts.isSubtype(o1, o2, null))
//                            return -1;
//                        return 1;
//                    }
//                });
//                syntheticDef.superType(Types.ref(onlyClasses.get(0)));
//            }
//            syntheticDef.setPackage(Types.ref(this.package_()));
//        }
//    	// Getting the type from the copied definition
//        syntheticClass = (ParsedClassType) def.get().asType();
//
//        return syntheticClass;
//    }
//
//    @Override
//    public Kind kind() {
//        return INTERSECTION;
//    }

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

	@Override
	public List<Type> boundsTypes() {
		return new TransformingList<Ref<? extends Type>, Type>(this.bounds(), new polyglot.types.DerefTransform<Type>());
	}
}
