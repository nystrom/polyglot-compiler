package ibex.runtime;

import java.io.IOException;

public interface Parser {
    /** Return the next character in the input stream, advancing the stream.
     * Return -1 if EOF, or throw an exception.
     */
    public int scan() throws IOException;
}
