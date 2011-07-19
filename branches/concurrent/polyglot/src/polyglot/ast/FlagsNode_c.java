/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2006-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;


import polyglot.types.Flags;
import polyglot.util.CodeWriter;
import polyglot.util.Position;

public class FlagsNode_c extends Node_c implements FlagsNode
{
  protected Flags flags;

  public FlagsNode_c(Position pos, Flags flags) {
    super(pos);
    assert(flags != null);
    this.flags = flags;
  }

  // Override to make Name.equals(Flags) a compile-time error
  public final void equals(Flags s) { }

  /** Get the name of the expression. */
  public Flags flags() {
    return this.flags;
  }

  /** Set the name of the expression. */
  public FlagsNode flags(Flags flags) {
    FlagsNode_c n = (FlagsNode_c) copy();
    n.flags = flags;
    return n;
  }

  public String toString() {
    return flags.toString();
  }

  public void dump(CodeWriter w) {
    w.write("(Flags \"" + flags + "\")");
  }
  

}