package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.ast.Call_c;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

public class JL5Call_c extends Call_c implements JL5Call {

    protected List typeArguments;

    public JL5Call_c(Position pos, Receiver target, String name, List arguments, List typeArguments) {
        super(pos, target, name, arguments);
        this.typeArguments = typeArguments;
    }

    public List typeArguments() {
        return typeArguments;
    }

    public JL5Call typeArguments(List args) {
        JL5Call_c n = (JL5Call_c) copy();
        n.typeArguments = args;
        return n;
    }

    protected JL5Call_c reconstruct(Receiver target, List arguments, List typeArgs) {
        if (target != this.target || !CollectionUtil.equals(arguments, this.arguments)
                || !CollectionUtil.equals(typeArgs, this.typeArguments)) {
            JL5Call_c n = (JL5Call_c) copy();
            n.target = target;
            n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
            n.typeArguments = TypedList.copyAndCheck(typeArgs, TypeNode.class, false);
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v) {
        Receiver target = (Receiver) visitChild(this.target, v);
        List arguments = visitList(this.arguments, v);
        List typeArgs = visitList(this.typeArguments, v);
        return reconstruct(target, arguments, typeArgs);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JL5Call_c n = null;
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        JL5Context c = (JL5Context) tc.context();
        ReferenceType targetType = null;
        List<Type> explicitTypeArgs = null;
        List<Type> paramTypes = new ArrayList<Type>();

        if (typeArguments != null && !typeArguments.isEmpty()) {
            explicitTypeArgs = new ArrayList<Type>();
            if (target() == null) {
                // should not actually happen. grammar doesn't allow it
                throw new SemanticException("Explicit target required when using explicit type arguments", position());
            }
            for (Iterator it = typeArguments().iterator(); it.hasNext();) {
                explicitTypeArgs.add(((TypeNode) it.next()).type());
            }
        }

        for (Iterator i = this.arguments().iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            paramTypes.add(e.type());
        }

        JL5MethodInstance mi;
        // JLS 15.12.1
        if (target == null) {
            return typeCheckNullTarget(tc, paramTypes, explicitTypeArgs);
        } else {
            targetType = this.findTargetType();
            mi = ts.findJL5Method(targetType, name, paramTypes, explicitTypeArgs, c);
        }


        boolean staticContext = (this.target instanceof TypeNode);

        if (staticContext && !mi.flags().isStatic()) {
            throw new SemanticException("Cannot call non-static method " + this.name + " of "
                    + targetType + " in static " + "context.", this.position());
        }

        if (this.target instanceof Special && ((Special) this.target).kind() == Special.SUPER
                && mi.flags().isAbstract()) {
            throw new SemanticException("Cannot call an abstract method " + "of the super class", this.position());
        }

        n = (JL5Call_c) this.methodInstance(mi).type(mi.returnType());
        //n.checkConsistency(c);
        return n;
    }

    protected Node typeCheckNullTarget(TypeChecker tc, List<Type> paramTypes, List<Type> explicitTypeArgs)
            throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        JL5NodeFactory nf = (JL5NodeFactory) tc.nodeFactory();
        JL5Context c = (JL5Context) tc.context();

        // the target is null, and thus implicit
        // let's find the target, using the context, and
        // set the target appropriately, and then type check
        // the result
        JL5MethodInstance mi = c.findJL5Method(this.name(), paramTypes, explicitTypeArgs);

        Receiver r;
        if (mi.flags().isStatic()) {
            r = nf.CanonicalTypeNode(position(), mi.container()).type(mi.container());
        } else {
            // The method is non-static, so we must prepend with "this", but we
            // need to determine if the "this" should be qualified. Get the
            // enclosing class which brought the method into scope. This is
            // different from mi.container(). mi.container() returns a super
            // type
            // of the class we want.
            ClassType scope = c.findMethodScope(name);

            if (!ts.equals(scope, c.currentClass())) {
                r = nf.This(position(), nf.CanonicalTypeNode(position(), scope)).type(scope);
            } else {
                r = nf.This(position()).type(scope);
            }
        }

        // we call typeCheck on the reciever too.
        r = (Receiver) r.del().typeCheck(tc);
        return this.targetImplicit(true).target(r).del().typeCheck(tc);
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
