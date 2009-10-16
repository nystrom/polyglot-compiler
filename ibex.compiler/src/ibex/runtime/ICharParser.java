package ibex.runtime;

import java.io.IOException;

public interface ICharParser<OutputType> {
     char scan() throws IOException;
}
