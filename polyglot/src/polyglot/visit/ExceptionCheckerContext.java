/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.*;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.*;

/** Visitor which checks if exceptions are caught or declared properly. */
public class ExceptionCheckerContext implements Copy
{
    TypeSystem ts;
    
    protected ExceptionCheckerContext outer;
    
    /**
     * Set of exceptions that can be caught. Combined with the outer
     * field, these sets form a stack of exceptions, representing
     * all and only the exceptions that may be thrown at this point in
     * the code.
     * 
     * Note: Consider the following code, where A,B,C,D are Exception subclasses.
     *    void m() throws A, B {
     *       try {
     *          ...
     *       }
     *       catch (C ex) { ... }
     *       catch (D ex) { ... }
     *    }
     *    
     *  Inside the try-block, the stack of catchable sets is:
     *     { C }
     *     { D }
     *     { A, B }
     */
    protected Set<Type> catchable;
    
    public Set<Type> getCatchable() {
		return catchable;
	}
    /**
     * The throws set, calculated bottom up.
     */
    protected SubtypeSet throwsSet;
    
    /**
     * Responsible for creating an appropriate exception.
     */
    protected UncaughtReporter reporter;
    
    /**
     * Should the propogation of eceptions upwards go past this point?
     */
    protected boolean catchAllThrowable;
    
    public ExceptionCheckerContext(Job job, TypeSystem ts, NodeFactory nf) {
	this.ts = ts;
        this.outer = null;
        this.catchAllThrowable = false;
    }
    
    public ExceptionCheckerContext push(UncaughtReporter reporter) {
        ExceptionCheckerContext ec = this.push();
        ec.reporter = reporter;
        ec.throwsSet = new SubtypeSet(ts.Throwable());
        return ec;
    }
    public ExceptionCheckerContext push(Type catchableType) {
        ExceptionCheckerContext ec = this.push();
        ec.catchable = Collections.<Type>singleton(catchableType);
        ec.throwsSet = new SubtypeSet(ts.Throwable());
        return ec;
    }
    public ExceptionCheckerContext push(Collection<Type> catchableTypes) {
        ExceptionCheckerContext ec = this.push();
        ec.catchable = new HashSet<Type>(catchableTypes);
        ec.throwsSet = new SubtypeSet(ts.Throwable());
        return ec;
    }
    
    public void push(ExceptionCheckerContext ec, Collection<Type> catchableTypes){
    	if(ec.catchable == null)
    		ec.catchable = new HashSet<Type>(catchableTypes);
    	else
    		ec.catchable.addAll(catchableTypes);
    	
    	return;
    }
    public ExceptionCheckerContext pushCatchAllThrowable() {
        ExceptionCheckerContext ec = this.push();
        ec.throwsSet = new SubtypeSet(ts.Throwable());
        ec.catchAllThrowable = true;
        return ec;
    }
    
    public ExceptionCheckerContext copy() {
	try {
	    return (ExceptionCheckerContext) super.clone();
	}
	catch (CloneNotSupportedException e) {
	    assert false;
	    return this;
	}
    }
    
    public ExceptionCheckerContext push() {
        throwsSet(); // force an instantiation of the throwsset.
        ExceptionCheckerContext ec = (ExceptionCheckerContext) this.copy();
        ec.outer = this;
        ec.catchable = null;
        ec.catchAllThrowable = false;
        return ec;
    }

    public ExceptionCheckerContext pop() {
        return outer;
    }

    /**
     * The ast nodes will use this callback to notify us that they throw an
     * exception of type t. This method will throw a SemanticException if the
     * type t is not allowed to be thrown at this point; the exception t will be
     * added to the throwsSet of all exception checkers in the stack, up to (and
     * not including) the exception checker that catches the exception.
     * @param t The type of exception that the node throws.
     * 
     * @throws SemanticException
     */
    public void throwsException(Type t, Position pos) throws SemanticException {
        if (! t.isUncheckedException()) {            
            // go through the stack of catches and see if the exception
            // is caught.
            boolean exceptionCaught = false;
            ExceptionCheckerContext ec = this;
            while (!exceptionCaught && ec != null) {
                if (ec.catchable != null) {
                    for (Iterator<Type> iter = ec.catchable.iterator(); iter.hasNext(); ) {
                        Type catchType = (Type)iter.next();
                        if (ts.isSubtype(t, catchType, ts.emptyContext())) {
                            exceptionCaught = true;
                            break;
                        }
                    }
                }           
                if(exceptionCaught){
                	break;
                }
                if (!exceptionCaught && ec.throwsSet != null) {
                    // add t to ec's throwsSet.
                    ec.throwsSet.add(t); 
                }
                if (ec.catchAllThrowable) {
                    // stop the propagation
                    exceptionCaught = true;
                }
                ec = ec.pop();
            }
            if (! exceptionCaught) {
                reportUncaughtException(t, pos);
            }
        }
    }

    public SubtypeSet throwsSet() {
        if (this.throwsSet == null) {
            this.throwsSet = new SubtypeSet(ts.Throwable());
        }
        return this.throwsSet;
    }
    
    protected void reportUncaughtException(Type t, Position pos) throws SemanticException {
        ExceptionCheckerContext ec = this;
        UncaughtReporter ur = null;
        while (ec != null && ur == null) {
            ur = ec.reporter;
            ec = ec.outer;
        }
        if (ur == null) {
            ur = new UncaughtReporter();
        }
        ur.uncaughtType(t, pos);
    }

    public static class UncaughtReporter {
        /**
         * This method must throw a SemanticException, reporting
         * that the Exception type t must be caught.
         * @throws SemanticException 
         */
        void uncaughtType(Type t, Position pos) throws SemanticException {
            throw new SemanticException("The exception \"" + t + 
              "\" must either be caught or declared to be thrown.", pos);
        }
    }
    public static class CodeTypeReporter extends UncaughtReporter {
        public final String codeType;
        public CodeTypeReporter(String codeType) {
            this.codeType = codeType;
        }
        void uncaughtType(Type t, Position pos) throws SemanticException {
            throw new SemanticException(codeType + " cannot throw a \"" + t + "\"; the exception must either be caught or declared to be thrown.", pos);
        }
    }
}
