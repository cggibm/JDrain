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
    public static int run(String szCmd) throws IOException, InterruptedException {
    	JLogHandler.printDbg("Running command: " + szCmd);
    	Runtime rt = Runtime.getRuntime();
    	Process proc = rt.exec(szCmd);

        //remove any output/error message
    	JStreamGobbler errorGobbler = new JStreamGobbler(proc.getErrorStream(), "ERROR");
    	JStreamGobbler outputGobbler = new JStreamGobbler(proc.getInputStream(), "OUTPUT");
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
	InputStream is;
    String szLogType;
    OutputStream os;

    /** JStreamGobbler constructor */
    JStreamGobbler(InputStream is, String szType) {
        this(is, szType, null);
    } /** JStreamGobbler - END */

    /** JStreamGobbler constructor */
    private JStreamGobbler(InputStream is, String szType, OutputStream redirect) {
        this.is = is;
        this.szLogType = szType;
        this.os = redirect;
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
                	JLogHandler.printErr(line);
                } else {
                	JLogHandler.printInfo(line);
                }  
            }
            if (pw != null)
                pw.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    } /** run - END */
} // JStreamGobbler class - END