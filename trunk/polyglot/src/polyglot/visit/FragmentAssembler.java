package polyglot.visit;

import java.util.Map;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.Def;

public class FragmentAssembler extends NodeVisitor {
    protected Job job;

    public FragmentAssembler(Job job) {
        super();
        this.job = job;
    }

    public Job job() { return job; }

    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        Map<Def, ASTFragment> fragmentMap = job.fragmentMap();

        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            return fragmentMap.get(cd.classDef()).node();
        }
        else if (n instanceof FieldDecl) {
            FieldDecl fd = (FieldDecl) n;
            return fragmentMap.get(fd.fieldDef()).node();
        }
        else if (n instanceof MethodDecl) {
            MethodDecl md = (MethodDecl) n;
            return fragmentMap.get(md.methodDef()).node();
        }
        else if (n instanceof ConstructorDecl) {
            ConstructorDecl cd = (ConstructorDecl) n;
            return fragmentMap.get(cd.constructorDef()).node();
        }
        else if (n instanceof LocalDecl) {
            LocalDecl ld = (LocalDecl) n;
            return fragmentMap.get(ld.localDef()).node();
        }
        else if (n instanceof Formal) {
            Formal f = (Formal) n;
            return fragmentMap.get(f.localDef()).node();
        }
        else if (n instanceof Initializer) {
            Initializer i = (Initializer) n;
            return fragmentMap.get(i.initializerDef()).node();
        }
        else if (n instanceof New) {
            New neu = (New) n;
            if (neu.anonType() != null) {
                return fragmentMap.get(neu.anonType()).node();
            }
        }

        return n;
    }

}
