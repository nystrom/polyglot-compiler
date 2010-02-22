package ibex.ast;

import ibex.types.ActionDef;
import ibex.types.ActionDef_c;
import ibex.types.TupleType_c;
import ibex.visit.Rewriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.IntLit;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Term;
import polyglot.frontend.Globals;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.UnknownType;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.AlphaRenamer;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

public class RhsAction_c extends RhsExpr_c implements RhsAction {
    Formal formal;
    RhsExpr item;
    Block body;
    
    ActionDef ad;

    public RhsAction_c(Position pos, RhsExpr item, Block body) {
        super(pos);
        this.item = item;
        this.body = body;
    }
    
    public ActionDef actionDef() {
        return ad;
    }

    public RhsAction actionDef(ActionDef ad) {
        RhsAction_c n = (RhsAction_c) copy();
        n.ad = ad;
        return n;
    }

    public Formal formal() {
        return this.formal;
    }
    
    public RhsAction formal(Formal formal) {
        RhsAction_c n = (RhsAction_c) copy();
        n.formal = formal;
        return n;
    }

    public RhsExpr item() {
        return this.item;
    }
    
    public RhsAction item(RhsExpr item) {
        RhsAction_c n = (RhsAction_c) copy();
        n.item = item;
        return n;
    }

    public Block body() {
        return this.body;
    }
    
    public RhsAction body(Block stmt) {
        RhsAction_c n = (RhsAction_c) copy();
        n.body = stmt;
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        RhsExpr item = (RhsExpr) visitChild(this.item, v);
        Formal formal = (Formal) visitChild(this.formal, v);
        Block body = (Block) visitChild(this.body, v);
        return item(item).formal(formal).body(body);
    }
    
    @Override
    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    @Override
    public Term firstChild() {
        return item;
    }

    @Override
    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        if (formal != null) {
            v.visitCFG(item, formal, ENTRY);
            v.visitCFG(formal, body, ENTRY);
        }
        else {
            v.visitCFG(item, body, ENTRY);
        }
        v.visitCFG(body, this, EXIT);
        v.visitCFG(item, this, EXIT); // hack to make this reachable
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(item, w, tr);
        w.write(" ");
        if (formal != null) {
            w.write("(");
            print(formal, w, tr);
            w.write(") => ");
        }
        print(body, w, tr);
    }

    @Override
    public String toString() {
        return item.toString() + (formal != null ? " (" + formal + ") => " : " ") + body.toString();
    }
    
    @Override
    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
        RhsExpr item = (RhsExpr) this.visitChild(this.item, tb);
        NodeFactory nf = tb.nodeFactory();
        
        TypeSystem ts = tb.typeSystem();
        final Ref<Type> type = Types.<Type>ref(ts.unknownType(position()));
        ActionDef mi = new ActionDef_c(ts, position(), type, 
                                      Collections.<Ref<? extends Type>>emptyList());

        // Introduce a formal for the semantic action.
        LocalDef adef = ts.localDef(item.position(), Flags.FINAL, Types.ref(ts.Object().arrayOf()), Name.makeFresh());
        adef.setNotConstant();
        Formal formal = nf.Formal(adef.position(), nf.FlagsNode(adef.position(), adef.flags()), nf.CanonicalTypeNode(adef.position(), adef.type()), nf.Id(adef.position(), adef.name()));
        formal = formal.localDef(adef);
        
        TypeBuilder tb2 = tb.pushCode(mi);
        Block body = (Block) visitChild(this.body, tb2);
        
        return actionDef(mi).formal(formal).item(item).body(body);
    }

    @Override
    public Node typeCheckOverride(Node parent, ContextVisitor tc) throws SemanticException {
        Context c = tc.context();
        c = c.pushBlock();
        ContextVisitor tc1 = tc.context(c);
        
        RhsExpr item = (RhsExpr) this.visitChild(this.item, tc1);
        Block body = this.body;
        
        // count occurrences of bound names
        final Map<Name,Integer> count = new HashMap<Name, Integer>();
        this.visitChild(item, new NodeVisitor() {
            @Override
            public Node override(Node n) {
                if (n instanceof RhsBind) {
                    RhsBind b = (RhsBind) n;
                    Name name = b.decl().name().id();
                    Integer k = count.get(name);
                    if (k == null)
                        count.put(name, 1);
                    else
                        count.put(name, k + 1);
                    return n;
                }
                if (n instanceof RhsMinus) {
                    RhsExpr e = (RhsExpr) n.visitChild(((RhsMinus) n).left(), this);
                    return ((RhsMinus) n).left(e);
                }
                if (n instanceof RhsLookahead) {
                    return n;
                }
                if (n instanceof RhsAction) {
                    return n;
                }
                return null;
            }
        });
        
        // rename synthetic binds used more than once by appending a number
        final Map<Name,Integer> curr = new HashMap<Name, Integer>();
        item = (RhsExpr) this.visitChild(item, new NodeVisitor() {
            @Override
            public Node override(Node n) {
                if (n instanceof RhsBind) {
                    RhsBind b = (RhsBind) n;
                    if (b.synthetic()) {
                        Name name = b.decl().name().id();
                        Integer k = count.get(name);
                        assert k != null;
                        if (k == 1) {
                            // only one occurrence, don't rename
                        }
                        else if (k > 1) {
                            Integer m = curr.get(name);
                            int h = m == null ? 1 : m+1;
                            curr.put(name, h);
                            Name newName = Name.make(name.toString() + h);
                            b.decl().localDef().setName(newName);
                            return b.decl(b.decl().name(b.decl().name().id(newName)));
                        }
                    }
                    return n;
                }
                if (n instanceof RhsMinus) {
                    RhsExpr e = (RhsExpr) n.visitChild(((RhsMinus) n).left(), this);
                    return ((RhsMinus) n).left(e);
                }
                if (n instanceof RhsLookahead) {
                    return n;
                }
                if (n instanceof RhsAction) {
                    return n;
                }
                return null;
            }
        });
        
        // Add the new vars to the context.
        Context c2 = tc.context();
        c2 = c2.pushBlock();
        ContextVisitor tc2 = tc.context(c2);
        
//        item.visit(new ContextVisitor(tc.job(), tc.typeSystem(), tc.nodeFactory()).context(c2));
        
        TypeSystem ts = tc.typeSystem();
        NodeFactory nf = tc.nodeFactory();
        
        // Introduce a formal for the semantic action.
        LocalDef adef = formal.localDef();
        c2.addVariable(adef.asInstance());
        
        // First rename all the locals in item to fresh names, saving a map from old names to new.
        AlphaRenamer ar = new AlphaRenamer();
        item = (RhsExpr) item.visit(ar);
        Map<LocalDef, Name> renaming = ar.getMap();
        
        for (Map.Entry<LocalDef, Name> e : renaming.entrySet()) {
            RhsExpr r = item;
            Name oldName = e.getValue();
            LocalDef newDef = e.getKey();
            
            Expr l = (Local) nf.Local(r.position(), formal.name()).localInstance(adef.asInstance()).type(adef.asInstance().type());

            if (! item.type().isArray()) {
                l = nf.ArrayAccess(r.position(), l, nf.IntLit(r.position(), IntLit.INT, 0).type(ts.Int())).type(ts.Object());
                l = Rewriter.unbox(item.type(), l, nf);
            }
            
            Expr init = deconstruct(item, l, newDef.name());

            if (init != null) {
                LocalDef def = ts.localDef(init.position(), Flags.FINAL, Types.ref(init.type()), oldName);
                def.setNotConstant();
                LocalDecl ld = nf.LocalDecl(def.position(), nf.FlagsNode(def.position(), def.flags()), nf.CanonicalTypeNode(def.position(), def.type()), nf.Id(def.position(), def.name()), init);
                ld = ld.localDef(def);
                body = body.prepend(ld);
            }
        }
        
        // Next create formals for the item.
        // These are the formals of the action method.
        
        // Next, introduce the old variables as LocalDecls in the body.
        // Initialize these from the formals, deconstructing as follows.
        
        // case invoke: -- old.type neu = old
        // case option: -- old.type neu = old
        // case star:   -- old = new item.type[]; for (x in neu) old[i++] = x;  
        
        tc2 = tc2.context(c2.pushCode(ad));
        Formal formal = (Formal) this.visitChild(this.formal, tc2);
        body = (Block) this.visitChild(body, tc2);

        RhsAction_c n = (RhsAction_c) this.item(item).body(body);
        n = (RhsAction_c) n.formal(formal);
        try {
            return tc.leave(parent, this, n, tc1);
        }
        catch (InternalCompilerError e) {
            if (e.position() == null && n != null)
                e.setPosition(n.position());
            throw e;
        }
    }
    
    private Expr deconstruct(RhsExpr item, Expr l, Name oldName) throws SemanticException {
        NodeFactory nf = Globals.NF();
        TypeSystem ts = Globals.TS();
        
        if (item instanceof RhsInvoke) {
            RhsInvoke r = (RhsInvoke) item;
            if (r.call().name().id() == oldName)
                return l;
        }
        if (item instanceof RhsBind) {
            RhsBind r = (RhsBind) item;
            if (r.decl().localDef().name() == oldName)
                return l;
        }
        if (item instanceof RhsMinus) {
            RhsMinus r = (RhsMinus) item;
            Expr l1 = Rewriter.unbox(r.left().type(), l, nf);
            Expr e1 = deconstruct(r.left(), l1, oldName);
            if (e1 != null)
                return e1;
        }
        if (item instanceof RhsAnd) {
            RhsAnd r = (RhsAnd) item;
            
            if (r.type().isVoid())
                return null;
            
            assert r.type() instanceof TupleType_c;
            
            {
                int i = 1;
                RhsExpr ri = r.left();
                FieldInstance f = ts.findField(r.type(), ts.FieldMatcher(r.type(), Name.make("f" + i), ts.emptyContext()));
                Expr ai = nf.Field(ri.position(), l, nf.Id(ri.position(), f.name())).fieldInstance(f).type(f.type());
                if (! ai.type().isReference())
                    ai = Rewriter.box(ai.type(), ai, nf); // make it nullable!
                ai = nf.Conditional(ri.position(), nf.Binary(ri.position(), l, Binary.NE, nf.NullLit(ri.position()).type(ts.Null())).type(ts.Boolean()), ai, nf.NullLit(ri.position()).type(ts.Null())).type(ai.type());
                ai = Rewriter.unbox(ri.type(), ai, nf);
                Expr ei = deconstruct(ri, ai, oldName);
                if (ei != null)
                    return ei;
            }
            {
                int i = 2;
                RhsExpr ri = r.right();
                FieldInstance f = ts.findField(r.type(), ts.FieldMatcher(r.type(), Name.make("f" + i), ts.emptyContext()));
                Expr ai = nf.Field(ri.position(), l, nf.Id(ri.position(), f.name())).fieldInstance(f).type(f.type());
                if (! ai.type().isReference())
                    ai = Rewriter.box(ai.type(), ai, nf); // make it nullable!
                ai = nf.Conditional(ri.position(), nf.Binary(ri.position(), l, Binary.NE, nf.NullLit(ri.position()).type(ts.Null())).type(ts.Boolean()), ai, nf.NullLit(ri.position()).type(ts.Null())).type(ai.type());
                ai = Rewriter.unbox(ri.type(), ai, nf);
                Expr ei = deconstruct(ri, ai, oldName);
                if (ei != null)
                    return ei;
            }
//            Expr l1 = Rewriter.unbox(r.left().type(), l, nf);
//            Expr l2 = Rewriter.unbox(r.right().type(), l, nf);
//            Expr e1 = deconstruct(r.left(), l1, oldName);
//            Expr e2 = deconstruct(r.right(), l2, oldName);
//            if (e1 != null)
//                return e1;
//            if (e2 != null)
//                return e2;
        }
        if (item instanceof RhsIteration) {
            RhsIteration r = (RhsIteration) item;
            assert r instanceof RhsOption || l.type().isArray() : l.type();
            Expr e = deconstruct(r.item(), l, oldName);
            if (e != null)
                return e;
        }
        if (item instanceof RhsIterationList) {
            RhsIterationList r = (RhsIterationList) item;
            RhsExpr ri = r.item();
            RhsExpr qi = r.item();
            assert l.type().isArray() : l.type();
            Expr zero = nf.IntLit(r.position(), IntLit.INT, 0).type(ts.Int());
            Expr ei = nf.ArrayAccess(r.position(), l, zero).type(l.type().toArray().base());
            ei = Rewriter.unbox(ri.type(), ei, nf);
            Expr e = deconstruct(r.item(), ei, oldName);
            Expr one = nf.IntLit(r.position(), IntLit.INT, 1).type(ts.Int());
            Expr si = nf.ArrayAccess(r.position(), l, one).type(l.type().toArray().base());
            si = Rewriter.unbox(qi.type(), si, nf);
            Expr s = deconstruct(r.sep(), si, oldName);
            if (e != null)
                return e;
            if (s != null)
                return s;
        }
        if (item instanceof RhsSequence) {
            RhsSequence r = (RhsSequence) item;
            int i = 0;
            for (RhsExpr ri : r.items()) {
                if (ri instanceof RhsLookahead)
                    continue;
                if (ri.type().isVoid() || ri.type().isNull())
                    continue;
                i++;
                FieldInstance f = ts.findField(r.type(), ts.FieldMatcher(r.type(), Name.make("f" + i), ts.emptyContext()));
                Expr ai = nf.Field(ri.position(), l, nf.Id(ri.position(), f.name())).fieldInstance(f).type(f.type());
                if (! ai.type().isReference())
                    ai = Rewriter.box(ai.type(), ai, nf); // make it nullable!
                ai = nf.Conditional(ri.position(), nf.Binary(ri.position(), l, Binary.NE, nf.NullLit(ri.position()).type(ts.Null())).type(ts.Boolean()), ai, nf.NullLit(ri.position()).type(ts.Null())).type(ai.type());
                ai = Rewriter.unbox(ri.type(), ai, nf);
                Expr ei = deconstruct(ri, ai, oldName);
                if (ei != null)
                    return ei;
            }
        }
        
        return null;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        Type t = ad.returnType().get();
        if (t instanceof UnknownType)
            throw new SemanticException("Could not compute type of action; action does not return.", position());
//        final List<Type> types = new ArrayList<Type>();
//        body.visit(new NodeVisitor() {
//            @Override
//            public Node leave(Node old, Node n, NodeVisitor v) {
//                if (n instanceof Return) {
//                    Return r = (Return) n;
//                    if (r.expr() != null) {
//                        types.add(r.expr().type());
//                    }
//                }
//                return super.leave(old, n, v);
//            }
//        });
//        if (types.isEmpty())
//            return rhs(item.rhs()).type(ts.Null());
//        Type t = types.get(0);
//        for (Type tt : types) {
//            t = ts.leastCommonAncestor(t, tt, tc.context());
//        }
        return type(t);
    }
}
