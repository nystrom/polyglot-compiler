package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ast.TypeNode_c;
import polyglot.ext.jl5.types.JL5ClassDef_c;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.ArrayType;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

/**
 * Represents a parameterized TypeNode declaration.
 * 
 * For example '<T>' in: 
 * 
 * 		class Test<T> { }
 * 
 * or '<T>' in:
 * 
 * 		<T> void mth() { }
 * 
 * Note that this is different from a 'use' of a parameterized type
 * such as in:
 * 
 * 		Collection<String> l;
 * 
 * Those are first represented as ambiguous type node and later disambiguated 
 * to canonical type nodes having a ParameterizedType.
 */
public class ParamTypeNode_c extends TypeNode_c 
implements ParamTypeNode {

	/**
	 * The type variable name used
	 */
    protected String id;

    protected List<TypeNode> bounds;

    public ParamTypeNode_c(Position pos, List<TypeNode> bounds, String id) {
        super(pos);
        this.id = id;
        this.bounds = bounds;
    }

    public ParamTypeNode id(String id) {
        ParamTypeNode_c n = (ParamTypeNode_c) copy();
        n.id = id;
        return n;
    }

    public String id() {
        return this.id;
    }

    public ParamTypeNode bounds(List<TypeNode> l) {
        ParamTypeNode_c n = (ParamTypeNode_c) copy();
        n.bounds = l;
        return n;
    }

    public List<TypeNode> bounds() {
        return bounds;
    }

    public ParamTypeNode reconstruct(List bounds) {
        if (!CollectionUtil.allEqual(bounds, this.bounds)) {
            ParamTypeNode_c n = (ParamTypeNode_c) copy();
            n.bounds = bounds;
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v) {
        List bounds = visitList(this.bounds, v);
        return reconstruct(bounds);
    }

    public Context enterScope(Context c) {
        c = ((JL5Context) c).pushTypeVariable((TypeVariable) type.get());
        return super.enterScope(c);
    }

    public void addDecls(Context c) {
        ((JL5Context) c).addTypeVariable(Name.make(id()), Types.ref((TypeVariable) type()));
    }

    /**
     * Build the type representation of a ParamTypeNode
     * This is called when visiting a ClassDecl(aration) for example
     */
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
    	if (type == null) {
    		// makes a new TypeVariable with a list of bounds which are unknown types
    		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();

    		// Create a ClassDef for this TV
    		// CHECK not sure creating a class def for a paramtypenode is the right design decision 
    		Ref<? extends ClassDef> tvDef = Types.ref(new JL5ClassDef_c(ts));
    		List<Ref<? extends Type>> boundsTypeRef;
    		if (hasBounds()) {
    			// Getting bounds' type refs
    			boundsTypeRef = new ArrayList(this.bounds().size());
    			for (TypeNode tn : this.bounds()) {
    				boundsTypeRef.add(tn.typeRef());
    			}
    		} else {
    			boundsTypeRef = Collections.emptyList();
    		}

    		// Creating a type to represent this typenode
    		TypeVariable iType = ts.typeVariable(position(), Name.make(id), tvDef, boundsTypeRef);
    		return this.typeRef(Types.ref(iType));
    	} else {
    		return this;
    	}
    }
    
    public boolean hasBounds() {
    	return !bounds.isEmpty();
    }

    public Node disambiguate(ContextVisitor ar) throws SemanticException {
    	// don't need to build the type variable with resolved bounds since
    	// in polyglot 3 we can build references to types at 'build type' time.
//    	ArrayList<Type> typeList = new ArrayList<Type>();
//    	for (Iterator<TypeNode> it = bounds.iterator(); it.hasNext();) {
//    		TypeNode tn = it.next();
//    		Type t = tn.type();
//    		if (t instanceof ClassType)
//    			typeList.add(t);
//    		else
//    			throw new SemanticException("Unexpected type bound in type variable declaration", tn.position());
//    	}
//    	TypeVariable tv = (TypeVariable) type();
//    	tv.bounds(typeList);
    	return this;
    }

    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {

    	TypeVariable tv = (TypeVariable) type();
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        for (int i = 0; i < bounds.size(); i++) {
            TypeNode ti = (TypeNode) bounds.get(i);
            for (int j = i + 1; j < bounds.size(); j++) {
                TypeNode tj = (TypeNode) bounds.get(j);
                if (tc.typeSystem().typeEquals(ti.type(), tj.type(), tc.context())) {
                    throw new SemanticException("Duplicate bound in type variable declaration", tj.position());
                }
            }
        }
        // check no arrays in bounds list
        for (int i = 0; i < bounds.size(); i++) {
            TypeNode ti = (TypeNode) bounds.get(i);
            if (ti.type() instanceof ArrayType) {
                throw new SemanticException("Unexpected type bound in type variable declaration", ti.position());

            }
        }
        // only first bound can be a class otherwise must be interfaces
        for (int i = 0; i < bounds.size(); i++) {
            TypeNode tn = (TypeNode) bounds.get(i);
            if (i > 0 && !((ClassType) tn.type()).flags().isInterface()) {
                throw new SemanticException("Interface expected here.", tn.position());
            }
        }
        ts.checkIntersectionBounds(tv.bounds(), false);
        return super.conformanceCheck(tc);
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write(id);
        if (bounds() != null && !bounds().isEmpty()) {
            w.write(" extends ");
            for (Iterator it = bounds.iterator(); it.hasNext();) {
                TypeNode tn = (TypeNode) it.next();
                print(tn, w, tr);
                if (it.hasNext()) {
                    w.write(" & ");
                }
            }
        }
    }
}
