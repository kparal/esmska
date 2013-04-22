package esmska.gui;

import esmska.Context;
import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.History.Record;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Queue;
import esmska.data.Queue.Events;
import esmska.data.SMS;
import esmska.persistence.ExportManager;
import esmska.utils.ConfirmingFileChooser;
import esmska.utils.L10N;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.data.Links;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import org.apache.commons.lang.StringUtils;

/** Class containing common actions used in GUIs
 *
 * @author ripper
 */
public class Actions {
    private static final Logger logger = Logger.getLogger(Actions.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;

    private static Action aboutAction;
    private static ConfigAction configAction;
    private static Action quitAction;
    private static Action historyAction;
    private static Action importAction;
    private static Action exportAction;
    private static Action logAction;

    /** Show about frame */
    public static Action getAboutAction() {
        if (aboutAction == null) {
            aboutAction = new AboutAction();
        }
        return aboutAction;
    }

    /** Show config frame */
    public static Action getConfigAction() {
        if (configAction == null) {
            configAction = new ConfigAction();
        }
        return configAction;
    }

    /** Quit the program */
    public static Action getQuitAction() {
        if (quitAction == null) {
            quitAction = new QuitAction();
        }
        return quitAction;
    }

    /** Show the history frame */
    public static Action getHistoryAction() {
        if (historyAction == null) {
            historyAction = new HistoryAction();
        }
        return historyAction;
    }

    /** Import contacts from other programs/formats */
    public static Action getImportAction() {
        if (importAction == null) {
            importAction = new ImportAction();
        }
        return importAction;
    }

    /** Export contacts for other programs */
    public static Action getExportAction() {
        if (exportAction == null) {
            exportAction = new ExportAction();
        }
        return exportAction;
    }

    /** Show the log frame */
    public static Action getLogAction() {
        if (logAction == null) {
            logAction = new LogAction();
        }
        return logAction;
    }

    /** Pause/unpause the sms queue
     * @param showName show name of the action (in button text etc)
     */
    public static Action getQueuePauseAction(boolean showName) {
        return new QueuePauseAction(showName);
    }

    /** Get action to automatically select best suitable gateway in this list
     * according to number filled in specified text component.
     *
     * @param gatewayComboBox Gateway combobox which to adjust to suggested gateway
     * @param numberComponent Text component containing phone number
     * @return action to automatically select best suitable gateway in this list
     * according to number filled in specified text component
     */
    public static Action getSuggestGatewayAction(GatewayComboBox gatewayComboBox, JTextComponent numberComponent) {
        return new SuggestGatewayAction(gatewayComboBox, numberComponent);
    }

    /** Browse specific URL with a web browser */
    public static Action getBrowseAction(String url) {
        return new BrowseAction(url);
    }

    /** Browse specific URL with a web browser */
    private static class BrowseAction extends AbstractAction {
        private final String url;
        public BrowseAction(String URL) {
            super();
            this.url = URL;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (StringUtils.startsWith(url, "esmska://")) {
                //internal program action link
                if (Links.CONFIG_GATEWAYS.equals(url)) {
                    ((ConfigAction)getConfigAction()).showTab(ConfigFrame.Tabs.GATEWAYS);
                } else {
                    assert false : "Unknown internal action link: " + url;
                    logger.warning("Unknown internal action link: " + url);
                }
                return;
            }

            if (!Desktop.isDesktopSupported()) {
                logger.warning("Running browser not supported, can't browse URL: " + url);
                return;
            }
            //start browser
            Desktop desktop = Desktop.getDesktop();
            try {
                logger.fine("Browsing URL: " + url);
                desktop.browse(new URL(url).toURI());
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not browse URL: " + url, ex);
            }
        }
    }

    /** Show about frame */
    private static class AboutAction extends AbstractAction {
        AboutFrame aboutFrame;
        public AboutAction() {
            L10N.setLocalizedText(this, l10n.getString("About_"));
            putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.show_information_about_program"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing About frame...");
            if (aboutFrame != null && aboutFrame.isVisible()) {
                aboutFrame.requestFocus();
                aboutFrame.toFront();
            } else {
                aboutFrame = new AboutFrame();
                aboutFrame.setLocationRelativeTo(Context.mainFrame);
                aboutFrame.setVisible(true);
            }
        }
    }

    /** Show config frame */
    private static class ConfigAction extends AbstractAction {
        private ConfigFrame configFrame;
        public ConfigAction() {
            L10N.setLocalizedText(this, l10n.getString("Preferences_"));
            putValue(SMALL_ICON, Icons.get("config-16.png"));
            putValue(LARGE_ICON_KEY, Icons.get("config-32.png"));
            putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.configure_program_behaviour"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing config frame...");
            if (configFrame != null) {
                configFrame.setVisible(true);
                configFrame.requestFocus();
                configFrame.toFront();
            } else {
                configFrame = new ConfigFrame();
                configFrame.setLocationRelativeTo(Context.mainFrame);
                configFrame.setVisible(true);
            }
        }
        public void showTab(ConfigFrame.Tabs tab) {
            actionPerformed(null);
            configFrame.switchToTab(tab);
        }
    }

    /** Quit the program */
    private static class QuitAction extends AbstractAction {
        public QuitAction() {
            L10N.setLocalizedText(this, l10n.getString("Quit_"));
            putValue(SMALL_ICON, Icons.get("exit-16.png"));
            putValue(LARGE_ICON_KEY, Icons.get("exit-32.png"));
            putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.Quit_program"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Quitting application...");
            Context.mainFrame.exit();
        }
    }

    /** Show the history frame */
    private static class HistoryAction extends AbstractAction {
        private HistoryFrame historyFrame;
        public HistoryAction() {
            L10N.setLocalizedText(this, l10n.getString("Message_history_"));
            putValue(SMALL_ICON, Icons.get("history-16.png"));
            putValue(LARGE_ICON_KEY, Icons.get("history-32.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.show_history_of_sent_messages"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing history frame...");
            if (historyFrame != null) {
                historyFrame.setVisible(true);
                historyFrame.requestFocus();
                historyFrame.toFront();
            } else {
                historyFrame = new HistoryFrame();
                historyFrame.setLocationRelativeTo(Context.mainFrame);
                historyFrame.addValuedListener(new HistoryListener());
                historyFrame.setVisible(true);
            }
        }
    }

    /** Listens for events from history table */
    private static class HistoryListener implements ValuedListener<HistoryFrame.Events, History.Record> {
        @Override
        public void eventOccured(ValuedEvent<HistoryFrame.Events, Record> e) {
            switch (e.getEvent()) {
                case RESEND_SMS:
                    History.Record record = e.getValue();
                    if (record == null) {
                        return;
                    }

                    String fragmentID = SMS.generateID(0);
                    SMS sms = new SMS(record.getNumber(), record.getText(), record.getGateway(), fragmentID);
                    sms.setName(record.getName());

                    Context.mainFrame.getContactPanel().clearSelection();
                    Context.mainFrame.getSMSPanel().setSMS(sms);
            }
        }
    }

    /** Import contacts from other programs/formats */
    private static class ImportAction extends AbstractAction {
        private ImportFrame importFrame;
        public ImportAction() {
            L10N.setLocalizedText(this, l10n.getString("Contact_import_"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.import_contacts_from_other_applications"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing import contacts dialog...");
            if (importFrame != null && importFrame.isVisible()) {
                importFrame.requestFocus();
                importFrame.toFront();
            } else {
                importFrame = new ImportFrame();
                importFrame.setLocationRelativeTo(Context.mainFrame);
                importFrame.setVisible(true);
            }
        }
    }

    /** Export contacts for other programs */
    private static class ExportAction extends AbstractAction {
        private static final FileFilter csvFileFilter = 
            new FileNameExtensionFilter(l10n.getString("ExportManager.csv_filter"), "csv");
        private static final FileFilter vCardFileFilter = 
            new FileNameExtensionFilter(l10n.getString("ExportManager.vcard_filter"), "vcf", "vcard");
        private static ConfirmingFileChooser chooser;

        public ExportAction() {
            L10N.setLocalizedText(this, l10n.getString("Contact_export_"));
            this.putValue(SHORT_DESCRIPTION, l10n.getString("MainFrame.export_contacts_to_file"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<Contact> contacts = Contacts.getInstance().getAll();

            logger.fine("Showing export contacts dialog...");
            //show info
            String message = l10n.getString("ExportManager.export_info");
            JOptionPane.showMessageDialog(Context.mainFrame, new JLabel(message), l10n.getString("ExportManager.contact_export"),
                    JOptionPane.INFORMATION_MESSAGE,
                    Icons.get("contact-48.png"));

            //choose file
            if (chooser == null) {
                chooser = new ConfirmingFileChooser();
                chooser.setDialogTitle(l10n.getString("ExportManager.choose_export_file"));
                //set dialog type not to erase approve button text later
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                chooser.setApproveButtonText(l10n.getString("Save"));
                chooser.addChoosableFileFilter(csvFileFilter);
                chooser.addChoosableFileFilter(vCardFileFilter);
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileFilter(csvFileFilter);
            }
            if (chooser.showSaveDialog(Context.mainFrame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = chooser.getSelectedFile();
            logger.finer("File chosen for contacts export: " + file.getAbsolutePath());

            //check if file can be written
            if (file.exists() && !file.canWrite()) {
                logger.info("File '" + file.getAbsolutePath() + "' can't be written");
                JOptionPane.showMessageDialog(Context.mainFrame,
                        MessageFormat.format(l10n.getString("ExportManager.cant_write"), file.getAbsolutePath()),
                        null, JOptionPane.ERROR_MESSAGE);
                return;
            }

            //save
            logger.finer("About to export " + contacts.size() + " contacts");
            try {
                FileOutputStream out = new FileOutputStream(file);
                if (chooser.getFileFilter() == vCardFileFilter) {
                    ExportManager.exportContactsToVCard(contacts, out);
                } else {
                    ExportManager.exportContacts(contacts, out);
                }
                out.flush();
                out.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not export contacts to file", ex);
                Log.getInstance().addRecord(new Log.Record(
                        l10n.getString("ExportManager.export_failed"), null, Icons.STATUS_ERROR));
                JOptionPane.showMessageDialog(Context.mainFrame, l10n.getString("ExportManager.export_failed"), null,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Log.getInstance().addRecord(new Log.Record(
                    l10n.getString("ExportManager.export_ok"), null, Icons.STATUS_INFO));
            JOptionPane.showMessageDialog(Context.mainFrame, l10n.getString("ExportManager.export_ok!"), null,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Select suggested gateway in the combobox */
    public static class SuggestGatewayAction extends AbstractAction {
        private GatewayComboBox gatewayComboBox;
        private JTextComponent numberComponent;

        public SuggestGatewayAction(GatewayComboBox gatewayComboBox, JTextComponent numberComponent) {
            L10N.setLocalizedText(this, l10n.getString("Suggest_"));
            this.putValue(SHORT_DESCRIPTION, l10n.getString("GatewayComboBox.Choose_suitable_gateway_for_provided_number"));

            if (gatewayComboBox == null || numberComponent == null) {
                throw new IllegalArgumentException("Arguments cant be null");
            }

            this.gatewayComboBox = gatewayComboBox;
            this.numberComponent = numberComponent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String number = numberComponent.getText();
            gatewayComboBox.selectNextSuggestedGateway(number);
        }
    }

    /** Show the log frame */
    private static class LogAction extends AbstractAction {
        private LogFrame logFrame;
        public LogAction() {
            L10N.setLocalizedText(this, l10n.getString("Log_"));
            putValue(LARGE_ICON_KEY, Icons.get("log-48.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Show_application_log"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing Log frame...");
            if (logFrame != null) {
                logFrame.setVisible(true);
                logFrame.requestFocus();
                logFrame.toFront();
            } else {
                logFrame = new LogFrame();
                logFrame.setLocationRelativeTo(Context.mainFrame);
                logFrame.setVisible(true);
            }
        }
    }

    /** Pause/unpause the sms queue */
    private static class QueuePauseAction extends AbstractAction {
        private final String nameRunning = l10n.getString("Pause");
        private final String nameRunningLong = l10n.getString("Pause_queue");
        private final String nameStopped = l10n.getString("Unpause");
        private final String nameStoppedLong = l10n.getString("Unpause_queue");
        private final String descRunning = l10n.getString("QueuePanel.Pause_sending_of_sms_in_the_queue");
        private final String descStopped = l10n.getString("QueuePanel.Unpause_sending_of_sms_in_the_queue");
        private final ImageIcon pauseIcon = Icons.get("pause-22.png");
        private final ImageIcon pauseIconSmall = Icons.get("pause-16.png");
        private final ImageIcon startIcon = Icons.get("start-22.png");
        private final ImageIcon startIconSmall = Icons.get("start-16.png");
        private static final Queue queue = Queue.getInstance();
        private boolean longName;
        public QueuePauseAction() {
            this(true);
        }
        public QueuePauseAction(boolean longName) {
            super();
            this.longName = longName;
            setPaused(queue.isPaused());
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            queue.addValuedListener(new ValuedListener<Queue.Events, SMS>() {
                @Override
                public void eventOccured(ValuedEvent<Events, SMS> e) {
                    switch (e.getEvent()) {
                        case QUEUE_PAUSED:
                            setPaused(true);
                            break;
                        case QUEUE_RESUMED:
                            setPaused(false);
                            break;
                    }
                }
            });
            // enable this only once the program is fully loaded
            Context.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!StringUtils.equals(evt.getPropertyName(), "everythingLoaded")) {
                        return;
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateStatus();
                        }
                    });
                }
            });
            updateStatus();
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            queue.setPaused(!queue.isPaused());
        }
        private void setPaused(boolean paused) {
            if (paused) {
                if (longName) {
                    putValue(NAME, nameStoppedLong);
                } else {
                    putValue(NAME, nameStopped);
                }
                putValue(SMALL_ICON, startIconSmall);
                putValue(LARGE_ICON_KEY, startIcon);
                putValue(SHORT_DESCRIPTION, descStopped);
                putValue(SELECTED_KEY, true);
            } else {
                if (longName) {
                    putValue(NAME, nameRunningLong);
                } else {
                    putValue(NAME, nameRunning);
                }
                putValue(SMALL_ICON, pauseIconSmall);
                putValue(LARGE_ICON_KEY, pauseIcon);
                putValue(SHORT_DESCRIPTION, descRunning);
                putValue(SELECTED_KEY, false);
            }
        }
        private void updateStatus() {
            this.setEnabled(Context.everythingLoaded());
        }
    }
}
