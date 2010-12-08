package polyglot.dispatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import funicular.Clock;

import polyglot.ast.AbstractBlock_c;
import polyglot.ast.Branch_c;
import polyglot.ast.Case;
import polyglot.ast.Case_c;
import polyglot.ast.Catch;
import polyglot.ast.Catch_c;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ConstructorCall_c;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Do_c;
import polyglot.ast.Empty_c;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.For_c;
import polyglot.ast.Formal_c;
import polyglot.ast.If_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.Labeled;
import polyglot.ast.Labeled_c;
import polyglot.ast.LocalClassDecl_c;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.ast.Return_c;
import polyglot.ast.Stmt;
import polyglot.ast.SwitchElement;
import polyglot.ast.Switch_c;
import polyglot.ast.Synchronized_c;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.Throw_c;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode_c;
import polyglot.ast.While_c;
import polyglot.frontend.Globals;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Ref.Callable;
import polyglot.util.InternalCompilerError;

public class ReachSetup {

	TypeSystem ts;
	Map<Node, Ref<Collection<Name>>> breaks;
	Map<Node, Ref<Collection<Name>>> continues;
	Map<Node, Ref<Collection<Type>>> exceptions;
	Map<Node, Ref<Boolean>> completes;

	protected Clock jobClock() {
		return (Clock) Globals.currentJob().get("clock");
	}

	public ReachSetup(TypeSystem ts, Map<Node, Ref<Collection<Name>>> breaks,
			Map<Node, Ref<Collection<Name>>> continues,
			Map<Node, Ref<Collection<Type>>> exceptions,
			Map<Node, Ref<Boolean>> completes) {
		this.ts = ts;
		this.breaks = breaks;
		this.continues = continues;
		this.exceptions = exceptions;
		this.completes = completes;
	}

	public void visit(Node_c n, Node parent) {
	}

	public void visit(Term_c n, Node parent) {
		throw new InternalCompilerError("Missing case for "
				+ n.getClass().getName());
	}

	public void visit(TypeNode_c n, Node parent) {
		reachableRef(n).update(true);
		completesRef(n).update(true);
	}

	public void visit(Formal_c n, Node parent) {
		reachableRef(n).update(true);
		completesRef(n).update(true);
	}

	public void visit(ClassDecl_c n, Node parent) {
		reachableRef(n).update(true);
		completesRef(n).update(true);
	}

	public void visit(ClassBody_c n, Node parent) {
		reachableRef(n).update(true);
		completesRef(n).update(true);
	}

	Ref<Boolean> reachableRef(Term n) {
		return n.reachableRef();
	}

	Ref<Boolean> completesRef(Term n) {
		Ref<Boolean> r = completes.get(n);
		if (r == null) {
			r = Types.<Boolean> lazyRef(null);
			completes.put(n, r);
		}
		return r;
	}

	Ref<Collection<Name>> breaksRef(Term n) {
		Ref<Collection<Name>> r = breaks.get(n);
		if (r == null) {
			r = Types.<Collection<Name>> lazyRef(null);
			breaks.put(n, r);
		}
		return r;
	}

	Ref<Collection<Name>> continuesRef(Term n) {
		Ref<Collection<Name>> r = continues.get(n);
		if (r == null) {
			r = Types.<Collection<Name>> lazyRef(null);
			continues.put(n, r);
		}
		return r;
	}

	Ref<Collection<Type>> throwsRef(Term n) {
		return n.throwsRef();
	}

	public void equateInto(Ref<Boolean> a, final Ref<Boolean> b) {
		a.setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return b.get();
			}
		}, jobClock());
	}

	private void completesIfReachable(Term n) {
		equateInto(completesRef(n), reachableRef(n));
	}

	public void visit(final MethodDecl_c n, Node parent) {
		// The block that is the body of a constructor, method, instance
		// initializer or static initializer is reachable.
		reachableRef(n).update(true);
		if (n.body() != null) {
			reachableRef(n.body()).update(true);
			equateInto(completesRef(n), completesRef(n.body()));
		} else {
			completesRef(n).update(true);
		}
	}

	public void visit(final Initializer_c n, Node parent) {
		// The block that is the body of a constructor, method, instance
		// initializer or static initializer is reachable.
		reachableRef(n).update(true);
		if (n.body() != null) {
			reachableRef(n.body()).update(true);
			equateInto(completesRef(n), completesRef(n.body()));
		} else {
			completesRef(n).update(true);
		}
	}

	public void visit(final FieldDecl_c n, Node parent) {
		// The block that is the body of a constructor, method, instance
		// initializer or static initializer is reachable.
		reachableRef(n).update(true);
		if (n.init() != null) {
			reachableRef(n.init()).update(true);
			equateInto(completesRef(n), completesRef(n.init()));
		} else {
			completesRef(n).update(true);
		}
	}

	public void visit(final ConstructorDecl_c n, Node parent) {
		// The block that is the body of a constructor, method, instance
		// initializer or static initializer is reachable.
		reachableRef(n).update(true);
		if (n.body() != null) {
			reachableRef(n.body()).update(true);
			equateInto(completesRef(n), completesRef(n.body()));
		} else {
			completesRef(n).update(true);
		}
	}

	public void visit(final AbstractBlock_c n, Node parent) {
		// An empty block that is not a switch block can complete normally iff
		// it is reachable.
		// A nonempty block that is not a switch block can complete normally iff
		// the last statement in it can complete normally.
		// The first statement in a nonempty block that is not a switch block is
		// reachable iff the block is reachable.
		// Every other statement S in a nonempty block that is not a switch
		// block is reachable iff the statement preceding S can complete
		// normally.
		Ref<Boolean> prev = reachableRef(n);
		for (Stmt s : n.statements()) {
			equateInto(reachableRef(s), prev);
			prev = completesRef(s);
		}
		equateInto(completesRef(n), prev);
	}

	public void visit(LocalClassDecl_c n, Node parent) {
		// A local class declaration statement can complete normally iff it is
		// reachable.
		completesIfReachable(n);
	}

	public void visit(LocalDecl_c n, Node parent) {
		// A local variable declaration statement can complete normally iff it
		// is reachable.
		completesIfReachable(n);
		// if (n.init() != null)
		// equateInto(reachableRef(n.init()), reachableRef(n));
	}

	public void visit(Empty_c n, Node parent) {
		// An empty statement can complete normally iff it is reachable.
		completesIfReachable(n);
	}

	public void visit(Labeled_c n, Node parent) {
		// The contained statement is reachable iff the labeled statement is
		// reachable.
		equateInto(reachableRef(n.statement()), reachableRef(n));

		// A labeled statement can complete normally if at least one of the
		// following is true:
		// The contained statement can complete normally.
		// There is a reachable break statement that exits the labeled
		// statement.
		final Ref<Boolean> a = completesRef(n.statement());
		final Ref<Collection<Name>> b = breaksRef(n.statement());
		final Ref<Collection<Name>> c = continuesRef(n.statement());
		final Name labelId = n.labelNode().id();
		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return a.get() || b.get().contains(labelId);
			}
		}, jobClock());
	}

	// An expression statement can complete normally iff it is reachable.
	public void visit(Eval_c n, Node parent) {
		completesIfReachable(n);
	}

	public void visit(ConstructorCall_c n, Node parent) {
		completesIfReachable(n);
	}

	public void visit(final Case_c n, Node parent) {
		completesIfReachable(n);
	}

	public void visit(final Switch_c n, Node parent) {
		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				// A switch statement can complete normally iff at least one of
				// the following is true:
				boolean onlyCase = true;
				boolean hasDefault = false;
				for (SwitchElement x : n.elements()) {
					if (x instanceof Case) {
						if (((Case) x).isDefault())
							hasDefault = true;
					} else
						onlyCase = false;
				}
				// The switch block is empty or contains only switch labels.
				if (onlyCase)
					return true;
				// The switch block does not contain a default label.
				if (!hasDefault)
					return true;
				// The last statement in the switch block can complete normally.
				// There is at least one switch label after the last switch
				// block statement group.
				SwitchElement lastStmt = n.elements().get(
						n.elements().size() - 1);
				if (completesRef(lastStmt).get())
					return true;
				// There is a reachable break statement that exits the switch
				// statement.
				for (SwitchElement x : n.elements()) {
					if (breaksRef(x).get().contains(null))
						return true;
				}
				return false;
			}
		}, jobClock());

		Ref<Boolean> prev = reachableRef(n);
		for (SwitchElement x : n.elements()) {
			// A statement in a switch block is reachable iff its switch
			// statement is reachable and at least one of the following is true:
			// It bears a case or default label.
			// There is a statement preceding it in the switch block and that
			// preceding statement can complete normally.
			if (x instanceof Case)
				equateInto(reachableRef(x), reachableRef(n));
			else
				equateInto(reachableRef(x), prev);
			prev = completesRef(x);
		}
	}

	public void visit(final While_c n, Node parent) {
		// The contained statement is reachable iff the while statement is
		// reachable and the condition expression
		// is not a constant expression whose value is false.
		final Ref<Boolean> r = reachableRef(n);
		reachableRef(n.body()).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return r.get() && !isEqual(n.cond(), false);
			}
		}, jobClock());
		// equateInto(reachableRef(n.cond()), reachableRef(n.body()));

		// A while statement can complete normally iff at least one of the
		// following is true:
		// The while statement is reachable and the condition expression is not
		// a constant expression with value true.
		// There is a reachable break statement that exits the while statement.
		final Ref<Collection<Name>> b = breaksRef(n.body());
		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return (r.get() && !isEqual(n.cond(), true))
						|| b.get().contains(null);
			}
		}, jobClock());
	}

	// A do statement can complete normally iff at least one of the following is
	// true:
	// The contained statement can complete normally and the condition
	// expression is not a constant expression with value true.
	// The do statement contains a reachable continue statement with no label,
	// and the do statement is the innermost while, do, or for statement that
	// contains that continue statement, and the condition expression is not a
	// constant expression with value true.
	// The do statement contains a reachable continue statement with a label L,
	// and the do statement has label L, and the condition expression is not a
	// constant expression with value true.
	// There is a reachable break statement that exits the do statement.
	// The contained statement is reachable iff the do statement is reachable.
	public void visit(final Do_c n, Node parent) {
		equateInto(reachableRef(n.body()), reachableRef(n));

		final Ref<Collection<Name>> b = breaksRef(n.body());
		final Ref<Collection<Name>> c = continuesRef(n.body());

		final Name label = parent instanceof Labeled ? ((Labeled) parent)
				.labelNode().id() : null;

		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				if (completesRef(n.body()).get() && !isEqual(n.cond(), true))
					return true;
				if (c.get().contains(null) && !isEqual(n.cond(), true))
					return true;
				if (label != null && c.get().contains(label)
						&& !isEqual(n.cond(), true))
					return true;
				if (b.get().contains(null))
					return true;
				return false;
			}
		}, jobClock());
	}

	private boolean isEqual(Expr e, boolean f) {
		if (e == null)
			return false;
		if (e.constantValue() == null)
			return false;
		return e.constantValue().equals(f);
	}

	public void visit(final For_c n, Node parent) {
		// The contained statement is reachable iff the for statement is
		// reachable and the condition expression is not a constant expression
		// whose value is false.
		final Ref<Boolean> r = reachableRef(n);
		reachableRef(n.body()).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return r.get() && !isEqual(n.cond(), false);
			}
		}, jobClock());

		for (ForInit i : n.inits()) {
			equateInto(reachableRef(i), reachableRef(n));
		}
		for (ForUpdate i : n.iters()) {
			equateInto(reachableRef(i), reachableRef(n));
		}
		// if (n.cond() != null)
		// equateInto(reachableRef(n.cond()), reachableRef(n));

		// A for statement can complete normally iff at least one of the
		// following is true:
		// The for statement is reachable, there is a condition expression, and
		// the condition expression is not a constant expression with value
		// true.
		// There is a reachable break statement that exits the for statement.
		final Ref<Collection<Name>> b = breaksRef(n.body());

		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				if (r.get() && !isEqual(n.cond(), true))
					return true;
				if (b.get().contains(null))
					return true;
				return false;
			}
		}, jobClock());
	}

	// A break, continue, return, or throw statement cannot complete normally.
	public void visit(Return_c n, Node parent) {
		completesAbruptly(n);
	}

	private void completesAbruptly(Term n) {
		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				return false;
			}
		}, jobClock());
	}

	// A break, continue, return, or throw statement cannot complete normally.
	public void visit(final Throw_c n, Node parent) {
		completesAbruptly(n);
	}

	// A break, continue, return, or throw statement cannot complete normally.
	public void visit(final Branch_c n, Node parent) {
		completesAbruptly(n);
	}

	public void visit(final Synchronized_c n, Node parent) {
		// The contained statement is reachable iff the synchronized statement
		// is reachable.
		equateInto(reachableRef(n.body()), reachableRef(n));
		// A synchronized statement can complete normally iff the contained
		// statement can complete normally.
		equateInto(completesRef(n), completesRef(n.body()));
	}

	public void visit(final Catch_c n, Node parent) {
		equateInto(reachableRef(n.body()), reachableRef(n));
		equateInto(completesRef(n), completesRef(n.body()));
	}

	public void visit(final Try_c n, Node parent) {
		// The try block is reachable iff the try statement is reachable.
		equateInto(reachableRef(n.tryBlock()), reachableRef(n));

		// A try statement can complete normally iff both of the following are
		// true:
		// The try block can complete normally or any catch block can complete
		// normally.
		// If the try statement has a finally block, then the finally block can
		// complete normally.
		completesRef(n).setResolver(new Callable<Boolean>() {
			public Boolean call() {
				if (n.finallyBlock() != null
						&& !completesRef(n.finallyBlock()).get())
					return false;
				if (completesRef(n.tryBlock()).get())
					return true;
				for (Catch cb : n.catchBlocks()) {
					if (completesRef(cb).get())
						return true;
				}
				return false;
			}
		}, jobClock());

		// If a finally block is present, it is reachable iff the try statement
		// is reachable.
		if (n.finallyBlock() != null) {
			equateInto(reachableRef(n.finallyBlock()), reachableRef(n));
		}

		List<Ref<Type>> earlier = new ArrayList<Ref<Type>>();
		for (final Catch cb : n.catchBlocks()) {
			// A catch block C is reachable iff both of the following are true:
			// Some expression or throw statement in the try block is reachable
			// and can throw an exception whose type is assignable to the
			// parameter of the catch clause C.
			// (An expression is considered reachable iff the innermost
			// statement containing it is reachable.)
			// There is no earlier catch block A in the try statement such that
			// the type of C's parameter is the same as or a subclass of the
			// type of A's parameter.

			final List<Ref<Type>> e = new ArrayList<Ref<Type>>(earlier);
			reachableRef(cb).setResolver(new Callable<Boolean>() {
				public Boolean call() {
					Collection<Type> types = throwsRef(n.tryBlock()).get();
					boolean earlierCatch = false;
					for (Ref<Type> tr : e) {
						if (ts.isSubtype(tr.get(), cb.catchType(), n.context())) {
							earlierCatch = true;
						}
					}
					boolean catchesThrown = false;
					for (Type t : types) {
						if (ts.isCastValid(t, cb.catchType(), n.context())) {
							catchesThrown = true;
						}
					}
					return catchesThrown && !earlierCatch;
				}
			}, jobClock());

			earlier.add(cb.formal().typeNode().typeRef());
		}
	}

	public void visit(final Expr_c n, Node parent) {
		reachableRef(n).update(true);
		completesRef(n).update(true);
	}

	public void visit(final If_c n, Node parent) {
		final Ref<Boolean> r = reachableRef(n);

		// The then-statement is reachable iff the if-then-else statement is
		// reachable.
		equateInto(reachableRef(n.consequent()), r);

		if (n.alternative() == null) {
			// An if-then statement can complete normally iff it is reachable.
			completesIfReachable(n);
		} else {
			// The else-statement is reachable iff the if-then-else statement is
			// reachable.
			equateInto(reachableRef(n.alternative()), r);

			// An if-then-else statement can complete normally iff the
			// then-statement can complete normally or the else-statement can
			// complete normally.
			completesRef(n).setResolver(new Callable<Boolean>() {
				public Boolean call() {
					return completesRef(n.consequent()).get()
							|| completesRef(n.alternative()).get();
				}
			}, jobClock());
		}
	}

	private static <T> Set<T> packSet(Set<T> ns) {
		if (ns.size() == 0)
			return Collections.<T> emptySet();
		if (ns.size() == 1)
			return Collections.<T> singleton(ns.iterator().next());
		return ns;
	}

}
