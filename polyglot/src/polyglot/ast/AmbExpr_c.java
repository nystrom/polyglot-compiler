package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.types.*;
import jltools.visit.*;
import jltools.util.*;

/**
 * An <code>AmbExpr</code> is an ambiguous AST node composed of a single
 * identifier that must resolve to an expression.
 */
public class AmbExpr_c extends Expr_c implements AmbExpr
{
  protected String name;

  public AmbExpr_c(Del ext, Position pos, String name) {
    super(ext, pos);
    this.name = name;
  }

  /** Get the name of the expression. */
  public String name() {
    return this.name;
  }

  /** Set the name of the expression. */
  public AmbExpr name(String name) {
    AmbExpr_c n = (AmbExpr_c) copy();
    n.name = name;
    return n;
  }

  /** Disambiguate the expression. */
  public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
    Node n = ar.nodeFactory().disamb().disambiguate(ar, position(),
                                                    null, name);

    if (n instanceof Expr) {
      return n;
    }

    throw new SemanticException("Could not find field or local " +
                                "variable \"" + name + "\".", position());
  }

  /** Type check the expression. */
  public Node typeCheck(TypeChecker tc) throws SemanticException {
    throw new InternalCompilerError(position(),
                                    "Cannot type check ambiguous node "
                                    + this + ".");
  } 

  /** Check exceptions thrown by the expression. */
  public Node exceptionCheck(ExceptionChecker ec) throws SemanticException {
    throw new InternalCompilerError(position(),
                                    "Cannot exception check ambiguous node "
                                    + this + ".");
  } 

  /** Write the expression to an output file. */
  public void translate(CodeWriter w, Translator tr) {
    throw new InternalCompilerError(position(),
                                    "Cannot translate ambiguous node "
                                    + this + ".");
  }

  public String toString() {
    return name + "{amb}";
  }

  public void dump(CodeWriter w) {
    super.dump(w);
    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(name \"" + name + "\")");
    w.end();
  }
}
