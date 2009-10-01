package polyglot.bytecode;

import polyglot.ast.Binary;
import polyglot.ast.BooleanLit;
import polyglot.ast.Expr;
import polyglot.ast.NodeFactory;
import polyglot.ast.Unary;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.types.Type;
import polyglot.dispatch.Dispatch;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

/**
 * This class produces bytecode for a Thorn expression. It leaves a single
 * IObject on the operand stack.
 */
@SuppressWarnings("unused")
public class BranchTranslator extends AbstractExpTranslator {
    ILabel branchTarget;
    boolean branchOnTrue;
    
    public BranchTranslator(final Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, MethodContext context, final ILabel branchTarget_, final boolean branchOnTrue_) {
        super(job, ts, nf, bc, context);
        this.branchTarget = branchTarget_;
        this.branchOnTrue = branchOnTrue_;
        this.ts = Globals.TS();
    }

    boolean unreachable() {
        return il.isUnreachable();
    }

    public void visit(final Expr n) throws SemanticException {
        final polyglot.types.Type t = n.type();

        if (!t.isBoolean()) {
            throw new SemanticException("Cannot branch on non-boolean expression " + n + "; expression has type " + t + ".", n.position());
        }

        if (optimizeCall(n))
            return;
        
        visitChild(n, new ExprTranslator(job, ts, nf, bc, context));

        if (unreachable())
            return;

        il.uncheckedCoerce(Type.BOOLEAN, Type.INT);

        if (branchOnTrue) {
            il.IFNE(branchTarget, n.position());
        }
        else {
            il.IFEQ(branchTarget, n.position());
        }
    }

    public void visit(final BooleanLit n) {
        if (n.value() == branchOnTrue) {
            il.GOTO(branchTarget, n.position());
        }
        else {
            // fallthru
        }
    }

    abstract class EqualsNull implements Optimization {
        boolean branchOnTrue;

        EqualsNull(final boolean branchOnTrue) {
            this.branchOnTrue = branchOnTrue;
        }

        abstract void instruction();

        public boolean apply(final Expr e) {
            if (!(e instanceof Binary)) {
                return false;
            }
            Binary n = (Binary) e;
            
            if (n.operator() != Binary.EQ)
                return false;

            if (branchOnTrue != BranchTranslator.this.branchOnTrue)
                return false;

            visitChild(n.left());
            visitChild(n.right());

            instruction();

            return true;
        }
    }

    class Not implements Optimization {
        public boolean apply(final Expr e) {
            if (!(e instanceof Unary))
                return false;

            final Unary n = (Unary) e;

            final Position pos = n.position();
            if (n.operator() != Unary.NOT)
                return false;

            if (!typeof(n.expr()).equals(Type.BOOLEAN))
                return false;

            visitBranch(n.expr(), branchTarget, !branchOnTrue);

            return true;
        }
    }

    abstract class CompareOpt implements Optimization {
        Binary.Operator op;
        boolean branchOnTrue;
        Type leftType;
        Type rightType;
        Type resultType;

        CompareOpt(final Binary.Operator op, final boolean branchOnTrue, Type leftType, Type rightType, final Type resultType) {
            this.op = op;
            this.branchOnTrue = branchOnTrue;
            this.leftType = leftType;
            this.rightType = rightType;
            this.resultType = resultType;
        }

        abstract void instruction();

        public boolean apply(final Expr e) {
            if (branchOnTrue != BranchTranslator.this.branchOnTrue) {
                return false;
            }

            if (!(e instanceof Binary))
                return false;

            final Binary n = (Binary) e;

            final Position pos = n.position();
            final Binary.Operator name = n.operator();

            if (name != op)
                return false;

            if (!typeof(n.left()).equals(leftType))
                return false;
            if (!typeof(n.right()).equals(rightType))
                return false;

            visitChild(n.left(), leftType);
            if (il.isUnreachable())
                return true;
            visitChild(n.right(), rightType);

            instruction();
            return true;
        }
    }
    
    class ShortcutOp implements Optimization {
        Binary.Operator op;
        
        ShortcutOp(final Binary.Operator op) {
            this.op = op;
        }
        
        public boolean apply(final Expr e) {
            if (!(e instanceof Binary))
                return false;
            
            final Binary n = (Binary) e;
            
            final Position pos = n.position();
            final Binary.Operator name = n.operator();
            
            if (name != op)
                return false;
            
            if (op == Binary.COND_OR && BranchTranslator.this.branchOnTrue) {
                visitBranch(n.left(), BranchTranslator.this.branchTarget, true);
                if (il.isUnreachable())
                    return true;
                visitBranch(n.right(), BranchTranslator.this.branchTarget, true);
                return true;
            }
            
            if (op == Binary.COND_OR && ! BranchTranslator.this.branchOnTrue) {
                visitBranch(n.left(), BranchTranslator.this.branchTarget, false);
                if (il.isUnreachable())
                    return true;
                visitBranch(n.right(), BranchTranslator.this.branchTarget, false);
            }

            if (op == Binary.COND_AND && BranchTranslator.this.branchOnTrue) {
                ILabel L = il.makeLabel(pos);
                visitBranch(n.left(), L, false);
                if (il.isUnreachable())
                    return true;
                visitBranch(n.right(), BranchTranslator.this.branchTarget, true);
                il.addLabel(L);
                return true;
            }

            if (op == Binary.COND_AND && ! BranchTranslator.this.branchOnTrue) {
                ILabel L = il.makeLabel(pos);
                visitBranch(n.left(), L, false);
                if (il.isUnreachable())
                    return true;
                visitBranch(n.right(), BranchTranslator.this.branchTarget, false);
                il.addLabel(L);
                return true;
            }

            return true;
        }
    }

    private boolean optimizeCall(final Expr n) {
        final Position pos = n.position();
        final Optimization[] opts = new Optimization[] { new EqualsNull(true) {
            void instruction() {
                il.IFNULL(branchTarget, pos);
            }
        }, new EqualsNull(false) {
            void instruction() {
                il.IFNONNULL(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GT, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPEQ(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, true, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GT, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, false, Type.INT, Type.INT, Type.BOOLEAN) {
            void instruction() {
                il.IF_ICMPEQ(branchTarget, pos);
            }
        },

        new CompareOpt(Binary.GT, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFEQ(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, true, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GT, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, false, Type.LONG, Type.LONG, Type.BOOLEAN) {
            void instruction() {
                il.LCMP(pos);
                il.IFEQ(branchTarget, pos);
            }
        },

        // TODO: FCMPG pushes 1 if NaN, FCMPL pushes -1. Not sure which to use.
        // javac uses FCMPL when branching on < and <= and FCMPG on > and >=.
        new CompareOpt(Binary.GT, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPG(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPG(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFEQ(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, true, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GT, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPG(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPG(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, false, Type.FLOAT, Type.FLOAT, Type.BOOLEAN) {
            void instruction() {
                il.FCMPL(pos);
                il.IFEQ(branchTarget, pos);
            }
        },

        new CompareOpt(Binary.GT, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPG(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPG(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFEQ(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, true, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GT, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFLT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LT, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPG(pos);
                il.IFGT(branchTarget, pos);
            }
        }, new CompareOpt(Binary.GE, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFLE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.LE, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPG(pos);
                il.IFGE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.EQ, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFNE(branchTarget, pos);
            }
        }, new CompareOpt(Binary.NE, false, Type.DOUBLE, Type.DOUBLE, Type.BOOLEAN) {
            void instruction() {
                il.DCMPL(pos);
                il.IFEQ(branchTarget, pos);
            }
        },

        new Not(),
        
        new ShortcutOp(Binary.COND_AND),
        new ShortcutOp(Binary.COND_OR),
        
        };

        for (final Optimization opt : opts) {
            if (opt.apply(n))
                return true;
        }

        return false;
    }

    public String toString() {
        return il.toString();
    }

}
