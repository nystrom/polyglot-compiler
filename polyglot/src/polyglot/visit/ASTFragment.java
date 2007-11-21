/**
 * 
 */
package polyglot.visit;

import polyglot.ast.FragmentRoot;
import polyglot.ast.Node;
import polyglot.types.Context;

public class ASTFragment {
    Node parent;
    FragmentRoot n;
    Context c;
    
    public ASTFragment(Node parent, FragmentRoot n, Context c) {
        this.parent = parent;
        this.n = n;
        this.c = c;
    }
    
    public Node parent() {
        return parent;
    }
    
    public FragmentRoot node() {
        return n;
    }
    
    public Context context() {
        return c;
    }

    public Node visit(ContextVisitor v) {
        FragmentRoot m = (FragmentRoot) parent.visitChild(n, v.context(c));
        this.n = m;
        return m;
    }

    public void setNode(FragmentRoot n) {
        this.n = n;
    }
    
    public String toString() {
        return "Fragment(" + n + " in " + c + ")";
    }
}