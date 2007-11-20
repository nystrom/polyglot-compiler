/**
 * 
 */
package polyglot.visit;

import polyglot.ast.Node;
import polyglot.types.Context;

public class ASTFragment {
    Node parent;
    Node n;
    Context c;
    
    public ASTFragment(Node parent, Node n, Context c) {
        this.parent = parent;
        this.n = n;
        this.c = c;
    }
    
    public Node parent() {
        return parent;
    }
    
    public Node node() {
        return n;
    }
    
    public Context context() {
        return c;
    }

    public Node visit(ContextVisitor v) {
        Node m = parent.visitChild(n, v.context(c));
        v.job().astMap().put(n, m);
        this.n = m;
        return m;
    }
}