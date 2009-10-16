package ibex.types;

import java.util.List;

public interface RSeq extends Rhs {
    List<Rhs> items();
    void setItems(List<Rhs> symbols);
}
