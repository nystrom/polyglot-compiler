/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * An <code>AmbTypeNode</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a type.
 */
public class AmbTypeNode_c extends TypeNode_c implements AmbTypeNode {
	protected Prefix prefix;
	protected Id name;

//  protected Expr dummy;
  
  public AmbTypeNode_c(Position pos, Prefix qual,
                       Id name) {
    super(pos);
    assert(name != null); // qual may be null
    this.prefix = qual;
    this.name = name;
  }

  public Id name() {
      return this.name;
  }
  
  public AmbTypeNode name(Id name) {
      AmbTypeNode_c n = (AmbTypeNode_c) copy();
      n.name = name;
      return n;
  }
  
  public Prefix prefix() {
    return this.prefix;
  }

  public AmbTypeNode prefix(Prefix prefix) {
    AmbTypeNode_c n = (AmbTypeNode_c) copy();
    n.prefix = prefix;
    return n;
  }

  protected AmbTypeNode_c reconstruct(Prefix qual, Id name) {
    if (qual != this.prefix || name != this.name) {
      AmbTypeNode_c n = (AmbTypeNode_c) copy();
      n.prefix = qual;
      n.name = name;
      return n;
    }

    return this;
  }

  public Node visitChildren(NodeVisitor v) {
      Prefix prefix = (Prefix) visitChild(this.prefix, v);
      Id name = (Id) visitChild(this.name, v);
      return reconstruct(prefix, name);
  }
  
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    if (prefix != null) {
        print(prefix, w, tr);
        w.write(".");
	w.allowBreak(2, 3, "", 0);
    }
            
    tr.print(this, name, w);
  }

  public String toString() {
    return (prefix == null
            ? name.toString()
            : prefix.toString() + "." + name.toString()) + "{amb}";
  }
}
