package jltools.ast;

import jltools.util.*;
import jltools.types.*;

/**
 * Class to handle statements such as ; or the statement inside of {};
 * This class makes the parser's job easier, as it doesn't have to sift
 * out the null's that would otherwise be generated by an empty statement.
 */
public class EmptyStatement extends Statement
{
  public EmptyStatement(Node ext) {this.ext = ext;}

  public EmptyStatement() {this.ext = null;}

    public EmptyStatement reconstruct( Node ext) {
	if (this.ext == ext) { 
	    return this;
	} else {
	    return new EmptyStatement(ext);
	}
    }

    public EmptyStatement reconstruct() {
	return this;
    }

  public Node visitChildren(NodeVisitor v)
  {
      return reconstruct(Node.condVisit(ext, v));
  }

  public Node typeCheck( LocalContext tc)
  {
    return this;
  }
  
  public void translate( LocalContext c, CodeWriter w)
  {
    w.write( ";");
  }

  public void dump( CodeWriter w) throws SemanticException
  {
    w.write( "EMPTY_STATEMENT");
    dumpNodeInfo( w);
  }
}
