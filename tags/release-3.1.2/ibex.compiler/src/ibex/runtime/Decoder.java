package ibex.runtime;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

class Decoder {
    protected final boolean zip = true;
    protected final boolean base64 = true;
    protected final boolean test = false;

    public Object decode(String[] table) {
        ObjectInputStream is;
        byte[] b;

        String s;

        if (table.length == 0) {
            return new int[0];
        }
        else if (table.length == 1) {
            s = table[0];
        }
        else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < table.length; i++) {
                sb.append(table[i]);
            }
            s = sb.toString();
        }

        if (base64) {
            b = Base64.decode(s.toCharArray());
        }
        else {
            char[] source = s.toCharArray();
            b = new byte[source.length];
            for (int i = 0; i < source.length; i++) {
                b[i] = (byte) source[i];
            }
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(b);

            if (zip) {
                is = new ObjectInputStream(new GZIPInputStream(bais));
            }
            else {
                is = new ObjectInputStream(bais);
            }

            return is.readObject();
        }
        catch (Exception e) {
            throw new RuntimeException("Exception while " +
                "deserialing parser table: " + e.getMessage(), e);
        }
    }
}
