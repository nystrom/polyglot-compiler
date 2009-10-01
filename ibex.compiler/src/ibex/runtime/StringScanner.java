package ibex.runtime;

public class StringScanner extends Copy_2_of_Scanner {

    public StringScanner(String s) {
        super(s.getBytes());
    }
}
