/*
 * Author : Stephen Chong
 * Created: Jan 9, 2004
 */
package polyglot.pth;

import java.io.PrintStream;
import java.util.*;

/**
 * 
 */
public class Main {
    public static void main(String[] args) {
        new Main().start(args);
    }
    
    public static Options options;
     
    public void start(String[] args) {
        options = new Options();
        try {
            options.parseCommandLine(args);
        }
        catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
            
        if (options.inputFilenames.isEmpty()) {
            System.err.println("Need at least one script file.");
            System.exit(1);
        }
        
        OutputController outCtrl = createOutputController(options);
        
        for (Iterator iter = options.inputFilenames.iterator(); iter.hasNext(); ) {
            String filename = (String)iter.next();
            ScriptTestSuite t = new ScriptTestSuite(filename);
            t.setOutputController(outCtrl);
            if (options.showResultsOnly) {
                outCtrl.displayTestSuiteResults(t.getTestSuiteResult());
            }
            else {          
                t.run();
            }
        }
    }
    
    protected OutputController createOutputController(Options options) {
        return new OutputController(System.out, options.verbosity);
    }    
    
}