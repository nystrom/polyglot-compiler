/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.LinkedHashMap;
import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.*;

/** Visitor which performs type checking on the AST. */
public class ASTFragmenter extends ContextVisitor
{
    public ASTFragmenter(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    protected Map<Def,ASTFragment> fragmentMap;
    
    public Map<Def,ASTFragment> fragmentMap() { return fragmentMap; }
    
    @Override
    public NodeVisitor begin() {
        fragmentMap = new LinkedHashMap<Def,ASTFragment>();
        return super.begin();
    }
    
    @Override
    public void finish(Node ast) {
        super.finish(ast);
        job.setFragmentMap(fragmentMap);
    }
    
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v) throws SemanticException {
        if (Report.should_report(Report.visit, 2))
            Report.report(2, ">> " + this + "::leave " + n);

        Context c = context().freeze();

        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            fragmentMap.put(cd.classDef(), new ASTFragment(parent, cd, c));
        }
        else if (n instanceof FieldDecl) {
            FieldDecl fd = (FieldDecl) n;
            ASTFragment fragment = new ASTFragment(parent, fd, c);
            fragmentMap.put(fd.fieldDef(), fragment);
            if (fd.init() != null) {
                fragmentMap.put(fd.initializerDef(), fragment);
            }
        }
        else if (n instanceof MethodDecl) {
            MethodDecl md = (MethodDecl) n;
            fragmentMap.put(md.methodDef(), new ASTFragment(parent, md, c));
        }
        else if (n instanceof ConstructorDecl) {
            ConstructorDecl cd = (ConstructorDecl) n;
            fragmentMap.put(cd.constructorDef(), new ASTFragment(parent, cd, c));
        }
        else if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl) n;
            fragmentMap.put(ld.localDef(), new ASTFragment(parent, ld, c));
        }
        else if (n instanceof Formal) {
            Formal f = (Formal) n;
            fragmentMap.put(f.localDef(), new ASTFragment(parent, f, c));
        }
        else if (n instanceof Initializer) {
            Initializer i = (Initializer) n;
            fragmentMap.put(i.initializerDef(), new ASTFragment(parent, i, c));
        }
        else if (n instanceof New) {
            New neu = (New) n;
            if (neu.anonType() != null) {
                fragmentMap.put(neu.anonType(), new ASTFragment(parent, neu, c));
            }
        }
        
        return super.leaveCall(old, n, v);
    }   
}
