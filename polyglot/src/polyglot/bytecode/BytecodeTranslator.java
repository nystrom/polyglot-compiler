package polyglot.bytecode;

import java.io.*;

import polyglot.ast.*;
import polyglot.bytecode.rep.IClassGen;
import polyglot.frontend.Job;
import polyglot.interp.BytecodeCache;
import polyglot.types.*;
import polyglot.util.ErrorInfo;

public class BytecodeTranslator {
    private Job job;
    private TypeSystem ts;
    private NodeFactory nf;
    private BytecodeCache cache;

    public BytecodeTranslator(Job job, TypeSystem ts, NodeFactory nf, BytecodeCache cache) {
        super();
        this.job = job;
        this.ts = ts;
        this.nf = nf;
        this.cache = cache;
    }

    public void visit(SourceFile n) {
//        n = (SourceFile) n.visit(new LocalClassRemover(job, ts, nf).context(ts.emptyContext()));
//        n = (SourceFile) n.visit(new InnerClassRemover(job, ts, nf).context(ts.emptyContext()));
        
        for (TopLevelDecl d : n.decls()) {
            if (d instanceof ClassDecl) {
                ClassDecl cd = (ClassDecl) d;
                ClassDef def = cd.classDef();
                ClassBody body = cd.body();
                IClassGen cg = newClassTranslator(this, def).translateClass(cd, body);
                cache.put(def.fullName(), cg.getName(), n.source(), cg.bytes());

                // Dump class to a file--this should be a separate pass!
                genClass(n, Types.get(def.package_()), cg);
            }
        }
    }
    
    public ClassTranslator newClassTranslator(BytecodeTranslator bc, ClassDef cd) {
        return new ClassTranslator(job, ts, nf, bc, cd);
    }

    private void genClass(SourceFile n, polyglot.types.Package pkg, IClassGen acg) {
        byte[] b = acg.bytes();
        File f = ts.extensionInfo().targetFactory().outputFile(acg.fullName().qualifier(), acg.fullName().name(), n.source());
        try {
            if (! f.getParentFile().exists()) {
                // Create the parent directory.  Don't bother checking if successful, the file open below will fail if not.
                f.getParentFile().mkdirs();
            }
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
