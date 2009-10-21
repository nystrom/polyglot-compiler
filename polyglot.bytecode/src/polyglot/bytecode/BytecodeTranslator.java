package polyglot.bytecode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    Map<ClassDef, List<IClassGen>> output = new HashMap<ClassDef, List<IClassGen>>();

    void addClassForOutput(final ClassDef sym, final IClassGen jc) {
        if (sym == null)
            return;
        List<IClassGen> classes = output.get(sym);
        if (classes == null) {
            classes = new ArrayList<IClassGen>();
            output.put(sym, classes);
        }
        classes.add(jc);
    }

    public void dumpClasses() {
        Set<ClassDef> symbols = output.keySet();
        for (final ClassDef sym : symbols) {
            final List<IClassGen> classes = output.get(sym);
        }

        output.clear();
    }

    public void visit(SourceFile n) {
        n = (SourceFile) n.visit(new LocalClassRemover(job, ts, nf).context(ts.emptyContext()));
        n = (SourceFile) n.visit(new InnerClassRemover(job, ts, nf).context(ts.emptyContext()));
        for (TopLevelDecl d : n.decls()) {
            if (d instanceof ClassDecl) {
                ClassDecl cd = (ClassDecl) d;
                ClassDef def = cd.classDef();
                ClassTranslator a = new ClassTranslator(job, ts, nf, this, def);
                new Dispatch.Dispatcher("visit").invoke(a, d);
                Set<ClassDef> symbols = output.keySet();
                for (final ClassDef sym : symbols) {
                    final List<IClassGen> classes = output.get(sym);
                    for (final IClassGen cg : classes) {
                        byte[] b = cg.bytes();
//              polyglot.types.Package p = Types.get(def.package_());
                        File f = ts.extensionInfo().targetFactory().outputFile(cg.fullName().qualifier(), cg.fullName().name(), n.source());
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
                    }
                }
                output.clear();
            }
        }
    }
}
