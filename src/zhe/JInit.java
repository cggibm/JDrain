/*** JInit.java
 *   Initializes test scripts
 *   Created by: Carlo G. Gagui (gaguigc@sg.ibm.com)
 */

package zhe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JInit {
	/** Constants */
	private static String TMPZIP = "tmp.zip";
	private static String SCRIPTSURL = "https://www.dropbox.com/sh/g5x11o26f1pqz1r/AAD6meTa0rJphuFoQL9RGqAJa?dl=1";

	/** Updates the scripts folder
	 *  Input:  szScriptDir - directory for the scripts to be updated
	 *          logHandlr - handle to JLogHandler class
	 *  Output: 0 if pass; other if otherwise
	 */
	public static int updateScripts(boolean bIsWindows, String szCodeDir, JLogHandler logHandlr) {
		String filename = TMPZIP;

		try {
		    URL download = new URL(SCRIPTSURL);
		    ReadableByteChannel rbc = Channels.newChannel(download.openStream());
		    FileOutputStream fileOut = new FileOutputStream(filename);
		    fileOut.getChannel().transferFrom(rbc, 0, 1 << 24);
		    fileOut.flush();
		    fileOut.close();
		    rbc.close();
		} catch (Exception ex) {
			logHandlr.printErr("Exception encountered while downloading files.");
			return logHandlr.EXERR;
		}
		logHandlr.printDbg("Copying of files done.");

		//unzip folder
		try {
			JUnzip.unZipIt(bIsWindows, TMPZIP, szCodeDir);
		} catch (IOException ex) {
			logHandlr.printDbg("IOException encountered while unzipping files.");
			return logHandlr.EXERR;
		}
		logHandlr.printDbg("Unzipping of files done.");

		//delete downloaded file
		File file = new File(TMPZIP);
		file.delete();
		return logHandlr.PASS;
	} /** updateScripts - END */
} // JInit - END

/*** Class used for unzipping zip files 
 **/
class JUnzip {
	/**
	 * Unzip it
	 * Input: szZipFile - zipFile input zip file
	 *        szOutputFolder - output zip file output folder
	 */
	public static void unZipIt(boolean bIsWindows, String szZipFile, String szOutputFolder) throws IOException {
        File destDir = new File(szOutputFolder);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(szZipFile));
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
			String szName = ze.getName();
			szName = szOutputFolder + (bIsWindows ? szName.replace("/", "\\") : szName);

			//do we need to create a directory ?
			File file = new File(szName);
			if ((bIsWindows ? szName.endsWith("\\") : szName.endsWith("/"))) {
			    file.mkdirs();
			} else {
				file = new File(file.getParent());
				file.mkdirs();

				//extract the file
				FileOutputStream fos = new FileOutputStream(szName);
				byte[] bytes = new byte[1024];
				int iLen;
				while ((iLen =zis.read(bytes)) >= 0) {
					fos.write(bytes, 0, iLen);
				}
				fos.close();
			}
			ze = zis.getNextEntry();
		}        zis.closeEntry();
        zis.close();
	} /** unZipIt - END */
} // JUnzip - END
