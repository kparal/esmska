/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska;

import esmska.utils.*;
import esmska.data.Config;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

/** Checks for newer version of the program on program's website
 *
 * @author ripper
 */
public class UpdateChecker {

    public static final int ACTION_UPDATE_FOUND = 0;
    private static final String UPDATE_FILE_URL = "http://esmska.googlecode.com/svn/files/version.txt";
    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
    private ActionEventSupport actionEventSupport;

    public UpdateChecker() {
        actionEventSupport = new ActionEventSupport(this);
    }

    /** Checks for updates and if updates are found notifies all added listeners
     */
    public void checkForUpdates() {
        SwingWorker updateCheckerWorker = new UpdateCheckerWorker();
        updateCheckerWorker.execute();
    }

    /** Check whether downloaded file text indicates newer version available
     * 
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
        AlphanumComparator comparator = new AlphanumComparator();
        return comparator.compare(downloadedVersion, currentVersion) > 0;
    }

    public void addActionListener(ActionListener actionListener) {
        actionEventSupport.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionEventSupport.removeActionListener(actionListener);
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
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));

                StringBuilder builder = new StringBuilder();
                String line = "";
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                    builder.append('\n');
                }
                con.disconnect();

                updateAvailable = parseUpdateFile(builder.toString());
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Nelze stáhnout aktualizační soubor", ex);
            }
            return updateAvailable;
        }

        @Override
        protected void done() {
            if (updateAvailable) {
                actionEventSupport.fireActionPerformed(ACTION_UPDATE_FOUND, null);
            }
        }
    }
}
