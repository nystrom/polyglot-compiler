/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;

/** Visitor which performs type checking on the AST. */
public class TypeChecker extends ContextVisitor
{
    protected boolean visitSigs;
    protected boolean visitBodies;
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
        this(job, ts, nf, true, true);
    }
    
    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf, boolean visitSigs, boolean visitBodies) {
        super(job, ts, nf);
        this.visitSigs = visitSigs;
        this.visitBodies = visitBodies;
    }
    
    public TypeChecker visitSupers() {
        if (this.visitSigs == false && this.visitBodies == false) return this;
        TypeChecker tc = (TypeChecker) copy();
        tc.visitSigs = false;
        tc.visitBodies = false;
        return tc;
    }
    
    public TypeChecker visitSignatures() {
        if (this.visitSigs == true && this.visitBodies == false) return this;
        TypeChecker tc = (TypeChecker) copy();
        tc.visitSigs = true;
        tc.visitBodies = false;
        return tc;
    }
    
    public TypeChecker visitBodies() {
        if (this.visitSigs == true && this.visitBodies == true) return this;
        TypeChecker tc = (TypeChecker) copy();
        tc.visitSigs = true;
        tc.visitBodies = true;
        return tc;
    }
    
    public boolean shouldVisitSupers() { return true; }
    public boolean shouldVisitSignatures() { return visitSigs; }
    public boolean shouldVisitBodies() { return visitBodies; }
    
    public Node override(Node parent, Node n) {
        if (! visitSigs && n instanceof ClassMember && ! (n instanceof ClassDecl)) {
            return n;
        }
        if ((! visitBodies || ! visitSigs) && parent instanceof ClassMember) {
            if (parent instanceof FieldDecl && ((FieldDecl) parent).init() == n) {
                return n;
            }
            if (parent instanceof CodeDecl && ((CodeDecl) parent).body() == n) {
                return n;
            }
        }

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
        TypeChecker tc = (TypeChecker) v;
        AmbiguityRemover ar = new AmbiguityRemover(tc.job(), tc.typeSystem(), tc.nodeFactory());
        ar = (AmbiguityRemover) ar.context(tc.context());
        m = m.del().disambiguate(ar);
        m = m.del().typeCheck(tc);
        m = m.del().checkConstants(tc);

        //            if (! m.isTypeChecked()) {
        //                throw new InternalCompilerError("Type checking failed for " + m + " (" + m.getClass().getName() + ")", m.position());
        //            }
        
        if (Report.should_report(Report.visit, 2))
            Report.report(2, "<< " + this + "::leave " + n + " -> " + m);
        
        return m;
    }   
}
