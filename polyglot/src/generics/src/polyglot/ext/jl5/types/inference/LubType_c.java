package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.ext.jl5.types.AnySubType;
import polyglot.ext.jl5.types.AnySuperType;
import polyglot.ext.jl5.types.AnyType;
import polyglot.ext.jl5.types.GenericTypeRef;
import polyglot.ext.jl5.types.IntersectionType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.JL5TypeSystem_c;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.RawType;
import polyglot.ext.jl5.types.Wildcard;
import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Type_c;
import polyglot.util.Position;

/**
 * The least upper bound type
 *
 */
public class LubType_c extends Type_c implements LubType {
	protected JL5TypeSystem ts;
	protected List<Type> lubElems;
	protected IntersectionType lubCalculated = null;
	
	public LubType_c(TypeSystem ts, List<Type> lubElems) {
		super(ts);
		this.lubElems = lubElems;
		this.ts = (JL5TypeSystem) ts;
	}

	public List<Type> lubElements() {
		return lubElems;
	}
	
	public IntersectionType calculateLub() {
		if (lubCalculated == null) {
			lubCalculated = lub_force();
		}
		return lubCalculated;
	}

	@Override
	public List<Type> bounds() {
		return calculateLub().boundsTypes();
	}

	private IntersectionType lub_force() {
		Set<Type> st = new HashSet<Type>();
		Set<Type> est = null;
		// for each lub elements
		for (Type u : lubElems) {
			// Get the list of all ancestors
			List<Type> u_supers = new ArrayList<Type>(ts.allAncestorsOf(u));
			st.addAll(u_supers);
			Set<Type> est_of_u = new HashSet<Type>();
			for (Type super_of_u : u_supers) {
				if (super_of_u instanceof GenericTypeRef) {
					// if an ancestor is generic, add the baseType to the set
					// The baseType is the Type with parameters
					GenericTypeRef g = (GenericTypeRef) super_of_u;
					est_of_u.add(g.baseType());
				} else {
					// not a generic, just add the ancestor to the set
					est_of_u.add(super_of_u);
				}
			}
			// 'est_of_u' contains all ancestors of 'u'
			if (est == null) {
				est = new HashSet<Type>();
				est.addAll(est_of_u);
			} else {
				// retain only elements in 'est' that are also in 'est_of_u'
				est.retainAll(est_of_u);
			}
		}
		// 'est' contains only common ancestors
		// to every class from the lubElems list

		// filter the 'est' to only retain most specific subtypes
		Set<Type> mec = new HashSet<Type>(est);
		for (Type e1 : est) {
			for (Type e2 : est) {
				if (!ts.typeEquals(e1,e2, null) && ts.isSubtype(e2, e1, null)) {
					mec.remove(e1);
					break;
				}
			}
		}
		List<Type> cand = new ArrayList<Type>();
		// For any element G of MEC that is a generic type declaration, 
		// define the relevant invocations of G, Inv(G)
		for (Type m : mec) {
			List<Type> inv = new ArrayList<Type>();
			// compare with every type in the all ancestors set
			for (Type t : st) {
				// if equals or basetype is, add to inv list
				if ( ts.typeEquals(m, t, null) || 
						( (t instanceof GenericTypeRef) && ((GenericTypeRef)t).baseType().typeEquals(m, null) ) ) {
					inv.add(t);
				}
			}
			// Add to the candidate list, lci(inv)
			cand.add((ClassType)lci(inv));
		}
		try {
			if (ts.checkIntersectionBounds(cand, true)) {
				return ts.intersectionType(ts.toRefTypes(cand));
			}
		} catch (SemanticException e) {
			//CHECK is this ok ?
		}

		// Make the bound implicitly to Object
		List l = Collections.singletonList(ts.Object());
		return ts.intersectionType(l);
	}

	/**
	 * Least containing invocation
	 * @param inv
	 * @return
	 */
	private Type lci(List<Type> inv) {
		Type first = inv.get(0);
		if (inv.size() == 1 || first instanceof RawType) return first;
		ParameterizedType res = (ParameterizedType) first;
		for (int i = 1; i < inv.size(); i++) {
			Type next = inv.get(i);
			if (next instanceof RawType) return next;
			List<Type> lcta_args = new ArrayList<Type>();
			ParameterizedType nextp = (ParameterizedType) next;
			for (int argi = 0; argi < res.typeArguments().size(); argi++) {
				Type a1 = res.typeArguments().get(argi);
				Type a2 = nextp.typeArguments().get(argi);
				lcta_args.add(lcta(a1,a2));
			}
			res = ts.parameterizedType(res.baseType());
			res.typeArguments(lcta_args);
		}
		return res;
	}

	private Type lcta(Type a1, Type a2) {
		if (!(a1 instanceof Wildcard)) {
			if (!(a2 instanceof Wildcard)) {
				if (ts.typeEquals(a1, a2, null)) return a1;
				else return ts.anySubType((ClassType) ts.lubType(a1,a2));
			}
			else if (a2 instanceof Wildcard) {
				Wildcard a2wc = (Wildcard) a2;
				if (a2wc instanceof AnyType) return a2wc;
				if (a2wc instanceof AnySubType) return ts.anySubType((ClassType) ts.lubType(a1,a2wc.bound()));
				if (a2wc instanceof AnySuperType) return ts.anySuperType((ClassType) glb(a1,a2wc.bound()));
			}
		} else {
			Wildcard a1wc = (Wildcard) a1;
			if (!(a2 instanceof Wildcard)) return ts.lubType(a1wc.bound(), a2);
			Wildcard a2wc = (Wildcard) a2;
			if ((a1wc instanceof AnyType) || (a2wc instanceof AnyType)) return ts.anyType();
			if ((a1wc instanceof AnySubType) && (a2wc instanceof AnySubType)) return ts.anySubType((ClassType) ts.lubType(a1wc.bound(), a2wc.bound()));
			if ((a1wc instanceof AnySuperType) && (a2wc instanceof AnySuperType)) return ts.anySuperType((ClassType) glb(a1wc.bound(), a2wc.bound()));
			if (ts.typeEquals(a1wc.bound(),a2wc.bound(), null)) return a1wc.bound();
			return ts.anyType();
		}
		return ts.anyType();
	}

	private Type glb(Type t1, Type t2) {
		List<Type> l = new ArrayList<Type>();
		l.add(t1);
		l.add(t2);
		try {
			if (!ts.checkIntersectionBounds(l, true)) {
				return ts.Object();
			}
			else {
				return ts.intersectionType(ts.toRefTypes(l));
			}
		} catch (SemanticException e) {
			return ts.Object();
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("lub(");
		sb.append(JL5TypeSystem_c.listToString(lubElems));
		sb.append(")");
		return sb.toString();
	}

	@Override
	public String translate(Resolver c) {
		// TODO Auto-generated method stub
		return null;
	}
}
