/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.Translator;

/**
 * A <code>CanonicalTypeNode</code> is a type node for a canonical type.
 */
public class CanonicalTypeNode_c extends TypeNode_c implements CanonicalTypeNode
{
  public CanonicalTypeNode_c(Position pos, Ref<Type> type) {
    super(pos);
    assert(type != null);
    this.type = type;
  }
  
  public CanonicalTypeNode typeRef(Ref<Type> type) {
      return (CanonicalTypeNode) super.typeRef(type);
  }
  
  /**
   * Translate the type.
   * If the "use-fully-qualified-class-names" options is used, then the
   * fully qualified names is written out (<code>java.lang.Object</code>).
   * Otherwise, the string that originally represented the type in the
   * source file is used.
   */
  public void translate(CodeWriter w, Translator tr) {
      w.write(type.get().translate(tr.context()));
  }

  public String toString() {
    if (type == null) return "<unknown-type>";
    return type.toString();
  }

  public void dump(CodeWriter w) {
    super.dump(w);
    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(type " + type + ")");
    w.end();
  }
}