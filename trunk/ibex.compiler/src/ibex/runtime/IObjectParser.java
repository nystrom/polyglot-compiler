package ibex.runtime;

import java.io.IOException;

public interface IObjectParser<InputType,OutputType> {
     InputType scan() throws IOException;
}
