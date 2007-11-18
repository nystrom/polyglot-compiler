/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;

/**
 * A LazyClassInitializer is responsible for initializing members of a class
 * after it has been created. Members are initialized lazily to correctly handle
 * cyclic dependencies between classes.
 * 
 * SchedulerClassInitializer ensures that scheduler dependencies are enforced
 * when a ParsedClassType member is accessed.
 */
public class SchedulerClassInitializer implements LazyClassInitializer {
    protected TypeSystem ts;
    protected ClassDef ct;
    protected Scheduler scheduler;
    protected Job job;

    protected boolean init;
    protected boolean superclassInitialized;
    protected boolean interfacesInitialized;
    protected boolean memberClassesInitialized;
    protected boolean constructorsInitialized;
    protected boolean methodsInitialized;
    protected boolean fieldsInitialized;
    protected boolean constructorsCanonicalized;
    protected boolean methodsCanonicalized;
    protected boolean fieldsCanonicalized;

    public SchedulerClassInitializer(TypeSystem ts) {
        this.ts = ts;
        this.scheduler = ts.extensionInfo().scheduler();
    }
    
    public void setClass(ClassDef ct) {
        this.ct = ct;
    }

    public boolean fromClassFile() {
        return false;
    }

    public void initTypeObject() {
        this.init = true;
    }

    public boolean isTypeObjectInitialized() {
        return this.init;
    }

    public void initSuperclass() {
//        if (!superclassInitialized) {
//            if (ct.supertypesResolved()) {
//                this.superclassInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.SupertypesResolved(ct));
//            }
//        }
    }

    public void initInterfaces() {
//        if (!interfacesInitialized) {
//            if (ct.supertypesResolved()) {
//                this.interfacesInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.SupertypesResolved(ct));
//            }
//        }
    }

    public void initMemberClasses() {
//        if (!memberClassesInitialized) {
//            if (ct.membersAdded()) {
//                this.memberClassesInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.MembersAdded(ct));
//            }
//        }
    }

    public void canonicalConstructors() {
//        if (!constructorsCanonicalized) {
//            if (ct.signaturesResolved()) {
//                this.constructorsCanonicalized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.SignaturesResolved(ct));
//            }
//        }
    }

    public void canonicalMethods() {
//        if (!methodsCanonicalized) {
//            if (ct.signaturesResolved()) {
//                this.methodsCanonicalized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.SignaturesResolved(ct));
//            }
//        }
    }

    public void canonicalFields() {
//        if (!fieldsCanonicalized) {
//            if (ct.signaturesResolved()) {
//                this.fieldsCanonicalized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.SignaturesResolved(ct));
//            }
//        }
    }
    
    public void initConstructors() {
//        if (!constructorsInitialized) {
//            if (ct.membersAdded()) {
//                this.constructorsInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.MembersAdded(ct));
//            }
//        }
    }

    public void initMethods() {
//        if (!methodsInitialized) {
//            if (ct.membersAdded()) {
//                this.methodsInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.MembersAdded(ct));
//            }
//        }
    }

    public void initFields() {
//        if (!fieldsInitialized) {
//            if (ct.membersAdded()) {
//                this.fieldsInitialized = true;
//            }
//            else {
//                scheduler.runPass(scheduler.MembersAdded(ct));
//            }
//        }
    }
}
