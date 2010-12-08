/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * An <code>AmbExpr</code> is an ambiguous AST node composed of a single
 * identifier that must resolve to an expression.
 */
public class AmbExpr_c extends Expr_c implements AmbExpr
{
  protected Node child;

  public AmbExpr_c(Position pos, Node child) {
    super(pos);
    assert(child != null);
    this.child = child;
  }
  
  public Node child() {
      return this.child;
  }
  
  public AmbExpr child(Node child) {
      AmbExpr_c n = (AmbExpr_c) copy();
      n.child = child;
      return n;
  }

  /** Reconstruct the expression. */
  protected AmbExpr_c reconstruct(Node child) {
      if (child != this.child) {
          AmbExpr_c n = (AmbExpr_c) copy();
          n.child = child;
          return n;
      }
      return this;
  }
  
  /** Visit the children of the constructor. */
  public Node visitChildren(NodeVisitor v) {
      Node child = (Node) visitChild(this.child, v);
      return reconstruct(child);
  }

  public String toString() {
    return child.toString() + "{amb}";
  }
}
