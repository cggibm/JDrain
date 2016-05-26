/*** JLogHandler.java
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/*** JLogHandler - performs logging on command line
 **/
class JLogHandler {
	//error types
	public final int ERROR = -1;
	public final int EXERR = -2;
	public final int PASS  =  0;

	private final static DateFormat DF = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private final static int DBGON = 0;

	//global variables
	private static String szFile;
	private static JTextPane textPane;

	/** JLogHandler constructor */
	public JLogHandler(String szLogFile) {
		szFile = szLogFile;
		textPane = null;
	} /** JLogHandler - END */

	/** JLogHandler constructor */
	public void setTextPane(JTextPane obj) {
		textPane = obj;
	} /** JLogHandler - END */

	/** Display info popup message */
	public synchronized int displayInfoPopUpMessage(String szMessage) {
		//JOptionPane.showMessageDialog(null, szMessage);
		return JOptionPane.showConfirmDialog(null,
				szMessage, "Info",
				JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE);
	} /** displayInfoPopUpMessage - END */

	/** Display error popup message */
	public synchronized void displayErrPopUpMessage(String szMessage) {
		JOptionPane.showMessageDialog(null, szMessage, "Error", JOptionPane.ERROR_MESSAGE);
	} /** displayErrPopUpMessage - END */

	/** Display popup confirm message */
	public synchronized int displayConfirmMessage(String szMessage) {
		return JOptionPane.showConfirmDialog(null, szMessage, "Confirm Message",  JOptionPane.YES_NO_OPTION); 
	} /** displayConfirmMessage - END */

	/** Prints console error message
	 *  Input:  szMessage - message to print
	 */
    public synchronized void printInfo(String szMessage) {
    	printAndDisplay(DF.format(new Date()) + " (INFO)    " + szMessage);
    } /** printInfo - END */

	/** Prints console error message
	 *  Input:  szMessage - error message to print
	 *  Output: error code
	 */
    public synchronized int printErr(String szMessage) {
    	printAndDisplay(DF.format(new Date()) + " (ERROR)    " + szMessage);
    	return ERROR;
    } /** printErr - END */

	/** Prints console error message
	 *  Input: szMessage - error message to print
	 *         ex        - exception info
	 *  Output: error code
	 */
    public synchronized int printErr(String szMessage, Exception ex) {
    	printAndDisplay(DF.format(new Date()) + " (EXERR)    " + szMessage);
   		ex.printStackTrace();
   		return EXERR;
    } /** printErr - END */

	/** Prints console debug message
	 *  Input: szMessage - message to print
	 */
    @SuppressWarnings("unused")
	public synchronized void printDbg(String szMessage) {
    	if (DBGON > 0) {
    		printAndDisplay(DF.format(new Date()) + " (DEBUG)    " + szMessage);
    	}
    } /** printDbg - END */

	/** Checks if the given file is locked
	 *  Input: file - file to check
	 *  Output: true  - all files are closed
	 *          false - one or more files are closed
	 */
	private synchronized static boolean isFileLocked(File file) {
		boolean bIsLocked = false; //return false f file does not exist
		if (file.exists() && !file.renameTo(file)) {
			bIsLocked = true;
		}
		return bIsLocked; 
	} /** isFileLocked - END */

    /** Display logs to GUI
     *  Input: szMsg - message to display
     **/
	private synchronized static void displayTxtPaneTxt(String szMsg) {
		if (textPane != null) {
			try {
				StyledDocument document = (StyledDocument)textPane.getDocument();
				Style style = textPane.addStyle(null, null);
				if ((szMsg.contains("(ERROR)")) || (szMsg.contains("(EXERR)"))) {
					StyleConstants.setForeground(style, Color.red);
				} else {
					StyleConstants.setForeground(style, Color.black);
				}
				document.insertString(document.getLength(), szMsg + "\n", style);
				StyleConstants.setForeground(style, Color.black);
				textPane.setCaretPosition(textPane.getDocument().getLength());
			} catch (Exception ex) {
				JTextPane tmpPane = textPane;
				textPane = null;
				printAndDisplay("Error in displaying message to text pane.");
				ex.printStackTrace();
				textPane = tmpPane;
			}
		}
	} /** displayTxtPaneTxt - END */

    /** Display and writes logs to file
     *  Input: szMsg - message to write and display
     **/
    private synchronized static void printAndDisplay(String szMsg) {
    	System.out.println(szMsg);
    	displayTxtPaneTxt(szMsg);

    	File file = new File(szFile);
    	if (!file.exists()) {
    		try {
				file.createNewFile();
			} catch (IOException ex) {
				System.out.println("Exception encountered while creating " + szFile);
				ex.printStackTrace();
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
		} catch (IOException ex) {
			System.out.println("Exception encountered while writing on " + szFile);
			ex.printStackTrace();
		}
		writer = null;
    } /** printAndDisplay - END */
} // JLogHandler - END