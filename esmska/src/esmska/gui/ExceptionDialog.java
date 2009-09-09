/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ExceptionDialog.java
 *
 * Created on 8.9.2009, 23:06:43
 */
package esmska.gui;

import esmska.data.Config;
import esmska.data.Links;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.gui.JHtmlLabel.Events;
import esmska.persistence.PersistenceManager;
import esmska.utils.L10N;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.openide.awt.Mnemonics;

/**
 *
 * @author ripper
 */
public class ExceptionDialog extends javax.swing.JDialog {

    private static final Logger logger = Logger.getLogger(ExceptionDialog.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final EsmskaExceptionHandler exceptionHandler = new EsmskaExceptionHandler();
    private static ExceptionDialog instance;
    private String logFilePath = "--"; //by default we don't know

    /** Creates new form ExceptionDialog */
    private ExceptionDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        //get path to logfile
        try {
            if (PersistenceManager.isInstantiated()) {
                logFilePath = PersistenceManager.getInstance().getLogFile().getAbsolutePath();
            } else {
                //don't try to instantiate PersistenceManager on your own,
                //either it already failed or the error happened before
                //it's default initialization
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not access PersistenceManager", ex);
        }

        initComponents();

        closeButton.requestFocusInWindow();
        this.getRootPane().setDefaultButton(closeButton);

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(new ImageIcon(getClass().getResource(RES + "error-16.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "error-32.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "error-48.png")).getImage());
        setIconImages(images);

        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonActionPerformed(e);
            }
        });

        //if not Substance LaF, add clipboard popup menu to text components
        if (!Config.getInstance().getLookAndFeel().equals(ThemeManager.LAF.SUBSTANCE)) {
            ClipboardPopupMenu.register(detailsTextArea);
        }

        //center the dialog
        this.setLocationRelativeTo(this.getParent());
    }

    /** Get instance of ExceptionDialog */
    public static ExceptionDialog getInstance() {
        if (instance == null) {
            JFrame parent = MainFrame.isInstantiated() ? MainFrame.getInstance() : null;
            instance = new ExceptionDialog(parent, true);
        }
        return instance;
    }

    /** Get UncaughtExceptionHandler that will log the exception and show ExceptionDialog */
    public static UncaughtExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /** Set the traceback text that should appear in the dialog */
    public void setExceptionText(String traceback) {
        detailsTextArea.setText(traceback);
        detailsTextArea.setCaretPosition(0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {


        jLabel1 = new JLabel();
        closeButton = new JButton();
        jPanel1 = new JPanel();
        detailsLabel = new JLabel();
        jScrollPane1 = new JScrollPane();
        detailsTextArea = new JTextArea();
        copyButton = new JButton();
        errorLabel = new JHtmlLabel();

        jLabel1.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/error-48.png"))); // NOI18N
        jLabel1.setVerticalAlignment(SwingConstants.TOP);

        closeButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(closeButton, l10n.getString("Close_")); // NOI18N
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        detailsLabel.setFont(detailsLabel.getFont().deriveFont(detailsLabel.getFont().getStyle() | Font.BOLD));
        Mnemonics.setLocalizedText(detailsLabel, l10n.getString("ExceptionDialog.detailsLabel.text"));
        detailsTextArea.setEditable(false);
        detailsTextArea.setLineWrap(true);
        detailsTextArea.setTabSize(2);
        detailsTextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(detailsTextArea);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(detailsLabel)
                .addContainerGap(461, Short.MAX_VALUE))
            .addComponent(jScrollPane1)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(detailsLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE))
        );

        copyButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/copy-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(copyButton, l10n.getString("CopyToClipboard_"));
        copyButton.setToolTipText(l10n.getString("ExceptionDialog.copyButton.toolTipText")); // NOI18N
        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });
        Mnemonics.setLocalizedText(errorLabel, MessageFormat.format(l10n.getString("UncaughtExceptionDialog.errorLabel"), Links.ISSUES, logFilePath));
    errorLabel.setVerticalAlignment(SwingConstants.TOP);
    errorLabel.setVerticalTextPosition(SwingConstants.TOP);
    errorLabel.addValuedListener(new ValuedListener<JHtmlLabel.Events, String>() {
        @Override
        public void eventOccured(ValuedEvent<Events, String> e) {
            if (e.getEvent() == Events.LINK_CLICKED) {
                    String url = e.getValue();
                    if (!Desktop.isDesktopSupported()) {
                        return;
                    }
                    Desktop desktop = Desktop.getDesktop();
                try {
                    logger.fine("Browsing URL: " + url);
                    desktop.browse(new URL(url).toURI());
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Could not browse URL: " + url, ex);
                }
            }
        }
    });

        GroupLayout layout = new GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                .addComponent(jPanel1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(Alignment.LEADING, layout.createSequentialGroup()
                    .addComponent(jLabel1)
                    .addGap(18, 18, 18)
                    .addComponent(errorLabel, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(copyButton)
                    .addPreferredGap(ComponentPlacement.RELATED, 265, Short.MAX_VALUE)
                    .addComponent(closeButton)))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                .addComponent(errorLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(closeButton)
                .addComponent(copyButton))
            .addContainerGap())
    );

    pack();
    }// </editor-fold>//GEN-END:initComponents

    private void copyButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
        try {
            logger.fine("Copying logs to clipboard");
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable text = new StringSelection(detailsTextArea.getText());
            clipboard.setContents(text, null);
        } catch (IllegalStateException ex) {
            logger.log(Level.WARNING, "System clipboard not available", ex);
        }
    }//GEN-LAST:event_copyButtonActionPerformed

    private void closeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_closeButtonActionPerformed

    /** Exception handler that will log the exception and show ExceptionDialog */
    private static class EsmskaExceptionHandler implements UncaughtExceptionHandler {

        private static boolean dialogAlreadyShown = false;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            //do magic to extract traceback text with some usable formatting
            StringWriter stringWr = new StringWriter();
            PrintWriter wr = new PrintWriter(stringWr);
            e.printStackTrace(wr);
            wr.close();
            final String traceback = stringWr.toString();
            logger.severe("Uncaught exception detected: " + traceback);

            if (SwingUtilities.isEventDispatchThread()) {
                displayDialog(traceback);
            } else {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            displayDialog(traceback);
                        }
                    });
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Could not show exception dialog", ex);
                }
            }

            //if main thread was interrupted then shutdown the program, because
            //otherwise it would remain hanging
            if ("main".equals(Thread.currentThread().getName())) {
                logger.severe("Encountered uncaught exception at the main thread, exiting application...");
                System.exit(4);
            }
        }

        /** display the exception dialog */
        private void displayDialog(String traceback) {
            if (dialogAlreadyShown) {
                //show the dialog only once, to avoid recursive pop-up
                logger.fine("Exception dialog already shown once, skipping this time.");
                return;
            } else {
                dialogAlreadyShown = true;
            }
            logger.fine("Showing exception dialog...");
            ExceptionDialog dialog = getInstance();
            dialog.setExceptionText(traceback);
            dialog.setVisible(true);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton closeButton;
    private JButton copyButton;
    private JLabel detailsLabel;
    private JTextArea detailsTextArea;
    private JHtmlLabel errorLabel;
    private JLabel jLabel1;
    private JPanel jPanel1;
    private JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
