package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

/**
 * A <code>LocalAssign</code> represents a Java assignment expression to
 * a local variable, e.g. x = foo, where x is a local variable
 * 
 * The class of the Expr returned by LocalAssign.left() is guaranteed
 * to be a Local (although only after disambiguation...)
 */
public class LocalAssign_c extends Assign_c implements LocalAssign
{
  public LocalAssign_c(Position pos, Local left, Operator op, Expr right) {
    super(pos, left, op, right);
  }

  public Assign left(Expr left) {
      LocalAssign_c n = (LocalAssign_c)super.left(left);
      n.assertLeftType();
      return n;
  }

  private void assertLeftType() {
      if (!(left() instanceof Local)) {
          throw new InternalCompilerError("left expression of an LocalAssign must be a local");
      }
  }

  public Term entry() {
    if (operator() != Assign.ASSIGN) {
      return left();
    }

    return right().entry();
  }
  
  protected void acceptCFGAssign(CFGBuilder v) {
      Local l = (Local)left();

      // x OP= e: visit e -> (l OP= e)      
      v.visitCFG(right(), this);
  }
  
  protected void acceptCFGOpAssign(CFGBuilder v) {
      Local l = (Local)left();
      
      // l OP= e: visit l -> e -> (l OP= e)
      v.visitThrow(l);
      v.edge(l, right().entry());
      v.visitCFG(right(), this);
  }
}
