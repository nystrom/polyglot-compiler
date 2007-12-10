/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

/*
 * Scheduler.java
 * 
 * Author: nystrom
 * Creation date: Feb 6, 2005
 */
package polyglot.frontend;

import java.util.*;

import polyglot.ast.NodeFactory;
import polyglot.main.Version;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * Comment for <code>Scheduler</code>
 *
 * @author nystrom
 */
public class JLScheduler extends Scheduler {

    /**
     * @param extInfo
     */
    public JLScheduler(ExtensionInfo extInfo) {
        super(extInfo);
    }

    public List<Goal> goals(Job job) {
        List<Goal> goals = new ArrayList<Goal>();

        goals.add(Parsed(job));
        goals.add(TypesInitialized(job));
        goals.add(TypesInitializedForCommandLine());
        goals.add(ImportTableInitialized(job));
        goals.add(FragmentAST(job));
        goals.add(TypeChecked(job));
        goals.add(ReassembleAST(job));
        goals.add(ReachabilityChecked(job));
        goals.add(ExceptionsChecked(job));
        goals.add(ExitPathsChecked(job));
        goals.add(InitializationsChecked(job));
        goals.add(ConstructorCallsChecked(job));
        goals.add(ForwardReferencesChecked(job));
        goals.add(Serialized(job));
        goals.add(CodeGenerated(job));
        goals.add(End(job));
        
        return goals;
    }

    public Goal Parsed(Job job) {
        return new SourceGoal_c("Parsed", job) {
            public Pass createPass() {
                return new ParserPass(this, extInfo.compiler(), this.job);
            }  
        }.intern(this);
    }    

    public Goal ImportTableInitialized(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        Goal g = new VisitorGoal("ImportTableInitialized", job, new InitImportsVisitor(job, ts, nf));
        Goal g2 = g.intern(this);
        if (g == g2) {
            g.addPrereq(TypesInitializedForCommandLine());
        }
        return g2;
    }

    public Goal TypesInitialized(Job job) {
        // For this one, the goal is stored in the job.  This is called a lot from the system resolver and interning is expensive.
        return job.TypesInitialized(this);
    }

    public Goal TypesInitializedForCommandLine() {
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        return new BarrierGoal("TypesInitializedForCommandLine", commandLineJobs()) {
            public Goal prereqForJob(Job job) {
                return TypesInitialized(job);
            }
        }.intern(this);
    }

    public Goal FragmentAST(Job job) {
        return job.FragmentAST(this);
    }
    
    public Goal SupertypeDef(Job job, final Def def) {
        return new SupertypeDef("SupertypeDef", job, def).intern(this);
    }
    
    public Goal SignatureDef(Job job, final Def def, int key) {
        return new SignatureDef("SignatureDef", job, def, key).intern(this);
    }
    
    public Goal TypeCheckDef(Job job, final Def def) {
        return new TypeCheckDef("TypeCheckedDef", job, def).intern(this);
    }

    public Goal TypeChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("TypeChecked", job, new TypeChecker(job, ts, nf)).intern(this);
    }

    public Goal ReassembleAST(Job job) {
        return new SourceGoal_c("ReassembleAST", job) {
            public List<Goal> prereqs() {
                ArrayList<Goal> l = new ArrayList<Goal>();
                l.addAll(super.prereqs());
                for (Def def : job().fragmentMap().keySet()) {
                    l.add(TypeCheckDef(job(), def));
                }
                return l;
            }

            public Pass createPass() {
                return new VisitorPass(this, job, new FragmentAssembler(job)) {
                    public boolean run() {
//                        for (Def def : job().fragmentMap().keySet()) {
//                            Goal g = TypeCheckDef(job(), def);
//                            assert g.hasBeenReached();
//                        }
                        return super.run();
                    }
                };
            }
        }.intern(this);
    }

    public Goal ReachabilityChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("ReachChecked", job, new ReachChecker(job, ts, nf)).intern(this);
    }

    public Goal ExceptionsChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("ExceptionsChecked", job, new ExceptionChecker(job, ts, nf)).intern(this);
    }

    public Goal ExitPathsChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("ExitChecked", job, new ExitChecker(job, ts, nf)).intern(this);
    }

    public Goal InitializationsChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("InitializationsChecked", job, new InitChecker(job, ts, nf)).intern(this);
    }

    public Goal ConstructorCallsChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("ContructorCallsChecked", job, new ConstructorCallChecker(job, ts, nf)).intern(this);
    }

    public Goal ForwardReferencesChecked(Job job) {
        TypeSystem ts = job.extensionInfo().typeSystem();
        NodeFactory nf = job.extensionInfo().nodeFactory();
        return new VisitorGoal("ForwardRefsChecked", job, new FwdReferenceChecker(job, ts, nf)).intern(this);
    }

    public Goal Serialized(Job job) {
        return new SourceGoal_c("Serialized", job) {
            public Pass createPass() {
                Compiler compiler = extInfo.compiler();
                if (compiler.serializeClassInfo()) {
                    TypeSystem ts = extInfo.typeSystem();
                    NodeFactory nf = extInfo.nodeFactory();
                    return new VisitorPass(this, job,
                            createSerializer(ts,
                                    nf,
                                    job.source().lastModified(),
                                    compiler.errorQueue(),
                                    extInfo.version()));
                }
                else {
                    return new EmptyPass(this);
                }
            }
        }.intern(this);
    }

    protected ClassSerializer createSerializer(TypeSystem ts, NodeFactory nf,
            Date lastModified, ErrorQueue eq, Version version) {
        return new ClassSerializer(ts, nf, lastModified, eq, version);
    }

    public Goal CodeGenerated(Job job) {
        return new SourceGoal_c("CodeGenerated", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new OutputPass(this, job, new Translator(job, ts, nf, extInfo.targetFactory()));
            }
        }.intern(this);
    }
    
    public Goal FieldConstantsChecked(Symbol<FieldDef> f) {
        final FieldDef fd = f.get(GoalSet.EMPTY);
        ReferenceType t = fd.container() != null ? fd.container().get(GoalSet.EMPTY) : null;
        if (t instanceof ClassType) {
            ClassType ct = (ClassType) t;
            ClassDef cd = ct.def();
            if (cd.job() != null) {
                return TypeCheckDef(cd.job(), fd);
            }
        }
        return new FieldConstantsChecked(f).intern(this);
    }

    @Override
    public Goal LookupGlobalType(LazyRef<Type> sym) {
        return new LookupGlobalType("LookupGlobalType", sym).intern(this);
    }

    @Override
    public Goal LookupGlobalTypeDef(LazyRef<ClassDef> sym, String className) {
        return LookupGlobalTypeDefAndSetFlags(sym, className, null);
    }

    @Override
    public Goal LookupGlobalTypeDefAndSetFlags(LazyRef<ClassDef> sym,
            String className, Flags flags) {
        return new LookupGlobalTypeDefAndSetFlags(sym, className, flags).intern(this);
    }
    
    public static class LookupGlobalType extends TypeObjectGoal_c<Type> {
        public LookupGlobalType(String name, Ref<Type> v) {
            super(name, v);
        }
        
        @Override
        public GoalSet requiredView() {
            final Goal goal = this;
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return LookupGlobalType.super.requiredView().contains(g) ||
                    g instanceof LookupGlobalType ||
                    g instanceof LookupGlobalTypeDefAndSetFlags;
                }
                
                public String toString() { return "DefGoalRuleSet(" + LookupGlobalType.this + ")"; }
            };
        }

        public Pass createPass() {
            return new EmptyPass(this);
        }
    }

    public static class SupertypeDef extends FragmentGoal {
        protected SupertypeDef(String name, Job job, Def def) {
            super(name, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), def));
        }

        @Override
        public GoalSet createRequiredView() {
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return SupertypeDef.super.defaultRequiredView().contains(g) ||
                    g instanceof LookupGlobalType ||
                    g instanceof LookupGlobalTypeDefAndSetFlags ||
                    g instanceof FieldConstantsChecked ||
                    g instanceof SupertypeDef ||
                    g instanceof SignatureDef ||
                    g instanceof TypeCheckDef;
                }
                
                public String toString() { return "DefGoalRuleSet(" + SupertypeDef.this + ")"; }
            };
        }
    }
    
    public static class SignatureDef extends FragmentGoal {
        int key;
        
        protected SignatureDef(String name, Job job, Def def, int key) {
            super(name + key, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), def, key));
            this.key = key;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof SignatureDef) {
                SignatureDef s = (SignatureDef) o;
                return super.equals(s) && key == s.key;
            }
            return false;
        }
        
        @Override
        public GoalSet createRequiredView() {
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return SignatureDef.super.defaultRequiredView().contains(g) ||
                    g instanceof LookupGlobalType ||
                    g instanceof LookupGlobalTypeDefAndSetFlags ||
                    g instanceof FieldConstantsChecked ||
                    g instanceof SupertypeDef ||
                    g instanceof SignatureDef ||
                    g instanceof TypeCheckDef;
                }
                
                public String toString() { return "DefGoalRuleSet(" + SignatureDef.this + ")"; }
            };
        }

        
        @Override
        public List<Goal> prereqs() {
            List<Goal> l = new ArrayList<Goal>();
            l.addAll(super.prereqs());
//            if (def() instanceof ClassDef) {
//                l.add(Globals.Scheduler().SupertypeDef(job(), (ClassDef) def()));
//            }
            return l;
        }

        public int key() {
            return key;
        }
    }
    
    public static class TypeCheckDef extends FragmentGoal {
        protected TypeCheckDef(String name, Job job, Def def) {
            super(name, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), def));
        }
        
        @Override
        public GoalSet createRequiredView() {
            final Goal goal = this;
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return TypeCheckDef.super.defaultRequiredView().contains(g) ||
                    g instanceof LookupGlobalType ||
                    g instanceof LookupGlobalTypeDefAndSetFlags ||
                    g instanceof FieldConstantsChecked ||
                    g instanceof SupertypeDef ||
                    g instanceof SignatureDef ||
                    g instanceof TypeCheckDef;
                }
                
                public String toString() { return "DefGoalRuleSet(" + TypeCheckDef.this + ")"; }
            };
        }

        @Override
        public List<Goal> prereqs() {
            List<Goal> l = new ArrayList<Goal>();
            l.addAll(super.prereqs());
//            if (def() instanceof ClassDef) {
//                l.add(Globals.Scheduler().SupertypeDef(job(), (ClassDef) def()));
//            }
//            l.add(Globals.Scheduler().SignatureDef(job(), def()));
            return l;
        }
    }

    protected static class FieldConstantsChecked extends
            TypeObjectGoal_c<FieldDef> {
        protected FieldConstantsChecked(Ref<FieldDef> v) {
            super(v);
        }
        
        public Pass createPass() {
            final FieldDef f = typeRef().get(GoalSet.EMPTY);
            return new AbstractPass(this) {
                public boolean run() {
                    f.setNotConstant();
                    f.<FieldDef>symbol().update(f, goal);
                    return true;
                }
            };
        }
    }

    protected static class LookupGlobalTypeDefAndSetFlags extends TypeObjectGoal_c<ClassDef> {
        protected String className;
        protected Flags flags;

        private LookupGlobalTypeDefAndSetFlags(Ref<ClassDef> v, String className, Flags flags) {
            super(v);
            this.className = className;
            this.flags = flags;
        }
        
        @Override
        public GoalSet requiredView() {
            final Goal goal = this;
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return LookupGlobalTypeDefAndSetFlags.super.requiredView().contains(g) ||
                    g instanceof LookupGlobalType ||
                    g instanceof LookupGlobalTypeDefAndSetFlags;
                }
                
                public String toString() { return "DefGoalRuleSet(" + LookupGlobalTypeDefAndSetFlags.this + ")"; }
            };
        }

        public String toString() {
            if (flags == null)
                return name + "(" + className + ")";
            else 
                return name + "(" + className + ", " + flags + ")";
        }

        public Pass createPass() {
            return new AbstractPass(this) {
                public boolean run() {
                    LazyRef<ClassDef> ref = (LazyRef<ClassDef>) typeRef();
                    try {
                        Named n = Globals.TS().systemResolver().find(className);
                        if (n instanceof ClassType) {
                            ClassType ct = (ClassType) n;
                            ClassDef def = ct.def();
                            if (flags != null) {
                                // The flags should be overwritten only for a member class.
                                assert def.isMember();
                                def.setFlags(flags);
                            }
                            ref.update(def);
                            return true;
                        }
                    }
                    catch (SemanticException e) {
                        Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), e.position());
                    }
                    return false;
                }
            };
        }
    }
}
