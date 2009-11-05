package ibex.runtime;

import java.io.IOException;

public class Util {
    public static Terminal scanChar(ICharParser p, int[] map) throws IOException {
        int ch = p.scan();
        if (ch < 0 || ch >= map.length || map[ch] == ((ParserImpl) p).eofSymbol())
            throw new IOException("unexpected character " + (char) ch);
        return new CharTerminal(map[ch], (char) ch);
    }
    public static Terminal scanByte(IByteParser p, int[] map) throws IOException {
        byte ch = p.scan();
        if (ch < 0 || ch >= map.length || map[ch] == ((ParserImpl) p).eofSymbol())
            throw new IOException("unexpected character " + (char) ch);
        return new ByteTerminal(map[ch & 0xff], ch);
    }
    
    public static int[] decodeTerminalTable(ParserImpl parser) {
        String[] t = parser.encodedTerminalTable();
        return (int[]) new Decoder().decode(t);
    }
    

}
