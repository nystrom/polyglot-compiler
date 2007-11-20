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
        goals.add(MakeDictionary(job));
        goals.add(TypeChecked(job));
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
        return new SourceGoal_c("ImportTableInitialized", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new InitImportsVisitor(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal TypesInitialized(Job job) {
        return new SourceGoal_c("TypesInitialized", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new TypeBuilder(job, ts, nf));
            }
        }.intern(this);
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

//    public Goal Disambiguated(Job job) {
//        return TypeChecked(job);
//    
//    
//        return new SourceGoal_c("Disambiguated", job) {
//            @Override
//            public GoalSet requiredView() {
//                return super.requiredView().union(new RuleBasedGoalSet() {
//                    public boolean contains(Goal g) {
//                        return g instanceof LookupGlobalTypeDefAndSetFlags;
//                    }
//                });
//            }
//
//            public Pass createPass() {
//                TypeSystem ts = extInfo.typeSystem();
//                NodeFactory nf = extInfo.nodeFactory();
//                return new VisitorPass(this, job, new AmbiguityRemover(job, ts, nf));
//            }
//        }.intern(this);
//    }
    
    public Goal MakeDictionary(Job job) {
        return new SourceGoal_c("MakeDictionary", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new ASTFragmenter(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal TypeCheckDef(Job job, final Def def) {
        return new TypeCheckDef("TypeCheckedDef", job, def).intern(this);
    }
    
    public Goal SupertypeDef(Job job, final ClassDef def) {
        return new SupertypeDef("SupertypeDef", job, def).intern(this);
    }
    
    public Goal SignatureDef(Job job, final ClassDef def) {
        return new SupertypeDef("SignatureDef", job, def).intern(this);
    }

    public Goal TypeChecked(Job job) {
        return new SourceGoal_c("TypeChecked", job) {
            public List<Goal> prereqs() {
                ArrayList<Goal> l = new ArrayList<Goal>();
                l.addAll(super.prereqs());
                for (Def def : job().fragmentMap().keySet()) {
                    l.add(TypeCheckDef(job(), def));
                }
                return l;
            }
            
            public Pass createPass() {
                return new VisitorPass(this, job, new FragmentAssembler(job));
            }
        }.intern(this);
    }

    public Goal ReachabilityChecked(Job job) {
        return new SourceGoal_c("ReachChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new ReachChecker(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal ExceptionsChecked(Job job) {
        return new SourceGoal_c("ExceptionsChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new ExceptionChecker(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal ExitPathsChecked(Job job) {
        return new SourceGoal_c("ExitChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new ExitChecker(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal InitializationsChecked(Job job) {
        return new SourceGoal_c("InitializationsChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new InitChecker(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal ConstructorCallsChecked(Job job) {
        return new SourceGoal_c("ContructorCallsChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new ConstructorCallChecker(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal ForwardReferencesChecked(Job job) {
        return new SourceGoal_c("ForwardRefsChecked", job) {
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new FwdReferenceChecker(job, ts, nf));
            }
        }.intern(this);
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
        return new FieldConstantsChecked(f);
    }

    @Override
    public Goal LookupGlobalType(TypeRef<Type> sym) {
        return new TypeObjectGoal_c<Type>("LookupGlobalType", sym) {
            public Pass createPass() {
                return new EmptyPass(this);
            }
        };
    }

    @Override
    public Goal LookupGlobalTypeDef(TypeRef<ClassDef> sym, String className) {
        return LookupGlobalTypeDefAndSetFlags(sym, className, null);
    }

    @Override
    public Goal LookupGlobalTypeDefAndSetFlags(TypeRef<ClassDef> sym,
            String className, Flags flags) {
        return new LookupGlobalTypeDefAndSetFlags(sym, className, flags);
    }
    
    protected static class DefGoal extends SourceGoal_c {
        protected Def def;
        protected ContextVisitor v;
        
        protected DefGoal(String name, Job job, Def def, ContextVisitor v) {
            super(name, job);
            this.def = def;
            this.v = v;
        }
        
        public Def def() {
            return def;
        }
        
        @Override
        public List<Goal> prereqs() {
            List<Goal> l = new ArrayList<Goal>();
            l.addAll(super.prereqs());
            l.add(Globals.Scheduler().MakeDictionary(job()));
            return l;
        }
        
        @Override
        public GoalSet requiredView() {
            return new RuleBasedGoalSet() {
                public boolean contains(Goal g) {
                    return DefGoal.super.requiredView().contains(g) ||
                    g instanceof LookupGlobalTypeDefAndSetFlags ||
                    g instanceof FieldConstantsChecked;
                }
                
                public String toString() { return "DefGoalRuleSet"; }
            };
        }
        
        public boolean equals(Object o) {
            if (o instanceof DefGoal) {
                DefGoal g = (DefGoal) o;
                return super.equals(o) && def.equals(g.def) && v.getClass() == g.v.getClass();
            }
            return false;
        }
        
        public int hashCode() {
            return super.hashCode() + def.hashCode();
        }
        
        public String toString() {
            return job() + ":" + job().extensionInfo() + ":"
            + name() + ":" + def + " (" + stateString() + ")";
        }
        
        public Pass createPass() {
            ExtensionInfo extInfo = Globals.Extension();
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new FragmentPass(this, job, def, v);
        }
    }
    
    protected static class SupertypeDef extends DefGoal {
        protected SupertypeDef(String name, Job job, Def def) {
            super(name, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), false, false));
        }
    }
    
    protected static class SignatureDef extends DefGoal {
        protected SignatureDef(String name, Job job, Def def) {
            super(name, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), true, false));
        }
        
        @Override
        public List<Goal> prereqs() {
            List<Goal> l = new ArrayList<Goal>();
            l.addAll(super.prereqs());
            if (def() instanceof ClassDef) {
            l.add(Globals.Scheduler().SupertypeDef(job(), (ClassDef) def()));
            }
            return l;
        }
    }
    
    protected static class TypeCheckDef extends DefGoal {
        protected TypeCheckDef(String name, Job job, Def def) {
            super(name, job, def, new TypeChecker(job, job.extensionInfo().typeSystem(), job.extensionInfo().nodeFactory(), true, true));
        }
        
        @Override
        public List<Goal> prereqs() {
            List<Goal> l = new ArrayList<Goal>();
            l.addAll(super.prereqs());
            if (def() instanceof ClassDef) {
                l.add(Globals.Scheduler().SignatureDef(job(), (ClassDef) def()));
                l.add(Globals.Scheduler().SupertypeDef(job(), (ClassDef) def()));
            }
            return l;
        }
    }

    protected static class SupertypesResolved extends TypeObjectGoal_c<ClassDef> {
        public SupertypesResolved(Ref<ClassDef> v) {
            super(v);
        }

        public Pass createPass() {
            ClassDef cd = typeRef().get();
            if (cd.job() != null) {
                TypeSystem ts = Globals.Extension().typeSystem();
                NodeFactory nf = Globals.Extension().nodeFactory();
                return new ReadonlyVisitorPass(this, cd.job(), new AmbiguityRemover(cd.job(), ts, nf));
            }
            throw new InternalCompilerError("Don't know how to resolve supertypes for " + cd + ".");
        }
    }

    protected static class SignaturesResolved extends
            TypeObjectGoal_c<ClassDef> {
        protected SignaturesResolved(Ref<ClassDef> v) {
            super(v);
        }

        public Pass createPass() {
            ClassDef cd = typeRef().get();
            if (cd.job() != null) {
                TypeSystem ts = Globals.Extension().typeSystem();
                NodeFactory nf = Globals.Extension().nodeFactory();
                return new ReadonlyVisitorPass(this, cd.job(), new AmbiguityRemover(cd.job(), ts, nf));
            }
            throw new InternalCompilerError("Don't know how to resolve signatures for " + cd + ".");
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
        
        public String toString() {
            if (flags == null)
                return name + "(" + className + ")";
            else 
                return name + "(" + className + ", " + flags + ")";
        }

        public Pass createPass() {
            return new AbstractPass(this) {
                public boolean run() {
                    TypeRef<ClassDef> ref = (TypeRef<ClassDef>) typeRef();
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
