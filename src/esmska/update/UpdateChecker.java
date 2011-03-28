package esmska.update;

import esmska.data.Config;
import esmska.data.DeprecatedGateway;
import esmska.data.Gateway;
import esmska.data.Gateways;
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

/** Checks for newer program or gateway version on program's website
 *
 * First of all you must run the {@link #checkForUpdates()} method, only after it
 * has finished the other methods will give you correct answer about updates
 * availability.
 *
 * @author ripper
 */
public class UpdateChecker {
    private static UpdateChecker instance;

    /** new program version is available */
    public static final int ACTION_PROGRAM_UPDATE_AVAILABLE = 0;
    /** new or updated gateway script is available */
    public static final int ACTION_GATEWAY_UPDATE_AVAILABLE = 1;
    /** new program version and gateway script is available */
    public static final int ACTION_PROGRAM_AND_GATEWAY_UPDATE_AVAILABLE = 2;
    /** no updates found */
    public static final int ACTION_NO_UPDATE_AVAILABLE = 3;
    /** could not check updates - network error? */
    public static final int ACTION_CHECK_FAILED = 4;

    /** The interval in seconds how often to check for updates automatically */
    public static final int AUTO_CHECK_INTERVAL = 2 * 60 * 60; // 2 hours

    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());
    private static final String UPDATE_FILE_URL = 
            "http://ripper.profitux.cz/esmska/update/version.php?ref=" + Config.getLatestVersion();
    private static final Config config = Config.getInstance();

    private String onlineVersion = "0.0.0";
    private String onlineUnstableVersion = "0.0.0";
    private HashSet<GatewayUpdateInfo> gatewayUpdates = new HashSet<GatewayUpdateInfo>();
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

    /** Disabled constructor */
    private UpdateChecker() {
    }

    /** Get program instance */
    public static UpdateChecker getInstance() {
        if (instance == null) {
            instance = new UpdateChecker();
        }
        return instance;
    }

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
                        logger.fine("Found program update: " + (updateAvailable ?
                            getLatestProgramVersion() : "false"));
                        boolean gatewayUpdateAvailable = isGatewayUpdateAvailable(true);
                        logger.fine("Found gateway update: " + gatewayUpdateAvailable);
                        //send events
                        if (updateAvailable) {
                            if (gatewayUpdateAvailable) {
                                actionSupport.fireActionPerformed(ACTION_PROGRAM_AND_GATEWAY_UPDATE_AVAILABLE, null);
                            } else {
                                actionSupport.fireActionPerformed(ACTION_PROGRAM_UPDATE_AVAILABLE, null);
                            }
                        } else if (gatewayUpdateAvailable) {
                            actionSupport.fireActionPerformed(ACTION_GATEWAY_UPDATE_AVAILABLE, null);
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
        onlineUnstableVersion = doc.getElementsByTagName(VersionFile.TAG_LAST_UNSTABLE_VERSION).
                item(0).getTextContent();

        gatewayUpdates.clear();
        NodeList gateways = doc.getElementsByTagName(VersionFile.TAG_GATEWAY);
        for (int i = 0; i < gateways.getLength(); i++) {
            Node gateway = gateways.item(i);
            String name = xpath.evaluate(VersionFile.TAG_NAME + "/text()", gateway);
            String fileName = xpath.evaluate(VersionFile.TAG_FILENAME + "/text()", gateway);
            String version = xpath.evaluate(VersionFile.TAG_VERSION + "/text()", gateway);
            String minVersion = xpath.evaluate(VersionFile.TAG_MIN_VERSION + "/text()", gateway);
            String url = xpath.evaluate(VersionFile.TAG_DOWNLOAD + "/text()", gateway);
            String iconUrl = xpath.evaluate(VersionFile.TAG_ICON + "/text()", gateway);

            GatewayUpdateInfo info = new GatewayUpdateInfo(name, fileName,
                    version, minVersion, url, iconUrl);
            gatewayUpdates.add(info);
        }

        //only add new or updated gateways
        refreshUpdatedGateways();
    }

    /** Go through all downloaded update information and only leave those gateways
     * which are new or updated compared to current ones. This can be used to reload
     * update info after partial update. Also removes gateways requiring more recent
     * program version than available online (stable/unstable depending on config
     * settings) and deprecated gateways.
     */
    private void refreshUpdatedGateways() {
        for (Iterator<GatewayUpdateInfo> it = gatewayUpdates.iterator(); it.hasNext(); ) {
            GatewayUpdateInfo info = it.next();
            Gateway gw = Gateways.getInstance().get(info.getName());
            if (gw != null && info.getVersion().compareTo(gw.getVersion()) <= 0) {
                //gateway is same or older, remove it
                it.remove();
                continue;
            }
            if (Config.compareProgramVersions(info.getMinProgramVersion(),
                    getLatestProgramVersion()) > 0) {
                //required program version is newer than available online, remove it
                it.remove();
                continue;
            }
            //check for deprecated gateways
            for (DeprecatedGateway depr : Gateways.getInstance().getDeprecatedGateways()) {
                if (info.getName().equals(depr.getName()) &&
                        info.getVersion().compareTo(depr.getVersion()) <= 0) {
                    //gateway has been deprecated
                    it.remove();
                    continue;
                }
            }
        }
    }

    /** Return only visible gateways updates from all of the available gateway updates. */
    private Set<GatewayUpdateInfo> filterVisibleGateways() {
        HashSet<GatewayUpdateInfo> visible = new HashSet<GatewayUpdateInfo>();
        for (Iterator<GatewayUpdateInfo> it = gatewayUpdates.iterator(); it.hasNext(); ) {
            GatewayUpdateInfo info = it.next();
            Gateway gw = Gateways.getInstance().get(info.getName());
            //add just visible gateways
            if (gw != null && !gw.isHidden()) {
                visible.add(info);
            }
        }
        return visible;
    }

    /** Whether checking for updates is (still) running */
    public boolean isRunning() {
        return running.get();
    }

    /** Whether a new program update is available. According to user preference
     it checks against latest stable or unstable program version. */
    public synchronized boolean isProgramUpdateAvailable() {
        if (config.isAnnounceUnstableUpdates()) {
            return Config.compareProgramVersions(onlineUnstableVersion, Config.getLatestVersion()) > 0;
        } else {
            return Config.compareProgramVersions(onlineVersion, Config.getLatestVersion()) > 0;
        }
    }

    /** Get latest program version available online */
    public synchronized String getLatestProgramVersion() {
        return config.isAnnounceUnstableUpdates() ? onlineUnstableVersion : onlineVersion;
    }

    /** Whether an gateway update is available */
    public synchronized boolean isGatewayUpdateAvailable(boolean includeHidden) {
        refreshUpdatedGateways();
        if (includeHidden) {
            return !gatewayUpdates.isEmpty();
        } else {
            return !filterVisibleGateways().isEmpty();
        }
    }

    /** Get info about gateway updates
     * @return unmodifiable set of gateway updates info
     */
    public synchronized Set<GatewayUpdateInfo> getGatewayUpdates(boolean includeHidden) {
        refreshUpdatedGateways();
        if (includeHidden) {
            return Collections.unmodifiableSet(gatewayUpdates);
        } else {
            return filterVisibleGateways();
        }
    }

}
