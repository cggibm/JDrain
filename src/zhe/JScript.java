/*** JScript.java
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class JScript {
	/** Runs shell commands
	 *  Input:  szCmd - command to run
	 *  Output: 0 if pass; other if otherwise
	 */
    public static int run(String szCmd, JLogHandler logHandlr) throws IOException, InterruptedException {
    	logHandlr.printDbg("Running command: " + szCmd);
    	Runtime rt = Runtime.getRuntime();
    	//setup command line to support bash
    	Process proc = rt.exec(new String[]{"C:\\cygwin64\\bin\\bash.exe", "-c", "export DISPLAY=:0.0; " + szCmd},
    			new String[]{"PATH=/cygdrive/c/cygwin64/bin"});

        //remove any output/error message
    	JStreamGobbler errorGobbler = new JStreamGobbler(proc.getErrorStream(), "ERROR", logHandlr);
    	JStreamGobbler outputGobbler = new JStreamGobbler(proc.getInputStream(), "OUTPUT", logHandlr);
        errorGobbler.start();
        errorGobbler.join();
        outputGobbler.start();
        outputGobbler.join();

        return proc.waitFor();
    } /** run - END */
} // JScript class - END

/*** Class that gobbles bash output 
 **/
class JStreamGobbler extends Thread {
	/** Constants */

	/** Global variables */
	private InputStream is;
    private String szLogType;
    private OutputStream os;
    private JLogHandler logHandlr;

    /** JStreamGobbler constructor */
    JStreamGobbler(InputStream is, String szType, JLogHandler log) {
        this(is, szType, null, log);
    } /** JStreamGobbler - END */

    /** JStreamGobbler constructor */
    private JStreamGobbler(InputStream is, String szType, OutputStream redirect, JLogHandler log) {
        this.is = is;
        this.szLogType = szType;
        this.os = redirect;
        this.logHandlr = log;
    } /** JStreamGobbler - END */

    /** Starts thread */
    public void run() {
        try {
            PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null) {
                if (pw != null)
                    pw.println(line);
                if (szLogType.contains("ERROR")) {
                	logHandlr.printErr(line);
                } else {
                	logHandlr.printInfo(line);
                }  
            }
            if (pw != null)
                pw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    } /** run - END */
} // JStreamGobbler class - END