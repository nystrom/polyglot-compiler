package polyglot.ext.jl.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.frontend.goals.FieldConstantsChecked;
import polyglot.frontend.goals.Goal;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * A <code>LocalDecl</code> is an immutable representation of the declaration
 * of a local variable.
 */
public class LocalDecl_c extends Stmt_c implements LocalDecl {
    Flags flags;
    TypeNode type;
    String name;
    Expr init;
    LocalInstance li;

    public LocalDecl_c(Position pos, Flags flags, TypeNode type,
                       String name, Expr init)
    {
        super(pos);
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.init = init;
    }
    
    public boolean isCanonical() {
        return li != null && li.isCanonical() && super.isCanonical();
    }

    /** Get the type of the declaration. */
    public Type declType() {
        return type.type();
    }

    /** Get the flags of the declaration. */
    public Flags flags() {
        return flags;
    }

    /** Set the flags of the declaration. */
    public LocalDecl flags(Flags flags) {
        LocalDecl_c n = (LocalDecl_c) copy();
        n.flags = flags;
        return n;
    }

    /** Get the type node of the declaration. */
    public TypeNode type() {
        return type;
    }

    /** Set the type of the declaration. */
    public LocalDecl type(TypeNode type) {
        if (type == this.type) return this;
        LocalDecl_c n = (LocalDecl_c) copy();
        n.type = type;
        return n;
    }

    /** Get the name of the declaration. */
    public String name() {
        return name;
    }

    /** Set the name of the declaration. */
    public LocalDecl name(String name) {
        if (name.equals(this.name)) return this;
        LocalDecl_c n = (LocalDecl_c) copy();
        n.name = name;
        return n;
    }

    /** Get the initializer of the declaration. */
    public Expr init() {
        return init;
    }

    /** Set the initializer of the declaration. */
    public LocalDecl init(Expr init) {
        if (init == this.init) return this;
        LocalDecl_c n = (LocalDecl_c) copy();
        n.init = init;
        return n;
    }

    /** Set the local instance of the declaration. */
    public LocalDecl localInstance(LocalInstance li) {
        if (li == this.li) return this;
        LocalDecl_c n = (LocalDecl_c) copy();
        n.li = li;
        return n;
    }

    /** Get the local instance of the declaration. */
    public LocalInstance localInstance() {
        return li;
    }
    
    public VarInstance varInstance() {
        return li;
    }

    /** Reconstruct the declaration. */
    protected LocalDecl_c reconstruct(TypeNode type, Expr init) {
        if (this.type != type || this.init != init) {
            LocalDecl_c n = (LocalDecl_c) copy();
            n.type = type;
            n.init = init;
            return n;
        }

        return this;
    }

    /** Visit the children of the declaration. */
    public Node visitChildren(NodeVisitor v) {
        TypeNode type = (TypeNode) visitChild(this.type, v);
        Expr init = (Expr) visitChild(this.init, v);
        return reconstruct(type, init);
    }

    /**
     * Add the declaration of the variable as we enter the scope of the
     * intializer
     */
    public Context enterScope(Node child, Context c) {
        if (child == init) {
            c.addVariable(li);
        }
        return super.enterScope(child, c);
    }

    public void addDecls(Context c) {
        // Add the declaration of the variable in case we haven't already done
        // so in enterScope, when visiting the initializer.
        c.addVariable(li);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        LocalDecl_c n = (LocalDecl_c) super.buildTypes(tb);

        TypeSystem ts = tb.typeSystem();

        LocalInstance li = ts.localInstance(position(), Flags.NONE,
                                            ts.unknownType(position()), name());
        return n.localInstance(li);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        TypeSystem ts = ar.typeSystem();

        LocalInstance li = ts.localInstance(position(),
                                            flags(), declType(), name());

        return localInstance(li);
    }

    /**
     * Override superclass behaviour to check if the variable is multiply
     * defined.
     */
    public NodeVisitor typeCheckEnter(TypeChecker tc) throws SemanticException {
        // Check if the variable is multiply defined.
        // we do it in type check enter, instead of type check since
        // we add the declaration before we enter the scope of the
        // initializer.
        Context c = tc.context();

        LocalInstance outerLocal = null;

        try {
            outerLocal = c.findLocal(li.name());
        }
        catch (SemanticException e) {
            // not found, so not multiply defined
        }

        if (outerLocal != null && c.isLocal(li.name())) {
            throw new SemanticException(
                "Local variable \"" + name + "\" multiply defined.  "
                    + "Previous definition at " + outerLocal.position() + ".",
                position());
        }
        
        return super.typeCheckEnter(tc);

    }
    
    /** Type check the declaration. */
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        LocalInstance li = this.li;

        if (li.flags().isFinal() && init() != null && init().isConstant()) {
            Object value = init().constantValue();
            li = (LocalInstance) li.constantValue(value);
        }

        try {
            ts.checkLocalFlags(flags);
        }
        catch (SemanticException e) {
            throw new SemanticException(e.getMessage(), position());
        }

        if (init != null) {
            if (init instanceof ArrayInit) {
                ((ArrayInit) init).typeCheckElements(type.type());
            }
            else {
                if (! ts.isImplicitCastValid(init.type(), type.type()) &&
                    ! ts.equals(init.type(), type.type()) &&
                    ! ts.numericConversionValid(type.type(),
                                                init.constantValue())) {
                    throw new SemanticException("The type of the variable " +
                                                "initializer \"" + init.type() +
                                                "\" does not match that of " +
                                                "the declaration \"" +
                                                type.type() + "\".",
                                                init.position());
                }
            }
        }

        return localInstance(li);
    }
    
    public Node checkConstants(ConstantChecker cc) throws SemanticException {
        if (init != null && ! init.constantValueSet()) {
            // HACK to add dependencies for computing the constant value.
            final Scheduler scheduler = cc.typeSystem().extensionInfo().scheduler();
            final Goal ccgoal = cc.goal();
            
            init.visit(new NodeVisitor() {
               public Node leave(Node old, Node n, NodeVisitor v) {
                   if (n instanceof Field) {
                       Field f = (Field) n;
                       if (! f.fieldInstance().constantValueSet()) {
                           Goal g = scheduler.FieldConstantsChecked(f.fieldInstance());
                           try {
                               scheduler.addPrerequisiteDependency(ccgoal, g);
                           }
                           catch (CyclicDependencyException e) {
                               LocalDecl_c.this.li.setNotConstant();
                           }
                       }
                   }
                   if (n instanceof Local) {
                       Local l = (Local) n;
                       if (! l.localInstance().constantValueSet()) {
                           // Undefined variable or forward reference.
                           LocalDecl_c.this.li.setNotConstant();
                       }
                   }
                   return n;
               }
            });
            
            return this;
        }
        
        if (init == null || ! init.isConstant() || ! li.flags().isFinal()) {
            li.setNotConstant();
        }
        else {
            li.setConstantValue(init.constantValue());
        }

        return this;
    }
    
    public boolean constantValueSet() {
        return li != null && li.constantValueSet();
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        if (child == init) {
            TypeSystem ts = av.typeSystem();

            // If the RHS is an integral constant, we can relax the expected
            // type to the type of the constant.
            if (ts.numericConversionValid(type.type(), child.constantValue())) {
                return child.type();
            }
            else {
                return type.type();
            }
        }

        return child.type();
    }

    public String toString() {
        return flags.translate() + type + " " + name +
                (init != null ? " = " + init : "") + ";";
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        boolean printSemi = tr.appendSemicolon(true);
        boolean printType = tr.printType(true);

        w.write(flags.translate());
        if (printType) {
            print(type, w, tr);
            w.write(" ");
        }
        w.write(name);

        if (init != null) {
            w.write(" =");
            w.allowBreak(2, " ");
            print(init, w, tr);
        }

        if (printSemi) {
            w.write(";");
        }

        tr.printType(printType);
        tr.appendSemicolon(printSemi);
    }

    public void dump(CodeWriter w) {
        super.dump(w);

        if (li != null) {
            w.allowBreak(4, " ");
            w.begin(0);
            w.write("(instance " + li + ")");
            w.end();
        }

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(name " + name + ")");
	w.end();
    }

    public Term entry() {
        if (init() != null) {
            return init().entry();
        }
        return this;
    }

    public List acceptCFG(CFGBuilder v, List succs) {
        if (init() != null) {
            v.visitCFG(init(), this);
        }

        return succs;
    }
}
