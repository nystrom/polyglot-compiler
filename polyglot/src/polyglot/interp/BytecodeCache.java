//******************************************************************** 
//* Licensed Materials - Property of IBM 
//* Thorn programming language processor 
//* (C) Copyright IBM Corp. 2009. All Rights Reserved. 
//* US Government Users Restricted Rights - Use, duplication or 
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
//******************************************************************** 

package polyglot.interp;

import java.util.HashMap;
import java.util.Map;

import polyglot.frontend.Source;
import polyglot.types.QName;

public class BytecodeCache {

    public static class CEntry {
	CEntry(String className, QName sourceClassName, Source s, byte[] buf) {
	    this.fullName = sourceClassName;
	    this.className = className;
	    this.source = s;
	    this.bytes = buf;
	}
	public QName fullName; // may be null (e.g., for anonymous classes)
	public Source source; // may be null (e.g., for generated classes)
	public String className;
	public byte[] bytes;
	
	public String toString() {
	    return className;
	}
    }
    Map<String, CEntry> bytes = new HashMap<String, CEntry>();

    public byte[] getBytes(String className) {
	return bytes.get(className).bytes;
    }
    public CEntry get(String className) {
	return bytes.get(className);
    }

    public Iterable<CEntry> entries() {
	return bytes.values();
    }

    public void put(QName sourceClassName, String className, Source s, byte[] buf) {
	bytes.put(className, new CEntry(className, sourceClassName, s, buf));
    }

    public void remove(String className) {
	bytes.remove(className);
    }

    public String toString() {
	return bytes.toString();
    }
}
