package polyglot.bytecode;

import java.util.Collections;

import polyglot.ast.Assert;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Case;
import polyglot.ast.Catch;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Do;
import polyglot.ast.Empty;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.For;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.If;
import polyglot.ast.Labeled;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.Switch;
import polyglot.ast.Synchronized;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.ast.While;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;

public class StmtTranslator extends AbstractExpTranslator {

    public StmtTranslator(final Job job, TypeSystem ts, NodeFactory nf, BytecodeTranslator bc, MethodContext context) {
        super(job, ts, nf, bc, context);
    }
    
    public void visitChild(Stmt s) {
        visitChild(s, this);
    }
    public void visitChild(Expr s) {
        visitExpr(s);
    }
    public void visit(Stmt n) {
        assert false;
    }
    public void visit(Block n) {
        for (Stmt s : n.statements()) {
            visitChild(s);
            if (il.isUnreachable())
                break;
        }
    }

    public void visit(Assert n) throws SemanticException {
        ILabel L = il.makeLabel(n.position());
        visitBranch(n.cond(), L, true);
        
        StackType st = il.currentStack();
        
        if (il.isReachable()) {
            ClassType a = (ClassType) ts.systemResolver().find(QName.make("java.lang.AssertionError"));
            Expr e;

            if (n.errorMessage() != null) {
                e = n.errorMessage();
            }
            else {
                e = nf.StringLit(n.position(), "Assertion failure: " + n.cond());
            }
            alloc(a, Collections.singletonList(e), n.position());
            il.ATHROW(n.position());
        }
        
        il.setStack(st);
        il.addLabel(L);
    }
    public void visit(Branch n) {
        ILabel target;

        if (n.kind() == Branch.BREAK) {
            target = context.breakTarget(n.labelNode());
            popToStack(context.breakStack(n.labelNode()), n.position());
            
            for (MethodContext c = context; c != null; c = c.outer()) {
                if (il.isUnreachable())
                    break;
                if (c.finallyBlock != null)
                    c.finallyBlock.appendInstructions(il, c);
                if (c.breakTarget(n.labelNode()) == null)
                    break;
            }
        }
        else {
            target = context.continueTarget(n.labelNode());
            popToStack(context.continueStack(n.labelNode()), n.position());

            for (MethodContext c = context; c != null; c = c.outer()) {
                if (il.isUnreachable())
                    break;
                if (c.finallyBlock != null)
                    c.finallyBlock.appendInstructions(il, c);
                if (c.continueTarget(n.labelNode()) == null)
                    break;
            }
        }

        if (il.isUnreachable())
            return;

        il.GOTO(target, n.position());
    }
    public void visit(Empty n) {
        il.NOP(n.position());
    }
    public void visit(Eval n) {
        visitChild(n.expr());
        if (! n.expr().type().isVoid())
            il.pop(n.position());
        assert il.isReachable();
    }

    public void visit(final ConstructorCall n) {
        final int thisIndex = context.getThisIndex();

        // Call superclass constructors.
        il.ALOAD(thisIndex, context.localType(thisIndex), n.position());

        for (final Expr arg : n.arguments()) {
            visitChild(arg);
            if (il.isUnreachable())
                return;
        }

        if (n.kind() == ConstructorCall.THIS) {
            il.INVOKESPECIAL(Type.typeFromPolyglotType(context.currentClass.asType()), "<init>", typeof(n.arguments()), Type.VOID, n.position());
        }
        else {
            il.INVOKESPECIAL(Type.typeFromPolyglotType(Types.get(context.currentClass.superType())), "<init>", typeof(n.arguments()), Type.VOID, n.position());
        }
        
        if (n.kind() == ConstructorCall.SUPER) {
            context.fieldInits.appendInstructions(il, context);
        }
    }

    public void visit(If n) {
        ILabel ELSE;

        ELSE = il.makeLabel(n.position());

        final StackType endStack = il.currentStack();

        visitBranch(n.cond(), ELSE, false);
        
        if (il.isReachable())
            visitChild(n.consequent());

        StackType thenStack = il.currentStack();
        
        if (n.alternative() != null) {
            ILabel END;
            END = il.makeLabel(n.position());
            if (il.isReachable())
                il.GOTO(END, n.position());
            
            il.addLabel(ELSE);
            il.setStack(thenStack.merge(endStack));
            visitChild(n.alternative());
            il.addLabel(END);
            il.setStack(il.currentStack().merge(thenStack));
        }
        else {
            il.addLabel(ELSE);
            il.setStack(thenStack.merge(endStack));
        }
    }
    public void visit(Labeled n) {
        ILabel B = il.makeLabel(n.position());
        ILabel C = il.makeLabel(n.position());
     
        StackType st = il.currentStack();
        
        il.addLabel(C);

        MethodContext v = context.pushBreak(n.labelNode(), B, st);
        MethodContext v2 = v.pushContinue(n.labelNode(), C, st);
        MethodContext v3 = v2.pushLabeled(n);
        visitChild(n.statement(), context(v3));
        
        il.setStack(st);
        il.addLabel(B);
    }
    public void visit(LocalClassDecl n) {
        visitChild(n.decl(), new ClassTranslator(job, ts, nf, bc, n.decl().classDef(), this.context));
    }
    public void visit(LocalDecl n) {
        int index = context.addLocal(n.localDef());
        if (n.init() != null) {
            visitChild(n.init());
            coerce(il, typeof(n.init()), typeof(n.localDef().type()), n.position());
            il.store(index, typeof(n.localDef().type()), n.position());
        }
    }
    public void visit(Do n) {
        ILabel TOP, END;

        TOP = il.makeLabel(n.position());
        END = il.makeLabel(n.position());
        
        // HACK to avoid unreachable code
        il.LDC(1, n.position());
        il.IFEQ(END, n.position());

        final StackType endStack = il.currentStack();

        if (context.continueLabel(n) != null)
            il.addLabel(context.continueTarget(context.continueLabel(n)));

        il.addLabel(TOP);
        visitChild(n.body(), context(context.pushBreak(null, END, endStack).pushContinue(null, TOP, endStack)));

        if (il.isReachable()) {
            visitBranch(n.cond(), TOP, true);
        }

        if (il.isReachable()) {
            il.setStack(il.currentStack().merge(endStack));
        }
        else {
            il.setStack(endStack);
        }

        il.addLabel(END);
    }
    
    public void visit(final For n) {
        ILabel BODY, TOP, END;

        BODY = il.makeLabel(n.position());
        TOP = il.makeLabel(n.position());
        END = il.makeLabel(n.position());
        
        // HACK to avoid unreachable code
        il.LDC(1, n.position());
        il.IFEQ(END, n.position());

        final StackType endStack = il.currentStack();

        for (ForInit i : n.inits()) {
            visitChild(i);
        }
        
        StackType continueStack = il.currentStack();
        
        il.GOTO(BODY, n.position());
        if (context.continueLabel(n) != null)
            il.addLabel(context.continueTarget(context.continueLabel(n)));
        il.setStack(continueStack);
        il.addLabel(TOP);
        for (ForUpdate i : n.iters())
            visitChild(i);
        il.addLabel(BODY);
        visitBranch(n.cond(), END, false);
        visitChild(n.body(), context(context.pushContinue(null, TOP, continueStack).pushBreak(null, END, endStack)));

        if (il.isReachable()) {
                // optimize do .. while (false) -- this is used in match translation
            il.GOTO(TOP, n.position());
            il.setStack(il.currentStack().merge(endStack));
        }

        il.setStack(endStack);
        il.addLabel(END);
    }
    
    public void visit(While n) {
        ILabel TOP, END;

        TOP = il.makeLabel(n.position());
        END = il.makeLabel(n.position());
        
        // HACK to avoid unreachable code
        il.LDC(1, n.position());
        il.IFEQ(END, n.position());

        final StackType endStack = il.currentStack();

        if (context.continueLabel(n) != null)
            il.addLabel(context.continueTarget(context.continueLabel(n)));

        il.addLabel(TOP);
        visitBranch(n.cond(), END, false);
        visitChild(n.body(), context(context.pushBreak(null, END, endStack).pushContinue(null, TOP, endStack)));

        if (il.isReachable()) {
            il.GOTO(TOP, n.position());
            il.setStack(il.currentStack().merge(endStack));
        }

        il.setStack(endStack);
        il.addLabel(END);
    }
    public void visit(Return n) {
        if (n.expr() != null) {
            visitChild(n.expr());
        }

        for (MethodContext c = context; c != null; c = c.outer()) {
            if (il.isUnreachable())
                break;
            if (c.finallyBlock != null)
                c.finallyBlock.appendInstructions(il, c);
        }
        
        if (il.isUnreachable())
            return;
        
        if (n.expr() != null) {
            if (il.currentStack().top().isRef())
                il.ARETURN(n.position());
            else if (il.currentStack().top().isIType()) {
                il.uncheckedCoerce(Type.INT);
                il.IRETURN(n.position());
            }
            else if (il.currentStack().top().isLong())
                il.LRETURN(n.position());
            else if (il.currentStack().top().isFloat())
                il.FRETURN(n.position());
            else if (il.currentStack().top().isDouble())
                il.DRETURN(n.position());
        }
        else {
            il.RETURN(n.position());
        }
    }
    
    public void visit(final Synchronized n) {
        ILabel START = il.makeLabel(n.position());
        ILabel END = il.makeLabel(n.position());
        ILabel HANDLER = il.makeLabel(n.position());
        ILabel POST = il.makeLabel(n.position());
       
        final int index = context.freshLocal(Type.OBJECT);

        visitChild(n.expr());
        il.DUP(n.expr().position());
        il.ASTORE(index, n.position());
        
        il.MONITORENTER(n.position());
        il.addLabel(START);
        
        MethodContext.InstructionSequence post = new MethodContext.InstructionSequence() {
            public void appendInstructions(IOpcodes il, MethodContext context) {
                il.ALOAD(index, Type.OBJECT, n.position());
                il.MONITOREXIT(n.position());
            }
        };

        visitChild(n.body(), context(context.pushFinally(post)));
        il.addLabel(END);
        
        if (il.isReachable()) {
            il.ALOAD(index, Type.OBJECT, n.position());
            il.MONITOREXIT(n.position());
            il.GOTO(POST, n.position());
        }

        // Catch, monitorexit, and rethrow
        il.addLabel(HANDLER);
        il.setStack(polyglot.bytecode.types.Empty.it.push(Type.OBJECT));
        post.appendInstructions(il, context);
        il.ATHROW(n.position());
        il.addLabel(POST);

        il.addExceptionHandler(START, END, HANDLER, null);
    }
    public void visit(Throw n) {
        visitChild(n.expr());
        il.ATHROW(n.position());
    }
    public void visit(Switch n) {
        StackType caseStack = il.currentStack();
        
        visitChild(n.expr());
        assert il.currentStack().top().isIType();
        il.uncheckedCoerce(Type.INT);
        
        int count = 0;
        for (Stmt s : n.elements()) {
            if (s instanceof Case) {
                Case c = (Case) s;
                if (! c.isDefault())
                    count++;
            }
        }
        
        ILabel[] labels = new ILabel[count];
        int[] cases = new int[count];
        ILabel endLabel = il.makeLabel(n.position());
        ILabel defaultLabel = endLabel;
        
        int i = 0;
        for (Stmt s : n.elements()) {
            if (s instanceof Case) {
                Case c = (Case) s;
                if (c.isDefault())
                    defaultLabel = il.makeLabel(c.position());
                else {
                    labels[i] = il.makeLabel(c.position());
                    cases[i] = (Integer) c.expr().constantValue();
                    i++;
                }
            }
        }
        
        int stride = 0;
        int prev = cases[0];
        boolean lookup = false;
        for (int k = 1; k < cases.length && lookup; k++) {
            int newStride = cases[k] - prev;
            if (k == 1)
                stride = newStride;
            else if (newStride != stride)
                lookup = true;
            prev = cases[k];
        }
        
        if (stride == 1)
            il.TABLESWITCH(cases, labels, defaultLabel, n.position());
        else
            il.LOOKUPSWITCH(cases, labels, defaultLabel, n.position());
       
        i = 0;
        for (Stmt s : n.elements()) {
            if (s instanceof Case) {
                Case c = (Case) s;
                if (c.isDefault()) {
                    il.addLabel(defaultLabel);
                }
                else {
                    assert cases[i] == (int) (Integer) c.expr().constantValue();
                    il.addLabel(labels[i]);
                    i++;
                }
                il.setStack(caseStack);
            }
            else {
                if (il.isReachable())
                    visitChild(s, context(context.pushBreak(null, endLabel, caseStack)));
            }
        }

        il.setStack(caseStack);
        il.addLabel(endLabel);
    }
    
    public void visit(final Try n) {
        ILabel START = il.makeLabel(n.position());
        ILabel END = il.makeLabel(n.position());
        ILabel POST = il.makeLabel(n.position());
       
        StackType st = il.currentStack();
        
        il.addLabel(START);
        
        MethodContext.InstructionSequence post = new MethodContext.InstructionSequence() {
            public void appendInstructions(IOpcodes il, MethodContext context) {
                visitChild(n.finallyBlock(), new StmtTranslator(job, ts, nf, bc, context));
            }
        };

        if (n.finallyBlock() != null)
            visitChild(n.tryBlock(), context(context.pushFinally(post)));
        else
            visitChild(n.tryBlock());
        il.addLabel(END);
        
        String s = il.toString();

        if (il.isReachable()) {
            il.GOTO(POST, n.position());
        }
        
        for (Catch c : n.catchBlocks()) {
            int index = context.addLocal(c.formal().localDef());
            
            ILabel HANDLER = il.makeLabel(n.position());

            il.addExceptionHandler(START, END, HANDLER, typeof(c.formal().type()));
            
            il.setStack(polyglot.bytecode.types.Empty.it.push(Type.OBJECT));
            il.addLabel(HANDLER);
            il.ASTORE(index, n.position());
            
            if (n.finallyBlock() != null)
                visitChild(c.body(), context(context.pushFinally(post)));
            else
                visitChild(c.body(), this);
            
            s = il.toString();
            
            if (n.finallyBlock() != null)
                if (il.isReachable())
                    visitChild(n.finallyBlock());

            if (il.isReachable())
                il.GOTO(POST, n.position());
        }

        if (n.finallyBlock() != null) {
            ILabel HANDLER = il.makeLabel(n.position());
            il.addExceptionHandler(START, END, HANDLER, null);

            il.setStack(polyglot.bytecode.types.Empty.it.push(Type.OBJECT));
            il.addLabel(HANDLER);
            
            post.appendInstructions(il, context);
            
            if (il.isReachable())
                il.ATHROW(n.position());
        }

        il.addLabel(POST);
        il.setStack(st);
    }
}
