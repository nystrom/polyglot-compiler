package jltools.visit;

import jltools.ast.*;
import jltools.frontend.*;
import java.util.*;

/**
 * The <code>CodeCleaner</code> runs over the AST and performs some trivial
 * dead code elimination.
 **/
public class CodeCleaner extends BaseVisitor {

  protected NodeFactory nf;

  /**
   * Creates a visitor for eliminating dead code.
   * @param job  The job in which this visitor is being executed.
   **/
  public CodeCleaner(Job job) {
    super(job);

    nf = nodeFactory();
  }

  public Node leave( Node old, Node n, NodeVisitor v ) {
    if ( !(n instanceof Block) ) {
      return n;
    }

    Block b = (Block)n;
    List stmtList = new LinkedList();
    for ( Iterator it = b.statements().iterator(); it.hasNext(); ) {
      Stmt stmt = (Stmt)it.next();
      stmtList.add( stmt );

      if ( stmt instanceof Branch || stmt instanceof Return
           || stmt instanceof Throw ) {
        if ( b instanceof SwitchBlock ) {
          return nf.SwitchBlock( b.position(), stmtList );
        }

        return nf.Block( b.position(), stmtList );
      }
    }

    return n;
  }
}
