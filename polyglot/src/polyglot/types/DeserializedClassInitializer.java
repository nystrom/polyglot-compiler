/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.Iterator;

/**
 * A LazyClassInitializer is responsible for initializing members of a class
 * after it has been created. Members are initialized lazily to correctly handle
 * cyclic dependencies between classes.
 */
public class DeserializedClassInitializer implements LazyClassInitializer {
    protected TypeSystem ts;
    protected ClassDef ct;
    protected boolean init;
    
    public DeserializedClassInitializer(TypeSystem ts) {
        this.ts = ts;
    }
    
    public void setClass(ClassDef ct) {
        this.ct = ct;
    }

    public boolean fromClassFile() {
        return false;
    }

    public void initTypeObject() {
        if (this.init) return;
        if (ct.isMember() && ct.outer() instanceof ParsedClassType) {
            ClassDef outer = ct.outer().get();
            outer.addMemberClass(Ref_c.ref(ct.asType()));
        }
        for (Iterator i = ct.memberClasses().iterator(); i.hasNext(); ) {
            ClassType ct = (ClassType) i.next();
            ct.def().initializer().initTypeObject();
        }
        this.init = true;
    }

    public boolean isTypeObjectInitialized() {
        return this.init;
    }

    public void initSuperclass() {
    }

    public void initInterfaces() {
    }

    public void initMemberClasses() {
    }

    public void initConstructors() {
    }

    public void initMethods() {
    }

    public void initFields() {
    }

    public void canonicalConstructors() {
    }

    public void canonicalMethods() {
    }

    public void canonicalFields() {
    }
}
