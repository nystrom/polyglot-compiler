package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ast.TypeNode_c;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.frontend.SetResolverGoal;
import polyglot.types.ArrayType;
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

public class ParamTypeNode_c extends TypeNode_c implements ParamTypeNode {

	/**
	 * The type variable name used
	 */
    protected String id;

    protected List bounds;

    public ParamTypeNode_c(Position pos, List bounds, String id) {
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

    public ParamTypeNode bounds(List l) {
        ParamTypeNode_c n = (ParamTypeNode_c) copy();
        n.bounds = l;
        return n;
    }

    public List bounds() {
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
        c = ((JL5Context) c).pushTypeVariable((TypeVariable) type());
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
    		List<Ref<? extends Type>> typeList = new ArrayList(bounds.size());
    		for (int i = 0; i < bounds.size(); i++) {
    			typeList.add((Ref<? extends Type>) typeRef(Types.lazyRef(ts.unknownType(position()), new SetResolverGoal(tb.job()))));
    		}
    		TypeVariable iType = ts.typeVariable(position(), id, typeList);
    		return typeRef(Types.ref(iType));
    	} else {
    		return this;
    	}
    }

    //CHECK bypass bounds
//    public NodeVisitor disambiguateEnter(AmbiguityRemover ar) throws SemanticException {
//        if (ar.kind() == JL5AmbiguityRemover.TYPE_VARS) {
//            return ar.bypass(bounds);
//        }
//        else {
//            return super.disambiguateEnter(ar);
//        }
//    }

    public Node disambiguate(ContextVisitor ar) throws SemanticException {
    	// all of the children (bounds list) will have already been 
    	// disambiguated and should there for be actual types
    	JL5TypeSystem ts = (JL5TypeSystem) ar.typeSystem();

    	//CHECK AmbiguityRemove.ALL
    	//        if (ar.kind() == AmbiguityRemover.ALL) {
    	ArrayList<ClassType> typeList = new ArrayList<ClassType>();
    	for (Iterator<TypeNode> it = bounds.iterator(); it.hasNext();) {
    		TypeNode tn = it.next();
    		Type t = tn.type();
    		if (t instanceof ClassType)
    			typeList.add((ClassType) t);
    		else
    			throw new SemanticException("Unexpected type bound in type variable declaration", tn.position());
    	}
    	TypeVariable tv = (TypeVariable) type();
    	tv.bounds(typeList);
    	//note: bounds are checked in typeCheck()
    	//        }
    	return this;
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
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

        return super.typeCheck(tc);
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
