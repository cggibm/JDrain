package zhe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JQuery {
	/** Constants */
	private static String USER = "MDLOGIN";
	//CGGprivate static String PASSFILE = "/home/gem/scripts/mfsqry";
	private static String PASSFILE = "C:\\mfsqry";

	/** Global variables */
	private static String  szMfgn;
	private static boolean bIsReapply;
	private static JLogHandler log;

	/** JQuery constructor */
	public JQuery(String szMfgnNo, JLogHandler obj) {
		szMfgn = szMfgnNo;
		bIsReapply = false;
		log = obj;
	} /** JQuery - END */

    /** Perform reaaply query
     *  Output: error code if failed, 0 is pass
     **/
	public int perfReapplyQry() {
		boolean bFailed = false;
		String connectionPassword = getPassword();
		if ((connectionPassword == null) || (connectionPassword.isEmpty())) {
			return log.printErr("Cannot connect to QRYPROD. Please inform ME!");
		}

		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			Class.forName("com.ibm.as400.access.AS400JDBCDriver").newInstance();
			String connectionUrl = "jdbc:as400://QRYPROD.RCHLAND.IBM.COM;libraries=QRYPROD;";
			String connectionUser = USER;
			conn = DriverManager.getConnection(connectionUrl, connectionUser, connectionPassword);
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT CRMFGN, CRIDSP FROM MFS2P010A.FCSPCR10 WHERE CRMFGN='" + szMfgn + "' AND CRNMBR='0807' AND CRIDSP='R'");
			if (rs.next()) {
				log.printInfo("System is a reapply order.");
				bIsReapply = true;
			}
		} catch (Exception ex) {
			log.printErr("Exception encountered while performing query. Please inform ME!", ex);
			bFailed = true;
		} finally {
			try { if (rs   != null) rs.close();   } catch (SQLException e) { e.printStackTrace(); bFailed = true; }
			try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); bFailed = true; }
			try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); bFailed = true; }
		}

		if (bFailed) {
			return log.EXERR;
		}
		return log.PASS;
	} /** perfReapplyQry - END */

    /** Checks if given MFGN is a reapply
     *  Output: true  - reapply
     *          false - otherwise
     **/
	public boolean isReapply() {
		return bIsReapply;
	} /** isReapply - END */

    /** Returns password from a predefined file
     *  Output: password
     *          null or empty string if password is not found
     **/
	private static String getPassword() {
		String szPass = null;
    	File file = new File(PASSFILE);
    	if (file.exists()) {
    		BufferedReader br = null;
    		try {
    			br = new BufferedReader(new FileReader(PASSFILE));
    			szPass = br.readLine();
			} catch (IOException ex) {
				szPass = null;
				log.printErr("Exception encountered while performing query. Will ignore reapply check Please inform ME!", ex);
			}
    	}
    	return szPass;
	} /** getPassword - END */
}
