package ibex;

import ibex.ast.IbexNodeFactory_c;
import ibex.parse.Grm;
import ibex.parse.Lexer_c;
import ibex.types.IbexTypeSystem_c;
import ibex.visit.AmbiguityChecker;
import ibex.visit.ParserGenerator;
import ibex.visit.Rewriter;

import java.io.Reader;
import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.frontend.CupParser;
import polyglot.frontend.FileSource;
import polyglot.frontend.Goal;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.Parser;
import polyglot.frontend.Scheduler;
import polyglot.frontend.VisitorGoal;
import polyglot.lex.Lexer;
import polyglot.main.Options;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;

/**
 * Extension information for ibex extension.
 */
public class ExtensionInfo extends polyglot.frontend.JLExtensionInfo {
    public String defaultFileExtension() {
        return "ibex";
    }

    public String compilerName() {
        return "ibexc";
    }

    public static class IbexOptions extends Options {
        public IbexOptions(ExtensionInfo extensionInfo) {
            super(extensionInfo);
        }

        public boolean checkMergeActions = true;
        public int searchWidth = 20;
        public boolean reportPossibleMerges = true;
    }

    public Options createOptions() {
        return new IbexOptions(this);
    }

    public IbexOptions getIbexOptions() {
        return (IbexOptions) getOptions();
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        Lexer lexer = new Lexer_c(reader, source, eq);
        Grm grm = new Grm(lexer, ts, nf, eq);
        return new CupParser(grm, source, eq);
    }

    protected NodeFactory createNodeFactory() {
        return new IbexNodeFactory_c();
    }

    protected TypeSystem createTypeSystem() {
        return new IbexTypeSystem_c();
    }

    @Override
    protected Scheduler createScheduler() {
        return new IbexScheduler(this);
    }

    public static class IbexScheduler extends JLScheduler {

        public IbexScheduler(ExtensionInfo ext) {
            super(ext);
        }

        @Override
        public List<Goal> goals(Job job) {
            List<Goal> l = super.goals(job);

            GenerateParser(job).addPrereq(Serialized(job));
            CheckAmbiguities(job).addPrereq(GenerateParser(job));
            Rewrite(job).addPrereq(CheckAmbiguities(job));
            CodeGenerated(job).addPrereq(Rewrite(job));

            return l;
        }

        private Goal GenerateParser(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new VisitorGoal(job, new ParserGenerator(job, ts, nf)).intern(this);
        }

        private Goal CheckAmbiguities(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new VisitorGoal(job, new AmbiguityChecker(job, ts, nf)).intern(this);
        }

        private Goal Rewrite(Job job) {
            TypeSystem ts = extInfo.typeSystem();
            NodeFactory nf = extInfo.nodeFactory();
            return new VisitorGoal(job, new Rewriter(job, ts, nf)).intern(this);
        }

    }
}
