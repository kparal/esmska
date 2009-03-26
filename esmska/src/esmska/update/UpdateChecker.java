/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.data.Config;
import esmska.data.Operator;
import esmska.data.Operators;
import esmska.data.event.ActionEventSupport;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Checks for newer program or operator version on program's website
 *
 * First of all you must run the {@link #checkForUpdates()} method, only after it
 * has finished the other methods will give you correct answer about updates
 * availability.
 *
 * @author ripper
 */
public class UpdateChecker {

    /** new program version is available */
    public static final int ACTION_PROGRAM_UPDATE_AVAILABLE = 0;
    /** new or updated operator script is available */
    public static final int ACTION_OPERATOR_UPDATE_AVAILABLE = 1;
    /** new program version and operator script is available */
    public static final int ACTION_PROGRAM_AND_OPERATOR_UPDATE_AVAILABLE = 2;
    /** no updates found */
    public static final int ACTION_NO_UPDATE_AVAILABLE = 3;
    /** could not check updates - network error? */
    public static final int ACTION_CHECK_FAILED = 4;

    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
    private static final String UPDATE_FILE_URL = 
            "http://ripper.profitux.cz/esmska/update/version.php?ref=" + Config.getLatestVersion();

    private String onlineVersion = "0.0.0";
    private HashSet<OperatorUpdateInfo> operatorUpdates = new HashSet<OperatorUpdateInfo>();
    private AtomicBoolean running = new AtomicBoolean();

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Checks for updates asynchronously and notify all added listeners after being finished.
     * Does nothing if already running.
     */
    public void checkForUpdates() {
        if (running.get()) {
            //do nothing if already running
            return;
        }

        running.set(true);
        logger.fine("Checking for program updates...");

        final HttpDownloader downloader = new HttpDownloader(UPDATE_FILE_URL, false);
        downloader.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state") &&
                        evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        if (!downloader.isFinishedOk()) {
                            throw new IOException("Could not download version file");
                        }
                        //version file is downloaded ok
                        parseVersionFile(downloader.getTextContent());
                        boolean updateAvailable = isProgramUpdateAvailable();
                        logger.fine("Found program update: " + updateAvailable);
                        boolean operatorUpdateAvailable = !getOperatorUpdates().isEmpty();
                        logger.fine("Found operator update: " + operatorUpdateAvailable);
                        //send events
                        if (updateAvailable) {
                            if (operatorUpdateAvailable) {
                                actionSupport.fireActionPerformed(ACTION_PROGRAM_AND_OPERATOR_UPDATE_AVAILABLE, null);
                            } else {
                                actionSupport.fireActionPerformed(ACTION_PROGRAM_UPDATE_AVAILABLE, null);
                            }
                        } else if (operatorUpdateAvailable) {
                            actionSupport.fireActionPerformed(ACTION_OPERATOR_UPDATE_AVAILABLE, null);
                        } else {
                            actionSupport.fireActionPerformed(ACTION_NO_UPDATE_AVAILABLE, null);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Could not check for updates", e);
                        actionSupport.fireActionPerformed(ACTION_CHECK_FAILED, null);
                    } finally {
                        running.set(false);
                    }
                }
            }
        });
        downloader.execute();
    }

    /** Check whether downloaded file text indicates newer version available.
     * @param text contents of the version file
     */
    private synchronized void parseVersionFile(String text) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();

        Document doc = db.parse(IOUtils.toInputStream(text, "UTF-8"));

        onlineVersion = doc.getElementsByTagName(VersionFile.TAG_LAST_VERSION).
                item(0).getTextContent();

        operatorUpdates.clear();
        NodeList operators = doc.getElementsByTagName(VersionFile.TAG_OPERATOR);
        for (int i = 0; i < operators.getLength(); i++) {
            Node operator = operators.item(i);
            String name = xpath.evaluate(VersionFile.TAG_NAME + "/text()", operator);
            String version = xpath.evaluate(VersionFile.TAG_VERSION + "/text()", operator);
            String url = xpath.evaluate(VersionFile.TAG_DOWNLOAD + "/text()", operator);
            String minVersion = xpath.evaluate(VersionFile.TAG_MIN_VERSION + "/text()", operator);
            String iconUrl = xpath.evaluate(VersionFile.TAG_ICON + "/text()", operator);

            OperatorUpdateInfo info = new OperatorUpdateInfo(name, version, url, minVersion, iconUrl);
            operatorUpdates.add(info);
        }

        //only add new or updated operators
        refreshUpdatedOperators();
    }

    /** Go through all downloaded update information and only leave those operators
     which are new or updated compared to current ones. This can be used to reload
     update info after partial update. */
    public void refreshUpdatedOperators() {
        for (Iterator<OperatorUpdateInfo> it = operatorUpdates.iterator(); it.hasNext(); ) {
            OperatorUpdateInfo info = it.next();
            Operator op = Operators.getOperator(info.getName());
            if (op != null && Config.compareVersions(info.getVersion(), op.getVersion()) <= 0) {
                //operator is same or older, remove it
                it.remove();
            }
        }
    }

    /** Whether checking for updates is (still) running */
    public boolean isRunning() {
        return running.get();
    }

    /** Whether a new program update is available */
    public synchronized boolean isProgramUpdateAvailable() {
        return Config.compareVersions(onlineVersion, Config.getLatestVersion()) > 0;
    }

    /** Get latest program version available online */
    public synchronized String getLatestProgramVersion() {
        return onlineVersion;
    }

    /** Whether an operator update is available */
    public synchronized boolean isOperatorUpdateAvailable() {
        return !operatorUpdates.isEmpty();
    }

    /** Get info about operator updates 
     * @return unmodifiable set of operator updates info
     */
    public synchronized Set<OperatorUpdateInfo> getOperatorUpdates() {
        return Collections.unmodifiableSet(operatorUpdates);
    }

}
