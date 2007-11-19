package polyglot.frontend;

import polyglot.ast.Node;
import polyglot.visit.NodeVisitor;

public class ReadonlyVisitorPass extends VisitorPass {
    public ReadonlyVisitorPass(Goal goal, Job job, String name) {
        super(goal, job, name);
    }
    
    public ReadonlyVisitorPass(Goal goal, Job job, NodeVisitor v) {
        super(goal, job, v);
    }
    
    public boolean run() {
        Node ast = job().ast();

        try {
            return super.run();
        }
        finally {
            // Make sure the AST didn't change.
            assert job().ast() == ast; // for debuggging
            job().ast(ast);
        }
    }
}
