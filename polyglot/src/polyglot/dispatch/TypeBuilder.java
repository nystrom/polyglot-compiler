package polyglot.dispatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polyglot.ast.AmbQualifierNode_c;
import polyglot.ast.AmbReceiver_c;
import polyglot.ast.AmbTypeNode_c;
import polyglot.ast.ArrayTypeNode_c;
import polyglot.ast.Block;
import polyglot.ast.Call_c;
import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorCall_c;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Expr;
import polyglot.ast.Expr_c;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Field_c;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.Initializer_c;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Local_c;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Node_c;
import polyglot.ast.QualifierNode;
import polyglot.ast.SourceFile_c;
import polyglot.ast.TypeNode;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.FieldDef;
import polyglot.types.Flags;
import polyglot.types.InitializerDef;
import polyglot.types.LocalDef;
import polyglot.types.MethodDef;
import polyglot.types.ObjectType;
import polyglot.types.QName;
import polyglot.types.Qualifier;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Ref.Callable;
import polyglot.types.Ref.Handler;
import polyglot.types.VarDef_c.ConstantValue;
import polyglot.util.ErrorInfo;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeBuilderContext;

public class TypeBuilder extends Visitor {

	Job job;
	TypeSystem ts;
	NodeFactory nf;

	public TypeBuilder(Job job, TypeSystem ts, NodeFactory nf) {
		this.job = job;
		this.ts = ts;
		this.nf = nf;
	}

	protected funicular.Clock jobClock() {
		return (funicular.Clock) Globals.currentJob().get("clock");
	}

	public Node visit(Node_c n, TypeBuilderContext tb) throws SemanticException {
		// System.out.println("missing node " + n + " instanceof " +
		// n.getClass().getName());
		return (Node_c) acceptChildren(n, tb);
	}

	public Node visit(SourceFile_c n, TypeBuilderContext tb)
			throws SemanticException {
		if (n.package_() != null) {
			tb = tb.pushPackage(Types.get(n.package_().package_())).pushStatic(
					true);
		}
		SourceFile_c n1 = (SourceFile_c) acceptChildren(n, tb);
		return n1;
	}

	public Node visit(Expr_c n, TypeBuilderContext tb) throws SemanticException {
		n = (Expr_c) acceptChildren(n, tb);

		final Expr_c n0 = n;

		// n.checkedRef().setResolver(new Callable<Node>() {
		// public Node compute() {
		// return n0.accept(new TypeChecker(job, ts, nf));
		// }
		// });

		n.typeRef().setResolver(new Callable<Type>() {
			public Type call() {
				return ((Expr) n0.checked()).type();
			}
		}, jobClock());

		return n;
	}

	public Node visit(Call_c n, TypeBuilderContext tb) throws SemanticException {
		n = (Call_c) visit((Expr_c) n, tb);

		final Call_c n0 = n;

		n.methodInstanceRef().setResolver(new Runnable() {
			public void run() {
				n0.checked();
			}
		}, jobClock());

		return n;
	}

	public Node visit(LocalDecl_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (LocalDecl_c) acceptChildren(n, tb);
		final LocalDef li = ts.localDef(n.position(), n.flags().flags(), n
				.typeNode().typeRef(), n.name().id());

		if (n.init() != null) {
			final LocalDecl_c xx = n;
			Ref<ConstantValue> r = li.constantValueRef();
			r.setResolver(new Runnable() {
				boolean started = false;

				public void run() {
					if (started) {
						// The local is not constant if the initializer is
						// recursive.
						li.setNotConstant();
					} else {
						xx.checked();
					}
				}
			}, jobClock());
		}

		return n.localDef(li);
	}

	public Node visit(Local_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (Local_c) visit((Expr_c) n, tb);
		final Local_c n0 = n;
		n.localInstanceRef().setResolver(new Runnable() {
			public void run() {
				n0.checked();
			}
		}, jobClock());
		return n;
	}

	public Node visit(ConstructorCall_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (ConstructorCall_c) acceptChildren(n, tb.pushStatic(true));

		// Remove super() calls for java.lang.Object.
		if (n.kind() == ConstructorCall_c.SUPER
				&& tb.currentClass() == ts.Object()) {
			return nf.Empty(n.position());
		}

		final ConstructorCall_c n0 = n;
		n.constructorInstanceRef().setResolver(new Runnable() {
			public void run() {
				n0.checked();
			}
		}, jobClock());

		return n;
	}

	public Node visit(New_c n, TypeBuilderContext tb) throws SemanticException {
		final New_c n0 = n;
		n.constructorInstanceRef().setResolver(new Runnable() {
			public void run() {
				n0.checked();
			}
		}, jobClock());

		Expr qual = (Expr) accept(n.qualifier(), tb);
		final TypeNode objectType = (TypeNode) accept(n.objectType(), tb);
		List<Expr> arguments = (List<Expr>) accept(n.arguments(), tb);

		ClassBody body = null;

		if (n.body() != null) {
			TypeBuilderContext tb2 = tb.pushAnonClass(n.position()).pushStatic(
					false);
			ClassDef type = tb2.currentClass();

			n = (New_c) n.anonType(type);

			Ref<Type> t = Types.<Type> lazyRef(ts.unknownType(n.position()));
			type.superType(t);
			t.setResolver(new Callable<Type>() {
				public Type call() {
					Type ct = objectType.type();
					if (!ct.toClass().flags().isInterface()) {
						return ct;
					} else {
						return ts.Object();
					}
				}
			}, jobClock());

			Ref<Type> ti = Types.<Type> lazyRef(ts.unknownType(n.position()));
			type.addInterface(ti);
			ti.setResolver(new Callable<Type>() {
				public Type call() {
					Type ct = objectType.type();
					if (ct.toClass().flags().isInterface()) {
						return ct;
					} else {
						return null;
					}
				}
			}, jobClock());

			body = (ClassBody) accept(n.body(), tb2);
		}

		n = (New_c) n.qualifier(qual);
		n = (New_c) n.objectType(objectType);
		n = (New_c) n.arguments(arguments);
		n = (New_c) n.body(body);

		final Expr_c n1 = n;

		n.typeRef().setResolver(new Callable<Type>() {
			public Type call() {
				return ((Expr) n1.checked()).type();
			}
		}, jobClock());

		return n1;
	}

	public Node visit(Field_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (Field_c) visit((Expr_c) n, tb);

		final Field_c n0 = n;
		n.fieldInstanceRef().setResolver(new Runnable() {
			public void run() {
				n0.checked();
			}
		}, jobClock());

		return n;
	}

	public Node visit(AmbTypeNode_c n, final TypeBuilderContext tb)
			throws SemanticException {
		n = (AmbTypeNode_c) acceptChildren(n, tb);
		if (n.typeRef() == null) {
			final AmbTypeNode_c n1 = n;
			final Ref<Type> r = Types.<Type> lazyRef(ts.unknownType(n
					.position()));
			r.setResolver(new Callable<Type>() {
				public Type call() {
					TypeNode tn = (TypeNode) n1.checked();
					assert tn.typeRef().forced();
					return tn.typeRef().getCached();
				}
			}, jobClock());
			return n.typeRef(r);
		} else {
			return n;
		}
	}

	public Node visit(AmbQualifierNode_c n, final TypeBuilderContext tb)
			throws SemanticException {
		n = (AmbQualifierNode_c) acceptChildren(n, tb);
		final AmbQualifierNode_c n1 = n;
		final Ref<Qualifier> r = Types.<Qualifier> lazyRef(ts
				.unknownQualifier(n.position()));
		r.setResolver(new Callable<Qualifier>() {
			public Qualifier call() {
				QualifierNode tn = (QualifierNode) n1.checked();
				assert tn.qualifierRef().forced();
				return tn.qualifierRef().getCached();
			}
		}, jobClock());
		return n.qualifier(r);
	}

	public Node visit(AmbReceiver_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (AmbReceiver_c) acceptChildren(n, tb);
		return n.type(ts.unknownType(n.position()));
	}

	public Node visit(ArrayTypeNode_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (ArrayTypeNode_c) acceptChildren(n, tb);
		return n.typeRef(Types.<Type> ref(ts.arrayOf(n.position(), n.base()
				.typeRef())));
	}

	public Node visit(CanonicalTypeNode_c n, final TypeBuilderContext tb)
			throws SemanticException {
		n = (CanonicalTypeNode_c) acceptChildren(n, tb);
		if (n.typeRef() == null) {
			final CanonicalTypeNode_c n1 = n;
			final Ref<Type> r = Types.<Type> ref(ts.errorType(n.position()));
			return n.typeRef(r);
		} else {
			return n;
		}
	}

	public Node visit(MethodDecl_c n, TypeBuilderContext tb)
			throws SemanticException {
		ClassDef ct = tb.currentClass();
		assert ct != null;

		// System.out.println("n = " + n);
		// System.out.println("pos " + n.position());
		// System.out.println("ct = " + ct);
		// System.out.println("tb = " + tb);

		final Flags flags = ct.flags().isInterface() ? n.flags().flags()
				.Public().Abstract() : n.flags().flags();

		MethodDef mi = n.createMethodDef(ts, ct, flags);
		ct.addMethod(mi);

		TypeBuilderContext tbChk = tb.pushCode(mi).pushStatic(flags.isStatic());

		final TypeBuilderContext tbx = tb;
		final MethodDef mix = mi;

		MethodDecl_c n1 = (MethodDecl_c) n.visitSignature(new NodeVisitor() {
			public Node override(Node n) {
				return accept(n, tbx.pushCode(mix).pushStatic(flags.isStatic()));
			}
		});

		List<Ref<? extends Type>> formalTypes = new ArrayList<Ref<? extends Type>>(
				n1.formals().size());
		for (Formal f : n1.formals()) {
			formalTypes.add(f.typeNode().typeRef());
		}

		List<Ref<? extends Type>> throwTypes = new ArrayList<Ref<? extends Type>>(
				n1.throwTypes().size());
		for (TypeNode tn : n1.throwTypes()) {
			throwTypes.add(tn.typeRef());
		}

		mi.setReturnType(n1.returnType().typeRef());
		mi.setFormalTypes(formalTypes);
		mi.setThrowTypes(throwTypes);

		Block body = (Block) accept(n1.body(), tbChk);

		n1 = (MethodDecl_c) n1.body(body);
		return n1.methodDef(mi);
	}

	public Node visit(FieldDecl_c n, TypeBuilderContext tb)
			throws SemanticException {
		ClassDef ct = tb.currentClass();
		assert ct != null;

		final Flags flags = ct.flags().isInterface() ? n.flags().flags()
				.Public().Static().Final() : n.flags().flags();

		FieldDef fi = n.createFieldDef(ts, ct, flags);
		ct.addField(fi);

		TypeBuilderContext tbChk = tb.pushDef(fi).pushStatic(flags.isStatic());

		InitializerDef ii = null;

		if (n.init() != null) {
			Flags iflags = flags.isStatic() ? Flags.STATIC : Flags.NONE;
			ii = n.createInitializerDef(ts, ct, iflags);
			fi.setInitializer(ii);
			tbChk = tbChk.pushCode(ii).pushStatic(flags.isStatic());
		}

		final TypeBuilderContext tbx = tb;
		final FieldDef mix = fi;

		final FieldDecl_c n0 = n;
		FieldDecl_c n1 = (FieldDecl_c) n.visitSignature(new NodeVisitor() {
			public Node override(Node n) {
				return accept(n, tbx.pushDef(mix).pushStatic(flags.isStatic()));
			}
		});

		fi.setType(n1.type().typeRef());

		Expr init = (Expr) accept(n1.init(), tbChk);
		n1 = (FieldDecl_c) n1.init(init);

		n1 = (FieldDecl_c) n1.fieldDef(fi);

		if (ii != null) {
			n1 = (FieldDecl_c) n1.initializerDef(ii);
		}

		n1 = (FieldDecl_c) n1.flags(n1.flags().flags(flags));

		final Node xx = n1;

		final FieldDef def = n1.fieldDef();
		Ref<ConstantValue> r = def.constantValueRef();
		r.setResolver(new Runnable() {
			boolean started = false;

			public void run() {
				if (started) {
					// The field is not constant if the initializer is
					// recursive.
					//
					// But, we could be checking if the field is constant for
					// another
					// reference in the same file:
					//
					// m() { use x; }
					// final int x = 1;
					//
					// So this is incorrect. The goal below needs to be refined
					// to only visit the initializer.
					def.setNotConstant();
				} else {
					xx.checked();
				}
			}
		}, jobClock());

		return n1;
	}

	public Node visit(ConstructorDecl_c n, TypeBuilderContext tb)
			throws SemanticException {
		ClassDef ct = tb.currentClass();
		assert ct != null;

		final Flags flags = ct.flags().isInterface() ? n.flags().flags()
				.Public().Abstract() : n.flags().flags();

		ConstructorDef ci = n.createConstructorDef(ts, ct, flags);
		ct.addConstructor(ci);

		TypeBuilderContext tbChk = tb.pushCode(ci).pushStatic(flags.isStatic());

		final TypeBuilderContext tbx = tb;
		final ConstructorDef mix = ci;

		ConstructorDecl_c n1 = (ConstructorDecl_c) n
				.visitSignature(new NodeVisitor() {
					public Node override(Node n) {
						return accept(n,
								tbx.pushCode(mix).pushStatic(flags.isStatic()));
					}
				});

		List<Ref<? extends Type>> formalTypes = new ArrayList<Ref<? extends Type>>(
				n1.formals().size());
		for (Formal f : n1.formals()) {
			formalTypes.add(f.typeNode().typeRef());
		}

		List<Ref<? extends Type>> throwTypes = new ArrayList<Ref<? extends Type>>(
				n1.throwTypes().size());
		for (TypeNode tn : n1.throwTypes()) {
			throwTypes.add(tn.typeRef());
		}

		ci.setFormalTypes(formalTypes);
		ci.setThrowTypes(throwTypes);

		Block body = (Block) accept(n1.body(), tbChk);

		n1 = (ConstructorDecl_c) n1.body(body);
		return n1.constructorDef(ci);
	}

	public Node visit(ClassDecl_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = preBuildTypes(tb, n);
		n = buildTypesBody(tb, n);
		n = postBuildTypes(tb, n);
		n.classDef().inStaticContext(tb.inStaticContext());
		return n;
	}

	private ClassDecl_c postBuildTypes(TypeBuilderContext tb, ClassDecl_c n1)
			throws SemanticException {
		ClassDecl_c n = (ClassDecl_c) n1.copy();

		if (defaultConstructorNeeded(n)) {
			ConstructorDecl cd = createDefaultConstructor(n);
			TypeBuilderContext tb2 = tb.pushClass(n.classDef()).pushStatic(
					false);
			cd = (ConstructorDecl) accept(cd, tb2);
			n = (ClassDecl_c) n.body(n.body().addMember(cd));
		}

		return n;
	}

	private ConstructorDecl createDefaultConstructor(ClassDecl_c n)
			throws SemanticException {
		Position pos = n.body().position().startOf();

		Block block = null;

		Ref<? extends Type> superType = n.classDef().superType();

		if (superType != null) {
			ConstructorCall cc = nf.SuperCall(pos, Collections.EMPTY_LIST);
			block = nf.Block(pos, cc);
		} else {
			block = nf.Block(pos);
		}

		ConstructorDecl cd = nf.ConstructorDecl(pos,
				nf.FlagsNode(pos, Flags.PUBLIC), n.name(),
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, block);
		return cd;
	}

	private boolean defaultConstructorNeeded(ClassDecl_c n) {
		if (n.flags().flags().isInterface()) {
			return false;
		}
		for (ClassMember cm : n.body().members()) {
			if (cm instanceof ConstructorDecl) {
				return false;
			}
		}

		return true;
	}

	private ClassDecl_c buildTypesBody(TypeBuilderContext tb, ClassDecl_c n)
			throws SemanticException {
		TypeBuilderContext tb2 = tb.pushClass(n.classDef());
		ClassBody body = (ClassBody) accept(n.body(), tb2);
		n = (ClassDecl_c) n.body(body);
		return n;
	}

	private ClassDecl_c preBuildTypes(TypeBuilderContext tb, ClassDecl_c n1)
			throws SemanticException {
		tb = tb.pushClass(n1.position(), n1.flags().flags(), n1.name().id());

		ClassDef type = tb.currentClass();

		// Member classes of interfaces are implicitly public and static.
		if (type.isMember() && type.outer().get().flags().isInterface()) {
			type.flags(type.flags().Public().Static());
		}

		// Member interfaces are implicitly static.
		if (type.isMember() && type.flags().isInterface()) {
			type.flags(type.flags().Static());
		}

		// Interfaces are implicitly abstract.
		if (type.flags().isInterface()) {
			type.flags(type.flags().Abstract());
		}

		ClassDecl_c n = n1;
		FlagsNode flags = (FlagsNode) accept(n.flags(), tb);
		Id name = (Id) accept(n.name(), tb);

		TypeNode superClass = (TypeNode) accept(n.superClass(), tb);
		List<TypeNode> interfaces = (List<TypeNode>) accept(n.interfaces(), tb);

		n = (ClassDecl_c) n.flags(flags);
		n = (ClassDecl_c) n.name(name);
		n = (ClassDecl_c) n.superClass(superClass);
		n = (ClassDecl_c) n.interfaces(interfaces);

		n = (ClassDecl_c) n.classDef(type).flags(flags.flags(type.flags()));

		setSuperclass(n);
		setInterfaces(n);

		return n;
	}

	private void setInterfaces(ClassDecl_c n) throws SemanticException {
		final ClassDef cd = n.classDef();
		List<TypeNode> interfaces = n.interfaces();
		for (final TypeNode tn : interfaces) {
			final Ref<Type> tref = tn.typeRef();
			tref.addHandler(new Handler<Type>() {
				public void handle(Type t) {
					try {
						checkCycles(t, cd.asType());
					} catch (SemanticException e) {
						Globals.Compiler()
								.errorQueue()
								.enqueue(ErrorInfo.SEMANTIC_ERROR,
										e.getMessage(), tn.position());
						tref.updateSoftly(ts.errorType(tn.position()));
					}
				}
			});

			Ref<Type> s = tref; // cycleCheckingRef(cd, tn);

			if (Report.should_report(Report.types, 3))
				Report.report(3, "adding interface of " + cd + " to " + s);

			cd.addInterface(s);
		}
	}

	protected void checkCycles(Type curr, Type goal) throws SemanticException {
		if (curr == null) {
			return;
		}

		if (goal == curr) {
			throw new SemanticException("Circular inheritance involving "
					+ goal, curr.position());
		}

		if (curr instanceof ObjectType) {
			ObjectType ot = (ObjectType) curr;
			Type superType = null;

			if (ot.superClass() != null) {
				superType = ot.superClass();
			}

			checkCycles(superType, goal);

			for (Type si : ot.interfaces()) {
				checkCycles(si, goal);
			}
		}

		if (curr.isClass()) {
			checkCycles(curr.toClass().outer(), goal);
		}
	}

	private void setSuperclass(ClassDecl_c n) throws SemanticException {
		final ClassDef cd = n.classDef();
		final TypeNode superClass = n.superClass();

		QName objectName = ((ClassType) ts.Object()).fullName();

		if (superClass != null) {
			final Ref<Type> tref = superClass.typeRef();
			tref.addHandler(new Handler<Type>() {
				public void handle(Type t) {
					try {
						checkCycles(t, cd.asType());
					} catch (SemanticException e) {
						Globals.Compiler()
								.errorQueue()
								.enqueue(ErrorInfo.SEMANTIC_ERROR,
										e.getMessage(), superClass.position());
						tref.updateSoftly(ts.errorType(superClass.position()));
					}
				}
			});
			Ref<Type> s = tref; // cycleCheckingRef(cd, superClass);
			if (Report.should_report(Report.types, 3))
				Report.report(3, "setting superclass of " + n.classDef()
						+ " to " + s);
			cd.superType(s);
		} else if (cd.asType().equals((Object) ts.Object())
				|| cd.fullName().equals(objectName)) {
			// the type is the same as ts.Object(), so it has no superclass.
			if (Report.should_report(Report.types, 3))
				Report.report(3, "setting superclass of " + n.classDef()
						+ " to " + null);
			cd.superType(null);
		} else {
			// the superclass was not specified, and the type is not the same
			// as ts.Object() (which is typically java.lang.Object)
			// As such, the default superclass is ts.Object().
			if (Report.should_report(Report.types, 3))
				Report.report(3, "setting superclass of " + n.classDef()
						+ " to " + ts.Object());
			cd.superType(Types.<Type> ref(ts.Object()));
		}
	}

	private Ref<Type> cycleCheckingRef(final ClassDef cd,
			final TypeNode superClass) {
		Ref<Type> s = Types
				.<Type> lazyRef(ts.unknownType(superClass.position()));
		Callable<Type> resolver = cycleCheckingResolver(cd, superClass);
		s.setResolver(resolver, jobClock());
		return s;
	}

	private Callable<Type> cycleCheckingResolver(final ClassDef cd,
			final TypeNode superClass) {
		final Ref<? extends Type> t = superClass.typeRef();
		Callable<Type> resolver = new Callable<Type>() {
			public Type call() {
				Type s = t.get();
				try {
					System.out
							.println("checking cycle from " + s + " to " + cd);
					checkCycles(s, cd.asType());
				} catch (SemanticException e) {
					Globals.Compiler()
							.errorQueue()
							.enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(),
									superClass.position());
					return ts.errorType(superClass.position());
				}
				return s;
			}
		};
		return resolver;
	}

	public Node visit(Initializer_c n, final TypeBuilderContext tb)
			throws SemanticException {
		ClassDef ct = tb.currentClass();
		assert ct != null;

		final Flags flags = n.flags().flags();

		InitializerDef ii = n.createInitializerDef(ts, ct, flags);
		TypeBuilderContext tbChk = tb.pushCode(ii).pushStatic(flags.isStatic());

		final InitializerDef mix = ii;

		n = (Initializer_c) n.visitSignature(new NodeVisitor() {
			public Node override(Node n) {
				return accept(n, tb.pushCode(mix).pushStatic(flags.isStatic()));
			}
		});

		Block body = (Block) accept(n.body(), tbChk);
		n = (Initializer_c) n.body(body);

		n = (Initializer_c) n.initializerDef(ii);

		return n;
	}

	public Node visit(Formal_c n, TypeBuilderContext tb)
			throws SemanticException {
		n = (Formal_c) visit((Node_c) n, tb);

		LocalDef li = ts.localDef(n.position(), n.flags().flags(), n.typeNode()
				.typeRef(), n.name().id());

		// Formal parameters are never compile-time constants.
		li.setNotConstant();

		return n.localDef(li);
	}
}
