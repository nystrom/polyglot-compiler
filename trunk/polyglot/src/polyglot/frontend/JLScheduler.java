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
        goals.add(Disambiguated(job));
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

    public Goal Disambiguated(Job job) {
        return new SourceGoal_c("Disambiguated", job) {
            @Override
            public GoalSet requiredView() {
                return super.requiredView().union(new RuleBasedGoalSet() {
                    public boolean contains(Goal g) {
                        return g instanceof LookupGlobalTypeDefAndSetFlags;
                    }
                });
            }

            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new AmbiguityRemover(job, ts, nf));
            }
        }.intern(this);
    }

    public Goal TypeChecked(Job job) {
        return new SourceGoal_c("TypeChecked", job) {
            @Override
            public GoalSet requiredView() {
                return super.requiredView().union(new RuleBasedGoalSet() {
                    public boolean contains(Goal g) {
                        return g instanceof LookupGlobalTypeDefAndSetFlags ||
                               g instanceof FieldConstantsChecked ||
                               g instanceof SignaturesResolved ||
                               g instanceof SupertypesResolved;
                    }
                });
            }
            
            public Pass createPass() {
                TypeSystem ts = extInfo.typeSystem();
                NodeFactory nf = extInfo.nodeFactory();
                return new VisitorPass(this, job, new TypeChecker(job, ts, nf));
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
    
    public Goal SupertypesResolved(Symbol<ClassDef> cd) {
        return new SupertypesResolved(cd);
    }
    
    public Goal SignaturesResolved(Symbol<ClassDef> cd) {
        return new SignaturesResolved(cd);
    }

    public Goal FieldConstantsChecked(Symbol<FieldDef> f) {
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
            final FieldDef f = typeRef().get();
            ReferenceType t = f.container().get();
            if (t instanceof ClassType) {
                ClassType ct = (ClassType) t;
                ClassDef cd = ct.def();
                if (cd.job() != null) {
                    TypeSystem ts = Globals.Extension().typeSystem();
                    NodeFactory nf = Globals.Extension().nodeFactory();
                    final Goal goal = this;
                    return new ReadonlyVisitorPass(this, cd.job(), new TypeChecker(cd.job(), ts, nf)) {
                      public boolean run() {
                          boolean result = false;
                          try {
                              result = super.run();
                              return result;
                          }
                          finally {
                              if (result && hasBeenReached()) {
                                  f.<FieldDef>symbol().update(f, goal);
                              }
                          }
                      }
                    };
                }
            }
            throw new InternalCompilerError("Don't know how to check constants for " + f + ".");
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
