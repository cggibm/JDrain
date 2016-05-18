/*** JLogHandler.java
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;

/*** JLogHandler - performs logging on command line
 **/
class JLogHandler extends JDrain {
	//error types
	public final static int ERROR = -1;
	public final static int EXERR = -2;
	public final static int PASS  =  0;

	private final static DateFormat DF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private final static int DBGON = 0;
	private final static String szFile = getLogFile();

	/** JLogHandler constructor */
	public JLogHandler(String szLogFile, Object obj) {
		//TODO
	}

	/** Display info popup message */
	public static int displayInfoPopUpMessage(String szMessage) {
		//JOptionPane.showMessageDialog(null, szMessage);
		return JOptionPane.showConfirmDialog(null,
				szMessage, "Info",
				JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE);
	} /** displayInfoPopUpMessage - END */

	/** Display error popup message */
	public static void displayErrPopUpMessage(String szMessage) {
		JOptionPane.showMessageDialog(null, szMessage, "Error", JOptionPane.ERROR_MESSAGE);
	} /** displayErrPopUpMessage - END */

	/** Display popup confirm message */
	public static int displayConfirmMessage(String szMessage) {
		return JOptionPane.showConfirmDialog(null, szMessage, "Confirm Message",  JOptionPane.YES_NO_OPTION); 
	} /** displayConfirmMessage - END */

	/** Prints console error message
	 *  Input:  szMessage - message to print
	 */
    public static void printInfo(String szMessage) {
    	printAndDisplay(DF.format(new Date()) + " (INFO)    " + szMessage);
    } /** printInfo - END */

	/** Prints console error message
	 *  Input:  szMessage - error message to print
	 *  Output: error code
	 */
    public static int printErr(String szMessage) {
    	printAndDisplay(DF.format(new Date()) + " (ERROR)    " + szMessage);
    	return ERROR;
    } /** printErr - END */

	/** Prints console error message
	 *  Input: szMessage - error message to print
	 *         ex        - exception info
	 *  Output: error code
	 */
    public static int printErr(String szMessage, Exception ex) {
    	printAndDisplay(DF.format(new Date()) + " (EXERR)    " + szMessage);
   		ex.printStackTrace();
   		return EXERR;
    } /** printErr - END */

	/** Prints console debug message
	 *  Input: szMessage - message to print
	 */
    @SuppressWarnings("unused")
	public static void printDbg(String szMessage) {
    	if (DBGON > 0) {
    		printAndDisplay(DF.format(new Date()) + " (DEBUG)    " + szMessage);
    	}
    } /** printDbg - END */

	/** Checks if the given file is locked
	 *  Input: file - file to check
	 *  Output: true  - all files are closed
	 *          false - one or more files are closed
	 */
	private static boolean isFileLocked(File file) {
		boolean bIsLocked = false; //return false f file does not exist
		if (file.exists() && !file.renameTo(file)) {
			bIsLocked = true;
		}
		return bIsLocked; 
	} /** isFileLocked - END */

    /** Display and writes logs to file
     *  Input: szMsg - message to write and display
     **/
    private static void printAndDisplay(String szMsg) {
    	System.out.println(szMsg);

    	File file = new File(szFile);
    	if (!file.exists()) {
    		try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Exception encountered while creating " + szFile);
				e.printStackTrace();
			}
    	}

		if (isFileLocked(file)) {
			System.out.println("File " + szFile + " is locked. Please close file to record logs.");
			file = null;
			return;
		}

		//write table headers at the top of file
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			writer.write(szMsg + "\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Exception encountered while writing on " + szFile);
			e.printStackTrace();
		}
		writer = null;
    } /** printAndDisplay - END */
} // JLogHandler - END