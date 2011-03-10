/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.LocalInstance;
import polyglot.types.Ref;
import polyglot.types.Types;
import polyglot.types.VarInstance;
import polyglot.types.Types.Granularity;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/** 
 * A local variable expression.
 */
public class Local_c extends Expr_c implements Local
{
  protected Id name;
  protected Ref<LocalInstance> li;

  public Local_c(Position pos, Id name) {
    super(pos);
    assert(name != null);
    this.name = name;
    this.li = Types.<LocalInstance>lazyRef(Granularity.LOWER_LEVEL);
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
  
  /** Get the local instance of the local. */
  public VarInstance varInstance() {
    return li.get();
  }

  /** Get the local instance of the local. */
  public LocalInstance localInstance() {
    return li.get();
  }

  /** Set the local instance of the local. */
  public Local localInstance(LocalInstance li) {
      this.li.update(li);
      return this;
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


  public String toString() {
    return name.toString();
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

  public Ref<LocalInstance> localInstanceRef() {
      return li;
  }
}
