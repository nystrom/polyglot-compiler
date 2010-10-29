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
 * A <code>ClassLit</code> represents a class literal expression. 
 * A class literal expressions is an expression consisting of the 
 * name of a class, interface, array, or primitive type followed by a period (.) 
 * and the token class. 
 */
public class ClassLit_c extends Lit_c implements ClassLit
{
  protected TypeNode typeNode;

  public ClassLit_c(Position pos, TypeNode typeNode) {
    super(pos);
    assert(typeNode != null);
    this.typeNode = typeNode;
  }

  public TypeNode typeNode() {
    return this.typeNode;
  }

  public ClassLit typeNode(TypeNode typeNode) {
      if (this.typeNode == typeNode) {
          return this;
      }
    ClassLit_c n = (ClassLit_c) copy();
    n.typeNode = typeNode;
    return n;
  }
    
  /**
   * Cannot return the correct object (except for maybe
   * some of the primitive arrays), so we just return null here. 
   */
  public Object objValue() {
    return null;
  }

  public Node visitChildren(NodeVisitor v) {
    TypeNode tn = (TypeNode)visitChild(this.typeNode, v);
    return this.typeNode(tn);
  }

  public String toString() {
    return typeNode.toString() + ".class";
  }
}
