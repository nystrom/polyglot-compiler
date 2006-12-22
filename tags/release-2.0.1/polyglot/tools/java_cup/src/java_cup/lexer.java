package java_cup;

import java_cup.runtime.Symbol;
import java.util.Hashtable;

/** This class implements a small scanner (aka lexical analyzer or lexer) for
 *  the JavaCup specification.  This scanner reads characters from standard 
 *  input (System.in) and returns integers corresponding to the terminal 
 *  number of the next Symbol. Once end of input is reached the EOF Symbol is 
 *  returned on every subsequent call.<p>
 *  Symbols currently returned include: <pre>
 *    Symbol        Constant Returned     Symbol        Constant Returned
 *    ------        -----------------     ------        -----------------
 *    "package"     PACKAGE               "import"      IMPORT 
 *    "code"        CODE                  "action"      ACTION 
 *    "parser"      PARSER                "terminal"    TERMINAL
 *    "non"         NON                   "init"        INIT 
 *    "scan"        SCAN                  "with"        WITH
 *    "start"       START                 "precedence"  PRECEDENCE
 *    "left"        LEFT		  "right"       RIGHT
 *    "nonassoc"    NONASSOC		  "%prec        PRECENT_PREC  
 *      [           LBRACK                  ]           RBRACK
 *      ;           SEMI 
 *      ,           COMMA                   *           STAR 
 *      .           DOT                     :           COLON
 *      ::=         COLON_COLON_EQUALS      |           BAR
 *    identifier    ID                    {:...:}       CODE_STRING
 *    "nonterminal" NONTERMINAL
 *  </pre>
 *  All symbol constants are defined in sym.java which is generated by 
 *  JavaCup from parser.cup.<p>
 * 
 *  In addition to the scanner proper (called first via init() then with
 *  next_token() to get each Symbol) this class provides simple error and 
 *  warning routines and keeps a count of errors and warnings that is 
 *  publicly accessible.<p>
 *  
 *  This class is "static" (i.e., it has only static members and methods).
 *
 * @version last updated: 7/3/96
 * @author  Frank Flannery
 */
public class lexer {

  /*-----------------------------------------------------------*/
  /*--- Constructor(s) ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** The only constructor is private, so no instances can be created. */
  private lexer() { }

  /*-----------------------------------------------------------*/
  /*--- Static (Class) Variables ------------------------------*/
  /*-----------------------------------------------------------*/

  /** First character of lookahead. */
  protected static int next_char; 

  /** Second character of lookahead. */
  protected static int next_char2;

  /** Second character of lookahead. */
  protected static int next_char3;

  /** Second character of lookahead. */
  protected static int next_char4;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** EOF constant. */
  protected static final int EOF_CHAR = -1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of keywords.  Keywords are initially treated as identifiers.
   *  Just before they are returned we look them up in this table to see if
   *  they match one of the keywords.  The string of the name is the key here,
   *  which indexes Integer objects holding the symbol number. 
   */
  protected static Hashtable keywords = new Hashtable(23);

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Table of single character symbols.  For ease of implementation, we 
   *  store all unambiguous single character Symbols in this table of Integer
   *  objects keyed by Integer objects with the numerical value of the 
   *  appropriate char (currently Character objects have a bug which precludes
   *  their use in tables).
   */
  protected static Hashtable char_symbols = new Hashtable(11);

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Current line number for use in error messages. */
  protected static int current_line = 1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Character position in current line. */
  protected static int current_position = 1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Character position in current line. */
  protected static int absolute_position = 1;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Count of total errors detected so far. */
  public static int error_count = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Count of warnings issued so far */
  public static int warning_count = 0;

  /*-----------------------------------------------------------*/
  /*--- Static Methods ----------------------------------------*/
  /*-----------------------------------------------------------*/

  /** Initialize the scanner.  This sets up the keywords and char_symbols
    * tables and reads the first two characters of lookahead.  
    */
  public static void init() throws java.io.IOException
    {
      /* set up the keyword table */
      keywords.put("package",    new Integer(sym.PACKAGE));
      keywords.put("import",     new Integer(sym.IMPORT));
      keywords.put("code",       new Integer(sym.CODE));
      keywords.put("action",     new Integer(sym.ACTION));
      keywords.put("parser",     new Integer(sym.PARSER));
      keywords.put("terminal",   new Integer(sym.TERMINAL));
      keywords.put("non",        new Integer(sym.NON));
      keywords.put("nonterminal",new Integer(sym.NONTERMINAL));// [CSA]
      keywords.put("init",       new Integer(sym.INIT));
      keywords.put("scan",       new Integer(sym.SCAN));
      keywords.put("with",       new Integer(sym.WITH));
      keywords.put("start",      new Integer(sym.START));
      keywords.put("precedence", new Integer(sym.PRECEDENCE));
      keywords.put("left",       new Integer(sym.LEFT));
      keywords.put("right",      new Integer(sym.RIGHT));
      keywords.put("nonassoc",   new Integer(sym.NONASSOC));
      keywords.put("extends",    new Integer(sym.EXTENDS));
      keywords.put("implements", new Integer(sym.IMPLEMENTS));

      /* set up the table of single character symbols */
      char_symbols.put(new Integer(';'), new Integer(sym.SEMI));
      char_symbols.put(new Integer(','), new Integer(sym.COMMA));
      char_symbols.put(new Integer('*'), new Integer(sym.STAR));
      char_symbols.put(new Integer('.'), new Integer(sym.DOT));
      char_symbols.put(new Integer('|'), new Integer(sym.BAR));
      char_symbols.put(new Integer('['), new Integer(sym.LBRACK));
      char_symbols.put(new Integer(']'), new Integer(sym.RBRACK));

      /* read two characters of lookahead */
      next_char = System.in.read();
      if (next_char == EOF_CHAR) {
	next_char2 = EOF_CHAR;
        next_char3 = EOF_CHAR;
        next_char4 = EOF_CHAR;
      } else {
	next_char2 = System.in.read();
	if (next_char2 == EOF_CHAR) {
	  next_char3 = EOF_CHAR;
	  next_char4 = EOF_CHAR;
	} else {
	  next_char3 = System.in.read();
	  if (next_char3 == EOF_CHAR) {
	    next_char4 = EOF_CHAR;
	  } else {
	    next_char4 = System.in.read();
	  }
	}
      }
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Advance the scanner one character in the input stream.  This moves
   * next_char2 to next_char and then reads a new next_char2.  
   */
  protected static void advance() throws java.io.IOException
    {
      int old_char;

      old_char = next_char;
      next_char = next_char2;
      if (next_char == EOF_CHAR) {
	next_char2 = EOF_CHAR;
        next_char3 = EOF_CHAR;
	next_char4 = EOF_CHAR;
      } else {
	next_char2 = next_char3;
	if (next_char2 == EOF_CHAR) {
	  next_char3 = EOF_CHAR;
	  next_char4 = EOF_CHAR;
	} else {
	  next_char3 = next_char4;
	  if (next_char3 == EOF_CHAR) {
	    next_char4 = EOF_CHAR;
	  } else {
	    next_char4 = System.in.read();
	  }
	}
      }

      /* count this */
      absolute_position++;
      current_position++;
      if (old_char == '\n' || (old_char == '\r' && next_char!='\n'))
	{
	  current_line++;
	  current_position = 1;
	}
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit an error message.  The message will be marked with both the 
   *  current line number and the position in the line.  Error messages
   *  are printed on standard error (System.err).
   * @param message the message to print.
   */
  public static void emit_error(String message)
    {
      System.err.println("Error at " + current_line + "(" + current_position +
			 "): " + message);
      error_count++;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Emit a warning message.  The message will be marked with both the 
   *  current line number and the position in the line.  Messages are 
   *  printed on standard error (System.err).
   * @param message the message to print.
   */
  public static void emit_warn(String message)
    {
      System.err.println("Warning at " + current_line + "(" + current_position +
			 "): " + message);
      warning_count++;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Determine if a character is ok to start an id. 
   * @param ch the character in question.
   */
  protected static boolean id_start_char(int ch)
    {
      /* allow for % in identifiers.  a hack to allow my
	 %prec in.  Should eventually make lex spec for this 
	 frankf */
      return (ch >= 'a' &&  ch <= 'z') || (ch >= 'A' && ch <= 'Z') || 
	     (ch == '_');

      // later need to deal with non-8-bit chars here
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Determine if a character is ok for the middle of an id.
   * @param ch the character in question. 
   */
  protected static boolean id_char(int ch)
    {
      return id_start_char(ch) || (ch >= '0' && ch <= '9');
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Try to look up a single character symbol, returns -1 for not found. 
   * @param ch the character in question.
   */
  protected static int find_single_char(int ch)
    {
      Integer result;

      result = (Integer)char_symbols.get(new Integer((char)ch));
      if (result == null) 
	return -1;
      else
	return result.intValue();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Handle swallowing up a comment.  Both old style C and new style C++
   *  comments are handled.
   */
  protected static void swallow_comment() throws java.io.IOException
    {
      /* next_char == '/' at this point */

      /* is it a traditional comment */
      if (next_char2 == '*')
	{
	  /* swallow the opener */
	  advance(); advance();

	  /* swallow the comment until end of comment or EOF */
	  for (;;)
	    {
	      /* if its EOF we have an error */
	      if (next_char == EOF_CHAR)
		{
		  emit_error("Specification file ends inside a comment");
		  return;
		}

	      /* if we can see the closer we are done */
	      if (next_char == '*' && next_char2 == '/')
		{
		  advance();
		  advance();
		  return;
		}

	      /* otherwise swallow char and move on */
	      advance();
	    }
	}

      /* is its a new style comment */
      if (next_char2 == '/')
	{
	  /* swallow the opener */
	  advance(); advance();

	  /* swallow to '\n', '\r', '\f', or EOF */ 
	  while (next_char != '\n' && next_char != '\r' && 
		 next_char != '\f' && next_char!=EOF_CHAR)
	    advance();

	  return;

	}

      /* shouldn't get here, but... if we get here we have an error */
      emit_error("Malformed comment in specification -- ignored");
      advance();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Swallow up a code string.  Code strings begin with "{:" and include
      all characters up to the first occurrence of ":}" (there is no way to 
      include ":}" inside a code string).  The routine returns a String
      object suitable for return by the scanner.
   */
  protected static Symbol do_code_string() throws java.io.IOException
    {
      StringBuffer result = new StringBuffer();

      /* at this point we have lookahead of "{:" -- swallow that */
      advance(); advance();

      /* save chars until we see ":}" */
      while (!(next_char == ':' && next_char2 == '}'))
	{
	  /* if we have run off the end issue a message and break out of loop */
	  if (next_char == EOF_CHAR)
	    {
	      emit_error("Specification file ends inside a code string");
	      break;
	    }

	  /* otherwise record the char and move on */
	  result.append(new Character((char)next_char));
	  advance();
	}

      /* advance past the closer and build a return Symbol */
      advance(); advance();
      return new Symbol(sym.CODE_STRING, result.toString());
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Process an identifier.  Identifiers begin with a letter, underscore,
   *  or dollar sign, which is followed by zero or more letters, numbers,
   *  underscores or dollar signs.  This routine returns a String suitable
   *  for return by the scanner.
   */
  protected static Symbol do_id() throws java.io.IOException
    {
      StringBuffer result = new StringBuffer();
      String       result_str;
      Integer      keyword_num;
      char         buffer[] = new char[1];

      /* next_char holds first character of id */
      buffer[0] = (char)next_char;
      result.append(buffer,0,1);
      advance();

      /* collect up characters while they fit in id */ 
      while(id_char(next_char))
	{
          buffer[0] = (char)next_char;
	  result.append(buffer,0,1);
	  advance();
	}

      /* extract a string and try to look it up as a keyword */
      result_str = result.toString();
      keyword_num = (Integer)keywords.get(result_str);

      /* if we found something, return that keyword */
      if (keyword_num != null)
	return new Symbol(keyword_num.intValue());

      /* otherwise build and return an id Symbol with an attached string */
      return new Symbol(sym.ID, result_str);
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Return one Symbol.  This is the main external interface to the scanner.
   *  It consumes sufficient characters to determine the next input Symbol
   *  and returns it.  To help with debugging, this routine actually calls
   *  real_next_token() which does the work.  If you need to debug the 
   *  parser, this can be changed to call debug_next_token() which prints
   *  a debugging message before returning the Symbol.
   */
  public static Symbol next_token() throws java.io.IOException
    {
      return real_next_token();
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Debugging version of next_token().  This routine calls the real scanning
   *  routine, prints a message on System.out indicating what the Symbol is,
   *  then returns it.
   */
  public static Symbol debug_next_token() throws java.io.IOException
    {
      Symbol result = real_next_token();
      System.out.println("# next_Symbol() => " + result.sym);
      return result;
    }

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The actual routine to return one Symbol.  This is normally called from
   *  next_token(), but for debugging purposes can be called indirectly from
   *  debug_next_token(). 
   */
  protected static Symbol real_next_token() throws java.io.IOException
    {
      int sym_num;

      for (;;)
	{
	  /* look for white space */
	  if (next_char == ' ' || next_char == '\t' || next_char == '\n' ||
	      next_char == '\f' ||  next_char == '\r')
	    {
	      /* advance past it and try the next character */
	      advance();
	      continue;
	    }

	  /* look for a single character symbol */
	  sym_num = find_single_char(next_char);
	  if (sym_num != -1)
	    {
	      /* found one -- advance past it and return a Symbol for it */
	      advance();
	      return new Symbol(sym_num);
	    }

	  /* look for : or ::= */
	  if (next_char == ':')
	    {
	      /* if we don't have a second ':' return COLON */
	      if (next_char2 != ':') 
		{
		  advance();
		  return new Symbol(sym.COLON);
		}

	      /* move forward and look for the '=' */
	      advance();
	      if (next_char2 == '=') 
		{
		  advance(); advance();
		  return new Symbol(sym.COLON_COLON_EQUALS);
		}
	      else
		{
		  /* return just the colon (already consumed) */
		  return new Symbol(sym.COLON);
		}
	    }

	  /* find a "%prec" string and return it.  otherwise, a '%' was found,
	     which has no right being in the specification otherwise */
	  if (next_char == '%') {
	    advance();
	    if ((next_char == 'p') && (next_char2 == 'r') && (next_char3 == 'e') && 
		(next_char4 == 'c')) {
	      advance();
	      advance();
	      advance();
	      advance();
	      return new Symbol(sym.PERCENT_PREC);
	    } else {
	      emit_error("Found extraneous percent sign");
	    }
	  }

	  /* look for a comment */
	  if (next_char == '/' && (next_char2 == '*' || next_char2 == '/'))
	    {
	      /* swallow then continue the scan */
	      swallow_comment();
	      continue;
	    }

	  /* look for start of code string */
	  if (next_char == '{' && next_char2 == ':')
	    return do_code_string();

	  /* look for an id or keyword */
	  if (id_start_char(next_char)) return do_id();

	  /* look for EOF */
	  if (next_char == EOF_CHAR) return new Symbol(sym.EOF);

	  /* if we get here, we have an unrecognized character */
	  emit_warn("Unrecognized character '" + 
	    new Character((char)next_char) + "'(" + next_char + 
	    ") -- ignored");

	  /* advance past it */
	  advance();
	}
    }

  /*-----------------------------------------------------------*/
}

