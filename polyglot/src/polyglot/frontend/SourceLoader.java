/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import polyglot.main.Report;
import polyglot.util.FileUtil;
import polyglot.util.InternalCompilerError;

/** A <code>SourceLoader</code> is responsible for loading source files. */
public class SourceLoader
{
    protected ExtensionInfo sourceExt;
    protected Collection sourcePath;

    /** 0 if unknown, 1 if case insensitive, -1 if not. */
    protected int caseInsensitive;

    /** Set of sources already loaded.  An attempt to load a source
      * already loaded will cause an IOException. */
    protected Map loadedSources;

    /**
     * This is a map from Files (of directories) to Set[String]s, which
     * records the first level of contents of the directory. This cache
     * is used to avoid a number of File.exists() calls.
     */
    protected Map directoryContentsCache;

    public SourceLoader(ExtensionInfo sourceExt, Collection sourcePath) {
	this.sourcePath = sourcePath;
	this.sourceExt = sourceExt;
        this.directoryContentsCache = new HashMap();
        this.caseInsensitive = 0;
        this.loadedSources = new HashMap();
    }

    /** Load a source from a specific file. */
    public FileSource fileSource(String fileName) throws IOException {
        return fileSource(fileName, false);
    }
    public FileSource fileSource(String fileName, boolean userSpecified) throws IOException {
        File sourceFile = new File(fileName);
        
        if (! sourceFile.exists()) {
            throw new FileNotFoundException(fileName);
        }
        
        String[] exts = sourceExt.fileExtensions();
        boolean ok = false;
        
        for (int i = 0; i < exts.length; i++) {
            String ext = exts[i];
            
            if (fileName.endsWith("." + ext)) {
                ok = true;
                break;
            }
        }
        
        if (! ok) {
            String extString = "";
            
            for (int i = 0; i < exts.length; i++) {
                if (exts.length == 2 && i == exts.length-1) {
                    extString += " or ";
                }
                else if (exts.length != 1 && i == exts.length-1) {
                    extString += ", or ";
                }
                else if (i != 0) {
                    extString += ", ";
                }
                extString = extString + "\"." + exts[i] + "\"";
            }
            
            if (exts.length == 1) {
                throw new IOException("Source \"" + fileName +
                                      "\" does not have the extension "
                                      + extString + ".");
            }
            else {
                throw new IOException("Source \"" + fileName +
                                      "\" does not have any of the extensions "
                                      + extString + ".");
            }
        }
        
        if (Report.should_report(Report.loader, 2))
            Report.report(2, "Loading class from " + sourceFile);

        FileSource s = (FileSource) loadedSources.get(fileKey(sourceFile));
        
        if (s != null) {
            if (!s.userSpecified && userSpecified) {
                s.setUserSpecified(true);
            }
            return s;
        }
        
        s = sourceExt.createFileSource(sourceFile, userSpecified);
        loadedSources.put(fileKey(sourceFile), s);
        return s;
    }

    /**
     * The current user directory. We make it static so we don't need to
     * keep on making copies of it. 
     */
    protected static File current_dir = null;

    /**
     * The current user directory.
     */
    protected static File current_dir() {
        if (current_dir == null) {
            current_dir = new File(System.getProperty("user.dir"));
        }
        return current_dir;
    }

    /** Check if a directory for a package exists. */
    public boolean packageExists(String name) {
        String fileName = name.replace('.', File.separatorChar);

	/* Search the source path. */
        for (Iterator i = sourcePath.iterator(); i.hasNext(); ) {
            File directory = (File) i.next();

            File f = new File(directory, fileName);

            if (f.isDirectory() && FileUtil.checkNameFromRoot(directory, f)) {
                return true;
            }
        }

        return false;
    }
    
    /** Load the source file for the given class name using the source path. */
    public FileSource classSource(String className) {
	/* Search the source path. */
        String[] exts = sourceExt.fileExtensions();

        for (int k = 0; k < exts.length; k++) {
            String fileName = className.replace('.', File.separatorChar) +
                                      "." + exts[k];

            for (Iterator i = sourcePath.iterator(); i.hasNext(); ) {
                File directory = (File) i.next();
                if (! directory.isDirectory())
                    continue;
                Set dirContents = (Set)directoryContentsCache.get(directory);
                if (dirContents == null) {
                    dirContents = new HashSet();
                    directoryContentsCache.put(directory, dirContents);
                    if (directory.exists()) {
                        String[] contents = directory.list();
                        // May return null if directory is not found
                        if (contents == null)
                            continue;
                        for (int j = 0; j < contents.length; j++) {
                            dirContents.add(contents[j]);
                        }
                    }                
                }

                // check if the source file exists in the directory
                int index = fileName.indexOf(File.separatorChar);
                if (index < 0) index = fileName.length(); 
                String firstPart = fileName.substring(0, index);

                if (dirContents.contains(firstPart)) {
                    // the directory contains at least the first part of the
                    // file path. We will check if this file exists.
                    File sourceFile;
                    
                    if (directory != null && directory.equals(current_dir())) {
                        sourceFile = new File(fileName);
                    }
                    else {
                        sourceFile = new File(directory, fileName);
                    }
                    
                    // Skip it if already loaded
                    FileSource s = (FileSource) loadedSources.get(fileKey(sourceFile));

                    if (s != null) {
                        return s;
                    }
                    
                    try {
                        if (Report.should_report(Report.loader, 2))
                            Report.report(2, "Loading " + className + " from " + sourceFile);
                        s = sourceExt.createFileSource(sourceFile, false);
                        loadedSources.put(fileKey(sourceFile), s);
                        return s;
                    }
                    catch (IOException e) {
                    }
                }
            }
        }

        return null;
    }

    public Object fileKey(File file) {
	try {
	    return file.getCanonicalPath();
	}
	catch (IOException e) {
	    return file.getAbsolutePath();
	}
    }
}
