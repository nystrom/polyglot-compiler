package ibex.types;

public interface CharRangeTerminal extends Terminal {
    char lo();
    void setLo(char c);

    char hi();
    void setHi(char c);
}