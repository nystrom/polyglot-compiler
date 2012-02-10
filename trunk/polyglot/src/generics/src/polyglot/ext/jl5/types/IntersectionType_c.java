package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.Name;
import polyglot.types.ParsedClassType;
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
	protected ClassDef syntheticDef;

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
	public ClassType getSyntheticClassType() {
		if (syntheticDef == null) {
			List<ClassType> cb = (List) this.getConcreteBounds();
			List<ClassType> classesList = new ArrayList<ClassType>(cb.size());
			List<ClassType> interfacesList = new ArrayList<ClassType>(cb.size());        	
			//CHECK No name for that ?

			// First filter classes
			for (ClassType t : cb) {
				if (t.isClass() && t.flags().isInterface()) {
					interfacesList.add(t);
				} else  {
					classesList.add(t);
				}

			}
			ClassDef def = ts.createClassDef();
			def.name(Name.make(this.toString()));

			// Lookup for the most specific class
			if (!classesList.isEmpty()) {
				Collections.sort(classesList, new Comparator<ClassType>() {
					public int compare(ClassType o1, ClassType o2) {
						JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
						if (ts.typeEquals(o1, o2, null))
							return 0;
						if (ts.isSubtype(o1, o2, null))
							return -1;
						return 1;
					}
				});
				def.superType(Types.ref(classesList.get(0)));
			}
			
			// Lookup for interfaces
			if (!interfacesList.isEmpty()) {
				// Need to create a synthetic def being the type union of all interfaces
				for (ClassType iType : interfacesList) {
					def.addInterface(Types.ref(iType));
				}
			}
			// CHECK we are missing the package here because
			// in the previous implementation Intersection were
			// extending ClassType and not Type
			// syntheticDef.setPackage(this.package_())
			syntheticDef = def;
		}
		// Getting the type from the copied definition

		return syntheticDef.asType();
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

	@Override
	public List<Type> boundsTypes() {
		return new TransformingList<Ref<? extends Type>, Type>(this.bounds(), new polyglot.types.DerefTransform<Type>());
	}
}
