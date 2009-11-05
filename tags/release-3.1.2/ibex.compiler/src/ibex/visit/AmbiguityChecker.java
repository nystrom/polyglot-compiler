package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.ast.RuleDecl;
import ibex.lr.GLR;
import ibex.types.RSeq;
import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;
import ibex.types.RAnd;
import ibex.types.Nonterminal;
import ibex.types.Nonterminal_c;
import ibex.types.IbexClassType;
import ibex.types.Rhs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which traverses the AST constructing type objects. */
public class AmbiguityChecker extends ContextVisitor
{
    IbexTypeSystem ts;
    IbexNodeFactory nf;

    public AmbiguityChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
        this.job = job;
        this.ts = (IbexTypeSystem) ts;
        this.nf = (IbexNodeFactory) nf;
    }

    public Job job() {
        return job;
    }

    public ErrorQueue errorQueue() {
        return job().compiler().errorQueue();
    }

    public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        assert (v instanceof ContextVisitor);
        Context context = ((ContextVisitor) v).context();
        
        if (n instanceof ClassBody) {
            IbexClassDef def = (IbexClassDef) context.currentClassDef();
            IbexClassType ct = (IbexClassType) def.asType();
            GLR glr = def.glr();

            // Check for ambiguities in all nonterminals, including inherited
            // ones: a new rule can introduce a new ambiguity.
            ibex.ExtensionInfo extInfo =
                (ibex.ExtensionInfo) job().extensionInfo();

            if (extInfo.getIbexOptions().checkMergeActions) {
                for (Iterator<Nonterminal> i = ct.allNonterminals().iterator(); i.hasNext(); ) {
                    Nonterminal sym = (Nonterminal) i.next();

                    Nonterminal nt = (Nonterminal) sym;
                    if (! glr.isReachable(nt)) {
                        continue;
                    }

                    try {
                        checkAmbiguities(glr, nt);
                    }
                    catch (SemanticException e) {
                        errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                             e.getMessage(), nt.position());
                    }
                }
            }
        }

        if (n instanceof RuleDecl) {
            IbexClassDef def = (IbexClassDef) context.currentClassDef();
            GLR glr = def.glr();

            RuleDecl rd = (RuleDecl) n;

            try {
                Nonterminal nt = rd.rule().asNonterminal();
                if (! glr.isReachable(nt)) {
                    throw new SemanticException("Nonterminal " +
                        nt +" is not reachable from a start symbol.", rd.position());
                }
            }
            catch (SemanticException e) {
                errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                    e.getMessage(), rd.position());
            }
        }

        return super.leave(old, n, v);
    }

    public void checkAmbiguities(GLR glr, Nonterminal nonterminal)
        throws SemanticException
    {
        // Check that there is a merge action, if needed.
        if (! nonterminal.type().isVoid()) {
          {
            // Get the merge pairs.
            Collection<Collection<Rhs>> groups = glr.mergeRulesForNonterminal(nonterminal);

            // Check for groups that are not covered by a merge action.
            // Remove from the collection any covered groups.

            for (Iterator<Collection<Rhs>> i = groups.iterator(); i.hasNext(); ) {
                Collection<Rhs> group = i.next();

                boolean found = false;
            
                for (Iterator<Rhs> j = nonterminal.rule().choices().iterator(); j.hasNext(); ) {
                    Rhs rhs = j.next();

                    if (rhs instanceof RAnd) {
                        RAnd a = (RAnd) rhs;
                        Rhs case1 = a.choice1();
                        Rhs case2 = a.choice2();

                        boolean found1 = false;
                        boolean found2 = false;

                        for (Iterator<Rhs> k = group.iterator(); k.hasNext(); ) {
                            Rhs rhsk = k.next();

                            if (rhsk.matches(case1)) {
                                found1 = true;
                            }
                            if (rhsk.matches(case2)) {
                                found2 = true;
                            }
                            if (found1 && found2) {
                                break;
                            }
                        }

                        if (found1 && found2) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    i.remove();
                }
            }

            // Now check that all groups were covered.
            if (groups.size() > 0) {
                String message = "The following pairs of rules for nonterminal \"" + nonterminal + "\" are ambiguous and require a merge action, or the semantic action return type must be void:\n";

                for (Iterator i = groups.iterator(); i.hasNext(); ) {
                    Collection group = (Collection) i.next();

                    message += "\n";

                    for (Iterator j = group.iterator(); j.hasNext(); ) {
                        RSeq rhs = (RSeq) j.next();
                        message += "    " + nonterminal + " ::= " + rhs + "\n";
                    }
                }

                throw new SemanticException(message, nonterminal.position());
            }
          }

          {
            // Get the merge pairs.
            Collection groups = glr.possibleMergeRulesForNonterminal(nonterminal);
            // Check for groups that are not covered by a merge action.
            // Remove from the collection any covered groups.

            for (Iterator i = groups.iterator(); i.hasNext(); ) {
                Collection group = (Collection) i.next();

                boolean found = false;
            
                for (Iterator<Rhs> j = nonterminal.rule().choices().iterator(); j.hasNext(); ) {
                    Rhs rhs = j.next();

                    if (rhs instanceof RAnd) {
                        RAnd a = (RAnd) rhs;
                        Rhs case1 = a.choice1();
                        Rhs case2 = a.choice2();

                        boolean found1 = false;
                        boolean found2 = false;

                        for (Iterator k = group.iterator(); k.hasNext(); ) {
                            RSeq rhsk = (RSeq) k.next();

                            if (rhsk.matches(case1)) {
                                found1 = true;
                            }
                            if (rhsk.matches(case2)) {
                                found2 = true;
                            }
                            if (found1 && found2) {
                                break;
                            }
                        }

                        if (found1 && found2) {
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    i.remove();
                }
            }

            // Now check that all groups were covered.
            if (groups.size() > 0) {
                String message = "The following pairs of rules for nonterminal \"" + nonterminal + "\" are possibly ambiguous and may require either a merge action, or the semantic action return type must be void:\n";

                for (Iterator i = groups.iterator(); i.hasNext(); ) {
                    Collection group = (Collection) i.next();

                    message += "\n";

                    for (Iterator j = group.iterator(); j.hasNext(); ) {
                        RSeq rhs = (RSeq) j.next();
                        message += "    " + nonterminal + " ::= " + rhs + "\n";
                    }
                }

                throw new SemanticException(message, nonterminal.position());
            }
          }
        }
    }
}
