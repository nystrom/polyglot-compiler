/*
 * Created on Feb 26, 2007
 */
/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package ibex.types;

import java.util.List;

import polyglot.types.CodeInstance;
import polyglot.types.ProcedureInstance;
import polyglot.types.Ref;
import polyglot.types.Type;

/**
 * Represents a closure.<br>
 * You'd think it wouldn't be necessary to have a representation of (nameless) closures in the
 * type system outside of <code>ClosureTypes</code>, but the control-/data-flow framework requires
 * that all code bodies have an associated instance. See <code>CodeNode.codeInstance()</code>, and
 * <code>Closure.enterScope()</code>, which needs to push something on the Context stack, but the
 * only reasonable thing to push is some form of <code>CodeInstance</code>. Perhaps there should be
 * a more general kind of "code context" class, with a corresponding push method on
 * <code>Context</code>.
 * @author rfuhrer
 */
public interface ActionInstance extends CodeInstance<ActionDef> {
        public List<Type> throwTypes();
//        public ClosureInstance throwTypes(List<Type> throwTypes);
        public boolean throwsSubset(CodeInstance<ActionDef> pi);
        
        public Type returnType();
//        public ClosureInstance returnTypeRef(Ref<? extends Type> returnType);
}
