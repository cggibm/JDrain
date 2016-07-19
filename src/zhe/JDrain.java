/*** JDrain.java
 *   Main file for JDrain app
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.TextAction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JDrain extends JFrame {
	/** Enumerations */
	public enum panels {
		LOGIN, START, WORKUNIT, ARCHLIST, DEFAULT
	} // panels enum - END
	
	/** Constants */
	private static String VERSION = "v1.40";
	private static String DUMMY = "DUMMY";
	private static Font TITLE  = new Font("Tahoma", Font.BOLD, 14);
	private static Font TSTLBL = new Font("Tahoma", Font.PLAIN, 8);
	private static Font PLAIN  = new Font("Tahoma", Font.PLAIN, 11);
	private static Font BOLD = new Font("Tahoma", Font.BOLD, 11);
	private static Font LOGFONTSMALL  = new Font("Tahoma", Font.PLAIN, 11);
	private static Font LOGFONTMEDIUM = new Font("Tahoma", Font.PLAIN, 13);
	private static Font LOGFONTLARGE  = new Font("Tahoma", Font.PLAIN, 15);
	private static boolean CLAIMEDIN = true;
	private static boolean ISHYBRID  = true;
	private static int MINWORKUNITLEN = 8;
	private static int MINDRAINSTLEN  = 3;
	private static int MINPRODLNLEN = 7;
	private static long WARNINGTIME = 10000; // 10s
	private static double VALIDJAVAVER = 1.7;

	/** Command list */
	private static String DEFAULTUSER = "reefuser";
	private static String DEFAULTSRVR = "sgad06e0";
	private static String LOGINCMD	= " val_user";
	private static String CLAIMINCMD  = "claim_in";
	private static String CLAIMOUTCMD = "claim_out";
	private static String SUSPEND	 = "suspend";
	private static String STEPSTART   = "stepStart";
	private static String STEPDATA	= "stepData";
	private static String STEPEND	 = "stepEnd";
	
	/** OS-specific */
	//Windows
	private static String WINUSER = "Orchid";
	private static String WINSEPARATR = "\\";
	private static String WINCODELOC  = "C:\\cygwin64\\home\\Orchid\\scripts\\";
	private static String WINTESTSTAT = "claimedWU\\teststat";
	private static String WINDRAINCMD = "/bin/bash /home/Orchid/scripts/drainHE.sh " + DEFAULTUSER + " " + DEFAULTSRVR;
	//Linux
	private static String LINUSER = "gem";
	private static String LINSEPARATR = "/";
	private static String LINCODELOC  = "/home/gem/scripts/";
	private static String LINTESTSTAT = "claimedWU/teststat";
	private static String LINDRAINCMD = "/bin/bash /home/gem/scripts/drainHE.sh " + DEFAULTUSER + " " + DEFAULTSRVR;

	/** Files to read */
	private static String INTROFILE= "0.PNG";
	private static String IBMIMG = "IBM.PNG";

	/** XML file variables */
	private static String TAGSTEP = "step";
	private static String STEPCNT = "count";
	private static String SMAXNOD = "maxnode";
	private static String TIMECHK = "timecheck";
	private static String USRENTY = "userentry";
	private static String NXTNODE = "nextnode";
	private static String NXTSTEP = "nextstep";
	private static String TAGNODE = "node";
	private static String MODEL   = "model";
	private static String NODECNT = "ncount";

	/** Timer format */
	private final static SimpleDateFormat TIMERFORMAT = new SimpleDateFormat("mm : ss");

	/** Global variables */
	private static boolean bIsClaimedIn;
	private static boolean bIsHybrid;
	private static JTestCaseControl tstCtl;
	private static JLogHandler tstLogs;
	private Font logFont;
	//OS specific variables
	private static String szUser;
	private static String szSeparatr;
	private static String szCodeLoc;
	private static String szTeststat;
	private static String szDrainCmd;
	private static boolean bIsWindows;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final JDrain frame = new JDrain();
					frame.addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
		   					if (bIsClaimedIn) {
		   						JOptionPane.showMessageDialog(null, "System will be suspended.",
		   								"Error", JOptionPane.DEFAULT_OPTION);
		   						performSuspend(); //if claimed-in, do a suspend
		   						frame.dispose();
		   					}
						}
					});
					frame.setVisible(true);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	private JPanel contentPane;
	/** JDrain constructor */
	public JDrain() {
		setTitle("JDrain " + VERSION);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));
		setContentPane(contentPane);

		GridBagLayout gblContentPane = new GridBagLayout();
		gblContentPane.rowWeights = new double[]{2};
		gblContentPane.columnWeights = new double[]{2};
		contentPane.setLayout(gblContentPane);

		if (!isValidJava()) {
			JOptionPane.showMessageDialog(null, "Please install Java version " + VALIDJAVAVER + " or greater.", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		prepareOSSpecificVar();
		if ((szUser == null) || (szUser.isEmpty())) {
			JOptionPane.showMessageDialog(null, "Current OS is not supported. Please use either Windows or Linux", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		//set log font to medium at beginning
		logFont = LOGFONTSMALL;

		//set up logging file
		Date date = new Date();
		String szFileDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
		tstLogs = new JLogHandler(szCodeLoc + szFileDate + ".TXT");

		//setup flags
		bIsClaimedIn = !CLAIMEDIN;
		bIsHybrid = !ISHYBRID;

		createMenuBar();
		createDrainPanel();
		prepareDrainRunPanels();
		pack();
	} /** JDrain - END */

	/** Checks if valid Java is installed
	 *  Output: true  - valid
	 *		    false - not valid
	 */
	private boolean isValidJava() {
		if (Double.parseDouble(System.getProperty("java.specification.version")) >= VALIDJAVAVER) {
			return true;
		}
		return false;
	} /** isValidJava - END */

	/** Prepares OS-specific variables */
	private void prepareOSSpecificVar() {
		String szOS = System.getProperty("os.name").toUpperCase().trim();
		if (szOS.contains("LINUX")) {
			szUser     = LINUSER;
			szSeparatr = LINSEPARATR;
			szCodeLoc  = LINCODELOC;
			szTeststat = LINTESTSTAT;
			szDrainCmd = LINDRAINCMD;
			bIsWindows = false;
		} else if (szOS.contains("WINDOWS")) {
			szUser     = WINUSER;
			szSeparatr = WINSEPARATR;
			szCodeLoc  = WINCODELOC;
			//szCodeLoc  = "C:\\Users\\gaguigc\\Documents\\_Project\\DRAIN\\";//CGG
			szTeststat = WINTESTSTAT;
			szDrainCmd = WINDRAINCMD;
			bIsWindows = true;
		}
	} /** prepareOSSpecificVar - END */

	/** Runs command
	 *  Input : szCmd - command to run
	 *  Output: error code - if error is encountered
	 *		    0 - pass
	 */
	private static int runScript(String szCmd) {
		int iRet = tstLogs.PASS;
		try {
			iRet = JScript.run(bIsWindows, szCmd, tstLogs);
			switch (iRet) {
			case 11:
				tstLogs.printErr("MFS: Workunit is NOT valid or is NOT in the correct OP 0807.");
				break;
			case 12:
				tstLogs.printErr("Duluth Testcell: Retry or Ask SGP TE to cleanup MFS testcell in Duluth.");
				break;
			case 13:
				tstLogs.printErr("Duluth Testcell: Fail to copy file from cellproto. Please retry.");
				break;
			case 14:
				tstLogs.printErr("MFS: Fail to login, check user authority or OP authority.");
				break;
			case 15:
				tstLogs.printErr("Duluth Testcell: Fail to setstat. Please retry.");
				break;
			case 16:
				tstLogs.printErr("MFS: Workunit is not A frame workunit.");
				break;
			default:
				break;
			}
		} catch (IOException | InterruptedException ex) {
			iRet = tstLogs.printErr("Exception encountered while running command.", ex);
		}
		return iRet;
	} /** runScript - END */

	/** Perform login
	 *  Output: error code - if error is encountered
	 *		    0 - pass
	 */
	private int performLogin() {
		//call script to login
		tstLogs.printInfo("Logging IN.");
		if (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + LOGINCMD) != 0) {
			return tstLogs.printErr("Logging in failed");
		}
		return tstLogs.PASS;
	} /** performLogin - END */

	/** Perform claim in of given work unit
	 *  Input : szWorkUnit - work unit to claim in
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int performClaimIn(String szWorkUnit) {
		tstLogs.printInfo("Performing MFS Claim-IN for WU " + szWorkUnit);
		//call script to claim-in
		int iRet = tstLogs.PASS;
		if (bIsHybrid) {
			if (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + " " + szWorkUnit) != 0) {
				return tstLogs.printErr("Claim IN failed");
			}
			iRet = updateNodeCnt();
		} else {
			displayTxt("Claiming IN");
			tstLogs.displayInfoPopUpMessage("Will claim in WU.\nDo not close the application.");
			if (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + " " + CLAIMINCMD + " " + szWorkUnit) != 0) {
				return tstLogs.printErr("Claim IN failed");
			}
			iRet = updateWorkUnitPanel();
		}

		if (iRet == tstLogs.PASS) {
			tstLogs.printInfo("Claim IN Successful.");
			bIsClaimedIn = CLAIMEDIN;
		}
		return iRet;
	} /** performClaimIn - END */

	/** Perform claim out */
	private int performClaimOut() {
		if (bIsClaimedIn) {
			tstLogs.printInfo("Performing MFS Claim-OUT for WU " + textFieldWorkUnit.getText());
			//call script to claim-out
			String szScript = szDrainCmd + " " + textFieldDrainSt.getText() + " " + CLAIMOUTCMD;
			if (bIsHybrid) {
				szScript = szDrainCmd + " " + textFieldDrainSt.getText() + " " + "DUMMYEND";
			}

			if (runScript(szScript) != 0) {
				return tstLogs.printErr("Claim OUT failed");
			}

			tstLogs.printInfo("Claim OUT Successful.");
			bIsClaimedIn = !CLAIMEDIN;
		}
		return tstLogs.PASS;
	} /** performClaimOut - END */

	/** Perform suspend */
	private static int performSuspend() {
		if (bIsClaimedIn) {
			//perform step end first to close previous step start
			performStepEnd();

			tstLogs.printInfo("Performing MFS suspend for WU " + textFieldWorkUnit.getText());
			//call script to suspend
			String szScript = szDrainCmd + " " + textFieldDrainSt.getText() + " " + SUSPEND;
			if (bIsHybrid) {
				szScript = szDrainCmd + " " + textFieldDrainSt.getText() + " " + "DUMMYEND";
			}

			if (runScript(szScript) != 0) {
				return tstLogs.printErr("Suspend failed");
			}

			tstLogs.printInfo("Process suspended.");
			tstLogs.displayInfoPopUpMessage("Process suspended.");
			bIsClaimedIn = !CLAIMEDIN;
		}
		return tstLogs.PASS;
	} /** performSuspend - END */

	/** Perform step start */
	private int performStepStart() {
		//call script to step start
		String szStepName = "N" + tstCtl.getCurrentNodeCnt() + "S" + tstCtl.getOpCnt();
		tstLogs.printInfo("Performing Step Start " + szStepName);
		
		if ((bIsClaimedIn) && (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + " " + STEPSTART + " " + szStepName + " " + szStepName) != 0)) {
			return tstLogs.printErr("Step start failed");
		}
		return tstLogs.PASS;
	} /** performStepStart - END */

	/** Perform step end */
	private static int performStepEnd() {
		tstLogs.printInfo("Performing Step End");
		//call script to step end
		if ((bIsClaimedIn) && (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + " " + STEPEND) != 0)) {
			return tstLogs.printErr("Step End failed");
		}
		return tstLogs.PASS;
	} /** performStepEnd - END */

	/** Perform step data
	 *  Input: Data to be entered for current step
	 **/
	private int performStepData(String szData) {
		String szTrimmedData = szData.replaceAll("\\s","");
		tstLogs.printInfo("Performing Step Data " + szTrimmedData);
		//call script to step data
		if ((bIsClaimedIn) && (runScript(szDrainCmd + " " + textFieldDrainSt.getText() + " " + STEPDATA + " " + szTrimmedData) != 0)) {
			return tstLogs.printErr("Step Data failed");
		}
		return tstLogs.PASS;
	} /** performStepData - END */

	/** Find text being searched in text pane
	 *  Input: textPaneToFind - textpane that contains string to be searched
	 *  	   textField      - contains the search word
	 *         btnNext        - button used to find the next instance of the search word in the given text pane
	 */
	private void findSearchWord(JTextPane textPaneToFind, JTextField textField, JButton btnNext) {
		int iPos = textPaneToFind.getCaretPosition();
		textPaneToFind.setCaretPosition(iPos);
		btnNext.setVisible(false);

		String szStrToFind = textField.getText().trim();
		if ((szStrToFind != null) && (szStrToFind.length() > 0)) {
			textPaneToFind.requestFocusInWindow();
			javax.swing.text.Document doc = textPaneToFind.getDocument();
			int iStrFindLen = szStrToFind.length();

			try {
				String szTextPaneContents = doc.getText(0, doc.getLength());
				Rectangle viewRect;
			   	if ( ((iPos = szTextPaneContents.toUpperCase().indexOf(szStrToFind.toUpperCase(), iPos)) >= 0) ||
			   		 ((iPos = szTextPaneContents.toUpperCase().indexOf(szStrToFind.toUpperCase(), 0   )) >= 0) ) {
					//get the rectangle of the where the text would be visible...
					viewRect = textPaneToFind.modelToView(iPos);
					//scroll to make the rectangle visible
					textPaneToFind.scrollRectToVisible(viewRect);
					//highlight the text
					textPaneToFind.setCaretPosition(iPos);
					textPaneToFind.moveCaretPosition(iPos + iStrFindLen);
					//enable next button
					btnNext.setVisible(true);
			   	} //if - END
			} catch (Exception ex) {
				tstLogs.printErr("Exception encountered while searching text", ex);
			}
		} else {
			tstLogs.displayErrPopUpMessage("Please enter search string");
		}
	} /** findSearchWord - END */

	/** Initializes test cases
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int initTestCase() {
		//get product line
		String szProdLn;
		if (bIsHybrid) {
			JComboItem item = (JComboItem)comboProductIdList.getSelectedItem();
			szProdLn = item.getDesc();
		} else {
			szProdLn = textFieldProd.getText();
		}
		tstLogs.printDbg("Init test cases for ProdLn = " + szProdLn);

		String szProdLnDir = szCodeLoc + szProdLn;
		File dirProd = new File(szProdLnDir);
		if (!dirProd.exists() && !dirProd.isDirectory()) {
			return tstLogs.printErr(szProdLnDir + " directory missing.");
		}

		tstCtl = new JTestCaseControl(szProdLnDir, Integer.parseInt(textFieldNodeCnt.getText()));
		if (getTestCaseDetails(TAGSTEP, szProdLnDir, szProdLn) != tstLogs.PASS) {
			return tstLogs.printErr("Failed to get test case details.");
		}
		
		int iMaxTestCnt = tstCtl.getMaxOpCnt();
		if (iMaxTestCnt <= 0) {
			return tstLogs.printErr("No tests found.");
		}

		tstLogs.printDbg("iMaxTestCnt = " + iMaxTestCnt);
		initializeTestCaseImgPanel(szProdLnDir);
		initializeNextPanel();
		return tstLogs.PASS;
	} /** initTestCase - END */

	/** Get test case details
	 *  Input : szXmlTag	- XML tag to find
	 *		  szProdLnDir - directory for the images to be displayed
	 *		  szProdLn	- product line
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int getTestCaseDetails(String szXmlTag, String szProdLnDir, String szProdLn) {
		String szXmlFile = szProdLnDir + szSeparatr + szProdLn + ".xml";
		//check if XML file exists
		File fXmlFile = new File(szXmlFile);
		if (!fXmlFile.exists()) {
			return tstLogs.printErr(szXmlFile + " is missing");
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			JStepInfoLink linkStepInfo;

			//optional, but recommended
			doc.getDocumentElement().normalize();

			NodeList nList = null;
			if (szXmlTag.contains(TAGSTEP)) {
				nList = doc.getElementsByTagName(TAGSTEP);
				//loop through the XML file
				for (int iIter = 0; iIter < nList.getLength(); iIter++) {
					Node nNode = nList.item(iIter);
					linkStepInfo = new JStepInfoLink();
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						linkStepInfo.iOpNum = Integer.parseInt(eElement.getAttribute(STEPCNT));
						linkStepInfo.iOpMaxNode = Integer.parseInt(eElement.getElementsByTagName(SMAXNOD).item(0).getTextContent());
						linkStepInfo.dTimeChk = Double.parseDouble(eElement.getElementsByTagName(TIMECHK).item(0).getTextContent());
						linkStepInfo.dUserEntry = Double.parseDouble(eElement.getElementsByTagName(USRENTY).item(0).getTextContent());
						linkStepInfo.iNextNodeStep = Integer.parseInt(eElement.getElementsByTagName(NXTNODE).item(0).getTextContent());
						linkStepInfo.iNextSameNodeStep = Integer.parseInt(eElement.getElementsByTagName(NXTSTEP).item(0).getTextContent());
						tstCtl.qlinkSteps.add(linkStepInfo);
					}
				} // for loop - END
				tstCtl.setMaxOpCnt(tstCtl.qlinkSteps.size());
			} else if (szXmlTag.contains(TAGNODE)) {
				nList = doc.getElementsByTagName(TAGNODE);
				//loop through the XML file
				for (int iIter = 0; iIter < nList.getLength(); iIter++) {
					Node nNode = nList.item(iIter);
					if (nNode.getNodeType() == Node.ELEMENT_NODE) {
						Element eElement = (Element) nNode;
						
						String szModel;
						if (bIsHybrid) {
							JComboItem item = (JComboItem)comboModelList.getSelectedItem();
							szModel = item.getDesc();
						} else {
							szModel = textFieldModel.getText();
						}

						if (eElement.getAttribute(MODEL).contains(szModel)) {
							textFieldNodeCnt.setText(eElement.getElementsByTagName(NODECNT).item(0).getTextContent());
							break;
						}
					}
				} // for loop - END
			} else {
				return tstLogs.printErr("Unknown XML tag.");
			}
		} catch (Exception ex) {
			return tstLogs.printErr("Exception encountered while getting operation details.", ex);
		}

		return tstLogs.PASS;
	} /** getTestCaseDetails - END */

	/** Get model list from XML file
	 *  Input : szProdLnDir - directory for the images to be displayed
	 *		    szProdLn	- product line
	 *  Output: error code - if error is encountered
	 *		    0 - pass
	 */
	private int getModelList(JComboBox<JComboItem> comboModelList, String szProdId) {
		String szXmlFile = szCodeLoc + szProdId + szSeparatr + szProdId + ".xml";
		//check if XML file exists
		File fXmlFile = new File(szXmlFile);
		if (!fXmlFile.exists()) {
			return tstLogs.printErr(szXmlFile + " is missing");
		}

		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			//optional, but recommended
			doc.getDocumentElement().normalize();

			NodeList nList = null;
			nList = doc.getElementsByTagName(TAGNODE);
			//loop through the XML file
			for (int iIter = 0; iIter < nList.getLength(); iIter++) {
				Node nNode = nList.item(iIter);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element)nNode;
					comboModelList.addItem(new JComboItem(eElement.getAttribute(MODEL)));
				}
			} // for loop - END
		} catch (Exception ex) {
			return tstLogs.printErr("Exception encountered while getting operation details.", ex);
		}

		return tstLogs.PASS;
	} /** getModelList - END */

	/** Draws the test case images
	 *  Input: step info */
	private void prepareOpPanels(JStepInfoLink stepInfo) {
		if (stepInfo.dTimeChk > 0) {
			//set up start timer button
			tstLogs.printDbg("Timer set to " + stepInfo.dTimeChk);
			setupStartTimer(stepInfo.dTimeChk);
		} else if (stepInfo.dUserEntry > 0) {
			setupValueReading(stepInfo.dUserEntry);
		}
	} /** prepareOpPanels - END */

	/** Draws the test case images
	 *  Input: op to be drawn
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 **/
	private int drawTestCase(int iCurrOp) {
		//display current step for current node
		String szOpImgToDisp = tstCtl.getTstFileLoc() + szSeparatr + iCurrOp + ".PNG";
		if (new File(szOpImgToDisp).exists()) {
			displayImg(szOpImgToDisp);
		} else {
			tstLogs.printErr("Test cannot find file. (" + iCurrOp + ".PNG)");
			return tstLogs.ERROR;
		}
		return tstLogs.PASS;
	} /** drawTestCase - END */

	/** Displays image on image panel
	 *  Input: szImg - image to be displayed
	 **/
	private void displayImg(String szImg) {
		//clear panelImg
		panelImg.removeAll();
		panelImg.repaint();

		//display test image
		ImageIcon icon = new ImageIcon(szImg);
		GridBagConstraints gbc = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		panelImg.add(new JLabel(icon), gbc);

		//display step number
		JLabel lblTstNum = new JLabel(szImg.replace(tstCtl.getTstFileLoc() + szSeparatr, "").replace(".PNG", ""));
		lblTstNum.setFont(TSTLBL);
		panelImg.add(lblTstNum);
		pack();
	} /** displayImg - END */

	/** Displays resource on image panel
	 *  Input: szImg - image to be displayed
	 **/
	private void displayLogo(String szImg) {
		//clear panelImg
		panelImg.removeAll();
		panelImg.repaint();

		URL url = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getResource(szImg);
		ImageIcon icon = new ImageIcon(url);
		GridBagConstraints gbc = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		panelImg.add(new JLabel(icon), gbc);
		pack();
	} /** displayLogo - END */

	/** Displays text on image panel
	 *  Input: szTxt - text to be displayed
	 **/
	private void displayTxt(String szTxt) {
		//clear panelImg
		panelImg.removeAll();
		panelImg.repaint();

		//add label
		JLabel lblStart = new JLabel(szTxt);
		lblStart.setFont(TITLE);
		panelImg.add(lblStart);
		pack();
	} /** displayTxt - END */

	JMenuItem mnitmLogout;
	/** Creates the menu bar */
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		//File menu
		JMenu mnFile = new JMenu("File");
		mnFile.setMnemonic(KeyEvent.VK_F);
		menuBar.add(mnFile);

		JMenuItem mnitmExit = mnFile.add("Exit");
		mnitmExit.setMnemonic(KeyEvent.VK_X);

		//Format menu
		JMenu mnFormat = new JMenu("Format");
		mnFormat.setMnemonic(KeyEvent.VK_O);
		menuBar.add(mnFormat);

		JMenu mnLogsFont = new JMenu("Log Font");
		mnLogsFont.setMnemonic(KeyEvent.VK_L);
		mnLogsFont.setToolTipText("Sets the font size of the text displayed in the tabbed panels.");
		mnFormat.add(mnLogsFont);

		JMenuItem mnitmSmall = mnLogsFont.add("1: Small");
		KeyStroke keyStrokectrl1 = KeyStroke.getKeyStroke(JControlCmdDef.CTRL1);
		mnitmSmall.setAccelerator(keyStrokectrl1);
		mnitmSmall.setMnemonic(KeyEvent.VK_1);
		final JMenuItem mnitmMedium = mnLogsFont.add("2: Medium");
		KeyStroke keyStrokectrl2 = KeyStroke.getKeyStroke(JControlCmdDef.CTRL2);
		mnitmMedium.setAccelerator(keyStrokectrl2);
		mnitmMedium.setMnemonic(KeyEvent.VK_2);
		final JMenuItem mnitmLarge = mnLogsFont.add("3: Large");
		KeyStroke keyStrokectrl3 = KeyStroke.getKeyStroke(JControlCmdDef.CTRL3);
		mnitmLarge.setAccelerator(keyStrokectrl3);
		mnitmLarge.setMnemonic(KeyEvent.VK_3);

		//Help menu
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setMnemonic(KeyEvent.VK_H);
		menuBar.add(mnHelp);

		JMenuItem mnitmAbout = mnHelp.add("About");

		setJMenuBar(menuBar);

		//add action listeners
		mnitmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (tstLogs.displayConfirmMessage("Are you sure you want to exit?") == JOptionPane.YES_OPTION) {
					if (bIsClaimedIn) {
						//if claimed-in, do a suspend
						displayTxt("Performing Suspend.");
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.");
						}
					}
					System.exit(0);
				}
			}
		});

		ActionListener fontSizeAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == mnitmMedium) {
					logFont = LOGFONTMEDIUM;
				} else if (e.getSource() == mnitmLarge) {
					logFont = LOGFONTLARGE;
				} else {
					logFont = LOGFONTSMALL;
				}

				textPaneLogs.setFont(logFont);
			}
		};
		mnitmSmall.addActionListener(fontSizeAction);
		mnitmMedium.addActionListener(fontSizeAction);
		mnitmLarge.addActionListener(fontSizeAction);

		mnitmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tstLogs.displayInfoPopUpMessage("<html>JDrain Tool Version " + VERSION +
						"<br><br><font size=1>Developed by Carlo Gagui & Shahril Selamat</font></html>");
			}
		});
	} /** createMenuBar - END */

	/** Initialize drain panel */
	private void initDrainPanel() {
		//initialize work unit panel
		initWorkUnitPanel();

		//initialize start panel
		initStartPanel();

		//initialize the test case panel
		initTestCasePanel();

		//display drain panel
		//reset flag every init
		bIsClaimedIn = !CLAIMEDIN;
	} /** initDrainPanel - END */

	/** Prepares the start and drain panels */
	private void prepareDrainRunPanels() {
		//initialize start panel
		initStartPanel();
		panelStart.setVisible(true);

		//initialize test panel
		initTestCasePanel();
		panelTestCase.setVisible(true);
	} /** prepareDrainRunPanels - END */

	/** Create drain panel */
	private JPanel panelDrain;
	private void createDrainPanel() {
		GridBagLayout gblDrainPanel = new GridBagLayout();
		gblDrainPanel.columnWeights = new double[]{1.0};
		gblDrainPanel.rowWeights = new double[]{0, 0, 10};
		GridBagConstraints gbc = JGridConstraint.getDefaultPanelGbc();
		panelDrain = new JPanel();
		contentPane.setBorder(new EmptyBorder(2, 2, 2, 2));
		panelDrain.setLayout(gblDrainPanel);
		contentPane.add(panelDrain, gbc);

		createWorkUnitPanel();
		createStartPanel();
		createTabbedPanel();
	} /** createDrainPanel - END */

	/** Initialize the start panel */
	private void initStartPanel() {
		textFieldStartDate.setText("");
		textFieldStartTime.setText("");
		btnAbort.setEnabled(false);
	} /** initStartPanel - END */

	/** Sets the start panel for running test */
	private void setRunningStartPanel() {
		//update date and time text fields
		Date date = new Date();
		String szStartDate = new SimpleDateFormat("yyyy/MM/dd").format(date);
		String szStartTime = new SimpleDateFormat("HH:mm:ss").format(date);
		textFieldStartDate.setText(szStartDate);
		textFieldStartTime.setText(szStartTime);

		btnAbort.setEnabled(true);
	} /** setRunningStartPanel - END */

	private JPanel panelStart;
	private JButton btnAbort;
	private JTextField textFieldStartDate;
	private JTextField textFieldStartTime;
	/** Create start panel */
	private void createStartPanel() {
		//create start panel
		GridBagLayout gblStart = new GridBagLayout();
		gblStart.rowHeights = new int[]{28};
		gblStart.columnWidths = new int[]{90, 100, 90, 100, 90, 90};
		gblStart.columnWeights = new double[]{0.01, 0.2, 0.01, 0.2, 0.02, 0.02};
		GridBagConstraints gbcStart = JGridConstraint.getDefaultPanelGbc();
		panelStart = new JPanel();
		panelStart.setVisible(false);
		panelStart.setLayout(gblStart);
		panelStart.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelDrain.add(panelStart, gbcStart);

		//start date
		GridBagConstraints gbcStartDateLabel = JGridConstraint.getDefaultObjectGbc(panels.START);
		gbcStartDateLabel.gridx = 0;
		JLabel lblStartDate = new JLabel("Start Date:");
		lblStartDate.setFont(PLAIN);
		panelStart.add(lblStartDate, gbcStartDateLabel);

		GridBagConstraints gbcStartDateField = JGridConstraint.getDefaultObjectGbc(panels.START);
		gbcStartDateField.gridx = 1;
		textFieldStartDate = new JTextField("");
		textFieldStartDate.setEditable(false);
		textFieldStartDate.setFont(PLAIN);
		textFieldStartDate.setToolTipText("Will be updated once Start pressed.");
		createPopUpMenu(textFieldStartDate);
		panelStart.add(textFieldStartDate, gbcStartDateField);

		//start time
		GridBagConstraints gbcStartTimeLabel = JGridConstraint.getDefaultObjectGbc(panels.START);
		gbcStartTimeLabel.gridx = 2;
		JLabel lblStartTime = new JLabel("Start Time:");
		lblStartTime.setFont(PLAIN);
		panelStart.add(lblStartTime, gbcStartTimeLabel);

		GridBagConstraints gbcStartTime = JGridConstraint.getDefaultObjectGbc(panels.START);
		gbcStartTime.gridx = 3;
		textFieldStartTime = new JTextField("");
		textFieldStartTime.setEditable(false);
		textFieldStartTime.setFont(PLAIN);
		textFieldStartTime.setToolTipText("Will be updated once Start pressed.");
		panelStart.add(textFieldStartTime, gbcStartTime);

		//abort button
		GridBagConstraints gbcBtn = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcBtn.gridx = 4;
		btnAbort = new JButton("Abort");
		btnAbort.setEnabled(false);
		btnAbort.setFont(PLAIN);
		panelStart.add(btnAbort, gbcBtn);
		
		//create action listeners
		btnAbort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (tstLogs.displayConfirmMessage("Are you sure you want to abort?") == JOptionPane.YES_OPTION) {
					if (bIsClaimedIn) {
						displayTxt("Performing Suspend.");
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.\nPlease inform ME team.");
						}
					}

					initDrainPanel();
					pack();
				}
			}
		});
	} /** createStartPanel - END */

	/** Update node count
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int updateNodeCnt() {
		String szProdLn;
		if (bIsHybrid) {
			JComboItem item = (JComboItem)comboProductIdList.getSelectedItem();
			szProdLn = item.getDesc();
		} else {
			szProdLn = textFieldProd.getText();
		}

		if ((getTestCaseDetails(TAGNODE, szCodeLoc + szProdLn, szProdLn) != tstLogs.PASS) ||
			(textFieldNodeCnt.getText().isEmpty())) {
			performSuspend();
			return tstLogs.printErr("Failed to get node count.");
		}
		return tstLogs.PASS;
	} /** updateNodeCnt - END */

	/** Update work unit panel
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int updateWorkUnitPanel() {
		String[] aszTmp;
		BufferedReader in = null;
		try {
			String szTmp = new File(".").getAbsolutePath();
			szTmp = szTmp.charAt(szTmp.length() - 1) == '.' ? szTmp.substring(0, szTmp.length() - 1) : szTmp;
			szTmp = szTmp + szTeststat;
			if (!new File(szTmp).exists()) {
				tstLogs.printErr("Missing teststat file. Claim-IN failed. Please inform ME.");
				return tstLogs.ERROR;
			}

			in = new BufferedReader(new FileReader(szTmp));
			while ((szTmp = in.readLine()) != null) {
				//all text are based on script
				if (szTmp.contains("(SYSTEM ") || szTmp.contains("(ORDRNUM ") || szTmp.contains("(SN ") ||
					szTmp.contains("(PR_ID ") || szTmp.contains("(MODEL ")) {
					tstLogs.printDbg("Reading line " + szTmp);
					aszTmp = szTmp.toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim().split(" ");
					switch (aszTmp[0]) {
					case "SYSTEM":
						if (aszTmp.length >= 2) {
							textFieldMfgn.setText(aszTmp[1]);
							if (textFieldOrder.getText().trim().isEmpty()) {
								textFieldOrder.setText(aszTmp[1].substring(1));
							}
						} else {
							tstLogs.printErr("MFGN not found. Please inform ME.");
						}
						break;
					case "ORDRNUM":
						if (aszTmp.length >= 2) {
							textFieldOrder.setText(aszTmp[1]);
						} else {
							tstLogs.printErr("Order No. not found. Please inform ME.");
						}
						break;
					case "SN":
						if (aszTmp.length >= 2) {
							textFieldSer.setText(aszTmp[1]);
						} else {
							tstLogs.printErr("Serial No. not found. Please inform ME.");
						}
						break;
					case "PR_ID":
						if (aszTmp.length >= 2) {
							textFieldProd.setText(aszTmp[1]);
						} else {
							tstLogs.printErr("Cannot identify machine product line. Please inform ME.");
						}
						break;
					case "MODEL":
						if (aszTmp.length >= 2) {
							textFieldModel.setText(aszTmp[1]);
						} else {
							tstLogs.printErr("Cannot identify machine model. Please inform ME.");
						}
						break;
					default:
						// do nothing
						break;
					} // switch - END
				}
			} // while loop - END
			in.close();
			in = null;
		} catch (FileNotFoundException ex) {
			performSuspend();
			return tstLogs.printErr("File not found.", ex);
		} catch (IOException ex) {
			performSuspend();
			return tstLogs.printErr("IOException while reading file.", ex);
		}

		if (textFieldMfgn.getText().isEmpty() ||
			textFieldOrder.getText().isEmpty() ||
			textFieldSer.getText().isEmpty() ||
			textFieldProd.getText().isEmpty() ||
			textFieldModel.getText().isEmpty()) {
			performSuspend();
			return tstLogs.printErr("Failed to get complete WU info.");
		}
		return updateNodeCnt();
	} /** updateWorkUnitPanel - END */
	
	/** Initialize work unit panel */
	private void initWorkUnitPanel() {
		//setup top part
		textFieldWorkUnit.setText("");
		textFieldWorkUnit.setEditable(true);
		textFieldDrainSt.setText("");
		textFieldDrainSt.setEditable(true);
		btnEnter.setVisible(true);

		//setup radio buttons
		bIsHybrid = !ISHYBRID;
		rdbtnPrime.setSelected(true);
		rdbtnPrime.setEnabled(true);
		rdbtnHybrid.setSelected(false);
		rdbtnHybrid.setEnabled(true);

		//bottom part
		textFieldMfgn.setText("");
		textFieldOrder.setText("");
		textFieldSer.setText("");
		textFieldProd.setText("");
		textFieldModel.setText("");
		textFieldNodeCnt.setText("");
		textFieldProd.setVisible(true);
		textFieldModel.setVisible(true);
		
		comboProductIdList.removeAllItems();
		comboProductIdList.setEnabled(true);
		comboProductIdList.setVisible(false);

		comboModelList.removeAllItems();
		comboModelList.setEnabled(true);
		comboModelList.setVisible(false);
	} /** initWorkUnitPanel - END */

	private JPanel panelWorkUnit;
	private static JTextField textFieldWorkUnit;
	private static JTextField textFieldDrainSt;
	private static JTextField textFieldMfgn;
	private static JTextField textFieldOrder;
	private static JTextField textFieldSer;
	private static JTextField textFieldProd;
	private static JTextField textFieldModel;
	private static JTextField textFieldNodeCnt;
	private static JComboBox<JComboItem> comboProductIdList;
	private static JComboBox<JComboItem> comboModelList;
	private static JRadioButton rdbtnPrime;
	private static JRadioButton rdbtnHybrid;
	private JButton btnEnter;
	/** Create work unit panel */
	private void createWorkUnitPanel() {
		//create serial panel
		GridBagLayout gblWorkUnit = new GridBagLayout();
		gblWorkUnit.rowHeights = new int[]{28, 1, 1, 28, 5, 28, 1};
		gblWorkUnit.rowWeights = new double[]{0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
		gblWorkUnit.columnWidths = new int[]{80, 80, 80, 80, 80, 80, 80};
		gblWorkUnit.columnWeights = new double[]{0, 0.01, 0, 0.01, 0, 0.01, 0.001};
		GridBagConstraints gbcWorkUnit = JGridConstraint.getDefaultPanelGbc();
		panelWorkUnit = new JPanel();
		panelWorkUnit.setLayout(gblWorkUnit);
		panelWorkUnit.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelDrain.add(panelWorkUnit, gbcWorkUnit);

		//work unit
		GridBagConstraints gbcWU = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcWU.gridx = 0;
		JLabel lblEnterWorkUnit = new JLabel("Work Unit:");
		lblEnterWorkUnit.setFont(PLAIN);
		panelWorkUnit.add(lblEnterWorkUnit, gbcWU);

		gbcWU.gridx = 1;
		textFieldWorkUnit = new JTextField();
		textFieldWorkUnit.setFont(PLAIN);
		textFieldWorkUnit.setColumns(10);
		textFieldWorkUnit.setToolTipText("Enter system work unit here.");
		createPopUpMenu(textFieldWorkUnit);
		panelWorkUnit.add(textFieldWorkUnit, gbcWU);

		//drain station
		gbcWU.gridx = 2;
		JLabel lblDrainSt = new JLabel("Drain Station:");
		lblDrainSt.setFont(PLAIN);
		panelWorkUnit.add(lblDrainSt, gbcWU);

		gbcWU.gridx = 3;
		textFieldDrainSt = new JTextField();
		textFieldDrainSt.setFont(PLAIN);
		textFieldDrainSt.setColumns(10);
		textFieldDrainSt.setToolTipText("Enter drain station name here.");
		createPopUpMenu(textFieldDrainSt);
		panelWorkUnit.add(textFieldDrainSt, gbcWU);

		//Prime order radio button
		gbcWU.gridx = 4;
		rdbtnPrime = new JRadioButton("Prime");
		rdbtnPrime.setFont(PLAIN);
		rdbtnPrime.setToolTipText("Select if Machine is Prime Box. (WITH MFS)");
		rdbtnPrime.setSelected(true); //prime is selected by default
		bIsHybrid = !ISHYBRID;
		panelWorkUnit.add(rdbtnPrime, gbcWU);

		//POK hybrid radio button
		gbcWU.gridx = 5;
		rdbtnHybrid = new JRadioButton("POK GARS");
		rdbtnHybrid.setFont(PLAIN);
		rdbtnHybrid.setToolTipText("Select if Machine is POK Hybrid. (NO MFS)");
		rdbtnHybrid.setSelected(false);
		panelWorkUnit.add(rdbtnHybrid, gbcWU);

		//enter button
		gbcWU.gridx = 6;
		btnEnter = new JButton("Enter");
		btnEnter.setFont(PLAIN);
		panelWorkUnit.add(btnEnter, gbcWU);

		//separator
		GridBagConstraints gbcSep = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcSep.gridwidth = 100;
		gbcSep.gridy = 2;
		JSeparator sep = new JSeparator();
		panelWorkUnit.add(sep, gbcSep);

		//MFGN
		GridBagConstraints gbcMfgn = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcMfgn.gridx = 0;
		gbcMfgn.gridy = 3;
		JLabel lblMfgn = new JLabel("MFGN:");
		lblMfgn.setFont(PLAIN);
		panelWorkUnit.add(lblMfgn, gbcMfgn);

		GridBagConstraints gbcMfgnField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcMfgnField.gridx = 1;
		gbcMfgnField.gridy = 3;
		textFieldMfgn = new JTextField();
		textFieldMfgn.setEditable(false);
		textFieldMfgn.setFont(PLAIN);
		textFieldMfgn.setColumns(10);
		textFieldMfgn.setToolTipText("Will be updated after work unit is entered.");
		createPopUpMenu(textFieldMfgn);
		panelWorkUnit.add(textFieldMfgn, gbcMfgnField);

		//order no.
		GridBagConstraints gbcOrderNo = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcOrderNo.gridx = 2;
		gbcOrderNo.gridy = 3;
		JLabel lblOrderNo = new JLabel("Order No:");
		lblOrderNo.setFont(PLAIN);
		panelWorkUnit.add(lblOrderNo, gbcOrderNo);

		GridBagConstraints gbcOrderField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcOrderField.gridx = 3;
		gbcOrderField.gridy = 3;
		textFieldOrder = new JTextField();
		textFieldOrder.setEditable(false);
		textFieldOrder.setFont(PLAIN);
		textFieldOrder.setColumns(10);
		textFieldOrder.setToolTipText("Will be updated after work unit is entered.");
		createPopUpMenu(textFieldOrder);
		panelWorkUnit.add(textFieldOrder, gbcOrderField);

		//serial no.
		GridBagConstraints gbcSerNo = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcSerNo.gridx = 4;
		gbcSerNo.gridy = 3;
		JLabel lblSerNo = new JLabel("Serial No:");
		lblSerNo.setFont(PLAIN);
		panelWorkUnit.add(lblSerNo, gbcSerNo);

		GridBagConstraints gbcSerField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcSerField.gridx = 5;
		gbcSerField.gridy = 3;
		textFieldSer = new JTextField();
		textFieldSer.setEditable(false);
		textFieldSer.setFont(PLAIN);
		textFieldSer.setColumns(10);
		textFieldSer.setToolTipText("Will be updated after work unit is entered.");
		createPopUpMenu(textFieldSer);
		panelWorkUnit.add(textFieldSer, gbcSerField);

		//product ID
		GridBagConstraints gbcProd = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcProd.gridx = 0;
		gbcProd.gridy = 5;
		JLabel lblProd = new JLabel("Product ID:");
		lblProd.setFont(PLAIN);
		panelWorkUnit.add(lblProd, gbcProd);

		GridBagConstraints gbcProdField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcProdField.gridx = 1;
		gbcProdField.gridy = 5;
		textFieldProd = new JTextField();
		textFieldProd.setEditable(false);
		textFieldProd.setFont(PLAIN);
		textFieldProd.setColumns(10);
		textFieldProd.setToolTipText("Will be updated after work unit is entered.");
		textFieldProd.setVisible(true);
		createPopUpMenu(textFieldProd);
		panelWorkUnit.add(textFieldProd, gbcProdField);

		comboProductIdList = new JComboBox<JComboItem>();
		comboProductIdList.setFont(PLAIN);
		comboProductIdList.setToolTipText("Select correct product ID.");
		comboProductIdList.setVisible(false);
		panelWorkUnit.add(comboProductIdList, gbcProdField);

		//model
		GridBagConstraints gbcModel = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcModel.gridx = 2;
		gbcModel.gridy = 5;
		JLabel lblModel = new JLabel("Model:");
		lblModel.setFont(PLAIN);
		panelWorkUnit.add(lblModel, gbcModel);

		GridBagConstraints gbcModelField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcModelField.gridx = 3;
		gbcModelField.gridy = 5;
		textFieldModel = new JTextField();
		textFieldModel.setEditable(false);
		textFieldModel.setFont(PLAIN);
		textFieldModel.setColumns(10);
		textFieldModel.setToolTipText("Will be updated after work unit is entered.");
		textFieldModel.setVisible(true);
		createPopUpMenu(textFieldModel);
		panelWorkUnit.add(textFieldModel, gbcModelField);

		comboModelList = new JComboBox<JComboItem>();
		comboModelList.setFont(PLAIN);
		comboModelList.setToolTipText("Select correct model.");
		comboModelList.setVisible(false);
		panelWorkUnit.add(comboModelList, gbcModelField);

		//node count
		GridBagConstraints gbcNodeCnt = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcNodeCnt.gridx = 4;
		gbcNodeCnt.gridy = 5;
		JLabel lblNodeCnt = new JLabel("Node Count:");
		lblNodeCnt.setFont(PLAIN);
		panelWorkUnit.add(lblNodeCnt, gbcNodeCnt);

		GridBagConstraints gbcNodeCntField = JGridConstraint.getDefaultObjectGbc(panels.WORKUNIT);
		gbcNodeCntField.gridx = 5;
		gbcNodeCntField.gridy = 5;
		textFieldNodeCnt = new JTextField();
		textFieldNodeCnt.setEditable(false);
		textFieldNodeCnt.setFont(PLAIN);
		textFieldNodeCnt.setColumns(10);
		textFieldNodeCnt.setToolTipText("Will be updated after work unit is entered.");
		createPopUpMenu(textFieldNodeCnt);
		panelWorkUnit.add(textFieldNodeCnt, gbcNodeCntField);

		//create action listeners
		comboProductIdList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (comboProductIdList.getItemCount() > 0) {
					JComboItem item = (JComboItem)comboProductIdList.getSelectedItem();
					String szProdId = item.getDesc();
					comboModelList.removeAllItems();
					getModelList(comboModelList, szProdId);
				}				
			}
		});

		rdbtnPrime.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tstLogs.printInfo("Prime Selected");
				rdbtnPrime.setSelected(true);
				rdbtnHybrid.setSelected(false);
				bIsHybrid = !ISHYBRID;

				//for prime, work unit needs to be entered
				textFieldWorkUnit.setEditable(true);
				textFieldWorkUnit.setText("");

				//for prime, entries will be taken from MFS
				textFieldProd.setVisible(true);
				comboProductIdList.setVisible(false);
				textFieldModel.setVisible(true);
				comboModelList.setVisible(false);
				textFieldMfgn.setText("");
				textFieldOrder.setText("");
				textFieldSer.setText("");
				textFieldProd.setText("");
				textFieldModel.setText("");
			}
		});

		rdbtnHybrid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rdbtnHybrid.setSelected(true);
				rdbtnPrime.setSelected(false);
				bIsHybrid = ISHYBRID;

				tstLogs.printInfo("Hybrid Selected");
				tstLogs.displayInfoPopUpMessage("Enter order details manually since POK Hybrid are not in MFS.\nNote that WU will be set to \"DUMMY\"");
				
				//for hybrid, work unit is always set to DUMMY
				textFieldWorkUnit.setText(DUMMY);
				textFieldWorkUnit.setEditable(false);
				textFieldMfgn.setText("0123456");
				textFieldOrder.setText("123456");
				textFieldSer.setText("12345");

				//for hybrid, some entries will be entered manually
				textFieldProd.setVisible(false);
				comboProductIdList.setVisible(true);
				textFieldModel.setVisible(false);
				comboModelList.setVisible(true);

				comboProductIdList.removeAllItems();
				comboModelList.removeAllItems();
				insertAllProdId(comboProductIdList);
			}
		});

		ActionListener submit = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				initTestCasePanel();
				tabbedPane.setSelectedIndex(0);
				tstLogs.printInfo("=========== PROCESS START ===========");

				//check if all fields are filled up
				String szWorkUnitNo = textFieldWorkUnit.getText().toUpperCase().trim();
				String szDrainStation = textFieldDrainSt.getText().toLowerCase().trim();
				if (szWorkUnitNo.isEmpty() || szDrainStation.isEmpty()) {
					tstLogs.displayErrPopUpMessage("Please fill up all fields.");
					return;
				}

				//if work unit starts with 'W', remove the 'W' character
				if (szWorkUnitNo.charAt(0) == 'W') {
					szWorkUnitNo = szWorkUnitNo.substring(1);
				}

				//process work unit
				if ((!szWorkUnitNo.contains(DUMMY)) && ((szWorkUnitNo.length() != MINWORKUNITLEN) || szWorkUnitNo.matches("[^a-zA-Z0-9]"))) {
					tstLogs.displayErrPopUpMessage("Please enter a valid work unit number");	
					return;
				}

				//process drain station
				if (szDrainStation.length() <= MINDRAINSTLEN) {
					tstLogs.displayErrPopUpMessage("Please enter a valid drain station name");
					return;
				}
				textFieldWorkUnit.setText(szWorkUnitNo);
				textFieldDrainSt.setText(szDrainStation);

				//for hybrid check if all fields are not empty
				if (bIsHybrid) {
					String szMfgn  = textFieldMfgn.getText().toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim();
					String szOrno  = textFieldOrder.getText().toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim();
					String szSerNo = textFieldSer.getText().toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim();

					JComboItem item = (JComboItem)comboProductIdList.getSelectedItem();
					String szProd  = item.getDesc().toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim();
					item = (JComboItem)comboModelList.getSelectedItem();
					String szModel = item.getDesc().toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim();

					if (szProd.isEmpty() || szModel.isEmpty() || (szProd.length() != MINPRODLNLEN)) {
						tstLogs.displayErrPopUpMessage("Invalid product ID. Please try again.");
						return;
					}
					szWorkUnitNo = szWorkUnitNo + " " + szProd + " " + szModel + " " + szSerNo + " " + szOrno + " " + szMfgn;
				}

				//perform login
				if (performLogin() != tstLogs.PASS) {
					displayTxt("<html><font size=5 color=\"red\">Login Failed</font></html>");
					tstLogs.displayErrPopUpMessage("Failed to login.\nPlease try again.");
					return;
				}
				if (performClaimIn(szWorkUnitNo) != tstLogs.PASS) {
					displayTxt("<html><font size=5 color=\"red\"><center>Failed to claim in WU " + szWorkUnitNo.split(" ")[0] +
							   ".<br><br>Please check if all info are correct and try again." +
							   "<br><br>If error persists, please inform ME team.</center></font></html>");
					tstLogs.displayErrPopUpMessage("Failed to claim in WU " + szWorkUnitNo.split(" ")[0] + ".\nPlease check if all info are correct.");
					return;
				}

				//check if order is a reapply
				JQuery qry = new JQuery(textFieldMfgn.getText(), tstLogs);
				tstLogs.printInfo("Performing reapply query.");
				if (qry.perfReapplyQry() != tstLogs.PASS) {
					String szMsg = "Failed to perform query on QRYPROD. Please inform ME!";
					displayTxt(szMsg);
					tstLogs.printErr(szMsg);
					tstLogs.displayErrPopUpMessage(szMsg);
					performSuspend();
					return;
				} else if (qry.isReapply()) {
					String szMsg = "There are components detected for pending removal at OP 0807. Please route MFS back to op 0807 to process the removal.";
					displayTxt(szMsg);
					tstLogs.printErr(szMsg);
					tstLogs.displayErrPopUpMessage("There are components detected for pending removal at OP 0807.\n"
							+ "Please log in to MFS client and process the removal first. Once done, return to this application.");
					performSuspend();
					return;
				}
				tstLogs.printInfo("Reapply query done.");

				//disable all clickables
				textFieldWorkUnit.setEditable(false);
				textFieldDrainSt.setEditable(false);
				rdbtnHybrid.setEnabled(false);
				rdbtnPrime.setEnabled(false);
				btnEnter.setVisible(false);
				prepareDrainRunPanels();
				if (bIsHybrid) {
					comboProductIdList.setEnabled(false);
					comboModelList.setEnabled(false);
				}

				//initialize test case
				if (initTestCase() != tstLogs.PASS) {
					tstLogs.displayErrPopUpMessage("Failed to start test.\nPlease reinitialize the work station or inform MEQ team.");
					performSuspend();
					initDrainPanel();
					return;
				}
				setRunningStartPanel();
				pack();
			}
		};
		btnEnter.addActionListener(submit);
	} /** createWorkUnitPanel - END */

	private JTabbedPane tabbedPane;
	/** Create tabbed panel */
	private void createTabbedPanel() {
		//create tabbed panel
		GridBagConstraints gbcTab = JGridConstraint.getDefaultPanelGbc();
		tabbedPane = new JTabbedPane();
		tabbedPane.setFont(PLAIN);
		panelDrain.add(tabbedPane, gbcTab);

		createTestCasePanel();
		createTestLogsPanel();
	} /** createTabbedPanel - END */

	/** Initialize test case panel */
	private void initTestCasePanel() {
		//remove all images/text from panelImg
		panelImg.removeAll();
		panelImg.repaint();
		//draw IBM logo
		displayLogo(IBMIMG);

		initializeNextPanel();
		panelNext.setVisible(false);
	} /** initTestCasePanel - END */

	private JPanel panelTestCase;
	/** Create test case tab where all test cases will be displayed */
	private void createTestCasePanel() {
		//create test case tab
		GridBagLayout gblTestCaseTab = new GridBagLayout();
		gblTestCaseTab.rowHeights = new int[]{400, 30};
		gblTestCaseTab.rowWeights = new double[]{3, 0.2};
		gblTestCaseTab.columnWidths = new int[]{500};
		gblTestCaseTab.columnWeights = new double[]{1};
		GridBagConstraints gbcTestCaseTab = JGridConstraint.getDefaultPanelGbc();
		panelTestCase = new JPanel();
		panelTestCase.setName("Test Cases");
		panelTestCase.setLayout(gblTestCaseTab);
		panelTestCase.setVisible(false);
		tabbedPane.add(panelTestCase, gbcTestCaseTab);

		drawTestCaseImgPanel();
		drawTestCaseNextPanel();
	} /** createTestCaseTab - END */

	private JPanel panelTestLogs;
	private JTextField textFieldFind;
	private JTextPane textPaneLogs;
	/** Create test logs tab where all test logs will be displayed */
	private void createTestLogsPanel() {
		//create test case tab
		GridBagLayout gblTestLogsTab = new GridBagLayout();
		gblTestLogsTab.columnWeights = new double[]{1.0};
		gblTestLogsTab.rowWeights = new double[]{0, 1.0};
		GridBagConstraints gbcTestCaseTab = JGridConstraint.getDefaultPanelGbc();
		panelTestLogs = new JPanel();
		panelTestLogs.setName("Test Logs");
		panelTestLogs.setLayout(gblTestLogsTab);
		tabbedPane.add(panelTestLogs, gbcTestCaseTab);

		// create tool panel
		GridBagLayout gblTool = new GridBagLayout();
		gblTool.columnWidths = new int[]{0, 80, 80, 100};
		gblTool.columnWeights = new double[]{0, 1.0, 0.1};
		GridBagConstraints gbcTools = JGridConstraint.getDefaultPanelGbc();
		JPanel panelTools = new JPanel();
		panelTools.setLayout(gblTool);
		panelTestLogs.add(panelTools, gbcTools);

		GridBagConstraints gbcLabel = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		JLabel lblLogs = new JLabel("Tool Info and Error Logs");
		lblLogs.setFont(BOLD);
		panelTools.add(lblLogs, gbcLabel);
	
		GridBagConstraints gbcFindRdbtn = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbcFindRdbtn.gridy = 1;
		JLabel lblFind = new JLabel("Enter Word To Find");
		lblFind.setFont(PLAIN);
		panelTools.add(lblFind, gbcFindRdbtn);

		GridBagConstraints gbcFindTextField = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbcFindTextField.gridx = 1;
		gbcFindTextField.gridy = 1;
		gbcFindTextField.gridwidth = 1;
		textFieldFind = new JTextField();
		textFieldFind.setFont(PLAIN);
		createPopUpMenu(textFieldFind);
		panelTools.add(textFieldFind, gbcFindTextField);

		GridBagConstraints gbcFindNextBtn = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbcFindNextBtn.gridx = 2;
		gbcFindNextBtn.gridy = 1;
		final JButton btnFindNext = new JButton("Next");
		btnFindNext.setFont(PLAIN);
		panelTools.add(btnFindNext, gbcFindNextBtn);

		// create text area
		GridBagLayout gblTextArea = new GridBagLayout();
		gblTextArea.rowWeights = new double[]{1.0};
		gblTextArea.columnWeights = new double[]{1.0};
		GridBagConstraints gbcArea = JGridConstraint.getDefaultPanelGbc();
		JPanel panelLogsTextArea = new JPanel();
		panelLogsTextArea.setLayout(gblTextArea);
		panelTestLogs.add(panelLogsTextArea, gbcArea);

		textPaneLogs = new JTextPane();
		textPaneLogs.setFont(logFont);
		textPaneLogs.setEditable(false);
		createPopUpMenu(textPaneLogs);

		GridBagConstraints gbcScroll = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		JScrollPane scrollPane = new JScrollPane(textPaneLogs);
		Dimension d = scrollPane.getPreferredSize();
		scrollPane.setPreferredSize(d);
		panelLogsTextArea.add(scrollPane, gbcScroll);

		//set text pane for the logs
		tstLogs.setTextPane(textPaneLogs);

		//create action listeners
		textFieldFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findSearchWord(textPaneLogs, textFieldFind, btnFindNext);
			}
		});
		btnFindNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				findSearchWord(textPaneLogs, textFieldFind, btnFindNext);
			}
		});
	} /** createTestLogsPanel - END */

	/** Inserts all product ID and adds in the given combo box
	 *  Input: szProdLnDir - product line directory
	 */
	private static void insertAllProdId(JComboBox<JComboItem> comboProductIdList) {
		//get all the directory starting in '2' in the scripts folder
		File dir = new File(szCodeLoc);
		File[] files = dir.listFiles();
		if (files.length == 0) {
		    System.out.println("The directory is empty.");
		} else {
		    for (File aFile : files) {
		    	if (aFile.isDirectory() && (aFile.getName().startsWith("2"))) {
		    		comboProductIdList.addItem(new JComboItem(aFile.getName()));
		    	}
		    }
		}
	} /** insertAllProdId - END */

	/** Initialize next panel
	 *  Input: szProdLnDir - product line directory */
	private void initializeTestCaseImgPanel(String szProdLnDir) {
		//if the product line folder has an intro message (usually in 0.PNG), display
		String szIntroFile = szProdLnDir + szSeparatr + INTROFILE;
		if (new File(szIntroFile).exists()) {
			displayImg(szIntroFile);
		} else {
			displayTxt("Starting System Drain Process ");
		}
		panelImg.setVisible(true);
	} /** initializeTestCaseImgPanel - END */

	JPanel panelImg;
	/** Draws image panel */
	private void drawTestCaseImgPanel() {
		GridBagLayout gblImg = new GridBagLayout();
		GridBagConstraints gbcImg = JGridConstraint.getDefaultPanelGbc();
		gbcImg.gridx = 0;
		gbcImg.gridy = 0;
		panelImg = new JPanel();
		panelImg.setLayout(gblImg);
		panelTestCase.add(panelImg, gbcImg);
	} /** drawTestCaseImgPanel - END */

	/** Initialize next panel */
	private void initializeNextPanel() {
		//timer
		lblTimer.setVisible(false);
		lblUpdateTimer.setVisible(false);
		btnStartTimer.setVisible(false);
		
		//reading
		lblSubmit.setVisible(false);
		textFieldReading.setVisible(false);
		btnSubmitVal.setVisible(false);

		//finish button
		btnFinish.setVisible(false);

		//next button
		btnNextOp.setEnabled(true);
		btnNextOp.setVisible(true);
		panelNext.setVisible(true);
	} /** initializeNextPanel - END */

	/** Setup start timer buttons
	 *  Input: Number of minutes to countdown */
	private void setupStartTimer(double dMinutes) {
		lblTimer.setVisible(true);

		int iSeconds = (int)Math.round(60 * dMinutes); //convert to seconds
		final long lMilliSeconds = 1000 * iSeconds;	// convert to milliseconds
		tstCtl.setTimer(lMilliSeconds);
		TIMERFORMAT.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
		lblUpdateTimer.setText(TIMERFORMAT.format(new Date(lMilliSeconds)));
		lblUpdateTimer.setForeground(Color.BLACK);
		lblUpdateTimer.setFont(PLAIN);
		lblUpdateTimer.setVisible(true);
		btnStartTimer.setVisible(true);
		btnStartTimer.setEnabled(true);

		//reading
		lblSubmit.setVisible(false);
		textFieldReading.setVisible(false);
		btnSubmitVal.setVisible(false);

		btnNextOp.setEnabled(false);
	} /** setupStartTimer - END */

	/** Setup value reading buttons
	 *  Input: dReadingNeeded - reading to be fulfilled */
	private void setupValueReading(double dReadingNeeded) {
		//timer
		lblTimer.setVisible(false);
		lblUpdateTimer.setVisible(false);
		btnStartTimer.setVisible(false);
		
		//reading
		tstCtl.setRecommendedReading(dReadingNeeded);
		lblSubmit.setVisible(true);
		textFieldReading.setText("");
		textFieldReading.setVisible(true);
		textFieldReading.setEditable(true);
		btnSubmitVal.setVisible(true);
		btnSubmitVal.setEnabled(true);

		btnNextOp.setEnabled(false);
	} /** setupValueReading - END */

	/** Setup buttons for ending drain process */
	private void setupEndProcess() {
		btnNextOp.setVisible(false);
		btnFinish.setEnabled(true);
		btnFinish.setVisible(true);
	} /** setupEndProcess - END */

	JPanel panelNext;
	JLabel lblTimer;
	JLabel lblUpdateTimer;
	JLabel lblSubmit;
	JTextField textFieldReading;
	JButton btnNextOp;
	JButton btnStartTimer;
	JButton btnSubmitVal;
	JButton btnFinish;
	/** Draws panel for operator buttons */
	private void drawTestCaseNextPanel() {
		GridBagLayout gblNxt = new GridBagLayout();
		gblNxt.rowHeights = new int[]{28};
		gblNxt.columnWidths = new int[]{90, 100, 90, 100, 90, 90};
		gblNxt.columnWeights = new double[]{0.01, 0.2, 0.01, 0.2, 0.02, 0.02};
		GridBagConstraints gbcNxt = JGridConstraint.getDefaultPanelGbc();
		gbcNxt.gridx = 0;
		gbcNxt.gridy = 1;
		panelNext = new JPanel();
		panelNext.setVisible(false);
		panelNext.setLayout(gblNxt);
		panelNext.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelTestCase.add(panelNext, gbcNxt);

		//label for timer
		GridBagConstraints gbc = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbc.gridx = 2;
		lblTimer = new JLabel("Timer: ", JLabel.RIGHT);
		lblTimer.setVisible(false);
		lblTimer.setFont(PLAIN);
		panelNext.add(lblTimer, gbc);

		//updatable label for timer
		gbc.gridx = 3;
		lblUpdateTimer = new JLabel("MM:SS", JLabel.CENTER);
		lblUpdateTimer.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		lblUpdateTimer.setVisible(false);
		lblUpdateTimer.setFont(PLAIN);
		panelNext.add(lblUpdateTimer, gbc);

		//label for value submission
		gbc.gridx = 2;
		lblSubmit = new JLabel("Enter reading here: ", JLabel.RIGHT);
		lblSubmit.setVisible(false);
		lblSubmit.setFont(PLAIN);
		panelNext.add(lblSubmit, gbc);

		//text field for reading
		gbc.gridx = 3;
		textFieldReading = new JTextField();
		textFieldReading.setVisible(false);
		textFieldReading.setFont(PLAIN);
		textFieldReading.setColumns(20);
		textFieldReading.setToolTipText("Enter reading here and press submit.");
		panelNext.add(textFieldReading, gbc);

		//start timer button
		gbc.gridx = 4;
		btnStartTimer = new JButton("Start Timer");
		btnStartTimer.setVisible(false);
		btnStartTimer.setFont(PLAIN);
		panelNext.add(btnStartTimer, gbc);

		//submit value button
		btnSubmitVal = new JButton("Submit");
		btnSubmitVal.setVisible(false);
		btnSubmitVal.setFont(PLAIN);
		panelNext.add(btnSubmitVal, gbc);

		//next button
		gbc.gridx = 5;
		btnNextOp = new JButton("Next");
		btnNextOp.setFont(PLAIN);
		panelNext.add(btnNextOp, gbc);

		//finish button
		btnFinish = new JButton("End Job");
		btnFinish.setVisible(false);
		btnFinish.setFont(PLAIN);
		panelNext.add(btnFinish, gbc);

		//create action listeners
		btnNextOp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				initializeNextPanel();
				if (tstCtl.isNewNode()) {
					displayTxt("<html><font size=6>" +
								"Please perform the next operation for Node " + tstCtl.getCurrentNodeCnt() +
								"</font></html>");
					tstCtl.resetNewNodeFlag();
					return;
				}

				//if there is a previous step, perform step end
				if (tstCtl.getOpCnt() > 0) {
					performStepEnd();
				}

				//check current OP
				int iCurrentOp = tstCtl.addOpCnt();
				if (iCurrentOp > tstCtl.getMaxOpCnt()) {
					setupEndProcess();
					return;
				}

				//get test case details
				JStepInfoLink currentStep = null;
				Iterator<JStepInfoLink> iter = tstCtl.qlinkSteps.iterator();
				while (iter.hasNext()) {
					currentStep = iter.next();
					if (currentStep.iOpNum == iCurrentOp) {
						//check if op is only performed for certain number of nodes
						if ((currentStep.iOpMaxNode != 0) && (tstCtl.getMaxNodeCnt() > currentStep.iOpMaxNode)) {
							iCurrentOp = tstCtl.addOpCnt();
							continue;
						}

						prepareOpPanels(currentStep);
						break;
					}
				}

				if (drawTestCase(currentStep.iOpNum) != tstLogs.PASS) {
					tstLogs.displayErrPopUpMessage("Failed to find test case.\nPlease reinitialize the work station or inform MEQ team.");
					if (bIsClaimedIn) {
						displayTxt("Performing Suspend.");
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.\nPlease inform ME team.");
						}
					}

					initDrainPanel();
					pack();
					return;
				}
				tstLogs.printInfo("Running step " + currentStep.iOpNum + " for node " + tstCtl.getCurrentNodeCnt());
				performStepStart();

				//prepare next op and/or node
				//check if there is a repeating step for every node
				if (currentStep.iNextNodeStep > 0) {
					if (tstCtl.getCurrentNodeCnt() < tstCtl.getMaxNodeCnt()) {
						tstCtl.addNodeCnt();
						tstCtl.setOpCnt(currentStep.iNextNodeStep - 1); //subtract '1' since pressing the next button will increment the step count
					} else {
						//restart node count if max node count is reached
						tstCtl.restartNodeCnt();
						tstCtl.setOpCnt(currentStep.iNextSameNodeStep - 1);  //subtract '1' since pressing the next button will increment the step count
					}
				}
			}
		});

		btnStartTimer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnStartTimer.setEnabled(false);

				//get timer info
				final long lMilliSeconds = 1000;//CGGDBG
				//final long lMilliSeconds = tstCtl.getTimer();

				final String szStartTime = TIMERFORMAT.format(new Date(lMilliSeconds));
				tstLogs.printInfo("Starting countdown of " + szStartTime);
				lblUpdateTimer.setText(szStartTime);
				new Timer(1000, new ActionListener() {
					long lLeftTime = lMilliSeconds - 1000;
					public void actionPerformed(ActionEvent e) {
						if (lLeftTime >= 0) {
							if (lLeftTime <= WARNINGTIME) {
								//if 10 seconds, change color to warning time
								lblUpdateTimer.setFont(BOLD);
								lblUpdateTimer.setForeground(Color.ORANGE);
							} else {
								lblUpdateTimer.setFont(PLAIN);
								lblUpdateTimer.setForeground(Color.BLACK);
							}
							lblUpdateTimer.setText(TIMERFORMAT.format(new Date(lLeftTime)));
							lLeftTime -= 1000;
						} else {
							tstLogs.printInfo("Countdown reached 00 : 00 which started from " + szStartTime);
							((Timer) e.getSource()).stop();
							lblUpdateTimer.setFont(BOLD);
							lblUpdateTimer.setForeground(Color.RED);

							//store info
							performStepData("TimerFin:" + szStartTime);
							tstLogs.displayInfoPopUpMessage(szStartTime + " (MM:SS) \nCountdown Timer done.\nPlease proceed to next step.");
							btnNextOp.setEnabled(true);
						}
					}
				}).start();
			}
		});

		btnSubmitVal.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String szuserEntry = textFieldReading.getText();
				tstLogs.printInfo("User entered reading of " + szuserEntry);
				if ((szuserEntry.isEmpty()) || (!szuserEntry.matches("[0-9]*\\.?[0-9]*"))) {
					tstLogs.displayInfoPopUpMessage("Please submit a correct value.");
					return;
				}
				textFieldReading.setEditable(false);
				btnSubmitVal.setEnabled(false);

				//get value in reading text field
				double dUserEntry = Double.parseDouble(szuserEntry);
				performStepData("UserReading:" + dUserEntry);

				double dRecommendedReading = tstCtl.getRecommendedReading();
				if (dUserEntry < dRecommendedReading) {
					String szMsg = "Reading did not reach recommended reading of " + dRecommendedReading;
					tstLogs.printErr(szMsg);
					tstLogs.displayErrPopUpMessage(szMsg + "\nPlease abort to suspend operation.");

					displayTxt("<html><font size=6 color=\"red\">" +
							   "Reading did not reach recommended reading of " + dRecommendedReading + "." +
							   "<br><br>" +
							   "Please inform ME/MEQ Team!" +
							   "</font></html>");
					return;
				}

				tstLogs.printInfo("Reading is valid.");
				tstLogs.displayInfoPopUpMessage("Reading of " + dUserEntry + " recorded.\nPlease proceed to next step.");
				btnNextOp.setEnabled(true);
			}
		});

		btnFinish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnFinish.setEnabled(false);
				displayTxt("Drain Operation Complete.");
				tstLogs.displayInfoPopUpMessage("Drain Operation complete.\nSystem will be claimed out.");
				tstLogs.printInfo("Drain process complete.");

				//perform claim out
				if (performClaimOut() != tstLogs.PASS) {
					tstLogs.printErr("Failed to claim out WU.\nPlease inform ME/MEQ Team.");
				}

				initDrainPanel();
				pack();
			}
		});
	} /** drawTestCaseNextPanel - END */
	
	/** Creates popup menu
	 *  Input : Component where popup menu will be added
	 **/
	private static void createPopUpMenu(JComponent component) {
		final JPopupMenu popup = new JPopupMenu();
		popup.setFont(PLAIN);

		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(JControlCmdDef.CTRLC));
		popup.add(copy);			

		if (component instanceof JTextField) {
			Action cut = new DefaultEditorKit.CutAction();
			cut.putValue(Action.NAME, "Cut");
			cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(JControlCmdDef.CTRLX));
			popup.add(cut);
	
			Action paste = new DefaultEditorKit.PasteAction();
			paste.putValue(Action.NAME, "Paste");
			paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(JControlCmdDef.CTRLV));
			popup.add(paste);
		} else {
			popup.addSeparator();
			popup.add(new JSelectAction());
		}

		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showMenu(e);
				}
			}
			private void showMenu(MouseEvent e) {
				Component component = e.getComponent();
				component.requestFocusInWindow();
				if (component.isEnabled()) {
					popup.show(component, e.getX(), e.getY());
				}
			}
		});
	} /** createPopUpMenu - END */
} // JDrain class - END

/*** Class used for get the default grid constraints
  **/
class JGridConstraint {
	/** Constants */
	private final static Insets DEFAULTINSETS = new Insets(2, 5, 2, 5);

	/** Returns default gbc for panels
	 *  Output: GridBagConstraints
	 */
	public static GridBagConstraints getDefaultPanelGbc() {
		GridBagConstraints gbcPanel = new GridBagConstraints();
		gbcPanel.fill = GridBagConstraints.BOTH;
		gbcPanel.gridwidth = 0;
		gbcPanel.gridheight = 1;
		gbcPanel.insets = DEFAULTINSETS;
		return gbcPanel;
	} /** getDefaultPanelGbc - END */

	/** Returns default gbc for panel objects
	 *  Output: GridBagConstraints
	 */
	public static GridBagConstraints getDefaultObjectGbc(JDrain.panels panel) {
		GridBagConstraints gbc = new GridBagConstraints();
		//gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = DEFAULTINSETS;
		switch (panel) {
		case LOGIN:
			gbc.gridy = GridBagConstraints.BOTH;
			break;
		case START:
			gbc.gridy = GridBagConstraints.RELATIVE;
			break;
		case WORKUNIT:
			gbc.gridy = GridBagConstraints.RELATIVE;
			break;
		case ARCHLIST:
			gbc.gridy = GridBagConstraints.REMAINDER;
			break;
		default:
			gbc.gridy = 0;
			break;
		} // switch - END
		return gbc;
	}
} // JGridConstraint class - END

/*** Contains all the variables for test case control
 **/
class JTestCaseControl {
	/** Global variables */
	public ConcurrentLinkedQueue<JStepInfoLink> qlinkSteps;

	private static int iCurrentNode;
	private static int iMaxNodeCnt;
	private static int iCurrentOp;
	private static int iMaxOpCnt;
	private static long lCurrTimerMilliSeconds;
	private static double dRecommendedReading;
	private static String szTstFileLoc;
	private static boolean bIsNewNode;

	/** JTestCaseControl constructor
	 *  Input: szTstCaseLoc - location of test cases
	 *		 iMaxNode	 - max node
	 **/
	public JTestCaseControl(String szTstCaseLoc, int iMaxNode) {
		iCurrentNode = 1; // always start at 1
		iMaxNodeCnt = iMaxNode;
		iCurrentOp  = 0;
		iMaxOpCnt   = 0;
		lCurrTimerMilliSeconds = 0;
		dRecommendedReading	= 0;
		szTstFileLoc = szTstCaseLoc;
		bIsNewNode   = false;
		qlinkSteps = new ConcurrentLinkedQueue<JStepInfoLink>();
	} /** JTestCaseControl - END */

	/** Sets the max op count
	 *  Input: max op count
	 **/
	public void setMaxOpCnt(int iMaxOp) {
		iMaxOpCnt = iMaxOp;
	} /** setMaxOpCnt - END */

	/** Check is a new node will be restarting the test */
	public boolean isNewNode() {
		return bIsNewNode;
	} /** isNewNode - END */

	/** Restart new node flag */
	public void resetNewNodeFlag() {
		bIsNewNode = false;
	} /** resetNewNodeFlag - END */

	/** Restart node count to 1 */
	public void restartNodeCnt() {
		iCurrentNode = 1;
	} /** restartNodeCnt - END */

	/** Increments the current node count */
	public int addNodeCnt() {
		bIsNewNode = true;
		return ++iCurrentNode;
	} /** addNodeCnt - END */

	/** Sets the timer
	 *  Input: step time to be set
	 **/
	public void setTimer(long lTime) {
		lCurrTimerMilliSeconds = lTime;
	} /** setTimer - END */

	/** Gets the time in timer
	 *  Output: step op to be set
	 **/
	public long getTimer() {
		return lCurrTimerMilliSeconds;
	} /** getTimer - END */

	/** Sets the recommended reading
	 *  Input: recommended reading
	 **/
	public void setRecommendedReading(double dReadingNeeded) {
		dRecommendedReading = dReadingNeeded;
	} /** setRecommendedReading - END */

	/** Gets the recommended reading
	 *  Output: recommended reading
	 **/
	public double getRecommendedReading() {
		return dRecommendedReading;
	} /** getRecommendedReading - END */

	/** Sets the current op count
	 *  Input: step op to be set
	 **/
	public void setOpCnt(int iNewOpCnt) {
		iCurrentOp = iNewOpCnt;
	} /** setOpCnt - END */

	/** Increments the current op count */
	public int addOpCnt() {
		return ++iCurrentOp;
	} /** addOpCnt - END */

	/** Returns the current node */
	public int getCurrentNodeCnt() {
		return iCurrentNode;
	} /** getCurrentNodeCnt - END */

	/** Returns the current op count */
	public int getOpCnt() {
		return iCurrentOp;
	} /** getOpCnt - END */

	/** Returns the max node count */
	public int getMaxNodeCnt() {
		return iMaxNodeCnt;
	} /** getMaxNodeCnt - END */

	/** Returns the max op count */
	public int getMaxOpCnt() {
		return iMaxOpCnt;
	} /** getMaxOpCnt - END */

	/** Returns the test file location */
	public String getTstFileLoc() {
		return szTstFileLoc;
	} /** getTstFileLoc - END */
} // JTestCaseControl - END

/*** Contains all the variables for a specific step
 **/
class JStepInfoLink {
	public int iOpNum;
	public int iOpMaxNode;
	public double dTimeChk;
	public double dUserEntry;
	public int iNextNodeStep;
	public int iNextSameNodeStep;

	/** JStepInfoLink constructor */
	public JStepInfoLink() {
		iOpNum = 0;            // always start at 1
		iOpMaxNode = 0;        // for special case where, an OP can only be performed for certain no. of nodes
		dTimeChk = 0.0;        // for ops with time countdown
		dUserEntry = 0.0;      // for ops that require user entry
		iNextNodeStep = 0;     // perform op num when there is an increment in node
		iNextSameNodeStep = 0; // perform next op num as per normal
	} /** JStepInfo - END */
} // JStepInfoLink - END

/*** Definitions of all control commands
  **/
class JControlCmdDef {
	/** Control commands */
	public final static String CTRLA = "control A"; // Select All
	public final static String CTRLB = "control B"; // Show Test Attributes
	public final static String CTRLC = "control C"; // Copy
	public final static String CTRLO = "control O"; // Open Incidents
	public final static String CTRLR = "control R"; // Refcode Check
	public final static String CTRLV = "control V"; // Paste
	public final static String CTRLX = "control X"; // Cut

	public final static String CTRL1 = "control 1"; // Small font
	public final static String CTRL2 = "control 2"; // Medium font
	public final static String CTRL3 = "control 3"; // Large font
} // JControlCmdDef - END

/*** Class used for select all function 
  **/
class JSelectAction extends TextAction {
	/** JSelectAction constructor */
	JSelectAction() {
		super(DefaultEditorKit.selectAllAction);
		super.putValue(NAME, "Select All");
		super.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(JControlCmdDef.CTRLA));
	} /** JSelectAction - END */

	/** Function called when action is received in the given component
	 *  Input: action event
	 */
	public void actionPerformed(ActionEvent e) {
		getFocusedComponent().selectAll();
	} /** actionPerformed - END */
} // JSelectAction - END 

/*** Class used for combo box items 
 **/
class JComboItem {
   public String szDesc;

	/** JComboItem constructor
	 *  Input: szDesc - combo box item description
	 */
   public JComboItem(String szDesc) {
       this.szDesc = szDesc;
   } /** JComboItem - END */

	/** Used to overwrite toString method that is used to know what text to display in combo box
	 *  Output: szDesc - string that describes what the link is about
	 */
   @Override
   public String toString() {
       return szDesc;
   } /** toString - END */

	/** Get the description of the link data
	 *  Output: the description to be displayed by a renderer
	 */
    public String getDesc() {
    	return szDesc;
    } /** getDesc - END */
} // JComboItem - END