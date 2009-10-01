/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 *
 */

package ibex.runtime;

import java.io.Serializable;

/**
 * This class represents a position within a file.
 **/
public class Position implements Serializable
{
    static final long serialVersionUID = -4588386982624074261L;

    static final boolean precise_compiler_generated_positions = false;

    private byte[] inputBytes;
    private String file_;

    private int line_;
    private int column_;

    private int endLine_;
    private int endColumn_;

    // Position in characters from the beginning of the containing character
    // stream
    private int offset;
    private int endOffset;

    public static final int UNKNOWN = -1;
    public static final int END_UNUSED = -2;
    public static final Position COMPILER_GENERATED = new Position(null, "Compiler Generated");

    public static final int THIS_METHOD = 1;
    public static final int CALLER = THIS_METHOD + 1;
    /**
     * Get a compiler generated position using the caller at the given stack
     * depth.  Depth 1 is the caller.  Depth 2 is the caller's caller, etc.
     */
    public static Position compilerGenerated(final int depth) {
        if (precise_compiler_generated_positions) {
            final StackTraceElement[] stack = new Exception().getStackTrace();
            if (depth < stack.length) {
                return new Position(null, stack[depth].getFileName() + " (compiler generated)", stack[depth].getLineNumber());
            }
        }
        return COMPILER_GENERATED;
    }

    /** Get a compiler generated position. */
    public static Position compilerGenerated() {
        return compilerGenerated(CALLER);
    }

    /** For deserialization. */
    protected Position() { }

    public Position(final byte[] input, final String file) {
        this(input, file, UNKNOWN, UNKNOWN);
    }

    public Position(final byte[] input, final String file, final int line) {
        this(input, file, line, UNKNOWN);
    }

    public Position(final byte[] input, final String file, final int line, final int column) {
        this(input, file, line, column, END_UNUSED, END_UNUSED);
    }

    public Position(final byte[] input, final String file, final int line, final int column, final int endLine, final int endColumn) {
        this(input, file, line, column, endLine, endColumn, 0, 0);
    }

    public Position(final byte[] input, final String file, final int line, final int column, final int endLine, final int endColumn,
			final int offset1, final int endOffset1) {
        this.inputBytes = input;
        this.file_ = file;
        this.line_ = line;
        this.column_ = column;
        this.endLine_ = endLine;
        this.endColumn_ = endColumn;
        this.offset = offset1;
        this.endOffset = endOffset1;
    }

    public Position(final Position start, final Position end) {
        this(start != null ? start.inputBytes() : new byte[0],
             start != null ? start.file() : "<unknown>",
             start != null ? start.line_ : UNKNOWN,
             start != null ? start.column_ : UNKNOWN,
             end != null ? end.endLine_ : END_UNUSED,
             end != null ? end.endColumn_ : END_UNUSED,
             start != null ? start.offset : UNKNOWN,
             end != null ? end.endOffset : END_UNUSED);
    }

    public boolean isSynthetic() {
        return this.file() == null;
    }

    public byte[] inputBytes() {
        return inputBytes;
    }

    public Position truncateEnd(final int len) {
    	if (this == COMPILER_GENERATED)
    	    return this;

	int eo = endOffset;
	int el = endLine_;
	int ec = endColumn_;

	if (eo >= offset + len)
	    eo -= len;
	else
	    eo = offset;

	if (line_ == el) {
	    if (ec >= column_ + len)
		ec -= len;
	    else
		ec = column_;
	}
	else {
	    if (ec >= len)
		ec -= len;
	    else {
		el = line_;
		ec = column_;
	    }
	}

	return new Position(inputBytes, file_, line_, column_, el, ec, offset, eo);
    }

    public Position startOf() {
    	if (this == COMPILER_GENERATED)
    	    return this;
        return new Position(inputBytes, file_, line_, column_, line_, column_, offset, offset);
    }

    public Position endOf() {
    	if (this == COMPILER_GENERATED)
    	    return this;
        return new Position(inputBytes, file_, endLine_, endColumn_, endLine_, endColumn_, endOffset, endOffset);
    }

    public int line() {
        return line_;
    }

    public int column() {
        return column_;
    }

    public int endLine() {
        if (endLine_ == UNKNOWN || line_ != UNKNOWN && endLine_ < line_) {
            return line_;
        }
        return endLine_;
    }

    public int endColumn() {
        if (endColumn_ == UNKNOWN || column_ != UNKNOWN && endLine_ == line_ && endColumn_ < column_) {
            return column_;
        }
        return endColumn_;
    }

    public int offset() {
        return offset;
    }

    public int endOffset() {
        if (endOffset == UNKNOWN || offset != UNKNOWN && endOffset < offset) {
            return offset;
        }
        return endOffset;
    }

    public String file() {
        return file_;
    }

    public String nameAndLineString() {
        // Maybe we should use path here, if it isn't too long...
        String s = file_;

        if (s == null) {
            s = "unknown file";
        }

        if (line_ != UNKNOWN) {
            s += ":" + line_;
            if (endLine_ != line_ &&
                    endLine_ != UNKNOWN && endLine_ != END_UNUSED) {
                s += "-" + endLine_;
            }
        }

        return s;
    }

    @Override
	public String toString() {
        String s = file_;

        if (s == null) {
            s = "unknown file";
        }

        if (line_ != UNKNOWN) {
            s += ":" + line_;

            if (column_ != UNKNOWN) {
                s += "," + column_;
                if (line_ == endLine_ &&
                    endColumn_ != UNKNOWN &&
                    endColumn_ != END_UNUSED &&
                    endColumn_ != column_) {
                    s += "-" + endColumn_;
                }
                if (line_ != endLine_ &&
                    endColumn_ != UNKNOWN &&
                    endColumn_ != END_UNUSED) {
                    s += "-" + endLine_ + "," + endColumn_;
                }
            }
        }

        return s;
    }
}
