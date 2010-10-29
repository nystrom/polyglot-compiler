package polyglot.dispatch;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.*;
import polyglot.ast.Binary.Operator;
import polyglot.frontend.Compiler;
import polyglot.frontend.Globals;
import polyglot.types.Flags;
import polyglot.util.*;

public class NewPrettyPrinter extends Visitor {

    CodeWriter w;
    
    Precedence precedence(Expr e) {
	return e.accept(new Object() {
	   public Precedence visit(Expr e) {
	       return Precedence.UNKNOWN;
	   }

	   public Precedence visit(FloatLit_c e) {
	       if (e.value() < 0) {
		   return Precedence.UNARY;
	       }
	       else {
		   return Precedence.LITERAL;
	       }
	   }

	   public Precedence visit(IntLit_c e) {
	       if (e.value() < 0L && ! e.boundary()) {
		   return Precedence.UNARY;
	       }
	       else {
		   return Precedence.LITERAL;
	       }
	   }

	   public Precedence visit(Binary_c e) {
	       Operator op = e.operator();
	       Expr left = e.left();
	       Expr right = e.right();
	       Precedence precedence = op.precedence();
	        if (op == Binary.ADD && (left instanceof StringLit || right instanceof StringLit)) {
	            precedence = Precedence.STRING_ADD;
	        }
	        return precedence;
	   }
	   public Precedence visit(Cast_c e) {
	       return Precedence.CAST;
	   }
	   public Precedence visit(AmbExpr_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Call_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Field_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(New_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Local_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(NewArray_c e) {
	       return Precedence.UNKNOWN;
	   }
	   public Precedence visit(Special_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Lit_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Conditional_c e) {
	       return Precedence.CONDITIONAL;
	   }
	   public Precedence visit(ArrayAccess_c e) {
	       return Precedence.LITERAL;
	   }
	   public Precedence visit(Assign_c e) {
	       return Precedence.ASSIGN;
	   }
	   public Precedence visit(Instanceof_c e) {
	       return Precedence.INSTANCEOF;
	   }
	   public Precedence visit(Unary_c e) {
	       return Precedence.UNARY;
	   }
	});
    }

    public NewPrettyPrinter(CodeWriter w) {
	this.w = w;
    }

    public NewPrettyPrinter(OutputStream os) {
	this.w = Compiler.createCodeWriter(os);
    }

    public NewPrettyPrinter(Writer w) {
	this.w = Compiler.createCodeWriter(w);
    }

    public NewPrettyPrinter() {
	this(System.out);
    }

    @Override
    public Node accept(Node n, Object... args) {
	return super.accept(n, args);
    }

    public Node visit(Node_c n) {
	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return (Node_c) acceptChildren(n);
    }

    /**
     * Flag indicating whether to print a ';' after certain statements. This is
     * used when pretty-printing for loops.
     */
    protected boolean appendSemicolon = true;
    /**
     * Flag indicating whether to print the type in a local declaration. This is
     * used when pretty-printing for loops.
     */
    protected boolean printType = true;

    /**
     * Print an AST node using the given code writer. The
     * <code>CodeWriter.flush()</code> method must be called after this method
     * to ensure code is output. Use <code>printAst</code> rather than this
     * method to print the entire AST; this method should only be called by
     * nodes to print their children.
     */
    public void print(Node parent, Node child, CodeWriter w) {
	if (child != null) {
	    accept(child);
	}
    }

    /**
     * Print an AST node using the given code writer. The code writer is flushed
     * by this method.
     */
    public void printAst(Node ast) {
	print(null, ast, w);

	try {
	    w.flush();
	}
	catch (IOException e) {
	}
    }

    public void printBlock(Node n, Node child) {
	w.begin(0);
	print(n, child);
	w.end();
    }

    public void printSubStmt(Node n, Stmt stmt) {
	if (stmt instanceof Block) {
	    w.write(" ");
	    print(n, stmt);
	}
	else {
	    w.allowBreak(4, " ");
	    printBlock(n, stmt);
	}
    }

    public void print(Node n, Node child) {
	print(n, child, w);
    }

    public Node visit(NewArray_c n) {
	w.write("new ");
	print(n, n.baseType());

	for (Iterator i = n.dims().iterator(); i.hasNext();) {
	    Expr e = (Expr) i.next();
	    w.write("[");
	    printBlock(n, e);
	    w.write("]");
	}

	for (int i = 0; i < n.additionalDims(); i++) {
	    w.write("[]");
	}

	if (n.init() != null) {
	    w.write(" ");
	    print(n, n.init());
	}
	return n;
    }

    /**
     * Correctly parenthesize the subexpression <code>expr<code> given
     * the its precendence and the precedence of the current expression.
     * 
     * If the sub-expression has the same precedence as this expression
     * we do not parenthesize.
     * 
     * @param expr
     *            The subexpression.
     * @param w
     *            The output writer.
     * @param pp
     *            The pretty printer.
     */
    public void printSubExpr(Expr n, Expr expr, CodeWriter w, NewPrettyPrinter pp) {
	printSubExpr(n, expr, true, w, pp);
    }

    /**
     * Correctly parenthesize the subexpression <code>expr<code> given
     * the its precedence and the precedence of the current expression.
     * 
     * If the sub-expression has the same precedence as this expression
     * we parenthesize if the sub-expression does not associate; e.g.,
     * we parenthesis the right sub-expression of a left-associative
     * operator.
     * 
     * @param expr
     *            The subexpression.
     * @param associative
     *            Whether expr is the left (right) child of a left- (right-)
     *            associative operator.
     * @param w
     *            The output writer.
     * @param pp
     *            The pretty printer.
     */
    public void printSubExpr(Expr n, Expr expr, boolean associative, CodeWriter w, NewPrettyPrinter pp) {
	if (!associative && precedence(n).equals(precedence(expr)) || precedence(n).isTighter(precedence(expr))) {
	    w.write("(");
	    printBlock(n, expr);
	    w.write(")");
	}
	else {
	    print(n, expr);
	}
    }

    public Node visit(Assign_c n) {
	printSubExpr(n, n.left(), true, w, this);
	w.write(" ");
	w.write(n.operator().toString());
	w.allowBreak(2, 2, " ", 1); // miser mode
	w.begin(0);
	printSubExpr(n, n.right(), false, w, this);
	w.end();
	return n;
    }

    public Node visit(ArrayAccess_c n) {
	printSubExpr(n, n.array(), w, this);
	w.write("[");
	printBlock(n, n.index());
	w.write("]");
	return n;
    }

    public Node visit(ArrayInit_c n) {
	w.write("{ ");

	for (Iterator<Expr> i = n.elements().iterator(); i.hasNext();) {
	    Expr e = i.next();

	    print(n, e);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0, " ");
	    }
	}

	w.write(" }");
	return n;
    }

    public Node visit(Assert_c n) {
	w.write("assert ");
	print(n, n.cond());

	if (n.errorMessage() != null) {
	    w.write(": ");
	    print(n, n.errorMessage());
	}

	w.write(";");
	return n;
    }

    public Node visit(Binary_c n) {
	printSubExpr(n, n.left(), true, w, this);
	w.write(" ");
	w.write(n.operator().toString());
	w.allowBreak(n.type() == null || n.type().isPrimitive() ? 2 : 0, " ");
	printSubExpr(n, n.right(), false, w, this);
	return n;
    }

    public Node visit(Unary_c n) {
	if (n.operator() == Unary_c.NEG && n.expr() instanceof IntLit && ((IntLit) n.expr()).boundary()) {
	    w.write(n.operator().toString());
	    w.write(((IntLit) n.expr()).positiveToString());
	}
	else if (n.operator().isPrefix()) {
	    w.write(n.operator().toString());
	    printSubExpr(n, n.expr(), false, w, this);
	}
	else {
	    printSubExpr(n, n.expr(), false, w, this);
	    w.write(n.operator().toString());
	}
	return n;
    }

    public Node visit(While_c n) {
	w.write("while (");
	printBlock(n, n.cond());
	w.write(")");
	printSubStmt(n, n.body());
	return n;
    }

    public Node visit(Do_c n) {
	w.write("do ");
	printSubStmt(n, n.body());
	w.write("while(");
	printBlock(n, n.cond());
	w.write("); ");
	return n;
    }

    public Node visit(If_c n) {
	w.write("if (");
	printBlock(n, n.cond());
	w.write(")");

	printSubStmt(n, n.consequent());

	if (n.alternative() != null) {
	    if (n.consequent() instanceof Block) {
		// allow the "} else {" formatting
		w.write(" ");
	    }
	    else {
		w.allowBreak(0, " ");
	    }

	    if (n.alternative() instanceof Block) {
		w.write("else ");
		print(n, n.alternative());
	    }
	    else {
		w.begin(4);
		w.write("else");
		printSubStmt(n, n.alternative());
		w.end();
	    }
	}
	return n;
    }

    public Node visit(Empty_c n) {
	w.write(";");
	return n;
    }

    public Node visit(Eval_c n) {
	boolean semi = appendSemicolon;
	appendSemicolon = true;

	print(n, n.expr());

	if (semi) {
	    w.write(";");
	}

	appendSemicolon = semi;
	return n;
    }

    public Node visit(Labeled_c n) {
	w.write(n.labelNode() + ": ");
	print(n, n.statement());
	return n;
    }

    public Node visit(Switch_c n) {
	w.write("switch (");
	printBlock(n, n.expr());
	w.write(") {");
	w.unifiedBreak(4);
	w.begin(0);

	boolean lastWasCase = false;
	boolean first = true;

	for (Iterator<SwitchElement> i = n.elements().iterator(); i.hasNext();) {
	    SwitchElement s = i.next();
	    if (s instanceof Case) {
		if (lastWasCase)
		    w.unifiedBreak(0);
		else if (!first)
		    w.unifiedBreak(0);
		printBlock(n, s);
		lastWasCase = true;
	    }
	    else {
		w.unifiedBreak(4);
		print(n, s);
		lastWasCase = false;
	    }

	    first = false;
	}

	w.end();
	w.unifiedBreak(0);
	w.write("}");
	return n;
    }

    public Node visit(Call_c n) {
	w.begin(0);
	if (!n.isTargetImplicit()) {
	    if (n.target() instanceof Expr) {
		printSubExpr(n, (Expr) n.target(), w, this);
	    }
	    else if (n.target() != null) {
		print(n, n.target());
	    }
	    w.write(".");
	    w.allowBreak(2, 3, "", 0);
	}

	w.write(n.name() + "(");
	if (n.arguments().size() > 0) {
	    w.allowBreak(2, 2, "", 0); // miser mode
	    w.begin(0);

	    for (Iterator<Expr> i = n.arguments().iterator(); i.hasNext();) {
		Expr e = i.next();
		print(n, e);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(0, " ");
		}
	    }

	    w.end();
	}
	w.write(")");
	w.end();
	return n;
    }

    public Node visit(Cast_c n) {
	w.begin(0);
	w.write("(");
	print(n, n.castType());
	w.write(")");
	w.allowBreak(2, " ");
	printSubExpr(n, n.expr(), w, this);
	w.end();
	return n;
    }

    public Node visit(For_c n) {
	w.write("for (");
	w.begin(0);

	if (n.inits() != null) {
	    boolean first = true;
	    for (Iterator i = n.inits().iterator(); i.hasNext();) {
		ForInit s = (ForInit) i.next();
		printForInits(n, first, s);
		first = false;

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(2, " ");
		}
	    }
	}

	w.write(";");
	w.allowBreak(0);

	if (n.cond() != null) {
	    printBlock(n, n.cond());
	}

	w.write(";");
	w.allowBreak(0);

	if (n.iters() != null) {
	    for (Iterator i = n.iters().iterator(); i.hasNext();) {
		ForUpdate s = (ForUpdate) i.next();
		printForUpdate(n, s);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(2, " ");
		}
	    }
	}

	w.end();
	w.write(")");

	printSubStmt(n, n.body());
	return n;
    }

    private void printForUpdate(For_c n, ForUpdate s) {
	boolean oldSemiColon = appendSemicolon;
	appendSemicolon = false;
	printBlock(n, s);
	appendSemicolon = oldSemiColon;
    }

    private void printForInits(For_c n, boolean first, ForInit s) {
	boolean oldSemiColon = appendSemicolon;
	appendSemicolon = false;
	boolean oldPrintType = printType;
	printType = first;
	printBlock(n, s);
	printType = oldPrintType;
	appendSemicolon = oldSemiColon;
    }

    public Node visit(LocalDecl_c n) {
	boolean oldSemiColon = appendSemicolon;
	appendSemicolon = true;
	boolean oldPrintType = printType;
	printType = true;

	print(n, n.flags());
	if (oldPrintType) {
	    print(n, n.typeNode());
	    w.write(" ");
	}
	print(n, n.name(), w);

	if (n.init() != null) {
	    w.write(" =");
	    w.allowBreak(2, " ");
	    print(n, n.init());
	}

	if (oldSemiColon) {
	    w.write(";");
	}

	printType = oldPrintType;
	appendSemicolon = oldSemiColon;

	return n;
    }

    public Node visit(Local_c n) {
	print(n, n.name(), w);
	return n;
    }

    public Node visit(Special_c n) {
	if (n.qualifier() != null) {
	    print(n, n.qualifier());
	    w.write(".");
	}
	switch (n.kind()) {
	case SUPER:
	    w.write("super");
	    break;
	case THIS:
	    w.write("this");
	    break;
	default:
	    throw new InternalCompilerError("Unrecognized Special kind " + n.kind());
	}
	return n;
    }

    public Node visit(ConstructorCall_c n) {
	if (n.qualifier() != null) {
	    print(n, n.qualifier());
	    w.write(".");
	}

	switch (n.kind()) {
	case SUPER:
	    w.write("super");
	    break;
	case THIS:
	    w.write("this");
	    break;
	default:
	    throw new InternalCompilerError("Unrecognized ConstructorCall kind " + n.kind());
	}

	w.write("(");

	w.begin(0);

	for (Iterator i = n.arguments().iterator(); i.hasNext();) {
	    Expr e = (Expr) i.next();
	    print(n, e);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();

	w.write(");");
	return n;
    }

    public Node visit(New_c n) {
	printQualifier(n);
	w.write("new ");

	// We need to be careful when pretty printing "new" expressions for
	// member classes. For the expression "e.new C()" where "e" has
	// static type "T", the TypeNode for "C" is actually the type "T.C".
	// But, if we print "T.C", the post compiler will try to lookup "T"
	// in "T". Instead, we print just "C".
	if (n.qualifier() != null) {
	    w.write(n.objectType().nameString());
	}
	else {
	    print(n, n.objectType());
	}

	printArgs(n);
	printBody(n);
	return n;
    }

    private void printBody(New_c n) {
	if (n.body() != null) {
	    w.write(" {");
	    print(n, n.body());
	    w.write("}");
	}
    }

    private void printArgs(New_c n) {
	w.write("(");
	w.allowBreak(2, 2, "", 0);
	w.begin(0);

	for (Iterator<Expr> i = n.arguments().iterator(); i.hasNext();) {
	    Expr e = i.next();

	    print(n, e);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();
	w.write(")");
    }

    private void printQualifier(New_c n) {
	if (n.qualifier() != null) {
	    print(n, n.qualifier());
	    w.write(".");
	}
    }

    public Node visit(Case_c n) {
	if (n.expr() == null) {
	    w.write("default:");
	}
	else {
	    w.write("case ");
	    print(n, n.expr());
	    w.write(":");
	}
	return n;
    }

    public Node visit(Block_c n) {
	w.write("{");
	w.unifiedBreak(4, 1, " ", 1);
	w.begin(0);
	visit((AbstractBlock_c) n);
	w.end();
	w.unifiedBreak(0, 1, " ", 1);
	w.write("}");
	return n;
    }
    
    public Node visit(AbstractBlock_c n) {
	w.begin(0);

	for (Iterator<Stmt> i = n.statements().iterator(); i.hasNext(); ) {
	    Stmt s = i.next();
	    
	    printBlock(n, s);

	    if (i.hasNext()) {
		w.newline();
	    }
	}

	w.end();
	return n;
    }

    public Node visit(Conditional_c n) {
	printSubExpr(n, n.cond(), false, w, this);
	w.unifiedBreak(2);
	w.write("? ");
	printSubExpr(n, n.consequent(), false, w, this);
	w.unifiedBreak(2);
	w.write(": ");
	printSubExpr(n, n.alternative(), false, w, this);
	return n;
    }

    public Node visit(QualifiedName_c n) {
	if (n.prefix() != null) {
	    print(n, n.prefix());
	    w.write(".");
	}

	print(n, n.name(), w);
	return n;
    }

    public Node visit(AmbExpr_c n) {
	print(n, n.child(), w);
	return n;
    }

    public Node visit(Field_c n) {
	w.begin(0);
	if (!n.isTargetImplicit()) {
	    // explicit target.
	    if (n.target() instanceof Expr) {
		printSubExpr(n, (Expr) n.target(), w, this);
	    }
	    else if (n.target() instanceof TypeNode || n.target() instanceof AmbReceiver) {
		print(n, n.target());
	    }

	    w.write(".");
	    w.allowBreak(2, 3, "", 0);
	}
	print(n, n.name(), w);
	w.end();
	return n;
    }

    public Node visit(AmbTypeNode_c n) {
	print(n, n.child(), w);
	return n;
    }

    public Node visit(AmbQualifierNode_c n) {
	print(n, n.child(), w);
	return n;
    }

    public Node visit(AmbReceiver_c n) {
	print(n, n.child(), w);
	return n;
    }

    public Node visit(ArrayTypeNode_c n) {
	print(n, n.base());
	w.write("[]");
	return n;
    }

    public Node visit(CanonicalTypeNode_c n) {
	if (n.typeRef() == null) {
	    w.write("<unknown-type>");
	}
	else {
	    n.typeRef().get().print(w);
	}
	return n;
    }

    public Node visit(Try_c n) {
	w.write("try");
	printSubStmt(n, n.tryBlock());

	for (Iterator<Catch> it = n.catchBlocks().iterator(); it.hasNext();) {
	    Catch cb = it.next();
	    w.newline(0);
	    printBlock(n, cb);
	}

	if (n.finallyBlock() != null) {
	    w.newline(0);
	    w.write("finally");
	    printSubStmt(n, n.finallyBlock());
	}
	return n;
    }

    public Node visit(Branch_c n) {
	switch (n.kind()) {
	case BREAK:
	    w.write("break");
	    break;
	case CONTINUE:
	    w.write("continue");
	    break;
	}
	if (n.labelNode() != null) {
	    w.write(" " + n.labelNode());
	}
	w.write(";");
	return n;
    }

    public Node visit(Catch_c n) {
	w.write("catch (");
	printBlock(n, n.formal());
	w.write(")");
	printSubStmt(n, n.body());
	return n;
    }

    public Node visit(FlagsNode_c n) {
	w.write(n.flags().translate());
	return n;
    }

    public Node visit(Id_c n) {
	w.write(n.id().toString());
	return n;
    }

    public Node visit(Import_c n) {
	if (!Globals.Options().fully_qualified_names) {
	    w.write("import ");
	    w.write(n.name().toString());

	    if (n.kind() == Import_c.PACKAGE) {
		w.write(".*");
	    }

	    w.write(";");
	    w.newline(0);
	}
	return n;
    }

    public Node visit(Return_c n) {
	w.write("return");
	if (n.expr() != null) {
	    w.write(" ");
	    print(n, n.expr());
	}
	w.write(";");
	return n;
    }

    public Node visit(PackageNode_c n) {
	if (n.package_() == null) {
	    w.write("<unknown-package>");
	}
	else {
	    n.package_().get().print(w);
	}
	return n;
    }

    public Node visit(Throw_c n) {
	w.write("throw ");
	print(n, n.expr());
	w.write(";");
	return n;
    }

    public Node visit(MethodDecl_c n) {
	printProcHeader(n);

	if (n.body() != null) {
	    printSubStmt(n, n.body());
	}
	else {
	    w.write(";");
	}
	return n;
    }

    public Node visit(FieldDecl_c n) {
	boolean isInterface = n.fieldDef() != null && n.fieldDef().container() != null && n.fieldDef().container().get().toClass().flags().isInterface();

	Flags f = n.flags().flags();

	if (isInterface) {
	    f = f.clearPublic();
	    f = f.clearStatic();
	    f = f.clearFinal();
	}

	w.write(f.translate());
	print(n, n.type());
	w.allowBreak(2, 2, " ", 1);
	print(n, n.name(), w);

	if (n.init() != null) {
	    w.write(" =");
	    w.allowBreak(2, " ");
	    print(n, n.init());
	}

	w.write(";");
	return n;
    }

    public Node visit(ConstructorDecl_c n) {
	printProcHeader(n);

	if (n.body() != null) {
	    printSubStmt(n, n.body());
	}
	else {
	    w.write(";");
	}
	return n;
    }

    /** Write the method to an output file. */
    private void printProcHeader(MethodDecl_c n) {
	w.begin(0);
	print(n, n.flags());
	print(n, n.returnType());
	w.allowBreak(2, 2, " ", 1);
	w.write(n.name() + "(");

	w.allowBreak(2, 2, "", 0);
	w.begin(0);

	for (Iterator<Formal> i = n.formals().iterator(); i.hasNext();) {
	    Formal f = i.next();

	    print(n, f);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0, " ");
	    }
	}

	w.end();
	w.write(")");

	if (!n.throwTypes().isEmpty()) {
	    w.allowBreak(6);
	    w.write("throws ");

	    for (Iterator i = n.throwTypes().iterator(); i.hasNext();) {
		TypeNode tn = (TypeNode) i.next();
		print(n, tn);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(4, " ");
		}
	    }
	}

	w.end();
    }

    private void printProcHeader(ConstructorDecl_c n) {
	w.begin(0);

	print(n, n.flags(), w);
	print(n, n.name(), w);
	w.write("(");

	w.begin(0);

	for (Iterator i = n.formals().iterator(); i.hasNext();) {
	    Formal f = (Formal) i.next();
	    print(n, f);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0, " ");
	    }
	}

	w.end();
	w.write(")");

	if (!n.throwTypes().isEmpty()) {
	    w.allowBreak(6);
	    w.write("throws ");

	    for (Iterator i = n.throwTypes().iterator(); i.hasNext();) {
		TypeNode tn = (TypeNode) i.next();
		print(n, tn);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(4, " ");
		}
	    }
	}

	w.end();
    }

    public Node visit(ClassBody_c n) {
	if (!n.members().isEmpty()) {
	    w.newline(4);
	    w.begin(0);
	    ClassMember prev = null;

	    for (Iterator<ClassMember> i = n.members().iterator(); i.hasNext();) {
		ClassMember member = i.next();
		if ((member instanceof polyglot.ast.CodeDecl) || (prev instanceof polyglot.ast.CodeDecl)) {
		    w.newline(0);
		}
		prev = member;
		printBlock(n, member);
		if (i.hasNext()) {
		    w.newline(0);
		}
	    }

	    w.end();
	    w.newline(0);
	}
	return n;
    }

    public Node visit(SwitchBlock_c n) {
	w.begin(0);

	for (Iterator<Stmt> i = n.statements().iterator(); i.hasNext();) {
	    Stmt n1 = i.next();

	    printBlock(n, n1);

	    if (i.hasNext()) {
		w.newline();
	    }
	}

	w.end();
	return n;
    }

    public Node visit(LocalClassDecl_c n) {
	printBlock(n, n.decl());
	w.write(";");
	return n;
    }

    public Node visit(IntLit_c n) {
	switch (n.kind()) {
	case INT:
	    w.write(Integer.toString((int) n.value()));
	    break;
	case LONG:
	    w.write(Long.toString(n.value()) + "L");
	    break;
	default:
	    throw new InternalCompilerError("Unrecognized IntLit kind " + n.kind());
	}

	return n;
    }

    public Node visit(FloatLit_c n) {
	switch (n.kind()) {
	case FLOAT:
	    w.write(Float.toString((float) n.value()) + "F");
	    break;
	case DOUBLE:
	    w.write(Double.toString(n.value()));
	    break;
	default:
	    throw new InternalCompilerError("Unrecognized FloatLit kind " + n.kind());
	}
	return n;
    }

    public Node visit(StringLit_c n) {
	List<String> l = n.breakupString();

	// If we break up the string, parenthesize it to avoid precedence bugs.
	if (l.size() > 1) {
	    w.write("(");
	}

	w.begin(0);

	for (Iterator i = l.iterator(); i.hasNext();) {
	    String s = (String) i.next();

	    w.write("\"");
	    w.write(StringUtil.escape(s));
	    w.write("\"");

	    if (i.hasNext()) {
		w.write(" +");
		w.allowBreak(0, " ");
	    }
	}

	w.end();

	if (l.size() > 1) {
	    w.write(")");
	}
	return n;
    }

    public Node visit(CharLit_c n) {
	w.write("'");
	w.write(StringUtil.escape((char) n.value()));
	w.write("'");
	return n;
    }

    public Node visit(BooleanLit_c n) {
	w.write(String.valueOf(n.value()));
	return n;
    }

    public Node visit(NullLit_c n) {
	w.write("null");
	return n;
    }

    public Node visit(ClassLit_c n) {
	w.begin(0);
	print(n, n.typeNode());
	w.write(".class");
	w.end();
	return n;
    }

    public Node visit(Instanceof_c n) {
	printSubExpr(n, n.expr(), w, this);
	w.write(" instanceof ");
	print(n, n.compareType());
	return n;
    }

    public Node visit(ClassDecl_c n) {
	printClassHeader(n);
	print(n, n.body());
	printClassFooter(n);
	return n;
    }

    private void printClassFooter(ClassDecl_c n) {
	w.write("}");
	w.newline(0);
    }

    private void printClassHeader(ClassDecl_c n) {
	w.begin(0);
	Flags flags = n.classDef() != null ? n.classDef().flags() : n.flags().flags();

	if (flags.isInterface()) {
	    w.write(flags.clearInterface().clearAbstract().translate());
	}
	else {
	    w.write(flags.translate());
	}

	if (flags.isInterface()) {
	    w.write("interface ");
	}
	else {
	    w.write("class ");
	}

	print(n, n.name(), w);

	if (n.superClass() != null) {
	    w.allowBreak(0);
	    w.write("extends ");
	    print(n, n.superClass());
	}

	if (!n.interfaces().isEmpty()) {
	    w.allowBreak(2);
	    if (flags.isInterface()) {
		w.write("extends ");
	    }
	    else {
		w.write("implements ");
	    }

	    w.begin(0);
	    for (Iterator<TypeNode> i = n.interfaces().iterator(); i.hasNext();) {
		TypeNode tn = (TypeNode) i.next();
		print(n, tn);

		if (i.hasNext()) {
		    w.write(",");
		    w.allowBreak(0);
		}
	    }
	    w.end();
	}
	w.unifiedBreak(0);
	w.end();
	w.write("{");
    }

    public Node visit(Initializer_c n) {
	w.begin(0);
	print(n, n.flags());
	print(n, n.body());
	w.end();
	return n;
    }

    public Node visit(Formal_c n) {
	print(n, n.flags());
	print(n, n.typeNode());
	w.write(" ");
	print(n, n.name(), w);
	return n;
    }

    public Node visit(SourceFile_c n) {
	w.write("<<<< " + n.source() + " >>>>");
	w.newline(0);

	if (n.package_() != null) {
	    w.write("package ");
	    print(n, n.package_());
	    w.write(";");
	    w.newline(0);
	    w.newline(0);
	}

	for (Iterator<Import> i = n.imports().iterator(); i.hasNext();) {
	    Import im = (Import) i.next();
	    print(n, im);
	}

	if (!n.imports().isEmpty()) {
	    w.newline(0);
	}

	for (Iterator<TopLevelDecl> i = n.decls().iterator(); i.hasNext();) {
	    TopLevelDecl d = (TopLevelDecl) i.next();
	    print(n, d);
	}
	return n;
    }
}
