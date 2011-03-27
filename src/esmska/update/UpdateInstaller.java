
package esmska.update;

import esmska.Context;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Queue;
import esmska.data.Queue.Events;
import esmska.data.SMS;
import esmska.data.Tuple3;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/** Class for managing new gateway updates installation.
 */
public class UpdateInstaller {
    private static UpdateInstaller instance;
    private static final Logger logger = Logger.getLogger(UpdateInstaller.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Log log = Log.getInstance();

    private final QueueListener queueListener = new QueueListener();
    private ArrayList<Tuple3<GatewayUpdateInfo, String, byte[]>> updateFiles = new ArrayList<Tuple3<GatewayUpdateInfo, String, byte[]>>();

    /** Disabled constructor */
    private UpdateInstaller() {
    }

    /* Get program instance */
    public static UpdateInstaller getInstance() {
        if (instance == null) {
            instance = new UpdateInstaller();
        }
        return instance;
    }

    /** Install all available gateway updates.
     * This will ask UpdateChecker instance for the list of gateway updates.
     * The update information must already be retrieved by it.
     * The gateways will be downloaded and installed in a new thread.
     */
    public void installNewGateways() {
        logger.fine("Starting up update installation process...");
        Set<GatewayUpdateInfo> updates = UpdateChecker.getInstance().getGatewayUpdates(true);

        //download updates
        final Downloader dl = new Downloader(updates);
        dl.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("state") &&
                        evt.getNewValue() == SwingWorker.StateValue.DONE) {
                    // everything downloaded, retrieve downloaded data
                    assert dl.isDone() : "Downloader should have finished";
                    boolean dlOk = dl.isFinishedOk();
                    try {
                        if (dl.get() != null) {
                            updateFiles = dl.get();
                        } else {
                            dlOk = false;
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Could not retrieve downloaded gateway information", ex);
                        dlOk = false;
                    }

                    if (!dlOk) {
                        log.addRecord(new Log.Record(l10n.getString("Update.downloadFailed"), null, Icons.STATUS_ERROR));
                    } 
                    
                    if (!updateFiles.isEmpty()) {
                        // let's install updates when queue is quiet (no sms being sent)
                        Queue.getInstance().addValuedListener(queueListener);
                        // simulate event to install updates immediately if possible
                        queueListener.eventOccured(null);
                    }
                }
            }
        });
        dl.execute();
    }

    /** Perform the gateway update installation (after all the data was downloaded) */
    private void doInstallation() {
        boolean installOk = true;

        //save the data to harddisk
        logger.log(Level.FINER, "Saving {0} updates to disk", updateFiles.size());
        for (Tuple3<GatewayUpdateInfo, String, byte[]> script : updateFiles) {
            try {
                Context.persistenceManager.saveGateway(
                        script.get1().getFileName(), script.get2(), script.get3());
                logger.log(Level.INFO, "Gateway updated: {0} ({1})",
                        new Object[]{script.get1().getName(), script.get1().getVersion()});
                log.addRecord(new Log.Record(
                        MessageFormat.format(l10n.getString("Update.gwUpdated"), script.get1().getName()),
                        null, Icons.STATUS_UPDATE));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not save gateway", ex);
                installOk = false;
            }
        }

        //reload all gateways
        logger.finer("Reloading gateways...");
        try {
            Context.persistenceManager.loadGateways();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not reload gateways", ex);
            installOk = false;
        }

        //inform if something went wrong
        if (!installOk) {
            log.addRecord(new Log.Record(l10n.getString("Update.installFailed"), null, Icons.STATUS_ERROR));
        }
    }

    /** Download all requested updates.
     * Returns collection of [update info;script contents;icon]. */
    private class Downloader extends SwingWorker<ArrayList<Tuple3<GatewayUpdateInfo,String,byte[]>>, Integer> {
        private boolean finishedOk;
        private Collection<GatewayUpdateInfo> infos;
        private ArrayList<Tuple3<GatewayUpdateInfo,String,byte[]>> scripts = new ArrayList<Tuple3<GatewayUpdateInfo,String, byte[]>>();
        int downloaded = 0;

        /** Constructor.
         * @param infos collection of infos for which to retrieve files
         */
        public Downloader(Collection<GatewayUpdateInfo> infos) {
            this.infos = infos;
        }
        @Override
        protected ArrayList<Tuple3<GatewayUpdateInfo,String,byte[]>> doInBackground() throws Exception {
            logger.log(Level.FINER, "Downloading {0} updates", infos.size());
            for (GatewayUpdateInfo info : infos) {
                try {
                    logger.log(Level.FINER, "Downloading gateway update: {0} {1}",
                            new Object[]{info.getName(), info.getVersion()});
                    //download script
                    HttpDownloader dl = new HttpDownloader(info.getDownloadUrl().toString(), false);
                    dl.execute();
                    String script = (String) dl.get();
                    if (!dl.isFinishedOk()) {
                        //if script is not downloaded, don't download the icon
                        continue;
                    }
                    byte[] icon = null;
                    if (info.getIconUrl() != null) {
                        //download icon
                        dl = new HttpDownloader(info.getIconUrl().toString(), true);
                        dl.execute();
                        icon = (byte[]) dl.get();
                        if (!dl.isFinishedOk()) {
                            continue;
                        }
                    }
                    Tuple3<GatewayUpdateInfo,String,byte[]> tuple =
                            new Tuple3<GatewayUpdateInfo,String,byte[]>(info, script, icon);
                    scripts.add(tuple);
                } finally {
                    //publish how many updates are downloaded up to now
                    publish(++downloaded);
                }
                if (isCancelled()) {
                    logger.fine("Updates downloading cancelled");
                    finishedOk = false;
                    return null;
                }
            }
            finishedOk = (infos.size() == scripts.size());
            if (!finishedOk) {
                logger.warning("Could not download all gateway updates");
            }
            return scripts;
        }
        /** Returns whether all requested updates were downloaded ok. */
        public boolean isFinishedOk() {
            return finishedOk;
        }
    }

    /** Listen for queue changes and install new gateways when no SMS is being sent */
    private class QueueListener implements ValuedListener<Queue.Events, SMS> {
        @Override
        public void eventOccured(ValuedEvent<Events, SMS> e) {
            Queue queue = Queue.getInstance();
            if (!queue.getAllWithStatus(SMS.Status.SENDING).isEmpty()) {
                logger.finer("Messages are still being sent, postponing gateway update");
                return;
            }
            // queue is quiet now, no SMS is being sent, let's update
            doInstallation();
            // and this listener is needed no more
            queue.removeValuedListener(this);
        }
    }
}
