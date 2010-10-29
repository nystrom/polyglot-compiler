/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/** 
 * A local variable expression.
 */
public class Local_c extends Expr_c implements Local
{
  protected Id name;
  protected LocalInstance li;

  public Local_c(Position pos, Id name) {
    super(pos);
    assert(name != null);
    this.name = name;
  }

  /** Get the precedence of the local. */
  public Precedence precedence() { 
    return Precedence.LITERAL;
  }

  /** Get the name of the local. */
  public Id name() {
    return this.name;
  }
  
  /** Set the name of the local. */
  public Local name(Id name) {
      Local_c n = (Local_c) copy();
      n.name = name;
      return n;
  }
  
  /** Return the access flags of the variable. */
  public Flags flags() {
    return li.flags();
  }

  /** Get the local instance of the local. */
  public VarInstance varInstance() {
    return li;
  }

  /** Get the local instance of the local. */
  public LocalInstance localInstance() {
    return li;
  }

  /** Set the local instance of the local. */
  public Local localInstance(LocalInstance li) {
    if (li == this.li) return this;
    Local_c n = (Local_c) copy();
    n.li = li;
    return n;
  }

  /** Reconstruct the expression. */
  protected Local_c reconstruct(Id name) {
      if (name != this.name) {
          Local_c n = (Local_c) copy();
          n.name = name;
          return n;
      }
      
      return this;
  }
  
  /** Visit the children of the constructor. */
  public Node visitChildren(NodeVisitor v) {
      Id name = (Id) visitChild(this.name, v);
      return reconstruct(name);
  }

  public Term firstChild() {
      return null;
  }

  public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
      return succs;
  }

  public String toString() {
    return name.toString();
  }

  /** Write the local to an output file. */
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
      tr.print(this, name, w);
  }

  /** Dumps the AST. */
  public void dump(CodeWriter w) {
    super.dump(w);

    if (li != null) {
	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(instance " + li + ")");
	w.end();
    }
  }
}