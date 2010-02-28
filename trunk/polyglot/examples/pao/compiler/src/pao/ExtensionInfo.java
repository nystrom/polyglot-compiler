/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package pao;

import java.io.Reader;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

import polyglot.ast.NodeFactory;
import pao.ast.PaoNodeFactory_c;
import pao.parse.Grm;
import pao.parse.Lexer_c;
import pao.types.PaoTypeSystem_c;
import pao.visit.PaoBoxer;
import polyglot.frontend.*;
import polyglot.lex.Lexer;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;

/**
 * Extension information for the PAO extension. This class specifies the
 * appropriate parser, <code>NodeFactory</code> and <code>TypeSystem</code>
 * to use, as well as inserting a new pass: <code>PaoBoxer</code>.
 * 
 * @see pao.visit.PaoBoxer
 * @see pao.ast.PaoNodeFactory_c 
 * @see pao.types.PaoTypeSystem 
 * @see pao.types.PaoTypeSystem_c 
 */
public class ExtensionInfo extends JLExtensionInfo {
    public String defaultFileExtension() {
        return "pao";
    }

    public String compilerName() {
        return "paoc";
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        Lexer lexer = new Lexer_c(reader, source, eq);
        Grm grm = new Grm(lexer, ts, nf, eq);
        return new CupParser(grm, source, eq);
    }

    protected NodeFactory createNodeFactory() {
        return new PaoNodeFactory_c();
    }
    protected TypeSystem createTypeSystem() {
        return new PaoTypeSystem_c();
    }

    public Scheduler createScheduler() {
        return new PAOScheduler(this);
    }

    static class PAOScheduler extends JLScheduler {
        PAOScheduler(ExtensionInfo extInfo) {
            super(extInfo);
        }

        public List<Goal> goals(Job job) {
            List<Goal> goals = super.goals(job);

            List<Goal> result = new ArrayList<Goal>();

            for (Goal g : goals) {
                if (g == Serialized(job))
                    result.add(Rewrite(job));
                result.add(g);
            }

            return result;
        }

        public Goal Rewrite(final Job job) { 
            TypeSystem ts = job.extensionInfo().typeSystem();
            NodeFactory nf = job.extensionInfo().nodeFactory();
            Goal g = new VisitorGoal("Rewrite", job, new PaoBoxer(job, ts, nf)).intern(this);
            return g;
        }
    }

    static {
        // Make sure the class Topics is loaded.
        new Topics(); 
    }
}
