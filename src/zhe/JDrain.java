/*** JDrain.java
 *   Main file for JDrain app
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
	private static String VERSION = "v1.20";
	private static String DUMMY = "DUMMY";
	private static int MINWORKUNITLEN = 8;
	private static Font TITLE  = new Font("Tahoma", Font.BOLD, 14);
	private static Font TSTLBL = new Font("Tahoma", Font.PLAIN, 8);
	private static Font PLAIN  = new Font("Tahoma", Font.PLAIN, 11);
	private static Font BOLD = new Font("Tahoma", Font.BOLD, 11);
	private static boolean CLAIMEDIN = true;
	private static boolean ISHYBRID  = true;

	/** Command list */
	private static String DEFAULTUSER = "reefuser";
	private static String DEFAULTSRVR = "sgad06e0";
	private static String DEFAULTPC   = "drain01";
	private static String DRAINCMD	= "/bin/bash /home/gem/scripts/drainHE.sh " + DEFAULTUSER + " " + DEFAULTSRVR;
	private static String LOGINCMD	= " val_user";
	private static String CLAIMINCMD  = " claim_in";
	private static String CLAIMOUTCMD = " claim_out";
	private static String SUSPEND	 = " suspend";
	private static String STEPSTART   = " stepStart";
	private static String STEPDATA	= " stepData";
	private static String STEPEND	 = " stepEnd";

	/** Files to read */
	private static String SEPARATR = "\\"; //CGG
	//private static String CODELOC  = "/home/gem/scripts/";
	private static String CODELOC  = "C:\\";
	private static String TESTSTAT = CODELOC + "claimedWU/teststat";
	private static String INTROFILE= "0.PNG";

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
	private static String szHostName;
	private static boolean bIsClaimedIn;
	private static boolean bIsHybrid;
	private static JTestCaseControl tstCtl;
	private static JLogHandler tstLogs;

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

		//if (!isValidOS()) {
		//	JOptionPane.showMessageDialog(null, "Cannot run application in current PC", "Error", JOptionPane.ERROR_MESSAGE);
		//	System.exit(0);
		//}

		//set up logging file
		Date date = new Date();
		String szFileDate = new SimpleDateFormat("yyyyMMddHHmmss").format(date);
		tstLogs = new JLogHandler(CODELOC + szFileDate + ".TXT");

		//setup flags
		bIsClaimedIn = !CLAIMEDIN;
		bIsHybrid = !ISHYBRID;

		//set the host name
		//szHostName = System.getenv("HOSTNAME");
		//if (szHostName.isEmpty()) {
		//	szHostName = DEFAULTPC;
		//} else {
		//	szHostName = szHostName.replaceAll(".sg.ibm.com", "");
		//}

		createMenuBar();
		//createLoginPanel();
		createDrainPanel();
		prepareDrainRunPanels();
		pack();
	} /** JDrain - END */

	/** Checks if OS is valid
	 *  Output: true  - valid
	 *		  false - not valid
	 */
	private boolean isValidOS() {
		boolean bRet = false;
		//check if Linux
		String szOS = System.getProperty("os.name").toUpperCase().trim();
		if ((szOS.length() > 0) && (szOS.contains("LINUX")) &&
			(Double.parseDouble(System.getProperty("java.specification.version")) >= 1.7)) {
			bRet = true;
		}
		return bRet;
	} /** isValidOS - END */

	/** Perform login
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int performLogin() {
		//call script to login
		try {
			if (JScript.run(DRAINCMD + " " + szHostName + LOGINCMD, tstLogs) != 0) {
				return tstLogs.printErr("Loggin in failed");
			}
		} catch (IOException | InterruptedException ex) {
			return tstLogs.printErr("Exception encountred while logging in", ex);
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
		/*CGGtry {
			if (JScript.run(DRAINCMD + " " + szHostName + CLAIMINCMD + " " + szWorkUnit, tstLogs) != 0) {
				return tstLogs.printErr("Claim in for WU " + szWorkUnit + " failed");
			}
		} catch (IOException | InterruptedException ex) {
			return tstLogs.printErr("Exception encountred while claiming in", ex);
		}*/

		tstLogs.printInfo("Claim IN Successful.");
		bIsClaimedIn = CLAIMEDIN;
		if (bIsHybrid) {
			return updateNodeCnt();
		}
		return updateWorkUnitPanel();
	} /** performClaimIn - END */

	/** Perform claim out */
	private int performClaimOut() {
		if (bIsClaimedIn) {
			tstLogs.printInfo("Performing MFS Claim-OUT for WU " + textFieldWorkUnit.getText());
			//call script to claim-out
			/*CGGtry {
				if (JScript.run(DRAINCMD + " " + szHostName + CLAIMOUTCMD, tstLogs) != 0) {
					return tstLogs.printErr("Claim out failed");
				}
			} catch (IOException | InterruptedException ex) {
				return tstLogs.printErr("Exception encountred while claiming out", ex);
			}*/

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
			/*CGGtry {
				if (JScript.run(DRAINCMD + " " + szHostName + SUSPEND, tstLogs) != 0) {
					return tstLogs.printErr("Suspend failed");
				}
			} catch (IOException | InterruptedException ex) {
				return tstLogs.printErr("Exception encountred while doing suspend", ex);
			}*/

			tstLogs.printInfo("Process suspended.");
			tstLogs.displayInfoPopUpMessage("Process suspended.");
			bIsClaimedIn = !CLAIMEDIN;
		}
		return tstLogs.PASS;
	} /** performSuspend - END */

	/** Perform step start */
	private int performStepStart() {
		//call script to step start
		//try {
			String szStepName = "N" + tstCtl.getCurrentNodeCnt() + "S" + tstCtl.getOpCnt();
			tstLogs.printInfo("Performing Step Start " + szStepName);
			/*CGGif ((bIsClaimedIn) && (JScript.run(DRAINCMD + " " + szHostName + STEPSTART + " " + szStepName + " " + szStepName, tstLogs) != 0)) {
				return tstLogs.printErr("Step start failed");
			}*/
		//} catch (IOException | InterruptedException ex) {
		//	return tstLogs.printErr("Exception encountred while doing step start", ex);
		//}
		return tstLogs.PASS;
	} /** performStepStart - END */

	/** Perform step end */
	private static int performStepEnd() {
		tstLogs.printInfo("Performing Step End");
		//call script to step end
		/*CGGtry {
			if ((bIsClaimedIn) && (JScript.run(DRAINCMD + " " + szHostName + STEPEND, tstLogs) != 0)) {
				return tstLogs.printErr("Step End failed");
			}
		} catch (IOException | InterruptedException ex) {
			return tstLogs.printErr("Exception encountred while doing step end", ex);
		}*/
		return tstLogs.PASS;
	} /** performStepEnd - END */

	/** Perform step data
	 *  Input: Data to be entered for current step
	 **/
	private int performStepData(String szData) {
		String szTrimmedData = szData.replaceAll("\\s","");
		tstLogs.printInfo("Performing Step Data " + szTrimmedData);
		//call script to step data
		/*CGGtry {
			if ((bIsClaimedIn) && (JScript.run(DRAINCMD + " " + szHostName + STEPDATA + " " + szTrimmedData, tstLogs) != 0)) {
				return tstLogs.printErr("Step Data failed");
			}
		} catch (IOException | InterruptedException ex) {
			return tstLogs.printErr("Exception encountred while doing step data " + szData, ex);
		}*/
		return tstLogs.PASS;
	} /** performStepData - END */

	/** Initializes test cases
	 *  Output: error code - if error is encountered
	 *		  0 - pass
	 */
	private int initTestCase() {
		//get product line
		String szProdLn = textFieldProd.getText();
		tstLogs.printDbg("Init test cases for ProdLn = " + szProdLn);

		String szProdLnDir = CODELOC + szProdLn;
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
		String szXmlFile = szProdLnDir + SEPARATR + szProdLn + ".xml";
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
						if (eElement.getAttribute(MODEL).contains(textFieldModel.getText())) {
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
		String szOpImgToDisp = tstCtl.getTstFileLoc() + SEPARATR + iCurrOp + ".PNG";
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

		ImageIcon icon = new ImageIcon(szImg);
		GridBagConstraints gbc = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		JLabel lblTstNum = new JLabel(szImg.replace(tstCtl.getTstFileLoc() + SEPARATR, "").replace(".PNG", ""));
		lblTstNum.setFont(TSTLBL);
		panelImg.add(new JLabel(icon), gbc);
		panelImg.add(lblTstNum);
		panelImg.validate();
		panelImg.repaint();
		pack();
	} /** displayImg - END */

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
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.");
						}
					}
					System.exit(0);
				}
			}
		});

		mnitmAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tstLogs.displayInfoPopUpMessage("Drain Tool Version " + VERSION + ".");
			}
		});
	} /** createMenuBar - END */

	/*JPanel panelLogin;*/
	/** Create login panel */
	/*private void createLoginPanel() {
		//create login panel
		GridBagLayout gblLogin = new GridBagLayout();
		gblLogin.rowHeights = new int[]{150, 1, 1, 1, 150};
		gblLogin.rowWeights = new double[]{1, 0, 0.1, 0.1, 1};
		gblLogin.columnWidths = new int[]{150, 1, 150};
		gblLogin.columnWeights = new double[]{1, 0, 1};

		panelLogin = new JPanel();
		panelLogin.setLayout(gblLogin);
		panelLogin.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		contentPane.add(panelLogin);
		
		//write title
		GridBagConstraints gbcTitleLabel = JGridConstraint.getDefaultObjectGbc(panels.LOGIN);
		gbcTitleLabel.gridx = 1;
		gbcTitleLabel.gridy = 1;
		JLabel lblTitle = new JLabel("Drain WorkStation Tool");
		lblTitle.setFont(TITLE);
		panelLogin.add(lblTitle, gbcTitleLabel);

		GridBagConstraints gbcLoginBtn = JGridConstraint.getDefaultObjectGbc(panels.LOGIN);
		gbcLoginBtn.gridx = 1;
		gbcLoginBtn.gridy = 2;
		JButton btnLogin = new JButton("LOGIN");
		btnLogin.setFont(PLAIN);
		panelLogin.add(btnLogin, gbcLoginBtn);
		//add action listeners
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (performLogin() == tstLogs.PASS) {
					mnitmLogout.setEnabled(true);
					panelLogin.setVisible(false);
					initDrainPanel();
					pack();
				} else {
					tstLogs.displayErrPopUpMessage("Failed to login.\nPlease try again.");
				}
			}
		});

		GridBagConstraints gbcCancelBtn = JGridConstraint.getDefaultObjectGbc(panels.LOGIN);
		gbcCancelBtn.gridx = 1;
		gbcCancelBtn.gridy = 3;
		JButton btnLoginCancel = new JButton("CANCEL");
		btnLoginCancel.setFont(PLAIN);
		panelLogin.add(btnLoginCancel, gbcCancelBtn);
		//add action listeners
		btnLoginCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
	}*/ /** createLoginPanel - END */
	
	/** Initialize drain panel */
	private void initDrainPanel() {
		//initialize work unit panel
		initWorkUnitPanel();

		//initialize start panel
		initStartPanel();

		//initialize the test case panel
		initTestCasePanel();

		//display drain panel
		//panelDrain.setVisible(true);
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
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.\nPlease inform ME team.");
						}
					}

					initDrainPanel();
					//CGGpanelTestCase.setVisible(false);
					//CGGpanelStart.setVisible(false);
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
		String szProdLn = textFieldProd.getText();
		if (getTestCaseDetails(TAGNODE, CODELOC + szProdLn, szProdLn) != tstLogs.PASS) {
			return tstLogs.printErr("Failed to get node count.");
		}
		if (textFieldNodeCnt.getText().isEmpty()) {
			return tstLogs.ERROR;
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
			String szTmp = System.getProperty("user.dir") + SEPARATR + TESTSTAT;
			in = new BufferedReader(new FileReader(szTmp));
			while ((szTmp = in.readLine()) != null) {
				//all text are based on script
				if (szTmp.contains("(TSYNM ") || szTmp.contains("(ORDRNUM ") || szTmp.contains("(SN ") ||
					szTmp.contains("(PR_ID ") || szTmp.contains("(MODEL ")) {
					tstLogs.printDbg("Reading line " + szTmp);
					aszTmp = szTmp.toUpperCase().replaceAll("[^A-Z0-9_ ]", "").trim().split(" ");
					switch (aszTmp[0]) {
					case "TSYNM":
						textFieldMfgn.setText(aszTmp[1]);
						break;
					case "ORDRNUM":
						textFieldOrder.setText(aszTmp[1]);
						break;
					case "SN":
						textFieldSer.setText(aszTmp[1]);
						break;
					case "PR_ID":
						textFieldProd.setText(aszTmp[1]);
						break;
					case "MODEL":
						textFieldModel.setText(aszTmp[1]);
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
			return tstLogs.printErr("File not found.", ex);
		} catch (IOException ex) {
			return tstLogs.printErr("IOException while reading file.", ex);
		}

		if (textFieldMfgn.getText().isEmpty() ||
			textFieldOrder.getText().isEmpty() ||
			textFieldSer.getText().isEmpty() ||
			textFieldProd.getText().isEmpty() ||
			textFieldModel.getText().isEmpty()) {
			return tstLogs.printErr("Failed to get WU info");
		}
		return updateNodeCnt();
	} /** updateWorkUnitPanel - END */
	
	/** Initialize work unit panel */
	private void initWorkUnitPanel() {
		//setup top part
		textFieldWorkUnit.setText("");
		//CGGtextFieldWorkUnit.setEditable(true);
		btnEnter.setVisible(true);

		//bottom part
		textFieldMfgn.setText("");
		textFieldOrder.setText("");
		textFieldSer.setText("");
		textFieldProd.setText("");
		textFieldModel.setText("");
		textFieldNodeCnt.setText("");
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
		rdbtnHybrid = new JRadioButton("POK Hybrid");
		rdbtnHybrid.setFont(PLAIN);
		rdbtnHybrid.setToolTipText("Select if Machine is POK Hybrid. (NO MFS)");
		String szHostName = System.getenv("HOSTNAME");
		if ((szHostName != null) && (szHostName.toUpperCase().contains("SG.IBM.COM"))) {
			//not only applicable to Singapore
			rdbtnHybrid.setEnabled(false);
		}
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
		createPopUpMenu(textFieldProd);
		panelWorkUnit.add(textFieldProd, gbcProdField);

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
		createPopUpMenu(textFieldModel);
		panelWorkUnit.add(textFieldModel, gbcModelField);

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
		rdbtnPrime.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rdbtnHybrid.setSelected(false);
				bIsHybrid = !ISHYBRID;

				//for prime, work unit needs to be entered
				textFieldWorkUnit.setEditable(true);
				textFieldWorkUnit.setText("");

				//for prime, entries will be taken from MFS
				textFieldMfgn.setEditable(false);
				textFieldOrder.setEditable(false);
				textFieldSer.setEditable(false);
				textFieldProd.setEditable(false);
				textFieldModel.setEditable(false);
				textFieldMfgn.setText("");
				textFieldOrder.setText("");
				textFieldSer.setText("");
				textFieldProd.setText("");
				textFieldModel.setText("");
			}
		});

		rdbtnHybrid.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tstLogs.displayInfoPopUpMessage("Enter order details manually since POK Hybrid are not in MFS");

				rdbtnPrime.setSelected(false);
				bIsHybrid = ISHYBRID;
				
				//for hybrid, work unit is always set to DUMMY
				textFieldWorkUnit.setText(DUMMY);
				textFieldWorkUnit.setEditable(false);

				//for hybrid, all entries will be manually entered
				textFieldMfgn.setEditable(true);
				textFieldOrder.setEditable(true);
				textFieldSer.setEditable(true);
				textFieldProd.setEditable(true);
				textFieldModel.setEditable(true);
			}
		});

		ActionListener submit = new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//check if all fields are filled up
				String szWorkUnitNo = textFieldWorkUnit.getText().toUpperCase().trim();
				String szDrainStation = textFieldDrainSt.getText().toUpperCase().trim();
				if (szWorkUnitNo.isEmpty() || szDrainStation.isEmpty()) {
					tstLogs.displayErrPopUpMessage("Please fill up all fields.");
					return;
				}

				//process work unit
				if ((!szWorkUnitNo.contains(DUMMY)) && ((szWorkUnitNo.length() != MINWORKUNITLEN) || szWorkUnitNo.matches("[^a-zA-Z0-9]"))) {
					tstLogs.displayErrPopUpMessage("Please enter a valid work unit number");	
					return;
				}
				textFieldWorkUnit.setText(szWorkUnitNo);

				//for hybrid check if all fields are not empty
				if (bIsHybrid) {
					String szMfgn  = textFieldMfgn.getText();
					String szOrno  = textFieldOrder.getText();
					String szSerNo = textFieldSer.getText();
					String szProd  = textFieldProd.getText();
					String szModel = textFieldModel.getText();
					if (szMfgn.isEmpty() || szOrno.isEmpty() || szSerNo.isEmpty() ||
						szProd.isEmpty() || szModel.isEmpty()) {
						tstLogs.displayErrPopUpMessage("Please fill up all fields since order is not in MFS.");
						return;
					}

					szWorkUnitNo = szWorkUnitNo + " " + szProd + " " + szModel + " " + szSerNo + " " + szOrno + " " + szMfgn;
				}

				//perform login
				displayTxt("Logging IN");
				//if (performLogin() != tstLogs.PASS) {
				//	tstLogs.displayErrPopUpMessage("Failed to login.\nPlease try again.");
				//	return;
				//}

				//perform claim-in
				if (!bIsHybrid) {
					displayTxt("Claiming IN");
					tstLogs.displayInfoPopUpMessage("Will claim in WU.\nDo not close the application.");
				}
				if (performClaimIn(szWorkUnitNo) != tstLogs.PASS) {
					tstLogs.displayErrPopUpMessage("Failed to claim in WU " + szWorkUnitNo.split(" ")[0] + "\nPlease inform ME team.");
					return;
				}
				//disable all clickables
				textFieldWorkUnit.setEditable(false);
				textFieldDrainSt.setEditable(false);
				rdbtnHybrid.setEnabled(false);
				rdbtnPrime.setEnabled(false);
				btnEnter.setVisible(false);
				if (bIsHybrid) {
					textFieldMfgn.setEditable(false);
					textFieldOrder.setEditable(false);
					textFieldSer.setEditable(false);
					textFieldProd.setEditable(false);
					textFieldModel.setEditable(false);
				}

				prepareDrainRunPanels();

				//initialize test case
				if (initTestCase() != tstLogs.PASS) {
					tstLogs.displayErrPopUpMessage("Failed to start test.\nPlease reinitialize the work station or inform MEQ team.");
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
		JRadioButton rdbtnFind = new JRadioButton("Enter Word To Find");
		rdbtnFind.setFont(PLAIN);
		rdbtnFind.setEnabled(false);
		panelTools.add(rdbtnFind, gbcFindRdbtn);

		GridBagConstraints gbcFindTextField = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbcFindTextField.gridx = 1;
		gbcFindTextField.gridy = 1;
		gbcFindTextField.gridwidth = 1;
		textFieldFind = new JTextField();
		textFieldFind.setFont(PLAIN);
		textFieldFind.setEnabled(false);
		createPopUpMenu(textFieldFind);
		panelTools.add(textFieldFind, gbcFindTextField);

		GridBagConstraints gbcFindNextBtn = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		gbcFindNextBtn.gridx = 2;
		gbcFindNextBtn.gridy = 1;
		JButton btnFindNext = new JButton("Next");
		btnFindNext.setFont(PLAIN);
		btnFindNext.setVisible(false);
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
		textPaneLogs.setFont(PLAIN);
		textPaneLogs.setEditable(false);
		createPopUpMenu(textPaneLogs);

		GridBagConstraints gbcScroll = JGridConstraint.getDefaultObjectGbc(panels.DEFAULT);
		JScrollPane scrollPane = new JScrollPane(textPaneLogs);
		panelLogsTextArea.add(scrollPane, gbcScroll);

		//set text pane for the logs
		tstLogs.setTextPane(textPaneLogs);

		//create action listeners
		rdbtnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				textFieldFind.setEnabled(true);
			}
		});
		textFieldFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: findSearchWord(textPaneVpd, textFieldVpdFind, btnFindNextVpd);
			}
		});
		btnFindNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//TODO: findSearchWord(textPaneVpd, textFieldVpdFind, btnFindNextVpd);
			}
		});
	} /** createTestLogsPanel - END */

	/** Initialize next panel
	 *  Input: szProdLnDir - product line directory */
	private void initializeTestCaseImgPanel(String szProdLnDir) {
		//if the product line folder has an intro message (usually in 0.PNG), display
		String szIntroFile = szProdLnDir + SEPARATR + INTROFILE;
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
		panelImg.setVisible(false);
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
				if (iCurrentOp >= tstCtl.getMaxOpCnt()) {
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
						if (performSuspend() != tstLogs.PASS) {
							tstLogs.displayErrPopUpMessage("Failed to perform suspend.\nPlease inform ME team.");
						}
					}

					initDrainPanel();
					//panelTestCase.setVisible(false);
					//panelStart.setVisible(false);
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
				final long lMilliSeconds = 1000;//DBG 
				//CGGfinal long lMilliSeconds = tstCtl.getTimer();

				final String szStartTime = TIMERFORMAT.format(new Date(lMilliSeconds));
				tstLogs.printInfo("Starting countdown of " + szStartTime);
				lblUpdateTimer.setText(szStartTime);
				new Timer(1000, new ActionListener() {
					long lLeftTime = lMilliSeconds - 1000;
					public void actionPerformed(ActionEvent e) {
						if (lLeftTime >= 0) {
							lblUpdateTimer.setText(TIMERFORMAT.format(new Date(lLeftTime)));
							lLeftTime -= 1000;
						} else {
							tstLogs.printInfo("Countdown reached 00 : 00 which started from " + szStartTime);
							((Timer) e.getSource()).stop();
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
					tstLogs.printInfo(szMsg);
					tstLogs.displayErrPopUpMessage(szMsg + "\nPlease abort to suspend operation.");

					displayTxt("<html><font size=7 color=\"red\">" +
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
				displayTxt("Test Done.");
				tstLogs.displayInfoPopUpMessage("Test Done.\nSystem will be claimed out.");
				tstLogs.printInfo("Drain process finished.");

				//perform claim out
				if (performClaimOut() != tstLogs.PASS) {
					tstLogs.printErr("Failed to claim out WU.\nPlease inform ME/MEQ Team.");
				}

				initDrainPanel();
				//CGGpanelTestCase.setVisible(false);
				//CGGpanelStart.setVisible(false);
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
