package polyglot.bytecode;

import java.util.Collections;
import java.util.List;

import polyglot.ast.ArrayAccess;
import polyglot.ast.ArrayAccessAssign;
import polyglot.ast.ArrayInit;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.BooleanLit;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.CharLit;
import polyglot.ast.ClassLit;
import polyglot.ast.Conditional;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldAssign;
import polyglot.ast.FloatLit;
import polyglot.ast.Instanceof;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.LocalAssign;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.NodeFactory;
import polyglot.ast.NullLit;
import polyglot.ast.Special;
import polyglot.ast.StringLit;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.bytecode.rep.IClassGen;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.bytecode.types.Unreachable;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.LocalDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * This class produces bytecode for a Java expression. It leaves a single value
 * on the operand stack.
 */
public class ExprTranslator extends AbstractExpTranslator {
    public ExprTranslator(final Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, MethodContext context) {
        super(job, ts, nf, bc, context);
    }

    public void visit(final FieldAssign n) {
        if (n.target() instanceof Expr) {
            if (n.operator() != Assign.ASSIGN) {
                visitChild(n.target());
                il.DUP(n.position());
                il.GETFIELD(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
                il.uncheckedCoerce(typeof(n));
                promote(n.right(), n.type());
                binaryOp(n.operator(), typeof(n), n.position()); // S e v
                if (il.currentStack().top().isWide())
                    il.DUP2_X1(n.position()); // S v e v  3(21) -> (21)3(21)
                else
                    il.DUP_X1(n.position());  // S v e v  21 -> 121
                coerce(il, typeof(n), typeof(n.fieldInstance().type()), n.position());
                il.PUTFIELD(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
            }
            else {
                visitChild(n.target());
                visitChild(n.right()); // S e v
                if (il.currentStack().top().isWide())
                    il.DUP2_X1(n.position()); // S v e v  3(21) -> (21)3(21)
                else
                    il.DUP_X1(n.position());  // S v e v  21 -> 121
                coerce(il, typeof(n.right()), typeof(n.fieldInstance().type()), n.position());
                il.PUTFIELD(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
            }
        }
        else {
            if (n.operator() != Assign.ASSIGN) {
                il.GETSTATIC(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
                il.uncheckedCoerce(typeof(n));
                promote(n.right(), n.type());
                binaryOp(n.operator(), typeof(n), n.position()); // S v
                if (il.currentStack().top().isWide())
                    il.DUP2(n.position()); // S v v
                else
                    il.DUP(n.position());  // S v v
                coerce(il, typeof(n), typeof(n.fieldInstance().type()), n.position());
                il.PUTSTATIC(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
            }
            else {
                visitChild(n.right()); // S v
                if (il.currentStack().top().isWide())
                    il.DUP2(n.position()); // S v v
                else
                    il.DUP(n.position());  // S v v
                coerce(il, typeof(n.right()), typeof(n.fieldInstance().type()), n.position());
                il.PUTSTATIC(typeof(n.fieldInstance().container()), n.fieldInstance().name().toString(), typeof(n.fieldInstance().type()), n.position());    
            }
        }
    }

    public void visit(LocalAssign n) {
        if (n.operator() != Assign.ASSIGN) {
            visitChild(n.local()); // S v
            visitChild(n.right()); // S v e
            binaryOp(n.operator(), typeof(n), n.position()); // S v+e
            il.dup(n.position());  // S v+e v+e
            coerce(il, typeof(n), typeof(n.local()), n.position());
            store(n.local().localInstance(), n.position());  // S v+e
        }
        else {
            visitChild(n.right());   // S e
            il.dup(n.position());    // S e e      
            coerce(il, typeof(n.right()), typeof(n.local()), n.position());
            store(n.local().localInstance(), n.position()); // S e
        }
    }

    public void visit(final ArrayAccessAssign n) {
        if (n.operator() != Assign.ASSIGN) {
            visitChild(n.array()); // S a
            visitChild(n.index()); // S a i
            il.DUP2(n.position());
            arrayLoad(n.type(), n.position());
            visitChild(n.right());   // S a i a[i] e
            binaryOp(n.operator(), typeof(n), n.position());  // S a i v
            if (il.currentStack().top().isWide())
                il.DUP2_X2(n.position()); // S v a i v  43(21) -> (21)43(21)
            else
                il.DUP_X2(n.position());  // S v a i v  321 -> 1321
            coerce(il, typeof(n), typeof(n), n.position());
            arrayStore(n.type(), n.position());
        }
        else {
            visitChild(n.array());
            visitChild(n.index());
            visitChild(n.right()); // S a i v
            if (il.currentStack().top().isWide())
                il.DUP2_X2(n.position()); // S v a i v  43(21) -> (21)43(21)
            else
                il.DUP_X2(n.position());  // S v a i v  321 -> 1321
            coerce(il, typeof(n.right()), typeof(n), n.position());
            arrayStore(n.type(), n.position());
        }
    }

    public void visit(final ArrayAccess n) {
        visitChild(n.array());
        if (il.isUnreachable()) return;
        visitChild(n.index());
        if (il.isUnreachable()) return;
        arrayLoad(n.type(), n.position());
    }

    public void visit(final New n) {
        if (n.body() != null) {
            IClassGen cg = new ClassTranslator(job, ts, nf, bc, n.anonType(), context).translateClass(n, n.body());
            context.cg.addInnerClass(cg);
        }

        alloc((ClassType) n.type(), n.constructorInstance().formalTypes(), n.arguments(), n.position());
    }

    Type IObject = Type.OBJECT;

    public void visit(final Local n) {
        final MethodContext ti = context.getDeclaringExp(n.localInstance().def());
        final MethodContext t = context;

        final boolean boxed = ti.isBoxedHere(n.localInstance().def());
        final Type type = typeof(n);

        if (boxed) {
            // Get a pointer to the frame
            //            if (ti == t) {
            //                final int frameIndex = t.getFrameIndex(t.frameLocal);
            //                il.ALOAD(frameIndex, IObject, n.position());
            //            }
            //            else {
            //                final String frameField = t.getOuterFrameField(ti);
            //                final String frameClass = ti.frameClass;
            //                final int index = t.getThisIndex();
            //                il.ALOAD(index, IObject, n.position());
            //                il.GETFIELD(ThornObjType.mk(t.currentClass), frameField, ClassType.mk(frameClass), n.position());
            //            }
            //
            //            il.GETFIELD(ClassType.mk(ti.frameClass), ti.boxedField(n.localDef()), type, n.position());
        }
        else {
            assert ti == t : "variable " + n.localInstance().def() + " is not boxed but is not declared in this scope";

            // Just do a load.
            final int index = t.getLocalIndex(n.localInstance().def());
            il.load(index, type, n.position());
        }
    }

    public void visit(final Cast n) {
        visitChild(n.expr());
        coerce(typeof(n.expr().type()), typeof(n.castType().type()), n.position());
    }

    public void visit(final Instanceof n) {
        visitChild(n.expr());
        il.INSTANCEOF(typeof(n.compareType()), n.position());
    }

    public void visit(final IntLit n) {
        if (n.kind() == IntLit.INT) {
            il.LDC((int) n.value(), n.position());
        }
        else {
            il.LDC(n.value(), n.position());
        }
    }

    public void visit(final BooleanLit n) {
        il.LDC(n.value() ? 1 : 0, n.position());
        il.uncheckedCoerce(Type.INT, Type.BOOLEAN);
    }

    public void visit(final CharLit n) {
        il.LDC((int) (char) (Character) n.value(), n.position());
        il.uncheckedCoerce(Type.INT, Type.CHAR);
    }

    public void visit(final FloatLit n) {
        if (n.kind() == FloatLit.FLOAT) {
            il.LDC((float) n.value(), n.position());
        }
        else {
            il.LDC((double) n.value(), n.position());
        }
    }

    public void visit(final StringLit n) {
        il.LDC(n.value(), n.position());
    }

    public void visit(final Field n) {
        if (n.target() instanceof TypeNode) {
            il.GETSTATIC(typeof(n.fieldInstance().container()), n.name().id().toString(), typeof(n.fieldInstance().type()), n.position());
        }
        else {
            visitChild((Expr) n.target());
            if (n.name().id() == Name.make("length") && typeof(n.fieldInstance().container()).isArray())
                il.ARRAYLENGTH(n.position());
            else
                il.GETFIELD(typeof(n.fieldInstance().container()), n.name().id().toString(), typeof(n.fieldInstance().type()), n.position());
        }
    }

    public void visit(Special n) {
        if (n.qualifier() == null)
            il.ALOAD(0, typeof(n), n.position());
        else
            assert false;
    }

    public void visit(final Call n) {
        if (n.target() instanceof Expr) {
            visitChild(n.target());
        }

        MethodInstance mi = n.methodInstance();
        
        pushArguments(mi.formalTypes(), n.arguments());

        if (n.target() instanceof Special && ((Special) n.target()).kind() == Special.SUPER) {
            il.INVOKESPECIAL(typeof(mi.container()), mi.name().toString(), typeofTypes(mi.formalTypes()), typeof(mi.returnType()), n.position());
        }
        else if (n.target() instanceof TypeNode) {
            il.INVOKESTATIC(typeof(mi.container()), mi.name().toString(), typeofTypes(mi.formalTypes()), typeof(mi.returnType()), n.position());
        }
        else if (mi.container().isClass() && mi.container().toClass().flags().isInterface()) {
            il.INVOKEINTERFACE(typeof(mi.container()), mi.name().toString(), typeofTypes(mi.formalTypes()), typeof(mi.returnType()), n.position());
        }
        else {
            il.INVOKEVIRTUAL(typeof(mi.container()), mi.name().toString(), typeofTypes(mi.formalTypes()), typeof(mi.returnType()), n.position());
        }
    }

    abstract class Op implements Optimization {
        Binary.Operator op;
        Type leftType;
        Type rightType;
        Type resultType;
        boolean promote;

        Op(final Binary.Operator op, Type leftType, Type rightType, final Type resultType, boolean promote) {
            this.op = op;
            this.leftType = leftType;
            this.rightType = rightType;
            this.resultType = resultType;
            this.promote = promote;
        }

        abstract void instruction();

        public boolean apply(final Expr e) {
            if (!(e instanceof Binary))
                return false;

            final Binary n = (Binary) e;

            final Position pos = n.position();
            final Binary.Operator name = n.operator();

            if (name != op)
                return false;

            if (isD(resultType))
                if (!n.left().type().isNumeric() || !n.right().type().isNumeric())
                    return false;

            Type lt = typeof(n.left());
            Type rt = typeof(n.right());

            if (promote) {
                if (resultType.isInt() && (! isI(lt) || ! isI(rt)))
                    return false;
                if (resultType.isLong() && (! isJ(lt) || ! isJ(rt)))
                    return false;
                if (resultType.isFloat() && (! isF(lt) || ! isF(rt)))
                    return false;
                if (resultType.isDouble() && (! isD(lt) || ! isD(rt)))
                    return false;
            }
            else {
                if (leftType.isRef()) {
                    if (!lt.isRef())
                        return false;
                }
                else {
                    if (!lt.equals(leftType))
                        return false;
                }
                if (rightType.isRef()) {
                    if (!rt.isRef())
                        return false;
                }
                else {
                    if (!rt.equals(rightType))
                        return false;
                }
            }

            visitChild(n.left(), lt);
            if (promote)
                coerce(lt, leftType, n.position());
            if (il.isUnreachable())
                return true;
            visitChild(n.right(), rt);
            if (promote)
                coerce(rt, rightType, n.position());
            instruction();
            return true;
        }
    }

    class StringAdd implements Optimization {
        public boolean apply(final Expr e) {
            if (! (e instanceof Binary))
                return false;

            Binary n = (Binary) e;

            if (n.operator() != Binary.ADD)
                return false;

            TypeSystem ts = e.type().typeSystem();
            if (! ts.typeEquals(n.type(), ts.String(), ts.emptyContext()))
                return false;

            stringify(n.left());
            stringify(n.right());
            il.INVOKEVIRTUAL(Type.STRING, "concat", new Type[] { Type.STRING }, Type.STRING, n.position());
            return true;
        }
    }

    class BoolOp implements Optimization {
        Binary.Operator op;

        BoolOp(final Binary.Operator op) {
            this.op = op;
        }        

        public boolean apply(final Expr e) {
            if (! (e instanceof Binary))
                return false;

            Binary n = (Binary) e;

            if (n.operator() != op)
                return false;

            ILabel F, J;

            F = il.makeLabel(n.position());
            J = il.makeLabel(n.position());

            StackType st = il.currentStack();

            visitBranch(n, F, false);

            // If not unreachable, generate the body.
            if (!il.isUnreachable()) {
                True(n.position());
                il.GOTO(J, n.position());
            }

            // false target
            il.addLabel(F);
            il.setStack(st);

            False(n.position());

            // boolean on stack
            il.addLabel(J);

            return true;
        }
    }

    public void visit(final Binary n) {
        Optimization[] opts = new Optimization[] {
                                                  new BoolOp(Binary.EQ),
                                                  new BoolOp(Binary.LT),
                                                  new BoolOp(Binary.LE),
                                                  new BoolOp(Binary.GT),
                                                  new BoolOp(Binary.GE),
                                                  new BoolOp(Binary.NE),
                                                  new BoolOp(Binary.COND_OR),
                                                  new BoolOp(Binary.COND_AND),
                                                  new Op(Binary.ADD, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IADD(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SUB, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.ISUB(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MUL, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IMUL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.DIV, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IDIV(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MOD, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IREM(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_AND, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IAND(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_OR, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IOR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_XOR, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IXOR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SHL, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.ISHL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SHR, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.ISHR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.USHR, Type.INT, Type.INT, Type.INT, true) {
                                                      void instruction() {
                                                          il.IUSHR(n.position());
                                                      }
                                                  },

                                                  new Op(Binary.ADD, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LADD(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SUB, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LSUB(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MUL, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LMUL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.DIV, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LDIV(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MOD, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LREM(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_AND, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LAND(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_OR, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LOR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.BIT_XOR, Type.LONG, Type.LONG, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LXOR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SHL, Type.LONG, Type.INT, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LSHL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SHR, Type.LONG, Type.INT, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LSHR(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.USHR, Type.LONG, Type.INT, Type.LONG, true) {
                                                      void instruction() {
                                                          il.LUSHR(n.position());
                                                      }
                                                  },

                                                  new Op(Binary.ADD, Type.FLOAT, Type.FLOAT, Type.FLOAT, true) {
                                                      void instruction() {
                                                          il.FADD(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SUB, Type.FLOAT, Type.FLOAT, Type.FLOAT, true) {
                                                      void instruction() {
                                                          il.FSUB(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MUL, Type.FLOAT, Type.FLOAT, Type.FLOAT, true) {
                                                      void instruction() {
                                                          il.FMUL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.DIV, Type.FLOAT, Type.FLOAT, Type.FLOAT, true) {
                                                      void instruction() {
                                                          il.FDIV(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MOD, Type.FLOAT, Type.FLOAT, Type.FLOAT, true) {
                                                      void instruction() {
                                                          il.FREM(n.position());
                                                      }
                                                  },

                                                  new Op(Binary.ADD, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE, true) {
                                                      void instruction() {
                                                          il.DADD(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.SUB, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE, true) {
                                                      void instruction() {
                                                          il.DSUB(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MUL, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE, true) {
                                                      void instruction() {
                                                          il.DMUL(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.DIV, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE, true) {
                                                      void instruction() {
                                                          il.DDIV(n.position());
                                                      }
                                                  },
                                                  new Op(Binary.MOD, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE, true) {
                                                      void instruction() {
                                                          il.DREM(n.position());
                                                      }
                                                  },

                                                  new Op(Binary.BIT_AND, Type.BOOLEAN, Type.BOOLEAN, Type.BOOLEAN, true) {
                                                      void instruction() {
                                                          il.setStack(il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                                                          il.IAND(n.position());
                                                          il.setStack(     il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
                                                          il.uncheckedCoerce(Type.BOOLEAN);
                                                      }
                                                  },
                                                  new Op(Binary.BIT_OR, Type.BOOLEAN, Type.BOOLEAN, Type.BOOLEAN, true) {
                                                      void instruction() {
                                                          il.setStack(       il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                                                          il.IOR(n.position());
                                                          il.setStack(     il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
                                                          il.uncheckedCoerce(Type.BOOLEAN);
                                                      }
                                                  },
                                                  new Op(Binary.BIT_XOR, Type.BOOLEAN, Type.BOOLEAN, Type.BOOLEAN, true) {
                                                      void instruction() {
                                                          il.setStack(   il.currentStack().pop(Type.BOOLEAN).pop(Type.BOOLEAN).push(Type.INT).push(Type.INT));
                                                          il.IXOR(n.position());
                                                          il.setStack(   il.currentStack().pop(Type.INT).push(Type.BOOLEAN));
                                                          il.uncheckedCoerce(Type.BOOLEAN);
                                                      }
                                                  },

                                                  new StringAdd()
        };

        for (Optimization opt : opts) {
            boolean result = opt.apply(n);
            if (result)
                return;
        }

        System.out.println("no op for " + n);

        assert false : "no op for " + n;

    }

    public void visit(Unary n) {
        if (n.operator() == Unary.POS) {
            visitChild(n.expr());
            if (n.type().isInt()) {
                il.uncheckedCoerce(Type.INT);
            }
        }
        if (n.operator() == Unary.NEG) {
            visitChild(n.expr());
            if (il.isUnreachable())
                return;
            if (n.type().isInt()) {
                il.uncheckedCoerce(Type.INT);
                il.INEG(n.position());
            }
            if (n.type().isLong()) {
                il.LNEG(n.position());
            }
            if (n.type().isFloat()) {
                il.FNEG(n.position());
            }
            if (n.type().isDouble()) {
                il.DNEG(n.position());
            }
        }
        if (n.operator() == Unary.POST_DEC || n.operator() == Unary.POST_INC || n.operator() == Unary.PRE_DEC || n.operator() == Unary.PRE_INC) {
            ppmm(n.operator(), n.expr());
        }
        if (n.operator() == Unary.NOT) {
            visitChild(n.expr());
            if (il.isUnreachable())
                return;
            il.uncheckedCoerce(Type.INT);
            il.LDC(1, n.position());
            il.IXOR(n.position());
            il.uncheckedCoerce(Type.BOOLEAN);
        }
        if (n.operator() == Unary.BIT_NOT) {
            promote(n.expr(), n.type());
            if (il.isUnreachable())
                return;
            if (n.type().isIntOrLess()) {
                il.uncheckedCoerce(Type.INT);
                il.LDC(-1, n.position());
                il.IXOR(n.position());
            }
            else if (n.type().isLong()) {
                il.LDC(-1L, n.position());
                il.LXOR(n.position());
            }
        }
    }

    private void ppmm(Unary.Operator operator, Expr e) {
        Position pos = e.position();

        if (e instanceof Local) {
            Local local = (Local) e;
            LocalDef def = local.localInstance().def();
            int index = context.getLocalIndex(def);
            if (local.type().isInt()) {
                if (operator == Unary.POST_INC) {
                    il.ILOAD(index, pos);
                    il.IINC(index, 1, pos);
                }
                if (operator == Unary.POST_DEC) {
                    il.ILOAD(index, pos);
                    il.IINC(index, -1, pos);
                }
                if (operator == Unary.PRE_INC) {
                    il.IINC(index, 1, pos);
                    il.ILOAD(index, pos);
                }
                if (operator == Unary.PRE_DEC) {
                    il.IINC(index, -1, pos);
                    il.ILOAD(index, pos);
                }
            }
            else {
                Type t = typeof(local.type());

                il.load(index, t, pos);
                if (operator == Unary.PRE_INC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.ADD, t, pos);
                    il.dup(pos);
                }
                if (operator == Unary.PRE_DEC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                    il.dup(pos);
                }
                if (operator == Unary.POST_INC) {
                    il.dup(pos);
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.ADD, t, pos);
                }
                if (operator == Unary.POST_DEC) {
                    il.dup(pos);
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                }
                il.store(index, t, pos);
            }
        }
        else if (e instanceof Field) {
            Field local = (Field) e;
            FieldInstance fi = local.fieldInstance();
            Type t = typeof(local.type());

            if (local.target() instanceof TypeNode) {
                il.GETSTATIC(Type.typeFromPolyglotType(fi.container()), fi.name().toString(), Type.typeFromPolyglotType(fi.type()), pos);
                if (operator == Unary.PRE_INC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.ADD, t, pos);
                    il.dup(pos);
                }
                if (operator == Unary.PRE_DEC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                    il.dup(pos);
                }
                if (operator == Unary.POST_INC) {
                    il.dup(pos);
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.ADD, t, pos);
                }
                if (operator == Unary.POST_DEC) {
                    il.dup(pos);
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                }
                il.PUTSTATIC(Type.typeFromPolyglotType(fi.container()), fi.name().toString(), Type.typeFromPolyglotType(fi.type()), pos);
            }
            else {
                // S
                visitChild((Expr) local.target());
                // S e
                il.DUP(pos);
                // S e e
                il.GETFIELD(Type.typeFromPolyglotType(fi.container()), fi.name().toString(), Type.typeFromPolyglotType(fi.type()), pos);
                // S e v
                if (operator == Unary.PRE_INC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    // S e v 1
                    binaryOp(Binary.ADD, t, pos);
                    // S e w
                    if (t.isWide())
                        il.DUP2_X1(pos);
                    else
                        il.DUP_X1(pos);
                    // S w e w
                }
                if (operator == Unary.PRE_DEC) {
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                    if (t.isWide())
                        il.DUP2_X1(pos);
                    else
                        il.DUP_X1(pos);
                }
                if (operator == Unary.POST_INC) {
                    if (t.isWide())
                        il.DUP2_X1(pos);
                    else
                        il.DUP_X1(pos);
                    // S v e v
                    il.LDC(1, pos);
                    // S v e v 1
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.ADD, t, pos);
                    // S v e w
                }
                if (operator == Unary.POST_DEC) {
                    if (t.isWide())
                        il.DUP2_X1(pos);
                    else
                        il.DUP_X1(pos);
                    il.LDC(1, pos);
                    coerce(Type.INT, t, pos);
                    binaryOp(Binary.SUB, t, pos);
                }
                il.PUTFIELD(Type.typeFromPolyglotType(fi.container()), fi.name().toString(), Type.typeFromPolyglotType(fi.type()), pos);
            }
        }
        else if (e instanceof ArrayAccess) {
        }
    }

    public void stringify(final Expr n) {
        visitChild(n);
        try {
            TypeSystem ts = n.type().typeSystem();
            MethodInstance mi = ts.findMethod(ts.String(), ts.MethodMatcher(ts.String(), Name.make("valueOf"), Collections.singletonList(n.type()), ts.emptyContext()));
            coerce(typeof(n.type()), typeof(mi.formalTypes().get(0)), n.position());
            il.INVOKESTATIC(typeof(mi.container()), mi.name().toString(), typeofTypes(mi.formalTypes()), typeof(mi.returnType()), n.position());
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }


    public void visit(final NullLit n) {
        il.ACONST_NULL(n.position());
    }

    public void visit(final Conditional n) {
        ILabel F, J;

        F = il.makeLabel(n.position());
        J = il.makeLabel(n.position());

        final StackType stack = il.currentStack();

        visitBranch(n.cond(), F, false);

        // If the fall thru is not unreachable,
        // generate the then case.
        if (!il.isUnreachable()) {
            visitChild(n.consequent());
            coerce(il.currentStack().top(), typeof(n.type()), n.consequent().position());
            final StackType thenStack = il.currentStack();

            if (!il.isUnreachable()) {
                il.GOTO(J, n.position());
            }

            il.setStack(stack);

            il.addLabel(F);

            visitChild(n.alternative());

            if (il.isUnreachable()) {
                il.setStack(thenStack);
            }
            else {
                coerce(il.currentStack().top(), typeof(n.type()), n.consequent().position());
                il.setStack(il.currentStack().merge(thenStack));
            }

            if (!(thenStack instanceof Unreachable)) {
                il.addLabel(J);
            }
        }
        else {
            il.setStack(stack);

            il.addLabel(F);

            visitChild(n.alternative());

            if (il.isUnreachable())
                return;

            il.uncheckedCoerce(typeof(n.type()));
        }
    }

    public void visit(ClassLit n) {
        polyglot.types.Type t = n.typeNode().type();
        Type klass = Type.typeFromClassName("java.lang.Class");
        Position pos = n.position();
        if (t.isBoolean())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Boolean"), "TYPE", klass, pos);
        else if (t.isByte())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Byte"), "TYPE", klass, pos);
        else if (t.isShort())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Short"), "TYPE", klass, pos);
        else if (t.isChar())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Character"), "TYPE", klass, pos);
        else if (t.isInt())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Integer"), "TYPE", klass, pos);
        else if (t.isLong())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Long"), "TYPE", klass, pos);
        else if (t.isFloat())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Float"), "TYPE", klass, pos);
        else if (t.isDouble())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Double"), "TYPE", klass, pos);
        else if (t.isVoid())
            il.GETSTATIC(Type.typeFromClassName("java.lang.Void"), "TYPE", klass, pos);
        else
            il.LDC(typeof(t), pos);
    }

    public void visit(final NewArray n) {
        for (Expr e : n.dims()) {
            visitChild(e);
        }

        if (n.init() != null) {
            il.LDC(n.init().elements().size(), n.position());
        }

        polyglot.types.Type baseType = n.baseType().type();
        Type base = typeof(baseType);

        if (n.numDims() == 1) {
            if (baseType.isReference())
                il.ANEWARRAY(base, n.position());
            else
                il.NEWARRAY(base, n.position());
        }
        else {
            il.MULTIANEWARRAY(Type.array(base, n.additionalDims()), n.dims().size(), n.position());
        }

        if (n.init() != null) {
            initArray((ArrayInit) n.init().type(n.type()), baseType);
        }
    }

    public void visit(final ArrayInit n) {
        il.LDC(n.elements().size(), n.position());
        polyglot.types.Type baseType = n.type().toArray().base();
        if (baseType.isReference())
            il.ANEWARRAY(typeof(baseType), n.position());
        else
            il.NEWARRAY(typeof(baseType), n.position());
        initArray(n, baseType);
    }

    /** Initialize an array.  Requires that the array be on the stack. */
    private void initArray(final ArrayInit n, polyglot.types.Type baseType) {
        // stack = A
        int i = 0;
        for (final Expr e : n.elements()) {
            il.DUP(e.position());
            // stack = A A
            il.LDC(i, e.position());
            // stack = A A i
            i++;
            if (e instanceof ArrayInit)
                visitChild(e.type(baseType));
            else
                visitChild(e);
            // stack = A A i e
            coerce(il.currentStack().top(), typeof(baseType), e.position());
            arrayStore(baseType, e.position());
            if (il.isUnreachable())
                return;
            // stack = A
        }
        // stack = A
    }

}