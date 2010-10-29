/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.visit;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;

/** Visitor which traverses the AST constructing type objects. */
public class InitImportsVisitor extends ErrorHandlingVisitor
{
    protected ImportTable importTable;
    
    public InitImportsVisitor(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }
    
    @Override
    public Node override(Node n) {
        if (n instanceof SourceFile) {
            SourceFile sf = (SourceFile) n;
            
            PackageNode pn = sf.package_();
            
            ImportTable it;
            
            if (pn != null) {
                it = ts.importTable(sf.source().name(), pn.package_());
            }
            else {
                it = ts.importTable(sf.source().name(), null);
            }
            
            InitImportsVisitor v = (InitImportsVisitor) copy();
            v.importTable = it;
            
            sf = (SourceFile) sf.visitChildren(v);

            return sf.importTable(it);
        }
        if (n instanceof Import) {
            Import im = (Import) n.visitChildren(this);
            
            if (im.kind() == Import.CLASS) {
                this.importTable.addExplicitImport(im.name(), im.position());
            }
            else if (im.kind() == Import.PACKAGE) {
                this.importTable.addOnDemandImport(im.name(), im.position());
            }
        }
    
        return n;
    }
}
