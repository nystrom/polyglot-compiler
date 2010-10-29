package polyglot.dispatch;

import polyglot.ast.*;
import polyglot.frontend.Globals;
import polyglot.types.Context;
import polyglot.util.CodeWriter;
import polyglot.visit.Translator;

public class NewTranslator extends NewPrettyPrinter {

    public NewTranslator(Translator tr, CodeWriter w) {
	super(w);
	this.tr = tr;
    }

    protected Translator tr;
    
    /** Print an ast node using the given code writer.  This method should not
     * be called directly to translate a source file AST; use
     * <code>translate(Node)</code> instead.  This method should only be called
     * by nodes to print their children.
     */
    @Override
    public void print(Node parent, Node child) {
        Translator tr = this.tr;
        
        try {
            if (tr.context() != null) {
        	if (parent == null) {
        	    Context c = child.enterScope(tr.context());
        	    tr = tr.context(c);
        	}
        	else {
        	    Context c = parent.enterChildScope(child, tr.context());
        	    tr = tr.context(c);
        	}
            }

            this.tr = tr;
            super.print(parent, child);

            if (tr.context() != null) {
        	child.addDecls(tr.context());
            }
        }
        finally {
            this.tr = tr;
        }
    }
    

    public Node visit(Assert_c n) {
        if (! Globals.Options().assertions) {
            w.write(";");
            return n;
        }
        else {
            return super.visit(n);
        }
    }
    public Node visit(CanonicalTypeNode_c n) {
	w.write(n.typeRef().get().translate(tr.context()));
	return n;
    }
    public Node visit(PackageNode_c n) {
        w.write(n.package_().get().translate(tr.context()));
        return n;
    }

}
