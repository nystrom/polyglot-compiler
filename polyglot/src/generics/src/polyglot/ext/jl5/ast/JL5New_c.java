package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ast.New_c;
import polyglot.ext.jl5.types.*;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

public class JL5New_c extends New_c implements JL5New  {

    protected List typeArguments;

    public JL5New_c(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body,
            List typeArguments) {
        super(pos, qualifier, tn, arguments, body);
        this.typeArguments = typeArguments;
    }

    public List typeArguments() {
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
                || !CollectionUtil.equals(arguments, this.arguments) || body != this.body
                || !CollectionUtil.equals(typeArgs, this.typeArguments)) {
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
        List typeArgs = visitList(this.typeArguments, v);
        return reconstruct(qualifier, tn, arguments, body, typeArgs);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (ar.kind() != AmbiguityRemover.ALL) {
            return this;
        }

        if (qualifier == null) {
            ClassType ct = tn.type().toClass();

            if (!ct.isMember() || ct.flags().isStatic()) {
                return this;
            }

            // If we're instantiating a non-static member class, add a "this"
            // qualifier.
            NodeFactory nf = ar.nodeFactory();
            TypeSystem ts = ar.typeSystem();
            Context c = ar.context();

            // Search for the outer class of the member.  The outer class is
            // not just ct.outer(); it may be a subclass of ct.outer().
            Type outer = null;

            String name = ct.name();
            ClassType t = c.currentClass();

            // We're in one scope too many.
            if (t == anonType) {
                t = t.outer();
            }

            while (t != null) {
                try {
                    // HACK: PolyJ outer() doesn't work
                    t = ts.staticTarget(t).toClass();
                    ClassType mt = ts.findMemberClass(t, name, c.currentClass());

                    if (ts.equals(mt, ct)
                            || (ct instanceof ParameterizedType && ts.equals(mt, ((ParameterizedType) ct).baseType()))) {
                        outer = t;
                        break;
                    }
                } catch (SemanticException e) {
                }

                t = t.outer();
            }

            if (outer == null) {
                throw new SemanticException("Could not find non-static member class \"" + name
                        + "\".", position());
            }

            // Create the qualifier.
            Expr q;

            if (outer.equals(c.currentClass())) {
                q = nf.This(position());
            }
            else {
                q = nf.This(position(), nf.CanonicalTypeNode(position(), outer));
            }

            return qualifier(q);
        }

        return this;
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        if (tn.type().isClass()) {
            ClassType ct = (ClassType) tn.type();
            if (JL5Flags.isEnumModifier(ct.flags())) {
                throw new SemanticException("Cannot instantiate an enum type.", tn.position());
            }
        }
        if (tn.type() instanceof TypeVariable) {
            throw new SemanticException("Cannot instantiate a type variable type.", tn.position());
        }
        JL5New_c n = this;

        if (qualifier != null) {
            // We have not disambiguated the type node yet.

            // Get the qualifier type first.
            Type qt = qualifier.type();

            if (!qt.isClass()) {
                throw new SemanticException("Cannot instantiate member class of a non-class type.", qualifier.position());
            }

            // Disambiguate the type node as a member of the qualifier type.
            TypeNode tn = disambiguateTypeNode(tc, qt.toClass());
            ClassType ct = tn.type().toClass();

            /*
             FIXME: check super types as well.
             if (! ct.isMember() || ! ts.isEnclosed(ct, qt.toClass())) {
             throw new SemanticException("Class \"" + qt +
             "\" does not enclose \"" + ct + "\".",
             qualifier.position());
             }
             */

            // According to JLS2 15.9.1, the class type being
            // instantiated must be inner.
            if (!ct.isInnerClass()) {
                throw new SemanticException("Cannot provide a containing instance for non-inner class "
                        + ct.fullName() + ".", qualifier.position());
            }

            n = (JL5New_c) n.objectType(tn);
        }
        else {
            ClassType ct = tn.type().toClass();

            if (ct.isMember()) {
                for (ClassType t = ct; t.isMember(); t = t.outer()) {
                    if (!t.flags().isStatic()) {
                        throw new SemanticException("Cannot allocate non-static member class \""
                                + t + "\".", position());
                    }
                }
            }
        }

        return n.typeCheckEpilogue(tc);
    }

    protected Node typeCheckEpilogue(TypeChecker tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();

        List<Type> paramTypes = new ArrayList<Type>(arguments.size());
        List<Type> explicitTypeArgs = null;
        
        for (Iterator i = this.arguments.iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            paramTypes.add(e.type());
        }
        
        if (typeArguments != null && !typeArguments.isEmpty()) {
            explicitTypeArgs = new ArrayList<Type>();
            for (Iterator it = typeArguments().iterator(); it.hasNext();) {
                explicitTypeArgs.add(((TypeNode) it.next()).type());
            }
        }

        ClassType ct = tn.type().toClass();

        if (this.body == null) {
            if (ct.flags().isInterface()) {
                throw new SemanticException("Cannot instantiate an interface.", position());
            }

            if (ct.flags().isAbstract()) {
                throw new SemanticException("Cannot instantiate an abstract class.", position());
            }
        }
        else {
            if (ct.flags().isFinal()) {
                throw new SemanticException("Cannot create an anonymous subclass of a final class.", position());
            }

            if (ct.flags().isInterface() && !arguments.isEmpty()) {
                throw new SemanticException("Cannot pass arguments to an anonymous class that "
                        + "implements an interface.", ((Expr) arguments.get(0)).position());
            }
        }

        if (!ct.flags().isInterface()) {
            Context c = tc.context();
            if (body != null) {
                // Enter the body of this class so we can access protected
                // super-constructors.

                // temporarily set the super type; we'll set it correctly below
                anonType.superType(ct);

                c = c.pushClass(anonType, anonType);
            }
            ci = ts.findJL5Constructor(ct, paramTypes, explicitTypeArgs, (JL5Context)c);
        }
        else {
            ci = ts.defaultConstructor(position(), ct);
        }

        JL5New_c n = (JL5New_c) this.constructorInstance(ci).type(ct);

        if (n.body == null) {
            return n;
        }

        // Now, need to read symbols, clean, disambiguate, and type check
        // the body.

        if (!ct.flags().isInterface()) {
            anonType.superType(ct);
        }
        else {
            anonType.superType(ts.Object());
            anonType.addInterface(ct);
        }

        // The type of the new expression is actually the anon type.
        n = (JL5New_c) n.type(anonType);

        // Now, run the four passes on the body.
        ClassBody body = n.typeCheckBody(tc, ct);

        return n.body(body);
    }
}
