/*
 * BinaryExpression.java
 */

package jltools.ast;

import jltools.util.CodeWriter;
import jltools.types.Context;
/**
 * BinaryExpression
 *
 * Overview: A BinaryExpression represents a Java binary expression, a
 * mutable pair of expressions combined with an operator.
 */

public class BinaryExpression extends Expression {

    public static final int ASSIGN         = 0; // = operator
    public static final int GT             = 1; // > operator
    public static final int LT             = 2; // < opereator
    public static final int EQUAL          = 3; // == operator
    public static final int LE             = 4; // <= operator
    public static final int GE             = 5; // >= operator
    public static final int NE             = 6; // != operator
    public static final int LOGIC_OR       = 7; // || operator
    public static final int LOGIC_AND      = 8; // && operator
    public static final int MULT           = 9; // * operator
    public static final int DIV            = 10; // / operator
    public static final int BIT_OR         = 11; // | operator
    public static final int BIT_AND        = 12; // & operator
    public static final int BIT_XOR        = 13; // ^ operator
    public static final int MOD            = 14; // % operator
    public static final int LSHIFT         = 15; // << operator
    public static final int RSHIFT         = 16; // >> operator
    public static final int RUSHIFT        = 17; // >>> operator
    public static final int PLUSASSIGN     = 18; // += operator
    public static final int SUBASSIGN      = 19; // -= operator
    public static final int MULTASSIGN     = 20; // *= operator
    public static final int DIVASSIGN      = 21; // /= operator
    public static final int ANDASSIGN      = 22; // &= operator
    public static final int ORASSIGN       = 23; // |= operator
    public static final int XORASSIGN      = 24; // ^= operator
    public static final int MODASSIGN      = 25; // %= operator
    public static final int LSHIFTASSIGN   = 26; // <<= operator
    public static final int RSHIFTASSIGN   = 27; // >>= operator
    public static final int RUSHIFTASSIGN  = 28; // >>>= operator

    public static final int PLUS           = 29; // + operator
    public static final int MINUS          = 30; // - operator

    // Largest operator used.
    public static final int MAX_OPERATOR   = MINUS;

    /**
     * Requires: A valid value for <operator> as listed in public
     *    static ints in this class.
     * Effects: Creates a new BinaryExpression of <operator> applied
     *    to <left> and <right>.
     */
    public BinaryExpression(Expression left, int operator, Expression right) {
	if (left == null || right == null) {
	    throw new NullPointerException ("BinaryExpression cannot " +
					    "take null Expressions");
	}
	this.left = left;
	this.right = right;
	setOperator(operator);
    }

    /**
     * Effects: Returns the operator corresponding to <this>.
     */
    public int getOperator() {
	return operator;
    }

    /**
     * Requires: <newOperator> to be one of the valid operators
     *    defined in BinaryExpression.
     * Effects: Changes the operator of <this> to <newOperator>.
     */
    public void setOperator(int newOperator) {
	if (newOperator < 0 || newOperator > MAX_OPERATOR) {
	    throw new IllegalArgumentException("Value for operator to " +
					       "BinaryExpression not valid.");
	}
	operator = newOperator;
    }

    /**
     * Effects: Returns the left-hand subexpression.
     */
    public Expression getLeftExpression() {
	return left;
    }

    /**
     * Effects: Sets the left-hand subexpression to <newExp>
     */
    public void setLeftExpression(Expression newExp) {
	if (newExp == null) {
	    throw new NullPointerException("BinaryExpression does not " +
					   "allow null subexpressions.");
	}
	left = newExp;
    }

    /**
     * Effects: Returns the right-hand subexpression.
     */
    public Expression getRightExpression() {
	return right;
    }

    /**
     * Effects: Sets the right-hand subexpression to <newExp>
     */
    public void setRightExpression(Expression newExp) {
	if (newExp == null) {
	    throw new NullPointerException("BinaryExpression does not " +
					   "allow null subexpressions.");
	}
	right = newExp;
    }

   /**
    *
    */
   void visitChildren(NodeVisitor vis)
   {
      left = (Expression)left.visit(vis);
      right = (Expression)right.visit(vis);
   }

   public Node typeCheck(Context c)
   {
      // FIXME: implement
      return this;
   }

   public void translate(Context c, CodeWriter w)
   {
      left.translate(c, w);
      w.write(" ");
      w.write(getOperatorString(operator));
      w.write(" ");
      right.translate(c, w);

   }

   public void  dump(Context c, CodeWriter w)
   {
      w.write(getOperatorString(operator));
      dumpNodeInfo(c, w);
      w.beginBlock();
      left.dump(c, w);
      right.dump(c, w);
      w.endBlock();

   }

    public Node copy() {
      BinaryExpression be = new BinaryExpression(left, operator, right);
      be.copyAnnotationsFrom(this);
      return be;
    }

    public Node deepCopy() {
      BinaryExpression be = new BinaryExpression((Expression) left.deepCopy(),
						operator,
						(Expression) right.deepCopy());
      be.copyAnnotationsFrom(this);
      return be;
    }

    private Expression left, right;
    private int operator;

   private static String getOperatorString( int operator)
   {
      switch( operator)
      {
      case ASSIGN:
         return "=";
      case GT:
         return ">";
      case LT:
         return "<";
      case EQUAL:
         return "==";
      case LE:
         return "<=";
      case GE:
         return ">=";
      case NE:
         return "!=";
      case LOGIC_OR:
         return "||";
      case LOGIC_AND:
         return "&&";
      case MULT:
         return "*";
      case DIV:
         return "/";
      case BIT_OR:
         return "|";
      case BIT_AND:
         return "&";
      case BIT_XOR:
         return "^";
      case MOD:
         return "%";
      case LSHIFT:
         return "<<";
      case RSHIFT:
         return ">>";
      case RUSHIFT:
         return ">>>";
      case PLUSASSIGN:
         return "+=";
      case SUBASSIGN:
         return "-=";
      case MULTASSIGN:
         return "*=";
      case DIVASSIGN:
         return "/=";
      case ANDASSIGN:
         return "&=";
      case ORASSIGN:
         return "|=";
      case XORASSIGN:
         return "^=";
      case MODASSIGN:
         return "%=";
      case LSHIFTASSIGN:
         return "<<=";
      case RSHIFTASSIGN:
         return ">>=";
      case RUSHIFTASSIGN:
         return ">>>=";
      default:
         return "???";
	}
   }
}
