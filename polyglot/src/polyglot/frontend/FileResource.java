/**
 * 
 */
package polyglot.frontend;

import java.io.*;

public class FileResource implements Resource {
    private final File source;

    public FileResource(File source) {
	this.source = source;
    }

    public int hashCode() {
	return source.hashCode();
    }

    public boolean equals(Object o) {
	if ( o instanceof FileResource) {
	    FileResource r = (FileResource) o;
	    return source.equals(r.source);
	}
	return false;
    }

    public InputStream getInputStream() throws IOException {
	FileInputStream in = new FileInputStream(source);
	return in;
    }

    public File file() {
	return source;
    }

    public String name() {
	return source.getName();
    }

    public String toString() {
	return source.getPath();
    }
}