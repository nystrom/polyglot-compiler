package ibex.runtime;

import java.io.IOException;

public interface IByteParser extends IParser {
     byte scan() throws IOException;
}
