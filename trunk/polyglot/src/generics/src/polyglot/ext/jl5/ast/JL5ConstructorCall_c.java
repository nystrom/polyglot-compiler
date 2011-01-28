package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.ConstructorCall_c;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

public class JL5ConstructorCall_c extends ConstructorCall_c implements JL5ConstructorCall {

    protected List typeArguments;

    public JL5ConstructorCall_c(Position pos, Kind kind, Expr qualifier, List arguments,
            List typeArguments) {
        super(pos, kind, qualifier, arguments);
        this.typeArguments = typeArguments;
    }

    public List typeArguments() {
        return typeArguments;
    }

    public JL5ConstructorCall typeArguments(List args) {
        JL5ConstructorCall_c n = (JL5ConstructorCall_c) copy();
        n.typeArguments = args;
        return n;
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        JL5Context c = (JL5Context) tc.context();

        ClassType ct = c.currentClass();
        Type superType = ct.superType();

        // The qualifier specifies the enclosing instance of this inner class.
        // The type of the qualifier must be the outer class of this
        // inner class or one of its super types.
        //
        // Example:
        //
        // class Outer {
        //     class Inner { }
        // }
        //
        // class ChildOfInner extends Outer.Inner {
        //     ChildOfInner() { (new Outer()).super(); }
        // }
        if (qualifier != null) {
            if (kind != SUPER) {
                throw new SemanticException("Can only qualify a \"super\""
                        + "constructor invocation.", position());
            }

            if (!superType.isClass() || !superType.toClass().isInnerClass()
                    || superType.toClass().inStaticContext()) {
                throw new SemanticException("The class \"" + superType + "\""
                        + " is not an inner class, or was declared in a static "
                        + "context; a qualified constructor invocation cannot " + "be used.", position());
            }

            Type qt = qualifier.type();

            if (!qt.isClass() || !qt.isSubtype(superType.toClass().outer())) {
                throw new SemanticException("The type of the qualifier " + "\"" + qt
                        + "\" does not match the immediately enclosing "
                        + "class  of the super class \"" + superType.toClass().outer() + "\".", qualifier.position());
            }
        }

        if (kind == SUPER) {
            if (!superType.isClass()) {
                throw new SemanticException("Super type of " + ct + " is not a class.", position());
            }

            // If the super class is an inner class (i.e., has an enclosing
            // instance of its container class), then either a qualifier 
            // must be provided, or ct must have an enclosing instance of the
            // super class's container class, or a subclass thereof.
            if (qualifier == null && superType.isClass() && superType.toClass().isInnerClass()) {
                ClassType superContainer = superType.toClass().outer();
                // ct needs an enclosing instance of superContainer, 
                // or a subclass of superContainer.
                ClassType e = ct;

                while (e != null) {
                    if (e.isSubtype(superContainer) && ct.hasEnclosingInstance(e)) {
                        break;
                    }
                    e = e.outer();
                }

                if (e == null) {
                    throw new SemanticException(ct + " must have an enclosing instance"
                            + " that is a subtype of " + superContainer, position());
                }
                if (e == ct) {
                    throw new SemanticException(ct + " is a subtype of " + superContainer
                            + "; an enclosing instance that is a subtype of " + superContainer
                            + " must be specified in the super constructor call.", position());
                }
            }

            ct = ct.superType().toClass();
        }

        List<Type> explicitTypeArgs = null;
        List<Type> paramTypes = new ArrayList<Type>();

        if (typeArguments != null && !typeArguments.isEmpty()) {
            explicitTypeArgs = new ArrayList<Type>();
            for (Iterator it = typeArguments().iterator(); it.hasNext();) {
                explicitTypeArgs.add(((TypeNode) it.next()).type());
            }
        }

        for (Iterator i = this.arguments().iterator(); i.hasNext();) {
            Expr e = (Expr) i.next();
            paramTypes.add(e.type());
        }

        ConstructorInstance ci = ts.findJL5Constructor(ct, paramTypes, explicitTypeArgs, c);

        return constructorInstance(ci);
    }
/*
    private Node checkTypeArguments(TypeChecker tc, JL5ConstructorCall_c n)
            throws SemanticException {

        // can only call a method with type args if it was declared as generic
        if (!typeArguments.isEmpty()
                && !((JL5ConstructorInstance) n.constructorInstance()).isGeneric()) {
            throw new SemanticException("Cannot invoke instructor " + this + " with type arguments", position());
        }

        if (!typeArguments().isEmpty()
                && typeArguments.size() != ((JL5ConstructorInstance) n.constructorInstance()).typeVariables().size()) {
            throw new SemanticException("Cannot invoke instructor " + this
                    + " with wrong number of type arguments", position());
        }

        JL5ConstructorInstance ci = (JL5ConstructorInstance) n.constructorInstance();
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();

        // wildcards are not allowed for type args for generic new
        for (int i = 0; i < typeArguments.size(); i++) {
            TypeNode correspondingArg = (TypeNode) typeArguments.get(i);
            if (correspondingArg instanceof BoundedTypeNode) {
                throw new SemanticException("Wilcard argument not allowed here", correspondingArg.position());
            }
        }

        if (!typeArguments.isEmpty()) {
            for (int i = 0; i < typeArguments().size(); i++) {
                Type arg = ((TypeNode) typeArguments().get(i)).type();
                Type decl = (Type) ci.typeVariables().get(i);

                for (int j = 0; j < ci.formalTypes().size(); j++) {
                    Type formal = (Type) ci.formalTypes().get(j);
                    Type argType = ((Expr) arguments().get(j)).type();

                    if (ts.equals(formal, decl)) {
                        if (!ts.isImplicitCastValid(argType, arg)) {
                            throw new SemanticException("Found arg of type: " + argType
                                    + " expected: " + arg, ((Expr) arguments().get(j)).position());
                        }
                    }
                }
            }
        }

        // type check arguments
        //         if (qualifier() != null && qualifier().type() instanceof ParameterizedType){
        //             for (int i = 0; i < ci.formalTypes().size(); i++){
        //                 Type t = (Type)ci.formalTypes().get(i);
        //                 if (t instanceof TypeVariable){
        //                     Type other = ts.findRequiredType((TypeVariable)t, (ParameterizedType)qualifier().type());
        //                     if (!ts.isImplicitCastValid(((Expr)arguments().get(i)).type(), other)){
        //                         throw new SemanticException("Found arg of type: "+((Expr)arguments().get(i)).type()+" expected: "+other, ((Expr)arguments().get(i)).position());
        //                     }
        //                 }
        //             }

        //         }
        return n;
    }*/
}
