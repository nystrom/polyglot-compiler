package ibex.runtime;

import java.io.IOException;

public interface ICharParser extends IParser {
     char scan() throws IOException;
}
