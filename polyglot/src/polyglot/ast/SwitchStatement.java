package jltools.ast;

import jltools.types.*;
import jltools.util.*;

import java.util.*;


/**
 * A <code>SwitchStatement</code> is an immutable representation of a Java
 * <code>swtich</code> statement.  Such a statement has an expression which
 * is evaluated to determine where to branch to, an a list of labels
 * and block statements which are conditionally evaluated.  One of the
 * labels, rather than having a constant expression, may be lablled
 * default.
 */
public class SwitchStatement extends Statement 
{
  private Expression expr;
  private List switchElems;
  
  /**
   * An element of a switch statement.  This can either be
   * a case label, or a set of statements.
   */
  public static abstract class SwitchElement extends Statement 
  {
  }
  
  /**
   * A case statement or a default label in a switch block.  
   */
  public static class CaseStatement extends SwitchElement 
  {
    /**
     * Effects: Creates a new CaseStement with <expr> as the value
     * for the case.
     */
    public CaseStatement( Expression expr) 
    {
      this.expr = expr;
      this.def = false;
    }

    /**
     * Lazily reconstructs the case statement.
     * 
     * @param expr the expression (may be null iff default = true)
     * @param def whether this is the "default" label
     */
    public CaseStatement reconstruct ( Expression expr, boolean def)
    {
      CaseStatement cs;
      if ( expr != this.expr || def != this.def)
      {
        cs = new CaseStatement( expr);
        cs.def = def;
        cs.copyAnnotationsFrom ( this ) ;
        cs.iValue = iValue;
        return cs;
      }
      return this;
    }
    
    /**
     * Effects: Creates a new CaseStatement which represents a default Label.
     */
    public CaseStatement() 
    {
      this.def = true;
    }
    
    /**
     * Effects: Returns true iff this CaseStatement represents a
     * default label.
     */
    public boolean isDefault() {
      return def;
    }

    /**
     * Effects: Returns the expresion associated with this
     * CaseStatement if it is not a default, otherwise returns null.
     */
    public Expression getExpression() {
      if (def) {
        return null;
      }
      else {
        return expr;
      }
    }
    
    public Node visitChildren ( NodeVisitor v)
    {
      if ( expr != null)
      {
        return reconstruct ( (Expression)expr.visit ( v ), def) ;
      }
      return this;
    }
    
    public Node typeCheck( LocalContext c ) throws SemanticException
    {

      if ( def)
      {
        return this;
      }
      
      if ( ! expr.getCheckedType().isImplicitCastValid ( 
                                    c.getTypeSystem().getInt()))
      {
        throw new SemanticException ( "The case label must be a byte, char,"
                                       + " short or int.");
      }
      
      if ( expr instanceof IntLiteral)
      {
        iValue = (int)((IntLiteral)expr).getLongValue();
      }
      else if (expr instanceof CharacterLiteral)
      {
        iValue = (int) ((CharacterLiteral)expr).getCharValue() ;
      }
      else if ( expr instanceof FieldExpression || 
                expr instanceof LocalVariableExpression)
      {
        FieldInstance fi;

        if ( expr instanceof FieldExpression)
        {
          fi = ((FieldExpression)expr).getFieldInstance();
        }
        else
          fi = ((LocalVariableExpression)expr).getFieldInstance();
        
        if ( fi == null)
          throw new InternalCompilerError("Field Instance not defined!");
        if ( ! fi.isConstant())
          throw new SemanticException(" Case must be a constant.");
        
        if ( fi.getConstantValue() instanceof Integer)
          iValue = (int)((Integer)fi.getConstantValue()).intValue();
        else if ( fi.getConstantValue() instanceof Long)
          iValue = (int)((Long)fi.getConstantValue()).longValue();
        else throw new InternalCompilerError("Unexpected Constant type.");
        
      }
      else
        throw new SemanticException (" Case must be a constant");
      
      return this;
    }
    
    public void translate( LocalContext c, CodeWriter w)
    {
      if (isDefault())
        w.write("default: ");
      else
      {
        w.write("case ");
        getExpression().translate(c, w);
        w.write(": " );
      }
    }
    
    public void dump( CodeWriter cw)
    {
      cw.write( "( CASE ");
      dumpNodeInfo( cw);
      cw.write( ")");
    }

    private boolean def;
    private Expression expr;
    private int iValue;
   }

   public static class SwitchBlock extends SwitchElement {
    
      private BlockStatement block;

      /**
       * Effects: Creates a new SwitchBlock which contains <block>.
       */
      public SwitchBlock (BlockStatement block) {
         this.block = block;
      }
      
      public SwitchBlock () {
         this( new BlockStatement());
      }
     
     public SwitchBlock reconstruct( BlockStatement block) 
     {
       if ( block != this.block) 
       {
         SwitchBlock sb =  new SwitchBlock ( block );
         sb.copyAnnotationsFrom ( this );
       }
       return this;
     }

      /** 
       * Effects: Returns the BlockStatement contained in this switch block.
       */
      public BlockStatement getBlock() {
         return block;
      }

     public Node visitChildren ( NodeVisitor v)
     {
       return reconstruct ( (BlockStatement)block.visit( v ));
     }

     public Node typeCheck( LocalContext c)
     {
       return this;
     }

     public void translate( LocalContext c, CodeWriter w)
     {
       block.translate(c, w);
     }

     public void  dump ( CodeWriter cw)
     {
     }
   }

   /**
    * Requires: <code>switchElems</code> contains only elements of type 
    * SwitchElement.
    *
    * Effects: Creates a new SwitchStatement which is conditioned on
    * <code>expr</code> and contains the elements of <code>switchElems</code> 
    * in order.
    */
   public SwitchStatement(Expression expr, List switchElems) {
      this.expr = expr;
      TypedList.check(switchElems, SwitchElement.class);
      this.switchElems = new ArrayList(switchElems);
   }

  /**
   * Lazily reconstruct the SwitchStatement; perform reconstruction only 
   * if an element of the list changed or expr changed
   */
  public Node  reconstruct ( Expression expr, List switchElems)
  {
    if ( expr != this.expr ||
         switchElems.size() != this.switchElems.size() )
    {
      SwitchStatement ss =  new SwitchStatement ( expr, switchElems);
      ss.copyAnnotationsFrom ( this );
      return ss;
    }
    
    for ( int i = 0; i < switchElems.size() ; i ++)
    {
      if ( switchElems.get( i ) != this.switchElems.get( i ) )
      {
        SwitchStatement ss =  new SwitchStatement ( expr, switchElems);
        ss.copyAnnotationsFrom ( this );
        return ss;
      }
    }
    return this;
  }

   /**
    * Effects: Returns the Expression which this SwitchStatement is
    * conditioned on.
    */
   public Expression getExpression() {
      return expr;
   }

   /**
    * Effects: Adds <sw> to the list of SwitchElements of this.
    */
  //   public void addSwitchElement(SwitchElement sw) {
  //      switchElems.add(sw);
  //   }

   /**
    * Effects: Returns the SwitchElement at position <pos>.  Throws an
    * IndexOutOfBoundsException when <pos> is not valid.
    */
   public SwitchElement getSwitchElementAt(int pos) {
      return (SwitchElement) switchElems.get(pos);
   }

   /**
    * Effects: Returns TypedListIterator which will return the
    * SwitchElements of this in order.
    */
   public TypedListIterator switchElements() {
      return new TypedListIterator (switchElems.listIterator(),
                                    SwitchElement.class,
                                    false);
   }


  public Node visitChildren(NodeVisitor v)
  {
    Expression e = (Expression) expr.visit( v);

    List newSwitchElems = new ArrayList ( switchElems.size() );

    for (ListIterator it = switchElems.listIterator(); it.hasNext(); ) {
      SwitchElement se = (SwitchElement)((SwitchElement) it.next()).visit( v );
      newSwitchElems.add ( se ) ;      
    }

    return reconstruct ( e, newSwitchElems);
  }

   public Node typeCheck(LocalContext c) throws SemanticException
   {
     List lDefinedCaseLabels = new ArrayList();


     for (ListIterator it = switchElems.listIterator(); it.hasNext(); ) {
       SwitchElement se = (SwitchElement) it.next();
       if ( se instanceof CaseStatement)
       {
         Object key;
         if ( ((CaseStatement)se).def)
           key = "default";
         else
           key = new Long ( ((CaseStatement)se).iValue);
         
         if ( lDefinedCaseLabels.contains( key ) )
           throw new SemanticException( "Duplicate case label: " + key, 
                                         Annotate.getLineNumber( se ) );
         lDefinedCaseLabels.add ( key );                                        
       }
     }     
     return this;
   }

   public void translate(LocalContext c, CodeWriter w)
   {
      SwitchElement se;
      CaseStatement cs;
      SwitchBlock cb;

      w.write("switch (");
      expr.translate(c, w);
      w.write(") {");
      w.beginBlock();

      for (ListIterator it = switchElems.listIterator(); it.hasNext(); )
      {
         se = (SwitchElement) it.next();
         se.translate(c, w);

         if (it.hasNext()) {
           w.newline(0);
         }
      }
      w.endBlock();      
      w.write("}");
   }

   public void dump( CodeWriter w)
   {
      w.write( "( SWITCH ");
      dumpNodeInfo( w);
      w.write( ")");
   }
}
