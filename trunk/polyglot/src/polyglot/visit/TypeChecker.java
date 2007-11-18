/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

/** Visitor which performs type checking on the AST. */
public class TypeChecker extends ContextVisitor
{
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
       
    public Node override(Node parent, Node n) {
        try {
            if (Report.should_report(Report.visit, 2))
                Report.report(2, ">> " + this + "::override " + n);
            
            Node m = n.del().typeCheckOverride(parent, this);
            
            if (Report.should_report(Report.visit, 2))
                Report.report(2, "<< " + this + "::override " + n + " -> " + m);
            
            return m;
        }
        catch (SemanticException e) {
            if (e.getMessage() != null) {
                Position position = e.position();
                
                if (position == null) {
                    position = n.position();
                }
                
                this.errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                     e.getMessage(), position);
            }
            else {
                // silent error; these should be thrown only
                // when the error has already been reported 
            }
            
            return n;
        }
    }
 
    protected NodeVisitor enterCall(Node n) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::enter " + n);
        
        TypeChecker v = (TypeChecker) n.del().typeCheckEnter(this);
        
        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::enter " + n + " -> " + v);
        
        return v;
    }
    
    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n);
        
        Node m = n;

        //          System.out.println("running typeCheck for " + m);
        m = m.del().typeCheck((TypeChecker) v);
        m = m.del().checkConstants((TypeChecker) v);

        //            if (! m.isTypeChecked()) {
        //                throw new InternalCompilerError("Type checking failed for " + m + " (" + m.getClass().getName() + ")", m.position());
        //            }
        
        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::leave " + n + " -> " + m);
        
        return m;
    }   
}
