package jltools.ext.covarRet;

import jltools.ext.jl.ast.ClassBody_c;
import jltools.ast.*;
import jltools.types.*;
import jltools.visit.*;
import jltools.util.*;
import jltools.frontend.*;
import java.util.*;

/**
 * A <code>ClassBody</code> represents the body of a class or interface
 * declaration or the body of an anonymous class.
 *
 * The covariant return extension overrides the overrideMethodCheck method
 * to allow the return type of a method to be a subclass of the return type
 * declared in a superclass.
 */
public class CovarRetClassBody_c extends ClassBody_c
{
    public CovarRetClassBody_c(Del ext, Position pos, List members) {
        super(ext, pos, members);
    }

    protected void overrideMethodCheck(TypeChecker tc) throws SemanticException {
        ClassType type = tc.context().currentClass();
        TypeSystem ts = tc.typeSystem();

        for (Iterator i = type.methods().iterator(); i.hasNext(); ) {
            MethodInstance mi = (MethodInstance) i.next();

            Type t = type.superType();

            while (t instanceof ReferenceType) {
                ReferenceType rt = (ReferenceType) t;
                t = rt.superType();

                for (Iterator j = rt.methods().iterator(); j.hasNext(); ) {
                    MethodInstance mj = (MethodInstance) j.next();

                    if (! mi.name().equals(mj.name()) ||
                        ! ts.hasSameArguments(mi, mj) ||
                        ! ts.isAccessible(mj, tc.context())) {

                        continue;
                    }

                    // This condition is the only change from the superclass!
                    if (! ts.isSubtype(mi.returnType(), mj.returnType())) {
                        throw new SemanticException("Cannot override " + mj + " in " + rt + " with " + mi + " in " + type + "; overridden method returns " + mi.returnType() + ", which is not a subtype of " + mj.returnType() + ".", mi.position());
                    }

                    if (! ts.throwsSubset(mi, mj)) {
                        throw new SemanticException("Cannot override " + mj + " in " + rt + " with " + mi + " in " + type + "; throws more exceptions than overridden method.", mi.position());
                    }

                    if (mi.flags().moreRestrictiveThan(mj.flags())) {
                        throw new SemanticException("Cannot override " + mj + " in " + rt + " with " + mi + " in " + type + "; overridden method is more restrictive.", mi.position());
                    }

                    if (! mi.flags().isStatic() && mj.flags().isStatic()) {
                        throw new SemanticException("Cannot override " + mj + " in " + rt + " with " + mi + " in " + type + "; overridden method is static.", mi.position());
                    }

                    if (mj.flags().isFinal()) {
                        throw new SemanticException("Cannot override " + mj + " in " + rt + " with " + mi + " in " + type + "; overridden method is final.", mi.position());
                    }
                }
            }
        }
    }
}