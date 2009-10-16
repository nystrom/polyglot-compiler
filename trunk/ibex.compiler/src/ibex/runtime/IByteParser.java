package ibex.runtime;

import java.io.IOException;

public interface IByteParser<OutputType> {
     byte scan() throws IOException;
}
