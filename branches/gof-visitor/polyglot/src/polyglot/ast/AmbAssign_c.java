/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.*;

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
  
  public Node typeCheck(ContextVisitor tc) throws SemanticException {
      AmbAssign_c n = (AmbAssign_c) super.typeCheck(tc);
      
      if (left instanceof Local) {
          LocalAssign a = tc.nodeFactory().LocalAssign(n.position(), (Local)left, operator(), right());
          return a.del().typeCheck(tc);
      }
      else if (left instanceof Field) {
          FieldAssign a = tc.nodeFactory().FieldAssign(n.position(), ((Field)left).target(), ((Field)left).name(), operator(), right());
          a = a.targetImplicit(((Field) left).isTargetImplicit());
          a = a.fieldInstance(((Field) left).fieldInstance());
          return a.del().typeCheck(tc);
      } 
      else if (left instanceof ArrayAccess) {
          ArrayAccessAssign a = tc.nodeFactory().ArrayAccessAssign(n.position(), ((ArrayAccess)left).array(), ((ArrayAccess)left).index(), operator(), right());
          return a.del().typeCheck(tc);
      }

      // LHS is still ambiguous.  The pass should get rerun later.
      return this;
      // throw new SemanticException("Could not disambiguate left side of assignment!", n.position());
  }
  
  public Assign typeCheckLeft(ContextVisitor tc) throws SemanticException {
      // Didn't finish disambiguation; just return.
      return this;
  }
}
