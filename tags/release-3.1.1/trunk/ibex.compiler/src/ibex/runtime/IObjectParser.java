package ibex.runtime;

import java.io.IOException;

public interface IObjectParser extends IParser {
     Object scan() throws IOException;
}
