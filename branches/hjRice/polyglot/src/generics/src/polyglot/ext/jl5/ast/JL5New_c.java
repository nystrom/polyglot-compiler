package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c.ConstructorMatcher;
import polyglot.types.Types;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

public class JL5New_c extends New_c implements JL5New  {

    protected List typeArguments;

    public JL5New_c(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body,
            List<TypeNode> typeArguments) {
        super(pos, qualifier, tn, arguments, body);
        this.typeArguments = typeArguments;
    }

    public List<TypeNode> typeArguments() {
        return typeArguments;
    }

    public JL5New typeArguments(List args) {
        JL5New_c n = (JL5New_c) copy();
        n.typeArguments = args;
        return n;
    }

    /** Reconstruct the expression. */
    protected JL5New_c reconstruct(Expr qualifier, TypeNode tn, List arguments, ClassBody body,
            List typeArgs) {
        if (qualifier != this.qualifier || tn != this.tn
                || !CollectionUtil.allEqual(arguments, this.arguments) || body != this.body
                || !CollectionUtil.allEqual(typeArgs, this.typeArguments)) {
            JL5New_c n = (JL5New_c) copy();
            n.tn = tn;
            n.qualifier = qualifier;
            n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
            n.body = body;
            n.typeArguments = TypedList.copyAndCheck(typeArgs, TypeNode.class, false);
            return n;
        }
        return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
        Expr qualifier = (Expr) visitChild(this.qualifier, v);
        TypeNode tn = (TypeNode) visitChild(this.tn, v);
        List arguments = visitList(this.arguments, v);
        ClassBody body = (ClassBody) visitChild(this.body, v);
        List<TypeNode> typeArgs = visitList(this.typeArguments, v);
        return reconstruct(qualifier, tn, arguments, body, typeArgs);
    }


    /**
     * @param ar
     * @param ct
     * @throws SemanticException
     */
    @Override
    protected New findQualifier(TypeChecker ar, ClassType ct) throws SemanticException {
        // If we're instantiating a non-static member class, add a "this"
        // qualifier.
        NodeFactory nf = ar.nodeFactory();
        TypeSystem ts = ar.typeSystem();
        Context c = ar.context();

        // Search for the outer class of the member.  The outer class is
        // not just ct.outer(); it may be a subclass of ct.outer().
        Type outer = null;
        
        Name name = ct.name();
        ClassType t = c.currentClass();
        
        // We're in one scope too many.
        if (t == anonType) {
            t = t.outer();
        }
        
        // Search all enclosing classes for the type.
        while (t != null) {
            try {
                Type mt = ts.findMemberType(t, name, c);

                if (mt instanceof ClassType) {
                    ClassType cmt = (ClassType) mt;
                    //CHECK class definition should be the same whether or not the type has been parameterized
                    //IF that's true we don't need to override this method
                    if (cmt.def() == ct.def()) {
                    	//JL5
                        //|| (t instanceof ParameterizedType && ts.equals(mt, ((ParameterizedType) c).baseType()))) {
                    	//JL5-
                    	outer = t;
                    	break;
                    }
                }
            }
            catch (SemanticException e) {
            }
            
            t = t.outer();
        }
        
        if (outer == null) {
            throw new SemanticException("Could not find non-static member class \"" +
                                        name + "\".", position());
        }
        
        // Create the qualifier.
        Expr q;

        if (outer.typeEquals(c.currentClass(), ar.context())) {
            q = nf.This(position().startOf());
        }
        else {
            q = nf.This(position().startOf(),
                        nf.CanonicalTypeNode(position(), outer));
        }
        
        q = q.type(outer);
        return qualifier(q);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        //JL5
        if (tn.type().isClass()) {
            ClassType ct = (ClassType) tn.type();
            if (JL5Flags.isEnumModifier(ct.flags())) {
                throw new SemanticException("Cannot instantiate an enum type.", tn.position());
            }
        }
        if (tn.type() instanceof TypeVariable) {
            throw new SemanticException("Cannot instantiate a type variable type.", tn.position());
        }
    	//JL5-

        List<Type> argTypes = new ArrayList<Type>(arguments.size());
        
        for (Iterator<Expr> i = this.arguments.iterator(); i.hasNext(); ) {
            Expr e = i.next();
            argTypes.add(e.type());
        }

    	//JL5
        List<Type> explicitTypeArgs = null;
        if (typeArguments != null && !typeArguments.isEmpty()) {
            explicitTypeArgs = new ArrayList<Type>();
            for (Iterator<TypeNode> it = typeArguments().iterator(); it.hasNext();) {
                explicitTypeArgs.add((it.next()).type());
            }
        }
    	//JL5-

        typeCheckFlags(tc);
        typeCheckNested(tc);
        
        ClassType ct = tn.type().toClass();
        ConstructorInstance ci;
        
        if (! ct.flags().isInterface()) {
            Context c = tc.context();
            if (anonType != null) {
                c = c.pushClass(anonType, anonType.asType());
            }
            //JL5
            ConstructorMatcher matcher = ((JL5TypeSystem) ts).JL5ConstructorMatcher(ct, argTypes, explicitTypeArgs, c);
            //JL5-
            
            ci = ts.findConstructor(ct, matcher);
        }
        else {
            ConstructorDef dci = ts.defaultConstructor(this.position(), Types.<ClassType>ref(ct));
            ci = dci.asInstance();
        }
        
        New n = this.constructorInstance(ci);
        
        if (anonType != null) {
            // The type of the new expression is the anonymous type, not the base type.
            ct = anonType.asType();
        }

        return n.type(ct);
    }
}
