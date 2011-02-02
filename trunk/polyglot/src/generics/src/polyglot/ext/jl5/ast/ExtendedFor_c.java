package polyglot.ext.jl5.ast;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.Term;
import polyglot.ast.Loop_c;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.types.ArrayType;
import polyglot.types.Context;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.AscriptionVisitor;
import polyglot.visit.CFGBuilder;
import polyglot.visit.FlowGraph;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

/**
 * An immutable representation of a Java language extended <code>for</code>
 * statement.  Contains a type and id and an expression
 * to be iterated over.
 */
public class ExtendedFor_c extends Loop_c implements ExtendedFor {
    protected List varDecls;
    protected Expr expr;
    protected Stmt body;

    public ExtendedFor_c(Position pos, List varDecls, Expr expr, Stmt body) {
	    super(pos);
        this.varDecls = varDecls;
        this.expr = expr;
	    this.body = body;
    }

    /** Loop body */
    public Stmt body() {
	    return this.body;
    }

    /** Set the body of the statement. */
    public ExtendedFor body(Stmt body) {
	    ExtendedFor_c n = (ExtendedFor_c) copy();
	    n.body = body;
	    return n;
    }

    /** Reconstruct the statement. */
    protected ExtendedFor_c reconstruct(List varDecls, Expr expr, Stmt body) {
	    if (! CollectionUtil.allEqual(varDecls, this.varDecls) || expr != this.expr || body != this.body) {
	        ExtendedFor_c n = (ExtendedFor_c) copy();
            n.varDecls = varDecls;
            n.expr = expr;
	        n.body = body;
	        return n;
	    }

	    return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
        List varDecls = visitList(this.varDecls, v);
        Expr expr = (Expr) visitChild(this.expr, v);
	    Stmt body = (Stmt) visitChild(this.body, v);
	    return reconstruct(varDecls, expr, body);
    }

    public Context enterScope(Context c) {
	    return c.pushBlock();
    }

    /** Type check the statement. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
	    JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();

        // Check that the expr is an array or of type Iterable
        Type t = expr.type();
        if (t.isArray()){
            ArrayType aType = (ArrayType)t;
            t = aType.base();
        }
        //Milan: not sure if it is better to return raw type from Iterable() or to rawify it on the fly
        else if (ts.isSubtype(t, ts.rawify(ts.Iterable()))){
            t = ts.findGenericSupertype(ts.Iterable(), (ReferenceType) t);
            if (t == null) {
                t = ts.Object();
            }
            else {
                t = ((ParameterizedType)t).typeArguments().get(0);
            }
                
        }
        else {
            throw new SemanticException("Can only iterate over an array or an instance of java.util.Iterable", expr.position()); 
        }
        // Check that type is the same as elements in expr
        LocalDecl ld = (LocalDecl)varDecls.get(0);
        Type declType = ld.type().type();
        if (!ts.isImplicitCastValid(t, declType)){
            throw new SemanticException("Incompatible types in for loop. Declared type is " + 
                    declType + " but the actual type is " + t + ".", expr.position());
        }

        if (expr instanceof Local && ld.localInstance().equals(((Local)expr).localInstance())){
            throw new SemanticException("Variable: "+expr+" may not have been initialized", expr.position());
        }
        if (expr instanceof NewArray){
            if (((NewArray)expr).init() != null){
                for (Iterator it = ((NewArray)expr).init().elements().iterator(); it.hasNext(); ){
                    Expr next = (Expr)it.next();
                    if (next instanceof Local && ld.localInstance().equals(((Local)next).localInstance())){
                        throw new SemanticException("Varaible: "+next+" may not have been initialized", next.position());
                    }
                }
            }
        }
        
	    return this;
    }

    public Block updateBody(Expr la, Stmt origLd, NodeFactory nf){
        Block b = null;
        if (body() instanceof Block){
            b = ((Block)body()).prepend(nf.Eval(position(), la)).prepend(origLd);
        }
        else {
            b = nf.Block(position()).prepend(body()).prepend(nf.Eval(position(), la)).prepend(origLd);
        }
        return b;
    }
    
    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        return child.type();
    }

    public void printVarDecl(Object decl, CodeWriter w, PrettyPrinter tr){
        if (decl instanceof LocalDecl){
            LocalDecl ld = (LocalDecl)decl;
            w.write(ld.flags().translate());
            print(ld.type(), w, tr);
            w.write(" ");
            w.write(ld.name());
        }
    }

    /** Write the statement to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("for (");
        for (Iterator it = varDecls.iterator(); it.hasNext();){
            printVarDecl(it.next(), w, tr);
        }
        w.write(" : ");
        print(expr, w, tr);
        w.write(")");
        printSubStmt(body, w, tr);
    }

    public String toString() {
	    return "for (...) ...";
    }

    public Term entry() {
        return expr;
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        v.visitCFG(expr, FlowGraph.EDGE_KEY_TRUE, body.entry(), FlowGraph.EDGE_KEY_FALSE, this);

        v.push(this).visitCFG(body, expr);
        return succs;
    }

    public Term continueTarget() {
        return body.entry();
    }

    public boolean condIsConstant(){
        return false;
    }

    public Expr cond(){
        return null;
    }
}