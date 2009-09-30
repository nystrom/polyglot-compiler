package polyglot.bytecode;

import polyglot.ast.NodeFactory;
import polyglot.dispatch.Dispatch;
import polyglot.frontend.AbstractGoal_c;
import polyglot.frontend.Compiler;
import polyglot.frontend.Goal;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.TargetFactory;
import polyglot.frontend.Topics;
import polyglot.types.TypeSystem;

/**
 * Extension information for ibex extension.
 */
public class ExtensionInfo extends polyglot.frontend.JLExtensionInfo {
    @Override
    protected Scheduler createScheduler() {
        return new BCScheduler(this);
    }
    
    @Override
    public void initCompiler(Compiler compiler) {
        getOptions().output_ext = "class";
        super.initCompiler(compiler);
    }
    
    static class BCScheduler extends JLScheduler {
        public BCScheduler(ExtensionInfo extInfo) {
            super(extInfo);
        }
        
        @Override
        protected Goal PostCompiled() {
            return new AbstractGoal_c("PostCompiled") {
                public boolean runTask() {
                    return true;
                }
            }.intern(this);
        }

        @Override
        public Goal CodeGenerated(final Job job) {
            final TypeSystem ts = extInfo.typeSystem();
            final NodeFactory nf = extInfo.nodeFactory();
            return new AbstractGoal_c("BCCodeGenerated") {
                @Override
                public boolean runTask() {
                    try {
                        new Dispatch.Dispatcher("visit").invoke(new BytecodeTranslator(job, ts, nf), job.ast());
                    }
                    catch (Exception e) {
                        return false;
                    }
                    return true;
                }
            }.intern(this);
        }
    }

}
