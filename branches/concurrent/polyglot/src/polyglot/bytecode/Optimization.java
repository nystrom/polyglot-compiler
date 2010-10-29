/**
 * 
 */
package polyglot.bytecode;

import polyglot.ast.Expr;

interface Optimization {
    boolean apply(Expr n);
}