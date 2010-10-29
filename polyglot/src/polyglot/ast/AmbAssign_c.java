/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;

/**
 * A <code>AmbAssign</code> represents a Java assignment expression to
 * an as yet unknown expression.
 */
public class AmbAssign_c extends Assign_c implements AmbAssign
{
  protected Expr left;
    
  public AmbAssign_c(Position pos, Expr left, Operator op, Expr right) {
    super(pos, op, right);
    this.left = left;
  }
  
  public Expr left() {
      return left;
  }
  
  public Type leftType() {
      return left.type();
  }
  
  @Override
  public Expr left(NodeFactory nf) {
      return left;
  }

  @Override
  public Assign visitLeft(NodeVisitor v) {
      Expr left = (Expr) visitChild(this.left, v);
      if (left != this.left) {
	  AmbAssign_c n = (AmbAssign_c) copy();
	  n.left = left;
	  return n;
      }
      return this;
  }
  
  public Term firstChild() {
      return left;
  }
  
  protected void acceptCFGAssign(CFGBuilder v) {
      v.visitCFG(left, right(), ENTRY);
      v.visitCFG(right(), this, EXIT);
  }
  
  protected void acceptCFGOpAssign(CFGBuilder v) {
      v.visitCFG(left, right(), ENTRY);
      v.visitCFG(right(), this, EXIT);
  }
}
