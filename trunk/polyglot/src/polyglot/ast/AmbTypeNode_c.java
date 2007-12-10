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
  protected QualifierNode qual;
  protected Id name;

//  protected Expr dummy;
  
  public AmbTypeNode_c(Position pos, QualifierNode qual,
                       Id name) {
    super(pos);
    assert(name != null); // qual may be null
    this.qual = qual;
    this.name = name;
//    this.dummy = Globals.NF().ExprFromQualifiedName(pos, "java.util.Collections.EMPTY_LIST");
  }

  public Id id() {
      return this.name;
  }
  
  public AmbTypeNode id(Id name) {
      AmbTypeNode_c n = (AmbTypeNode_c) copy();
      n.name = name;
      return n;
  }
  
  public QualifierNode qual() {
    return this.qual;
  }

  public AmbTypeNode qual(QualifierNode qual) {
    AmbTypeNode_c n = (AmbTypeNode_c) copy();
    n.qual = qual;
    return n;
  }

  protected AmbTypeNode_c reconstruct(QualifierNode qual, Id name) {
    if (qual != this.qual || name != this.name) {
      AmbTypeNode_c n = (AmbTypeNode_c) copy();
      n.qual = qual;
      n.name = name;
      return n;
    }

    return this;
  }

  public Node buildTypes(TypeBuilder tb) throws SemanticException {
      TypeSystem ts = tb.typeSystem();
      LazyRef<? extends Type> sym = Types.lazyRef(ts.unknownType(position()), tb.goal());
      return typeRef(sym);
  }

  public Node visitChildren(NodeVisitor v) {
      QualifierNode qual = (QualifierNode) visitChild(this.qual, v);
      Id name = (Id) visitChild(this.name, v);

//      Expr dummy = (Expr) visitChild(this.dummy, v);
//      if (dummy != this.dummy) {
//          AmbTypeNode_c tn = (AmbTypeNode_c) reconstruct(qual, name).copy();
//          tn.dummy = dummy;
//          return tn;
//      }

      return reconstruct(qual, name);
  }

  public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
      SemanticException ex;
      
      try {
          Node n = sc.nodeFactory().disamb().disambiguate(this, sc, position(), qual, name);

          if (n instanceof TypeNode) {
              TypeNode tn = (TypeNode) n;
              LazyRef<Type> sym = (LazyRef<Type>) type;
              sym.update(tn.typeRef().get());
              
              // Reset the resolver goal to one that can run when the ref is deserialized.
              Goal resolver = Globals.Scheduler().LookupGlobalType(sym);
              Globals.Scheduler().markReached(resolver);
              sym.setResolver(resolver);
              return n;
          }

          ex = new SemanticException("Could not find type \"" +
                                     (qual == null ? name.toString() : qual.toString() + "." + name.toString()) +
                                     "\".", position());
      }
      catch (SemanticException e) {
          ex = e;
      }

      // Mark the type as an error, so we don't try looking it up again.
      LazyRef<Type> sym = (LazyRef<Type>) type;
      sym.update(sc.typeSystem().unknownType(position()));

      throw ex;
  }

  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    if (qual != null) {
        print(qual, w, tr);
        w.write(".");
	w.allowBreak(2, 3, "", 0);
    }
            
    tr.print(this, name, w);
  }

  public String toString() {
    return (qual == null
            ? name.toString()
            : qual.toString() + "." + name.toString()) + "{amb}";
  }
  public Node copy(NodeFactory nf) {
      return nf.AmbTypeNode(this.position, this.qual, this.name);
  }
  
  public String name() {
      return name.id();
  }
}
