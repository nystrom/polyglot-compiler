package polyglot.bytecode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.TopLevelDecl;
import polyglot.bytecode.rep.IClassGen;
import polyglot.dispatch.Dispatch;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;
import polyglot.visit.InnerClassRemover;
import polyglot.visit.LocalClassRemover;

public class BytecodeTranslator {
    private Job job;
    private TypeSystem ts;
    private NodeFactory nf;

    public BytecodeTranslator(Job job, TypeSystem ts, NodeFactory nf) {
        super();
        this.job = job;
        this.ts = ts;
        this.nf = nf;
    }

    public void visit(final ClassDecl n) {
    }

    public void visit(SourceFile n) {
        n = (SourceFile) n.visit(new LocalClassRemover(job, ts, nf).context(ts.emptyContext()));
        n = (SourceFile) n.visit(new InnerClassRemover(job, ts, nf).context(ts.emptyContext()));
        
        for (TopLevelDecl d : n.decls()) {
            if (d instanceof ClassDecl) {
                ClassDecl cd = (ClassDecl) d;
                ClassDef def = cd.classDef();
                new Dispatch.Dispatcher("visit").invoke(this, d);
                final ClassDef sym = def;
                ClassBody body = cd.body();
                IClassGen cg = new ClassTranslator(job, ts, nf, this, sym).translateClass(cd, body);
                genClass(n, Types.get(sym.package_()), cg);
            }
        }
    }

    private void genClass(SourceFile n, polyglot.types.Package pkg, IClassGen acg) {
        byte[] b = acg.bytes();
        polyglot.types.Package p = pkg;
        File f = ts.extensionInfo().targetFactory().outputFile(acg.fullName().qualifier(), acg.fullName().name(), n.source());
        try {
            if (! f.getParentFile().exists())
                // Create the parent directory.  Don't bother checking if successful, the file open below will fail if not.
                f.getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(f);
            out.write(b);
            out.close();
        }
        catch (IOException e) {
            job.extensionInfo().compiler().errorQueue().enqueue(ErrorInfo.POST_COMPILER_ERROR, e.getMessage(), n.position());
        }
        
        for (IClassGen cg : acg.innerClasses()) {
            genClass(n, pkg, cg);
        }
    }
}