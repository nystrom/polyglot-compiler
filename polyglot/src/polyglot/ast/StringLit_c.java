/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.LinkedList;
import java.util.List;

import polyglot.util.*;

/** 
 * A <code>StringLit</code> represents an immutable instance of a 
 * <code>String</code> which corresponds to a literal string in Java code.
 */
public class StringLit_c extends Lit_c implements StringLit
{
    protected String value;

    public StringLit_c(Position pos, String value) {
	super(pos);
	assert(value != null);
	this.value = value;
    }

    /** Get the value of the expression. */
    public String value() {
	return this.value;
    }

    /** Set the value of the expression. */
    public StringLit value(String value) {
	StringLit_c n = (StringLit_c) copy();
	n.value = value;
	return n;
    }

    public String toString() {
        if (StringUtil.unicodeEscape(value).length() > 11) {
            return "\"" + StringUtil.unicodeEscape(value).substring(0,8) + "...\"";
        }
                
	return "\"" + StringUtil.unicodeEscape(value) + "\"";
    }

    protected int MAX_LENGTH = 60;

    /**
     * Break a long string literal into a concatenation of small string
     * literals.  This avoids messing up the pretty printer and editors. 
     */
    public List<String> breakupString() {
        List<String> result = new LinkedList<String>();
        int n = value.length();
        int i = 0;

        while (i < n) {
            int j;

            // Compensate for the unicode transformation by computing
            // the length of the encoded string.
            int len = 0;

            for (j = i; j < n; j++) {
                char c = value.charAt(j);
                int k = StringUtil.unicodeEscape(c).length();
                if (len + k > MAX_LENGTH) break;
                len += k;
            }

            result.add(value.substring(i, j));

            i = j;
        }

        if (result.isEmpty()) {
            // This should only happen when value == "".
            if (! value.equals("")) {
                throw new InternalCompilerError("breakupString failed");
            }
            result.add(value);
        }

        return result;
    }
}
