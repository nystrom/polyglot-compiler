package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.Call_c;
import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.ContextVisitor;

public class JL5Call_c extends Call_c implements JL5Call {

    protected List<TypeNode> typeArguments;

    public JL5Call_c(Position pos, Receiver target, Id name, List<Expr> arguments, List<TypeNode> typeArguments) {
        super(pos, target, name, arguments);
        this.typeArguments = typeArguments;
    }

    public List<TypeNode> typeArguments() {
        return typeArguments;
    }

    public JL5Call typeArguments(List<TypeNode> args) {
        JL5Call_c n = (JL5Call_c) copy();
        n.typeArguments = TypedList.copyAndCheck(args, TypeNode.class, true);
        return n;
    }

    /** Reconstruct the call. */
    protected Call_c reconstruct(Receiver target, Id name, List<Expr> arguments, List<TypeNode> typeArguments) {
      if (target != this.target || name != this.name || 
    		  ! CollectionUtil.allEqual(arguments, this.arguments) || 
    		  ! CollectionUtil.allEqual(typeArguments, this.typeArguments)) {
          JL5Call_c n = (JL5Call_c) super.reconstruct(target, name, arguments);
          n.typeArguments = TypedList.copyAndCheck(typeArguments, TypeNode.class, true);    	  
      }
      return this;
    }

    public Node visitChildren(NodeVisitor v) {
    	JL5Call_c visited = (JL5Call_c) super.visitChildren(v);
        List<TypeNode> newTypeArguments = visited.visitList(visited.typeArguments, v);
        return visited.reconstruct(visited.target(), visited.name(), visited.arguments(), newTypeArguments);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5Call_c n = null;
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        JL5Context c = (JL5Context) tc.context();
        
        // explicitTypeArgs is the list of Type generated from the typeArguments list<TypeNode>
        List<Type> explicitTypeArgs = null;
        List<Type> argsTypes = new ArrayList<Type>();

        if (typeArguments != null && !typeArguments.isEmpty()) {
            explicitTypeArgs = new ArrayList<Type>();
            if (target() == null) {
                // should not actually happen. grammar doesn't allow it
                throw new SemanticException("Explicit target required when using explicit type arguments", position());
            }
            for (Iterator<TypeNode> it = typeArguments().iterator(); it.hasNext();) {
                explicitTypeArgs.add(it.next().type());
            }
        }

        for (Iterator<Expr> i = this.arguments().iterator(); i.hasNext();) {
            argsTypes.add(i.next().type());
        }

        // JLS 15.12.1
        if (target == null) {
            return typeCheckNullTarget(tc, argsTypes, explicitTypeArgs);
        }
        
        Type targetType = target.type();
        JL5MethodInstance mi = (JL5MethodInstance) ts.findMethod(targetType, ts.JL5MethodMatcher(targetType, name().id(), argsTypes, explicitTypeArgs, c));

        /* This call is in a static context if and only if
         * the target (possibly implicit) is a type node.
         */
        boolean staticContext = (this.target instanceof TypeNode);

        if (staticContext && !mi.flags().isStatic()) {
            throw new SemanticException("Cannot call non-static method " + this.name.id()
                                  + " of " + target.type() + " in static "
                                  + "context.", this.position());
        }
        
        // If the target is super, but the method is abstract, then complain.
        if (this.target instanceof Special && 
            ((Special)this.target).kind() == Special.SUPER &&
            mi.flags().isAbstract()) {
                throw new SemanticException("Cannot call an abstract method " +
                               "of the super class", this.position());            
        }

        n = (JL5Call_c) this.methodInstance(mi).type(mi.returnType());
        //n.checkConsistency(c);
        return n;
    }

    protected Node typeCheckNullTarget(ContextVisitor tc, List<Type> paramTypes, List<Type> explicitTypeArgs)
            throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        JL5NodeFactory nf = (JL5NodeFactory) tc.nodeFactory();
        JL5Context c = (JL5Context) tc.context();

        // the target is null, and thus implicit
        // let's find the target, using the context, and
        // set the target appropriately, and then type check
        // the result
        JL5MethodInstance mi = (JL5MethodInstance) c.findMethod(ts.JL5MethodMatcher(null, name.id(), paramTypes, explicitTypeArgs, c));

        Receiver r;
        if (mi.flags().isStatic()) {
            Type container = findContainer(ts, mi);
            r = nf.CanonicalTypeNode(position().startOf(), container).typeRef(Types.ref(container));
        } else {
        	// The method is non-static, so we must prepend with "this", but we
        	// need to determine if the "this" should be qualified. Get the
        	// enclosing class which brought the method into scope. This is
        	// different from mi.container(). mi.container() returns a super
        	// type
        	// of the class we want.
        	ClassType scope = c.findMethodScope(name.id());

        	if (!ts.typeEquals(scope, c.currentClass(), c)) {
        		r = (Special) nf.This(position().startOf(),
        				nf.CanonicalTypeNode(position().startOf(), scope)).del().typeCheck(tc);
        	} else {
        		r = (Special) nf.This(position().startOf()).del().typeCheck(tc);
        	}
        }

        // we call computeTypes on the reciever too.
        Call_c call = (Call_c) this.targetImplicit(true).target(r);       
        call = (Call_c)call.methodInstance(mi).type(mi.returnType());
        return call;
    }
    
/*
    private Node checkTypeArguments(TypeChecker tc, JL5Call_c n) throws SemanticException {
        JL5MethodInstance mi = (JL5MethodInstance) n.methodInstance();
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();

        // can only call a method with type args if it was declared as generic
        if (!typeArguments.isEmpty() && !mi.isGeneric()) {
            throw new SemanticException("Cannot call method: " + mi.name() + " with type arguments", position());
        }

        if (!typeArguments().isEmpty() && typeArguments.size() != mi.typeVariables().size()) {
            throw new SemanticException("Cannot call " + n.name()
                    + " with wrong number of type arguments", position());
        }

        // wildcards are not allowed for type args for generic call
        for (int i = 0; i < typeArguments.size(); i++) {
            TypeNode correspondingArg = (TypeNode) typeArguments.get(i);
            if (correspondingArg instanceof BoundedTypeNode) {
                throw new SemanticException("Wildcard argument not allowed here", correspondingArg.position());
            }
        }
        return n;
    }
*/
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (!targetImplicit)
            sb.append(target.toString());

        if ((typeArguments != null) && typeArguments.size() != 0) {
            sb.append("<");
            for (Iterator it = typeArguments.iterator(); it.hasNext();) {
                sb.append(it.next().toString());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }

        if (!targetImplicit)
            sb.append(".");

        sb.append(name);
        sb.append("(");

        int count = 0;

        for (Iterator i = arguments.iterator(); i.hasNext();) {
            if (count++ > 2) {
                sb.append("...");
                break;
            }

            Expr n = (Expr) i.next();
            sb.append(n.toString());

            if (i.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(")");
        return sb.toString();
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (!targetImplicit) {
            if (target instanceof Expr) {
                printSubExpr((Expr) target, w, tr);
                w.write(".");
            } else if (target != null) {
                print(target, w, tr);
                w.write(".");
            }
        }

        if (typeArguments.size() != 0) {
            w.write("<");
            for (Iterator it = typeArguments.iterator(); it.hasNext();) {
                print((TypeNode) it.next(), w, tr);
                if (it.hasNext()) {
                    w.write(", ");
                }
            }
            w.write(">");
        }

        w.write(name + "(");
        w.begin(0);

        for (Iterator i = arguments.iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            print(e, w, tr);
            if (i.hasNext()) {
                w.write(",");
                w.allowBreak(0, " ");
            }
        }
        w.end();
        w.write(")");
    }
}
