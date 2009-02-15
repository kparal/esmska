/*
 * MainFrame.java
 *
 * Created on 6. červenec 2007, 15:37
 */

package esmska.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JToolBar.Separator;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import javax.swing.WindowConstants;
import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jvnet.substance.SubstanceLookAndFeel;

import esmska.update.UpdateChecker;
import esmska.data.Config;
import esmska.data.History;
import esmska.data.History.Record;
import esmska.data.Icons;
import esmska.data.SMS;
import esmska.integration.ActionBean;
import esmska.integration.IntegrationAdapter;
import esmska.integration.MacUtils;
import esmska.persistence.ExportManager;
import esmska.persistence.PersistenceManager;
import esmska.transfer.SMSSender;
import esmska.utils.L10N;
import esmska.utils.Nullator;
import esmska.utils.OSType;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import org.openide.awt.Mnemonics;

/**
 * MainFrame form
 *
 * @author ripper
 */
public class MainFrame extends javax.swing.JFrame {
    private static MainFrame instance;
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;

    /** custom beans binding group */
    private BindingGroup bindGroup = new BindingGroup();
    
    // actions
    private Action quitAction = new QuitAction();
    private Action aboutAction = new AboutAction();
    private Action configAction = new ConfigAction();
    private ImportAction importAction = new ImportAction();
    private Action exportAction = new ExportAction();
    private HistoryAction historyAction = new HistoryAction();

    /** sender of sms */
    private SMSSender smsSender;
    /** manager of persistence data */
    private PersistenceManager persistenceManager;
    /** program configuration */
    private Config config = Config.getInstance();
    /** sms history */
    private History history = History.getInstance();
    /** shutdown handler thread */
    private Thread shutdownThread = new ShutdownThread();
    
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        instance = this;
        initComponents();
        
        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(new ImageIcon(getClass().getResource(RES + "esmska-16.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "esmska-32.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "esmska-64.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "esmska.png")).getImage());
        setIconImages(images);
        
        //hide on Ctrl+W
        String command = "hide";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //hide only when notification icon present or on mac
                if (NotificationIcon.isInstalled() || OSType.isMac()) {
                    formWindowClosing(new WindowEvent(MainFrame.this, 0));
                }
            }
        });
        
        //on Mac, move program menu items to system menu
        if (OSType.isMac()) {
            logger.fine("Running on Mac OS, hiding some menu items...");
            try {
                ActionBean bean = new ActionBean();
                bean.setQuitAction(quitAction);
                bean.setAboutAction(aboutAction);
                bean.setConfigAction(configAction);
                
                IntegrationAdapter.getInstance().setActionBean(bean);
                programMenu.setVisible(false);
                aboutMenuItem.setVisible(false);
                //should be helpSeparator.setVisible(false); but bug #6365547 in Apple Java precludes it
                helpMenu.remove(helpSeparator);
            }
            catch (Throwable ex) {
                logger.log(Level.WARNING, "Can't integrate program menu items to " +
                        "Mac system menu", ex);
            }
        }
        
        //set tooltip delay
        ToolTipManager.sharedInstance().setInitialDelay(750);
        ToolTipManager.sharedInstance().setDismissDelay(60000);
        
        //init custom components
        smsSender = new SMSSender();
        
        //load config
        try {
            persistenceManager = PersistenceManager.getInstance();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not create program dir with config files", ex);
            statusPanel.setStatusMessage(l10n.getString("MainFrame.cant_create_program_dir"),
                    false, Icons.STATUS_ERROR, true);
        }
        loadConfig();
        if (PersistenceManager.getQueue().size() > 0) {
            queuePanel.setPaused(true);
        }
        
        //setup components
        contactPanel.requestFocusInWindow();
        contactPanel.ensureContactSelected();
        
        //check for valid operators
        if (PersistenceManager.getOperators().size() <= 0) {
            logger.warning("No usable operators found");
            JOptionPane.showMessageDialog(null,
                    new JLabel(l10n.getString("MainFrame.no_operators")),
                    null, JOptionPane.ERROR_MESSAGE);
        }
        
        //use bindings
        Binding bind = Bindings.createAutoBinding(UpdateStrategy.READ, config, 
                BeanProperty.create("toolbarVisible"), toolBar, BeanProperty.create("visible"));
        bindGroup.addBinding(bind);
        bindGroup.bind();
        
        //check for updates
        if (config.isCheckForUpdates()) {
            UpdateChecker updateChecker = new UpdateChecker();
            updateChecker.addActionListener(new UpdateListener());
            updateChecker.checkForUpdates();
        }

        //add shutdown handler, when program is closed externally (logout, SIGTERM, etc)
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }
    
    /** Get current instance */
    public static MainFrame getInstance() {
        if (instance == null) {
            instance = new MainFrame();
        }
        return instance;
    }
    
    /** Start the mainframe and let it be visible according to user preferences
     * (visible on the screen or hidden to notification area) */
    public void startAndShow() {
        logger.fine("Showing mainframe...");
        //if the window should be minimized into notification area
        if (config.isStartMinimized() && NotificationIcon.isInstalled()) {
            logger.fine("Starting hidden in notification icon");
            //hide splashscreen, otherwise on Windows it stays visible until mainframe is shown
            SplashScreen splash = SplashScreen.getSplashScreen();
            if (splash != null && splash.isVisible()) {
                splash.close();
            }
        } else {
            //show the form
            this.setVisible(true);
        }
    }

    /** Notifies about change in sms queue */
    public void smsProcessed(SMS sms) {
        logger.fine("SMS processed: " + sms.toDebugString());
        if (sms.getStatus() == SMS.Status.SENT_OK) {
            statusPanel.setStatusMessage(
                    MessageFormat.format(l10n.getString("MainFrame.sms_sent"), sms),
                    true, Icons.STATUS_MESSAGE, true);
            createHistory(sms);

            if (smsPanel.getText().length() > 0) {
                smsPanel.requestFocusInWindow();
            } else {
                contactPanel.requestFocusInWindow();
            }
        } else if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
            logger.info("Message for " + sms + " could not be sent");
            queuePanel.setPaused(true);
            statusPanel.setStatusMessage(
                    MessageFormat.format(l10n.getString("MainFrame.sms_failed"), sms),
                    true, Icons.STATUS_WARNING, true);

            //prepare dialog
            String cause = (sms.getErrMsg() != null ? sms.getErrMsg().trim() : "");
            JLabel label = new JLabel(
                    MessageFormat.format(l10n.getString("MainFrame.sms_failed2"),
                    sms, cause));
            label.setVerticalAlignment(SwingConstants.TOP);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(label, BorderLayout.CENTER);
            JOptionPane pane = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE);
            JDialog dialog = pane.createDialog(MainFrame.this, null);

            //check if the dialog is not wider than screen
            //(very ugly, but it seems there is no clean solution in Swing for this)
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = panel.getWidth();
            int height = panel.getHeight();
            if (dialog.getWidth() > screenSize.getWidth()) { //wider than screen
                width = (int) screenSize.getWidth() * 2/3;
                height = height * (panel.getWidth() / width);
                panel.setPreferredSize(new Dimension(width, height));
                dialog = pane.createDialog(MainFrame.this, null); //create dialog again
            }

            //show the dialog
            logger.fine("Showing reason why SMS sending failed...");
            MacUtils.setDocumentModalDialog(dialog);
            dialog.setResizable(true);
            dialog.pack(); //always pack after setting resizable, Windows LaF crops dialog otherwise
            dialog.setVisible(true);

            //transfer focus
            if (smsPanel.getText().length() > 0) {
                smsPanel.requestFocusInWindow();
            } else {
                queuePanel.requestFocusInWindow();
            }
        }

        //show operator message if present
        if (!Nullator.isEmpty(sms.getOperatorMsg())) {
            statusPanel.setStatusMessage(sms.getOperator() + ": " + sms.getOperatorMsg(),
                    true, Icons.STATUS_MESSAGE, true);
        }

        if (!smsSender.isRunning()) {
            statusPanel.setTaskRunning(false);
        }
        queuePanel.smsProcessed(sms);
    }

    /** Display random tip from the collection of tips */
    public void showTipOfTheDay() {
        try {
            List tips = IOUtils.readLines(
                    getClass().getResourceAsStream(RES + "tips.txt"), "UTF-8");
            int random = new Random().nextInt(tips.size());
            statusPanel.setStatusMessage(l10n.getString("MainFrame.tip") + " " +
                    l10n.getString((String)tips.get(random)), false, null, false);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Can't display tip of the day", ex);
        }
    }

    /** get action used to show history frame */
    public Action getHistoryAction() {
        return historyAction;
    }

    /** get action used to exit the application */
    public Action getQuitAction() {
        return quitAction;
    }

    /** get action used to show settings frame */
    public Action getConfigAction() {
        return configAction;
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public QueuePanel getQueuePanel() {
        return queuePanel;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        horizontalSplitPane = new JSplitPane();
        verticalSplitPane = new JSplitPane();
        smsPanel = new SMSPanel();
        queuePanel = new QueuePanel();
        contactPanel = new ContactPanel();
        statusPanel = new StatusPanel();
        jSeparator1 = new JSeparator();
        toolBar = new JToolBar();
        compressButton = new JButton();
        undoButton = new JButton();
        redoButton = new JButton();
        jSeparator2 = new Separator();
        historyButton = new JButton();
        jSeparator3 = new Separator();
        configButton = new JButton();
        exitButton = new JButton();
        menuBar = new JMenuBar();
        programMenu = new JMenu();
        configMenuItem = new JMenuItem();
        exitMenuItem = new JMenuItem();
        messageMenu = new JMenu();
        undoMenuItem = new JMenuItem();
        redoMenuItem = new JMenuItem();
        jSeparator5 = new JSeparator();
        compressMenuItem = new JMenuItem();
        sendMenuItem = new JMenuItem();
        toolsMenu = new JMenu();
        historyMenuItem = new JMenuItem();
        logMenuItem = new JMenuItem();
        jSeparator4 = new JSeparator();
        importMenuItem = new JMenuItem();
        exportMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        faqMenuItem = new JMenuItem();
        getHelpMenuItem = new JMenuItem();
        translateMenuItem = new JMenuItem();
        problemMenuItem = new JMenuItem();
        helpSeparator = new JSeparator();
        aboutMenuItem = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Esmska"); // NOI18N
        setLocationByPlatform(true);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        horizontalSplitPane.setBorder(null);
        horizontalSplitPane.setResizeWeight(0.5);
        horizontalSplitPane.setContinuousLayout(true);
        horizontalSplitPane.setOneTouchExpandable(true);
        horizontalSplitPane.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        verticalSplitPane.setBorder(null);
        verticalSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setResizeWeight(1.0);
        verticalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setOneTouchExpandable(true);
        verticalSplitPane.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        smsPanel.addActionListener(new SMSListener());
        smsPanel.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        verticalSplitPane.setLeftComponent(smsPanel);

        queuePanel.addActionListener(new QueueListener());
        queuePanel.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        verticalSplitPane.setRightComponent(queuePanel);

        horizontalSplitPane.setLeftComponent(verticalSplitPane);

        contactPanel.addActionListener(new ContactListener());
        contactPanel.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        horizontalSplitPane.setRightComponent(contactPanel);

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.add(Box.createRigidArea(new Dimension(5, 1)));

        compressButton.setAction(smsPanel.getCompressAction());
        compressButton.setToolTipText(l10n.getString("MainFrame.compressButton.toolTipText")); // NOI18N
        compressButton.setFocusable(false);
        compressButton.setHideActionText(true);
        toolBar.add(compressButton);

        undoButton.setAction(smsPanel.getUndoAction());
        undoButton.setToolTipText(l10n.getString("MainFrame.undoButton.toolTipText")); // NOI18N
        undoButton.setFocusable(false);
        undoButton.setHideActionText(true);
        toolBar.add(undoButton);

        redoButton.setAction(smsPanel.getRedoAction());
        redoButton.setToolTipText(l10n.getString("MainFrame.redoButton.toolTipText")); // NOI18N
        redoButton.setFocusable(false);
        redoButton.setHideActionText(true);
        toolBar.add(redoButton);
        toolBar.add(jSeparator2);

        historyButton.setAction(historyAction);
        historyButton.setToolTipText(historyAction.getValue(Action.NAME).toString() + " (Ctrl+T)");
        historyButton.setFocusable(false);
        historyButton.setHideActionText(true);
        toolBar.add(historyButton);
        toolBar.add(jSeparator3);

        configButton.setAction(configAction);
        configButton.setToolTipText(configAction.getValue(Action.NAME).toString());
        configButton.setFocusable(false);
        configButton.setHideActionText(true);
        toolBar.add(configButton);

        exitButton.setAction(quitAction);
        exitButton.setToolTipText(quitAction.getValue(Action.NAME).toString() + " (Ctrl+Q)");
        exitButton.setFocusable(false);
        exitButton.setHideActionText(true);
        toolBar.add(exitButton);

        for (Component comp : toolBar.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                //disable mnemonics for buttons
                button.setMnemonic(0);
                //make icons prettier on Mac OS X
                button.putClientProperty("JButton.buttonType", "gradient");
            }
        }

        Mnemonics.setLocalizedText(programMenu, l10n.getString("MainFrame.programMenu.text")); // NOI18N
        configMenuItem.setAction(configAction);
        programMenu.add(configMenuItem);

        exitMenuItem.setAction(quitAction);
        programMenu.add(exitMenuItem);

        menuBar.add(programMenu);


        Mnemonics.setLocalizedText(messageMenu, l10n.getString("MainFrame.messageMenu.text")); // NOI18N
        undoMenuItem.setAction(smsPanel.getUndoAction());
        messageMenu.add(undoMenuItem);

        redoMenuItem.setAction(smsPanel.getRedoAction());
        messageMenu.add(redoMenuItem);
        messageMenu.add(jSeparator5);

        compressMenuItem.setAction(smsPanel.getCompressAction());
        messageMenu.add(compressMenuItem);

        sendMenuItem.setAction(smsPanel.getSendAction());
        messageMenu.add(sendMenuItem);

        menuBar.add(messageMenu);


        Mnemonics.setLocalizedText(toolsMenu, l10n.getString("MainFrame.toolsMenu.text")); // NOI18N
        historyMenuItem.setAction(historyAction);
        toolsMenu.add(historyMenuItem);

        logMenuItem.setAction(statusPanel.getLogAction());
        toolsMenu.add(logMenuItem);
        toolsMenu.add(jSeparator4);

        importMenuItem.setAction(importAction);
        toolsMenu.add(importMenuItem);

        exportMenuItem.setAction(exportAction);
        toolsMenu.add(exportMenuItem);

        menuBar.add(toolsMenu);

        Mnemonics.setLocalizedText(helpMenu, l10n.getString("MainFrame.helpMenu.text")); // NOI18N
        faqMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        faqMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/faq-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(faqMenuItem, l10n.getString("MainFrame.faqMenuItem.text"));
        faqMenuItem.setToolTipText(l10n.getString("MainFrame.faqMenuItem.toolTipText")); // NOI18N
        faqMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                faqMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(faqMenuItem);

        getHelpMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/getHelp-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(getHelpMenuItem, l10n.getString("MainFrame.getHelpMenuItem.text")); // NOI18N
        getHelpMenuItem.setToolTipText(l10n.getString("MainFrame.getHelpMenuItem.toolTipText")); // NOI18N
        getHelpMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                getHelpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(getHelpMenuItem);

        translateMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/translate-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(translateMenuItem, l10n.getString("MainFrame.translateMenuItem.text")); // NOI18N
        translateMenuItem.setToolTipText(l10n.getString("MainFrame.translateMenuItem.toolTipText")); // NOI18N
        translateMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                translateMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(translateMenuItem);

        problemMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/bug-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(problemMenuItem, l10n.getString("MainFrame.problemMenuItem.text")); // NOI18N
        problemMenuItem.setToolTipText(l10n.getString("MainFrame.problemMenuItem.toolTipText")); // NOI18N
        problemMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                problemMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(problemMenuItem);
        helpMenu.add(helpSeparator);

        aboutMenuItem.setAction(aboutAction);
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(statusPanel, GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addComponent(jSeparator1, GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 596, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //if user clicked on close button (event non-null) and notification icon
        //installed or on mac, just hide the main window
        if (evt != null && (NotificationIcon.isInstalled() || OSType.isMac())) {
            logger.fine("Hiding main window");
            if (OSType.isMac()) {
                this.setVisible(false);
            } else {
                NotificationIcon.toggleMainFrameVisibility();
            }
            return;
        }

        logger.fine("Closing main window...");

        //user requested program close, shutdown handler not needed
        Runtime.getRuntime().removeShutdownHook(shutdownThread);

        //save end exit
        boolean saveOk = saveAll();
        if (!saveOk) { //some data were not saved
            JOptionPane.showMessageDialog(this,
                    l10n.getString("MainFrame.cant_save_config"),
                    null, JOptionPane.WARNING_MESSAGE);
        }
        int returnCode = saveOk ? 0 : 1;
        logger.fine("Exiting program with return code: " + returnCode);
        System.exit(returnCode);
    }//GEN-LAST:event_formWindowClosing

private void faqMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_faqMenuItemActionPerformed
    if (!Desktop.isDesktopSupported()) {
        return;
    }
    //start browser
    Desktop desktop = Desktop.getDesktop();
    String url = "http://code.google.com/p/esmska/wiki/FAQ";
    try {
        logger.fine("Browsing URL: " + url);
        desktop.browse(new URL(url).toURI());
    } catch (Exception e) {
        logger.log(Level.WARNING, "Could not browse URL: " + url, e);
    }
}//GEN-LAST:event_faqMenuItemActionPerformed

private void getHelpMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_getHelpMenuItemActionPerformed
    if (!Desktop.isDesktopSupported()) {
        return;
    }
    //start browser
    Desktop desktop = Desktop.getDesktop();
    String url = "https://answers.launchpad.net/esmska";
    try {
        logger.fine("Browsing URL: " + url);
        desktop.browse(new URL(url).toURI());
    } catch (Exception e) {
        logger.log(Level.WARNING, "Could not browse URL: " + url, e);
    }
}//GEN-LAST:event_getHelpMenuItemActionPerformed

private void translateMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_translateMenuItemActionPerformed
    if (!Desktop.isDesktopSupported()) {
        return;
    }
    //start browser
    Desktop desktop = Desktop.getDesktop();
    String url = "https://translations.launchpad.net/esmska";
    try {
        logger.fine("Browsing URL: " + url);
        desktop.browse(new URL(url).toURI());
    } catch (Exception e) {
        logger.log(Level.WARNING, "Could not browse URL: " + url, e);
    }
}//GEN-LAST:event_translateMenuItemActionPerformed

private void problemMenuItemActionPerformed(ActionEvent evt) {//GEN-FIRST:event_problemMenuItemActionPerformed
    if (!Desktop.isDesktopSupported()) {
        return;
    }
    //start browser
    Desktop desktop = Desktop.getDesktop();
    String url = "http://code.google.com/p/esmska/wiki/Issues";
    try {
        logger.fine("Browsing URL: " + url);
        desktop.browse(new URL(url).toURI());
    } catch (Exception e) {
        logger.log(Level.WARNING, "Could not browse URL: " + url, e);
    }
}//GEN-LAST:event_problemMenuItemActionPerformed

    /** Save all user data
     * @return true if all saved ok; false otherwise
     */
    private boolean saveAll() {
        logger.fine("Saving user data...");
        boolean saveOk = true;
        //save all settings
        try {
            saveOk = saveConfig() && saveOk;
            saveOk = saveContacts() && saveOk;
            saveOk = saveQueue() && saveOk;
            saveOk = saveHistory() && saveOk;
            saveOk = saveKeyring() && saveOk;
            return saveOk;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Serious error during saving user data", t);
            return false;
        }
    }
    
    /** Saves history of sent sms */
    private void createHistory(SMS sms) {
        History.Record record = new History.Record();
        record.setDate(new Date());
        record.setName(sms.getName());
        record.setNumber(sms.getNumber());
        record.setOperator(sms.getOperator());
        record.setSenderName(sms.getSenderName());
        record.setSenderNumber(sms.getSenderNumber());
        record.setText(sms.getText());
        
        history.addRecord(record);
    }
    
    /** save program configuration
     * @return true if saved ok; false otherwise
     */
    private boolean saveConfig() {
        //save frame layout
        config.setMainDimension(this.getSize());
        config.setHorizontalSplitPaneLocation(horizontalSplitPane.getDividerLocation());
        config.setVerticalSplitPaneLocation(verticalSplitPane.getDividerLocation());
        
        try {
            persistenceManager.saveConfig();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save config", ex);
            return false;
        }
    }
    
    /** load program configuration */
    private void loadConfig() {
        logger.finer("Initializing according to config...");
        //set frame layout
        if (!config.isForgetLayout()) {
            Dimension mainDimension = config.getMainDimension();
            Integer horizontalSplitPaneLocation = config.getHorizontalSplitPaneLocation();
            Integer verticalSplitPaneLocation = config.getVerticalSplitPaneLocation();
            if (mainDimension != null) {
                this.setSize(mainDimension);
            }
            if (horizontalSplitPaneLocation != null) {
                horizontalSplitPane.setDividerLocation(horizontalSplitPaneLocation);
            }
            if (verticalSplitPaneLocation != null) {
                verticalSplitPane.setDividerLocation(verticalSplitPaneLocation);
            }
        }
        
        //set window centered
        if (config.isStartCentered()) {
            setLocationRelativeTo(null);
        }
        
        //select last contact
        if (history.getRecords().size() > 0) {
            contactPanel.setSelectedContact(
                    history.getRecord(history.getRecords().size()-1).getName());
            contactPanel.makeNiceSelection();
        }
        
        //show notification icon
        if (config.isNotificationIconVisible()) {
            NotificationIcon.install();
        }
        
        //show tip of the day
        if (config.isShowTips()) {
            showTipOfTheDay();
        }
    }
    
    /** save contacts
     * @return true if saved ok; false otherwise
     */
    private boolean saveContacts() {
        try {
            persistenceManager.saveContacts();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save contacts", ex);
            return false;
        }
    }
    
    /** save sms queue
     * @return true if saved ok; false otherwise
     */
    private boolean saveQueue() {
        try {
            persistenceManager.saveQueue();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save queue", ex);
            return false;
        }
    }
    
    /** save sms history
     * @return true if saved ok; false otherwise
     */
    private boolean saveHistory() {
        //erase old messages from history if demanded
        if (config.isReducedHistory()) {
            List<Record> records = history.getRecords();
            //computer last acceptable record time
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.DAY_OF_MONTH, -config.getReducedHistoryCount());
            Date limit = limitCal.getTime();
            //traverse through history and erase all older records than the limit time
            logger.fine("Erasing all history records older than: " + limit);
            ListIterator<Record> iter = records.listIterator();
            while (iter.hasNext()) {
                Record record = iter.next();
                if (record.getDate().before(limit)) {
                    iter.remove();
                } else {
                    //records are sorted in time, therefore on first newer message
                    //stop iterating
                    break;
                }
            }
        }
        
        try {
            persistenceManager.saveHistory();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save history", ex);
            return false;
        }
    }
    
    /** save keyring 
     * @return true if saved ok; false otherwise
     */
    private boolean saveKeyring() {
        try {
            persistenceManager.saveKeyring();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save keyring", ex);
            return false;
        }
    }
    
    /** Show about frame */
    private class AboutAction extends AbstractAction {
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
                aboutFrame.setLocationRelativeTo(MainFrame.this);
                aboutFrame.setVisible(true);
            }
        }
    }
    
    /** Quit the program */
    private class QuitAction extends AbstractAction {
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
            MainFrame.this.formWindowClosing(null);
            System.exit(0);
        }
    }
    
    /** Show config frame */
    private class ConfigAction extends AbstractAction {
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
                configFrame.setLocationRelativeTo(MainFrame.this);
                configFrame.setVisible(true);
            }
        }
    }
    
    /** import data from other programs/formats */
    private class ImportAction extends AbstractAction {
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
                importFrame.setLocationRelativeTo(MainFrame.this);
                importFrame.addActionListener(new ImportListener());
                importFrame.setVisible(true);
            }
        }
        public ImportFrame getImportFrame() {
            return importFrame;
        }
    }
    
    /** export data for other programs */
    private class ExportAction extends AbstractAction {
        public ExportAction() {
            L10N.setLocalizedText(this, l10n.getString("Contact_export_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "contact-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("MainFrame.export_contacts_to_file"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Showing export contacts dialog...");
            ExportManager.exportContacts(MainFrame.this, PersistenceManager.getContacs());
        }
    }
    
    /** show the history frame */
    private class HistoryAction extends AbstractAction {
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
                historyFrame.setLocationRelativeTo(MainFrame.this);
                historyFrame.addActionListener(new HistoryListener());
                historyFrame.setVisible(true);
            }
        }
        public HistoryFrame getHistoryFrame() {
            return historyFrame;
        }
    }
    
    /** Listens for events from sms queue */
    private class QueueListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                //edit sms in queue
                case QueuePanel.ACTION_REQUEST_EDIT_SMS:
                    SMS sms = queuePanel.getEditRequestedSMS();
                    if (sms == null) {
                        return;
                    }
                    smsPanel.setSMS(sms);
                    break;
            }
        }
    }

    /** Listens for events from sms history table */
    private class HistoryListener implements ActionListener {
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
            
            contactPanel.clearSelection();
            smsPanel.setSMS(sms);
        }
    }
    
    /** Listener for new imported contacts */
    private class ImportListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                case ImportFrame.ACTION_IMPORT_CONTACTS:
                    contactPanel.clearSelection();
                    contactPanel.addContacts(importAction.getImportFrame().getImportedContacts());
                    statusPanel.setStatusMessage(l10n.getString("MainFrame.import_complete"),
                            true, Icons.STATUS_INFO, true);
                    break;
            }
        }
    }

    /** Listens for changes in contact list */
    private class ContactListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                case ContactPanel.ACTION_CONTACT_SELECTION_CHANGED:
                    smsPanel.setContacts(contactPanel.getSelectedContacts());
                    break;
                case ContactPanel.ACTION_CONTACT_CHOSEN:
                    smsPanel.requestFocusInWindow();
                    break;
            }
        }
    }
    
    /** Listens for changes in sms panel */
    private class SMSListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                case SMSPanel.ACTION_REQUEST_CLEAR_CONTACT_SELECTION:
                    contactPanel.clearSelection();
                    break;
                case SMSPanel.ACTION_REQUEST_SELECT_CONTACT:
                    contactPanel.setSelectedContact(smsPanel.getRequestedContactSelection());
                    break;
                case SMSPanel.ACTION_SEND_SMS:
                    for (SMS sms : smsPanel.getEnvelope().generate()) {
                        queuePanel.addSMS(sms);
                    }
                    break;
            }
        }
    }
    
    /** Listens for events from update checker */
    private class UpdateListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            statusPanel.setStatusMessage(l10n.getString("MainFrame.new_program_version"), 
                    false, Icons.STATUS_UPDATE, true);
        }
    }

    /** Thread used when program is externally forced to shut down (logout, SIGTERM, etc) */
    private class ShutdownThread extends Thread {
        @Override
        public void run() {
            logger.fine("Program closing down...");
            saveAll();
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JMenuItem aboutMenuItem;
    private JButton compressButton;
    private JMenuItem compressMenuItem;
    private JButton configButton;
    private JMenuItem configMenuItem;
    private ContactPanel contactPanel;
    private JButton exitButton;
    private JMenuItem exitMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem faqMenuItem;
    private JMenuItem getHelpMenuItem;
    private JMenu helpMenu;
    private JSeparator helpSeparator;
    private JButton historyButton;
    private JMenuItem historyMenuItem;
    private JSplitPane horizontalSplitPane;
    private JMenuItem importMenuItem;
    private JSeparator jSeparator1;
    private Separator jSeparator2;
    private Separator jSeparator3;
    private JSeparator jSeparator4;
    private JSeparator jSeparator5;
    private JMenuItem logMenuItem;
    private JMenuBar menuBar;
    private JMenu messageMenu;
    private JMenuItem problemMenuItem;
    private JMenu programMenu;
    private QueuePanel queuePanel;
    private JButton redoButton;
    private JMenuItem redoMenuItem;
    private JMenuItem sendMenuItem;
    private SMSPanel smsPanel;
    private StatusPanel statusPanel;
    private JToolBar toolBar;
    private JMenu toolsMenu;
    private JMenuItem translateMenuItem;
    private JButton undoButton;
    private JMenuItem undoMenuItem;
    private JSplitPane verticalSplitPane;
    // End of variables declaration//GEN-END:variables
}
