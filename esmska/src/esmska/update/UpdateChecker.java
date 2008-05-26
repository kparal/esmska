/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.utils.*;
import esmska.data.Config;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;
import org.apache.commons.io.IOUtils;

/** Checks for newer version of the program on program's website
 *
 * @author ripper
 */
public class UpdateChecker {

    public static final int ACTION_UPDATE_FOUND = 0;
    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
    private static final String UPDATE_FILE_URL = "http://esmska.googlecode.com/svn/files/version.txt";

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Checks for updates and if updates are found notifies all added listeners
     */
    public void checkForUpdates() {
        SwingWorker updateCheckerWorker = new UpdateCheckerWorker();
        updateCheckerWorker.execute();
    }

    /** Check whether downloaded file text indicates newer version available.
     * Handles if current version is marked as beta.
     * @param text contents of the update file
     * @return true if new program version is available, otherwise false
     */
    private boolean parseUpdateFile(String text) {
        String downloadedVersion = "0.0.0";
        Pattern pattern = Pattern.compile("^Esmska: ([0-9.]+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            downloadedVersion = matcher.group(1);
        }
        String currentVersion = Config.getLatestVersion();
        
        return compareVersions(downloadedVersion, currentVersion) > 0;
    }
    
    /** Compares two program versions. Handles if some of them is marked as beta.
     * @param version1 first version. Null means lowest possible version.
     * @param version2 second version. Null means lowest possible version.
     * @return positive number if version1 > version2, zero if version1 == version2,
     *         negative number otherwise
     */
    public static int compareVersions(String version1, String version2) {
        if (version1 == null) {
            return (version2 == null ? 0 : -1);
        }
        
        String v1 = version1;
        String v2 = version2;
        
        //handle beta versions
        boolean beta1 = version1.toLowerCase().contains("beta");
        boolean beta2 = version2.toLowerCase().contains("beta");
        if (beta1) {
            v1 = version1.substring(0, version1.toLowerCase().indexOf("beta")).trim();
        }
        if (beta2) {
            v2 = version2.substring(0, version2.toLowerCase().indexOf("beta")).trim();
        }
        
        AlphanumComparator comparator = new AlphanumComparator();
        if (beta1 && beta2) {
            return comparator.compare(version1, version2);
        } else if (beta1) {
            return (comparator.compare(v1, v2) == 0 ? -1 :
                comparator.compare(v1, v2));
        } else if (beta2) {
            return (comparator.compare(v1, v2) == 0 ? 1 :
                comparator.compare(v1, v2));
        } else {
            return comparator.compare(v1, v2);
        }
    }

    /** SwingWorker which checks downloads and check update file in another thread
     */
    private class UpdateCheckerWorker extends SwingWorker<Boolean, Object> {

        private boolean updateAvailable;

        @Override
        protected Boolean doInBackground() {
            try {
                URL url = new URL(UPDATE_FILE_URL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                
                String content = IOUtils.toString(con.getInputStream(), "UTF-8");
                con.getInputStream().close();
                con.disconnect();

                updateAvailable = parseUpdateFile(content);
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, "URL not correct", ex);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Can't download update file", ex);
            }
            return updateAvailable;
        }

        @Override
        protected void done() {
            if (updateAvailable) {
                actionSupport.fireActionPerformed(ACTION_UPDATE_FOUND, null);
            }
        }
    }
}
