package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

/** 
 * A local variable expression.
 */
public class Local_c extends Expr_c implements Local
{
  protected String name;
  protected LocalInstance li;

  public Local_c(Position pos, String name) {
    super(pos);
    this.name = name;
  }

  /** Get the precedence of the local. */
  public Precedence precedence() { 
    return Precedence.LITERAL;
  }

  /** Get the name of the local. */
  public String name() {
    return this.name;
  }

  /** Set the name of the local. */
  public Local name(String name) {
    Local_c n = (Local_c) copy();
    n.name = name;
    return n;
  }

  /** Get the local instance of the local. */
  public LocalInstance localInstance() {
    return li;
  }

  /** Set the local instance of the local. */
  public Local localInstance(LocalInstance li) {
    Local_c n = (Local_c) copy();
    n.li = li;
    return n;
  }

  public Node buildTypes(TypeBuilder tb) throws SemanticException {
      Local_c n = (Local_c) super.buildTypes(tb);

      TypeSystem ts = tb.typeSystem();

      LocalInstance li = ts.localInstance(position(), Flags.NONE,
                                          ts.unknownType(position()), name);
      return n.localInstance(li);
  }

  /** Type check the local. */
  public Node typeCheck(TypeChecker tc) throws SemanticException {
    Context c = tc.context();
    LocalInstance li = c.findLocal(name);
    return localInstance(li).type(li.type());
  }

  public String toString() {
    return name;
  }

  /** Write the local to an output file. */
  public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    w.write(name);
  }

  /** Dumps the AST. */
  public void dump(CodeWriter w) {
    super.dump(w);

    w.allowBreak(4, " ");
    w.begin(0);
    w.write("(name " + name + ")");
    w.end();
  }

  public boolean isConstant() {
    return li.isConstant();
  }

  public Object constantValue() {
    return li.constantValue();
  }
}