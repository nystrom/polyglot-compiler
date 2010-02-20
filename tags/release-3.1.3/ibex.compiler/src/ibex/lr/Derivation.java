package ibex.lr;

import ibex.types.*;
import polyglot.util.*;
import java.util.*;
import polyglot.main.Report;

public class Derivation {
    Grammar g;

    Collection[] immed;
    BitSet[] deriv;

    public Derivation(Grammar g) {
        this.g = g;

        this.deriv = new BitSet[g.nonterminals().size()];
        this.immed = new Collection[g.nonterminals().size()];

        for (GLRNonterminal A : g.nonterminals()) {
            deriv[A.index()] = new BitSet();
            immed[A.index()] = new HashSet();
        }

        for (Iterator i = g.nonterminals().iterator(); i.hasNext(); ) {
            GLRNonterminal A = (GLRNonterminal) i.next();

            for (Iterator j = A.rules().iterator(); j.hasNext(); ) {
                GLRRule r = (GLRRule) j.next();

                for (Iterator k = r.rhs.iterator(); k.hasNext(); ) {
                    GLRSymbol X = (GLRSymbol) k.next();
                    if (X instanceof GLRNonterminal) {
                        deriv[A.index()].set(X.index());
                        immed[X.index()].add(A);
                    }
                }
            }
        }

        boolean changed = true;

        while (changed) {
            changed = false;

            for (Iterator i = g.nonterminals().iterator(); i.hasNext(); ) {
                GLRNonterminal A = (GLRNonterminal) i.next();

                for (Iterator j = A.rules().iterator(); j.hasNext(); ) {
                    GLRRule r = (GLRRule) j.next();

                    for (Iterator k = r.rhs.iterator(); k.hasNext(); ) {
                        GLRSymbol X = (GLRSymbol) k.next();
                        if (X instanceof GLRNonterminal) {
                            int n = deriv[A.index()].cardinality();
                            deriv[A.index()].or(deriv[X.index()]);
                            if (n != deriv[A.index()].cardinality()) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        if (Report.should_report(TOPICS, 3)) {
            for (Iterator i = g.nonterminals().iterator(); i.hasNext(); ) {
                GLRNonterminal A = (GLRNonterminal) i.next();

                System.out.print(A + " derives");

                for (Iterator j = g.nonterminals().iterator(); j.hasNext(); ) {
                    GLRNonterminal B = (GLRNonterminal) j.next();

                    if (derives(A, B)) {
                        System.out.print(" " + B);
                    }
                }

                System.out.println();
            }

            for (Iterator i = g.nonterminals().iterator(); i.hasNext(); ) {
                GLRNonterminal A = (GLRNonterminal) i.next();
                System.out.println(A + " immediately derives from " + immed[A.index()]);
            }
        }
    }

    boolean derives(GLRNonterminal ancestor, GLRNonterminal descendent) {
        return deriv[ancestor.index()].get(descendent.index());
    }

    Collection immediatelyDerivesFrom(GLRNonterminal A) {
        return immed[A.index()];
    }

    static Collection TOPICS = Arrays.asList( new String[] { "lr", "ibex", "deriv" });
}
