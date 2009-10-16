package ibex.runtime;

import java.io.IOException;

public class Util {
    public static Terminal scanChar(Parser p) throws IOException {
        int ch = p.scan();
        if (ch == -1)
            return new EOF();
        return new CharTerminal((char) ch);
    }
}
