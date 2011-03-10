/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.dispatch.Dispatch;
import polyglot.dispatch.NewPrettyPrinter;
import polyglot.dispatch.PassthruError;
import polyglot.dispatch.TypeChecker;
import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.Context;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Ref.Handler;
import polyglot.types.Types.Granularity;
import polyglot.types.Types_noFuture;
import polyglot.util.CodeWriter;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.StringUtil;
import polyglot.visit.DumpAst;
import polyglot.visit.NodeVisitor;
import funicular.Clock;

/**
 * A <code>Node</code> represents an AST node.  All AST nodes must implement
 * this interface.  Nodes should be immutable: methods which set fields
 * of the node should copy the node, set the field in the copy, and then
 * return the copy.
 */
public abstract class Node_c implements Node
{
    protected Position position;
    protected List<ErrorInfo> error;
    protected Ref<Node> checked;
    protected Job job;

    protected Clock jobClock() {
        return (Clock) job.get("clock");
    }
    
    public Node checked() {
	Ref<Node> r = checkedRef();
	assert r != null;
	return Types.get(r);
    }

    public Ref<Node> checkedRef() {
	return checked;
    }
    
    protected Context context;
    
    public Node context(Context c) {
	Node_c n = (Node_c) copy();
	n.context = c;
	return n;
    }
    
    public Context context() {
	return context;
    }
    
    protected List<Handler<Node>> copyHooks;
    
    public void addCopyHook(Handler<Node> h) {
	if (copyHooks == null || copyHooks.size() == 0)
	    copyHooks = Collections.singletonList(h);
	else if (copyHooks.size() == 1) {
	    ArrayList<Handler<Node>> hs = new ArrayList<Handler<Node>>(copyHooks.size()+1);
	    hs.addAll(copyHooks);
	    hs.add(h);
	    copyHooks = hs;
	}
    }

    public Node acceptChildren(final Object v, final Object... args) {
	return visitChildren(new NodeVisitor() {
	    public Node override(Node n) {
		return n.accept(v, args);
	    }
	});
    }
    
    public <T> T accept(Object v, Object... args) {
	return (T) new Dispatch.Dispatcher("visit").invoke(v, this, args);
    }
    
    public final int hashCode() {
    	return super.hashCode();
    }
    
    public final boolean equals(Object o) {
    	return this == o;
    }
    
    public Node_c(Position pos) {
    	assert(pos != null);
        this.position = pos;
        this.error = null;
        this.job = Globals.currentJob();
        assert job != null;

        this.checked = Types.<Node>lazyRef(null, Types.Granularity.OTHER);
        setChecked();
        addCopyHook(new Handler<Node>() {
	    public void handle(Node t) {
		((Node_c) t).setChecked();
	    }
	});
    }
    
    public Node_c(Position pos, Types.Granularity grain) {
    	assert(pos != null);
        this.position = pos;
        this.error = null;
        this.job = Globals.currentJob();
        assert job != null;
//        this.checked = Types.<Node>lazyRef(null, Granularity.OTHER);
//        StackTraceElement[] ste = Thread.currentThread().getStackTrace();
//        String className = ste[2].getClassName();
        
        this.checked = Types.<Node>lazyRef(null, grain);
        setChecked();
        addCopyHook(new Handler<Node>() {
	    public void handle(Node t) {
		((Node_c) t).setChecked();
	    }
	});
    }

    // FIXME: should only spawn the ref AFTER TypesInitialized.
    public void setChecked() {
	if (this.checked == null || this.checked.forced())
	    return;
        this.checked.setResolver(
        		new Runnable() {
            public void run() {
        	Node_c n = Node_c.this;
        	try {
		    Job job = n.job;
		    TypeSystem ts = Globals.TS();
		    NodeFactory nf = Globals.NF();
        	    TypeChecker v = new TypeChecker(job, ts, nf);
        	    Node m = n.accept(v, n.context());
        	    n.checkedRef().update(m);
        	    m.checkedRef().update(m);
        	}
        	catch (PassthruError pe) {
        	    Exception e = pe;
        	    while (e.getCause() instanceof InvocationTargetException) {
        		e = (Exception) e.getCause();
        	    }
        	    if (e.getCause() instanceof SemanticException) {
        		SemanticException x = (SemanticException) e.getCause();
			ErrorInfo error = new ErrorInfo(ErrorInfo.SEMANTIC_ERROR, x.getMessage() != null ? x.getMessage() : "unknown error",
							x.position() != null ? x.position() : n.position());
			Node m = n.addError(error);
        		n.checkedRef().update(m);
        	    }
        	    else {
        		throw new InternalCompilerError(e.getCause());
        	    }
        	}
            }	   
        }, jobClock());
    }
    
    public void init(Node node) {
        if (node != this) {
            throw new InternalCompilerError("Cannot use a Node as a delegate or extension.");
        }
    }

    public Node node() {
        return this;
    }

    public Object copy() {
        try {
            Node_c n = (Node_c) super.clone();
            
            for (Handler<Node> h : n.copyHooks) {
        	try {
		    h.handle(n);
		}
		catch (Exception e) {
		    throw new InternalCompilerError(e.getMessage(), n.position(), e);
		}
            }
            
            return n;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalCompilerError("Java clone() weirdness.");
        }
    }

    public Position position() {
	return this.position;
    }

    public Node position(Position position) {
	Node_c n = (Node_c) copy();
	n.position = position;
	return n;
    }
    
    public boolean hasErrors() {
	return errors().size() > 0;
    }

    public List<ErrorInfo> errors() {
	if (error == null)
	    return Collections.EMPTY_LIST;
	else
	    return error;
    }
    
    public Node addError(ErrorInfo error) {
        Node_c n = (Node_c) copy();
        n.error = new ArrayList<ErrorInfo>((this.error != null ? this.error.size() : 0) + 1);
        if (this.error != null)
            n.error.addAll(this.error);
        n.error.add(error);
        return n;
    }
    
    public Node visitChild(Node n, NodeVisitor v) {
	if (n == null) {
	    return null;
	}

	return v.visitEdge(this, n);
    }

    public Node visit(NodeVisitor v) {
	return v.visitEdge(null, this);
    }

    /** 
     * @deprecated Call {@link Node#visitChild(Node, NodeVisitor)} instead.
     */
    public Node visitEdge(Node parent, NodeVisitor v) {
	Node n = v.override(parent, this);

	if (n == null) {
	    NodeVisitor v_ = v.enter(parent, this);

	    if (v_ == null) {
		throw new InternalCompilerError(
		    "NodeVisitor.enter() returned null.");
	    }

	    n = this.visitChildren(v_);

	    if (n == null) {
		throw new InternalCompilerError(
		    "Node_c.visitChildren() returned null.");
	    }

	    n = v.leave(parent, this, n, v_);

	    if (n == null) {
		throw new InternalCompilerError(
		    "NodeVisitor.leave() returned null.");
	    }
	}
    
	return n;
    }

    /**
     * Visit all the elements of a list.
     * @param l The list to visit.
     * @param v The visitor to use.
     * @return A new list with each element from the old list
     *         replaced by the result of visiting that element.
     *         If <code>l</code> is a <code>TypedList</code>, the
     *         new list will also be typed with the same type as 
     *         <code>l</code>.  If <code>l</code> is <code>null</code>,
     *         <code>null</code> is returned.
     */
    public List visitList(List l, NodeVisitor v) {
	if (l == null) {
	    return null;
	}

	List result = l;
	List vl = new ArrayList(l.size());
	
	for (Iterator i = l.iterator(); i.hasNext(); ) {
	    Node n = (Node) i.next();
	    Node m = visitChild(n, v);
	    if (n != m) {
	        result = vl;
	    }
            if (m instanceof NodeList) {
                vl.addAll(((NodeList) m).nodes());
            } else if (m != null) {
	        vl.add(m);
	    }
	}

	return result;
    }

    public Node visitChildren(NodeVisitor v) {
	return this;
    }

    /**
     * Push a new scope upon entering this node, and add any declarations to the
     * context that should be in scope when visiting children of this node.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting this node. 
     */
    public Context enterScope(Context c) { return c; }

    /**
     * Push a new scope for visiting the child node <code>child</code>. 
     * The default behavior is to delegate the call to the child node, and let
     * it add appropriate declarations that should be in scope. However,
     * this method gives parent nodes have the ability to modify this behavior.
     * @param child the child node about to be entered.
     * @param c the current <code>Context</code>
     * @return the <code>Context</code> to be used for visiting node 
     *           <code>child</code>
     */
    public Context enterChildScope(Node child, Context c) { 
        return child.enterScope(c); 
    }

    /**
     * Add any declarations to the context that should be in scope when
     * visiting later sibling nodes.
     */
    public void addDecls(Context c) { }
    
    /** Dump the AST for debugging. */
    public void dump(OutputStream os) {
        CodeWriter cw = Compiler.createCodeWriter(os);
        NodeVisitor dumper = new DumpAst(cw);
        dumper = dumper.begin();
        this.visit(dumper);
        cw.newline();
        dumper.finish();
    }
    
    /** Dump the AST for debugging. */
    public void dump(Writer w) {
        CodeWriter cw = Compiler.createCodeWriter(w);
        NodeVisitor dumper = new DumpAst(cw);
        dumper = dumper.begin();
        this.visit(dumper);
        cw.newline();
        dumper.finish();
    }
    
    /** Pretty-print the AST for debugging. */
    public void prettyPrint(OutputStream os) {
	new NewPrettyPrinter(os).printAst(this);
    }

    /** Pretty-print the AST for debugging. */
    public void prettyPrint(Writer w) {
	new NewPrettyPrinter(w).printAst(this);
    }

    public void dump(CodeWriter w) {
        w.write(StringUtil.getShortNameComponent(getClass().getName()));

        w.allowBreak(4, " ");
        w.begin(0);
        w.write("(position " + (position != null ? position.toString()
                                                  : "UNKNOWN") + ")");
        w.end();
    }

    public String toString() {
          // This is really slow and so you are encouraged to override.
          // return new StringPrettyPrinter(5).toString(this);

          // Not slow anymore.
          return getClass().getName();
    }
    public final Node copy(NodeFactory nf) {
        throw new InternalCompilerError("Unimplemented operation. This class " +
                                        "(" + this.getClass().getName() + ") does " +
                                        "not implement copy(NodeFactory). This compiler extension should" +
                                        " either implement the method, or not invoke this method.");
    }
    public final Node copy(ExtensionInfo extInfo) throws SemanticException {
        return this.copy(extInfo.nodeFactory());
    }

}
