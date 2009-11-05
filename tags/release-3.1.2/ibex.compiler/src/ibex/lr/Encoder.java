package ibex.lr;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

import polyglot.util.Base64;
import polyglot.util.InternalCompilerError;

public class Encoder {
    protected final boolean zip = true;
    protected final boolean base64 = true;
    protected final boolean test = false;

    public String[] encode(Object table) {
        ByteArrayOutputStream baos;
        ObjectOutputStream os;

        try {
            baos = new ByteArrayOutputStream();

            if (zip) {
                os = new ObjectOutputStream(new GZIPOutputStream(baos));
            }
            else {
                os = new ObjectOutputStream(baos);
            }

            os.writeObject(table);
            os.flush();
            os.close();
        }
        catch (Exception e) {
            throw new InternalCompilerError("Exception while " +
                "serializing parser table: " + e.getMessage(), e);
        }

        byte[] b = baos.toByteArray();

        String s;
        if (base64) {
            s = new String(Base64.encode(b));
        }
        else {
            StringBuffer sb = new StringBuffer(b.length);
            for (int i = 0; i < b.length; i++) {
                sb.append((char) b[i]);
            }
            s = sb.toString();
        }

        return new String[] { s };
    }
}
