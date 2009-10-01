package ibex.runtime;

import ibex.runtime.Scanner.MemoEntry;
import ibex.runtime.Scanner.ParseError;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public class FileScanner extends Scanner {

    final String file;
    

    /**
     * For each input character, the line number of that character. Could be made more
     * efficient.
     */
    private final int[] lineMap;

    /**
     * For each input character, the column number of that character. Could be made more
     * efficient.
     */
    private final int[] columnMap;
    
    public FileScanner(final String file_, final Reader reader) throws IOException {
        super(read(reader));
        this.file = file_;
        
        // build a map from offsets to line and column numbers. This can be made more
        // efficient.
        this.lineMap = new int[input.length];
        this.columnMap = new int[input.length];
        int line = 1;
        int column = 1;

        for (int i = 0; i < input.length; i++) {
            lineMap[i] = line;
            columnMap[i] = column;

            if (input[i] == '\r' && i+1 < input.length && input[i+1] == '\n') {
                // handle \r\n: use the same position for both characters.
                continue;
            }
            if (input[i] == '\r' || input[i] == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
    }
    
    
    private static byte[] read(final Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        // Reader r = new BufferedReader(reader);
        while (true) {
            try {
                final int n = reader.read(buf);
                if (n == -1) break;
                sb.append(buf, 0, n);
            } catch (final EOFException e) {
                break;
            }
        }
        return sb.toString().getBytes();
    }
    
    protected
    void reportError(ParseError e) {
        Position pos = position(e.pos, e.endPos);
        System.err.println(pos + ": " + e.msg);
    }

    /** Pop the position stack, returning the argument (the semantic action result) */
    public <T> T accept(final T o) {
        assert !(o instanceof MemoEntry);
        final Integer topPos = posStack.get(posStack.size() - 1);
        final Position position = position(topPos, pos);

        // Set the position of the node.
        if (o instanceof ISetPosition) {
            final ISetPosition n = (ISetPosition) o;
            n.setPosition(position);
        }

        pop();
        return o;
    }
    
    private Position position(final int position, final int endPos) {
        final int line = getLine(position);
        final int column = getColumn(position);
        final int endLine = getLine(endPos);
        final int endColumn = getColumn(endPos);
        return new Position(input, file, line, column, endLine, endColumn, position, endPos);
    }

    private int getLine(int offset) {
        if (offset < 0) offset = 0;
        if (offset >= lineMap.length) offset = lineMap.length - 1;
        return lineMap[offset];
    }

    private int getColumn(int offset) {
        if (offset < 0) offset = 0;
        if (offset >= columnMap.length) offset = columnMap.length - 1;
        return columnMap[offset];
    }

}
