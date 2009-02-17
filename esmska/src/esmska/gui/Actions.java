/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.SMS;
import esmska.persistence.ExportManager;
import esmska.utils.ConfirmingFileChooser;
import esmska.utils.L10N;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
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
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

/** Class containing common actions used in GUIs
 *
 * @author ripper
 */
public class Actions {
    private static final Logger logger = Logger.getLogger(Actions.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;

    private static Action aboutAction;
    private static Action configAction;
    private static Action quitAction;
    private static HistoryAction historyAction;
    private static Action importAction;
    private static Action exportAction;
    private static Action logAction;

    private static MainFrame mainFrame = MainFrame.getInstance();

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

    /** Get action to automatically select best suitable operator in this list
     * according to number filled in specified text component.
     *
     * @param operatorComboBox Operator combobox which to adjust to suggested operator
     * @param numberComponent Text component containing phone number
     * @return action to automatically select best suitable operator in this list
     * according to number filled in specified text component
     */
    public static Action getSuggestOperatorAction(OperatorComboBox operatorComboBox, JTextComponent numberComponent) {
        SuggestOperatorAction action = new SuggestOperatorAction(operatorComboBox, numberComponent);
        return action;
    }

    /** Show about frame */
    private static class AboutAction extends AbstractAction {
        AboutFrame aboutFrame;
        public AboutAction() {
            L10N.setLocalizedText(this, l10n.getString("About_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "about-16.png")));
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
                aboutFrame.setLocationRelativeTo(mainFrame);
                aboutFrame.setVisible(true);
            }
        }
    }

    /** Show config frame */
    private static class ConfigAction extends AbstractAction {
        private ConfigFrame configFrame;
        public ConfigAction() {
            L10N.setLocalizedText(this, l10n.getString("Preferences_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "config-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "config-32.png")));
            putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.configure_program_behaviour"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing config frame...");
            if (configFrame != null && configFrame.isVisible()) {
                configFrame.requestFocus();
                configFrame.toFront();
            } else {
                configFrame = new ConfigFrame();
                configFrame.setLocationRelativeTo(mainFrame);
                configFrame.setVisible(true);
            }
        }
    }

    /** Quit the program */
    private static class QuitAction extends AbstractAction {
        public QuitAction() {
            L10N.setLocalizedText(this, l10n.getString("Quit_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "exit-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "exit-32.png")));
            putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.Quit_program"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Quitting application...");
            mainFrame.exit();
        }
    }

    /** Show the history frame */
    private static class HistoryAction extends AbstractAction {
        private HistoryFrame historyFrame;
        public HistoryAction() {
            L10N.setLocalizedText(this, l10n.getString("Message_history_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "history-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "history-32.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.show_history_of_sent_messages"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing history frame...");
            if (historyFrame != null && historyFrame.isVisible()) {
                historyFrame.requestFocus();
                historyFrame.toFront();
            } else {
                historyFrame = new HistoryFrame();
                historyFrame.setLocationRelativeTo(mainFrame);
                historyFrame.addActionListener(new HistoryListener());
                historyFrame.setVisible(true);
            }
        }
        public HistoryFrame getHistoryFrame() {
            return historyFrame;
        }
    }

    /** Listens for events from history table */
    private static class HistoryListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //resend sms
            History.Record record = historyAction.getHistoryFrame().getSelectedHistory();
            if (record == null) {
                return;
            }

            SMS sms = new SMS();
            sms.setName(record.getName());
            sms.setNumber(record.getNumber());
            sms.setOperator(record.getOperator());
            sms.setText(record.getText());

            mainFrame.getContactPanel().clearSelection();
            mainFrame.getSMSPanel().setSMS(sms);
        }
    }

    /** Import contacts from other programs/formats */
    private static class ImportAction extends AbstractAction {
        private ImportFrame importFrame;
        public ImportAction() {
            L10N.setLocalizedText(this, l10n.getString("Contact_import_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "contact-16.png")));
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
                importFrame.setLocationRelativeTo(mainFrame);
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
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "contact-16.png")));
            this.putValue(SHORT_DESCRIPTION, l10n.getString("MainFrame.export_contacts_to_file"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<Contact> contacts = Contacts.getInstance().getAll();

            logger.fine("Showing export contacts dialog...");
            //show info
            String message = l10n.getString("ExportManager.export_info");
            JOptionPane.showMessageDialog(mainFrame, new JLabel(message), l10n.getString("ExportManager.contact_export"),
                    JOptionPane.INFORMATION_MESSAGE,
                    new ImageIcon(getClass().getResource(RES + "contact-48.png")));

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
            if (chooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File file = chooser.getSelectedFile();
            logger.finer("File chosen for contacts export: " + file.getAbsolutePath());

            //check if file can be written
            if (file.exists() && !file.canWrite()) {
                logger.info("File '" + file.getAbsolutePath() + "' can't be written");
                JOptionPane.showMessageDialog(mainFrame,
                        MessageFormat.format(l10n.getString("ExportManager.cant_write"), file.getAbsolutePath()),
                        null, JOptionPane.ERROR_MESSAGE);
                return;
            }

            //save
            logger.finer("About to export " + contacts.size() + " contacts");
            try {
                if (chooser.getFileFilter() == vCardFileFilter) {
                    ExportManager.exportContactsToVCard(contacts, file);
                } else {
                    ExportManager.exportContacts(contacts, file);
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not export contacts to file", ex);
                Log.getInstance().addRecord(new Log.Record(
                        l10n.getString("ExportManager.export_failed"), null, Icons.STATUS_ERROR));
                JOptionPane.showMessageDialog(mainFrame, l10n.getString("ExportManager.export_failed"), null,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            Log.getInstance().addRecord(new Log.Record(
                    l10n.getString("ExportManager.export_ok"), null, Icons.STATUS_INFO));
            JOptionPane.showMessageDialog(mainFrame, l10n.getString("ExportManager.export_ok!"), null,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Select suggested operator in the combobox */
    private static class SuggestOperatorAction extends AbstractAction {
        private OperatorComboBox operatorComboBox;
        private JTextComponent numberComponent;

        public SuggestOperatorAction(OperatorComboBox operatorComboBox, JTextComponent numberComponent) {
            super(l10n.getString("OperatorComboBox.Choose_suitable_gateway"),
                    new ImageIcon(Actions.class.getResource(RES + "search-22.png")));
            this.putValue(SHORT_DESCRIPTION, l10n.getString("OperatorComboBox.Choose_suitable_gateway_for_provided_number"));

            if (operatorComboBox == null || numberComponent == null) {
                throw new IllegalArgumentException("Arguments cant be null");
            }

            this.operatorComboBox = operatorComboBox;
            this.numberComponent = numberComponent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String number = numberComponent.getText();
            operatorComboBox.selectSuggestedOperator(number);
            operatorComboBox.requestFocusInWindow();
        }
    }

    /** Show the log frame */
    private static class LogAction extends AbstractAction {
        private LogFrame logFrame;
        public LogAction() {
            L10N.setLocalizedText(this, l10n.getString("Log_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "log-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "log-48.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Show_application_log"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing Log frame...");
            if (logFrame != null && logFrame.isVisible()) {
                logFrame.requestFocus();
                logFrame.toFront();
            } else {
                logFrame = new LogFrame();
                logFrame.setLocationRelativeTo(mainFrame);
                logFrame.setVisible(true);
            }
        }
    }
}
