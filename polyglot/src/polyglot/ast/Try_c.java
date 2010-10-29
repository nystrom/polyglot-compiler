/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * An immutable representation of a <code>try</code> block, one or more
 * <code>catch</code> blocks, and an optional <code>finally</code> block.
 */
public class Try_c extends Stmt_c implements Try
{
    protected Block tryBlock;
    protected List<Catch> catchBlocks;
    protected Block finallyBlock;

    public Try_c(Position pos, Block tryBlock, List<Catch> catchBlocks, Block finallyBlock) {
	super(pos);
	assert(tryBlock != null && catchBlocks != null); // finallyBlock may be null, catchBlocks empty
	assert(! catchBlocks.isEmpty() || finallyBlock != null); // must be either try-catch or try(-catch)-finally
	this.tryBlock = tryBlock;
	this.catchBlocks = TypedList.copyAndCheck(catchBlocks, Catch.class, true);
	this.finallyBlock = finallyBlock;
    }

    /** Get the try block of the statement. */
    public Block tryBlock() {
	return this.tryBlock;
    }

    /** Set the try block of the statement. */
    public Try tryBlock(Block tryBlock) {
	Try_c n = (Try_c) copy();
	n.tryBlock = tryBlock;
	return n;
    }

    /** Get the catch blocks of the statement. */
    public List<Catch> catchBlocks() {
	return Collections.unmodifiableList(this.catchBlocks);
    }

    /** Set the catch blocks of the statement. */
    public Try catchBlocks(List<Catch> catchBlocks) {
	Try_c n = (Try_c) copy();
	n.catchBlocks = TypedList.copyAndCheck(catchBlocks, Catch.class, true);
	return n;
    }

    /** Get the finally block of the statement. */
    public Block finallyBlock() {
	return this.finallyBlock;
    }

    /** Set the finally block of the statement. */
    public Try finallyBlock(Block finallyBlock) {
	Try_c n = (Try_c) copy();
	n.finallyBlock = finallyBlock;
	return n;
    }

    /** Reconstruct the statement. */
    protected Try_c reconstruct(Block tryBlock, List<Catch> catchBlocks, Block finallyBlock) {
	if (tryBlock != this.tryBlock || ! CollectionUtil.allEqual(catchBlocks, this.catchBlocks) || finallyBlock != this.finallyBlock) {
	    Try_c n = (Try_c) copy();
	    n.tryBlock = tryBlock;
	    n.catchBlocks = TypedList.copyAndCheck(catchBlocks, Catch.class, true);
	    n.finallyBlock = finallyBlock;
	    return n;
	}

	return this;
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Block tryBlock = (Block) visitChild(this.tryBlock, v);
	List<Catch> catchBlocks = visitList(this.catchBlocks, v);
	Block finallyBlock = (Block) visitChild(this.finallyBlock, v);
	return reconstruct(tryBlock, catchBlocks, finallyBlock);
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("try ");
        sb.append(tryBlock.toString());

        int count = 0;

	for (Iterator<Catch> it = catchBlocks.iterator(); it.hasNext(); ) {
	    Catch cb = it.next();

            if (count++ > 2) {
              sb.append("...");
              break;
            }

            sb.append(" ");
            sb.append(cb.toString());
        }

        if (finallyBlock != null) {
            sb.append(" finally ");
            sb.append(finallyBlock.toString());
        }

        return sb.toString();
    }

}
