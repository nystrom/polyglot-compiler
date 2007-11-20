package polyglot.frontend;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.main.Report;
import polyglot.types.Def;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;
import polyglot.visit.*;

public class FragmentPass extends VisitorPass {

    Def def;
    
    public FragmentPass(Goal goal, Job job, Def def, ContextVisitor v) {
        super(goal, job, v);
        this.def = def;
    }
    
    public Def def() { return def; }

    public boolean run() {
        Job job = job();
        Def def = def();

        Map<Def,ASTFragment> fmap = job.fragmentMap();

        if (fmap == null) {
            throw new InternalCompilerError("Null AST fragment map for job " + job + ": did the dict maker run?");
        }

        ASTFragment fragment = fmap.get(def);
    
        if (fragment == null) {
            throw new InternalCompilerError("Null AST fragment for job " + job + " and definition " + def);
        }
        
        try {
            ErrorQueue q = job().compiler().errorQueue();
            int nErrsBefore = q.errorCount();

            if (Report.should_report(Report.frontend, 3))
                Report.report(3, "Running " + v + " on " + fragment);

            fragment.visit((ContextVisitor) v);

            int nErrsAfter = q.errorCount();

            if (nErrsBefore != nErrsAfter) {
                // because, if they're equal, no new errors occurred,
                // so the run was successful.
                return false;
            }

            return true;
        }
        finally {
        }
    }
        
}
