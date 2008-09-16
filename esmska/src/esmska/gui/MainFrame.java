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
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jvnet.substance.SubstanceLookAndFeel;

import esmska.update.UpdateChecker;
import esmska.data.Config;
import esmska.data.Contact;
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
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.ResourceBundle;
import javax.swing.JComponent;

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

    /** actual queue of sms's */
    private List<SMS> smsQueue = PersistenceManager.getQueue();
    /** sender of sms */
    private SMSSender smsSender;
    /** manager of persistence data */
    private PersistenceManager persistenceManager;
    /** program configuration */
    private Config config = PersistenceManager.getConfig();
    /** sms contacts */
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    /** sms history */
    private History history = PersistenceManager.getHistory();
    /** whether user data were saved successfully */
    private boolean saveOk = true;
    
    
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
            try {
                ActionBean bean = new ActionBean();
                bean.setQuitAction(quitAction);
                bean.setAboutAction(aboutAction);
                bean.setConfigAction(configAction);
                
                IntegrationAdapter.getInstance().setActionBean(bean);
                programMenu.setVisible(false);
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
        if (smsQueue.size() > 0) {
            queuePanel.setPaused(true);
        }
        
        //setup components
        contactPanel.requestFocusInWindow();
        contactPanel.ensureContactSelected();
        
        //check for valid operators
        if (PersistenceManager.getOperators().size() <= 0) {
            JOptionPane.showMessageDialog(null,
                    l10n.getString("MainFrame.no_operators"),
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
        //if the window should be minimized into notification area
        if (config.isStartMinimized() && NotificationIcon.isInstalled()) {
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        horizontalSplitPane = new javax.swing.JSplitPane();
        verticalSplitPane = new javax.swing.JSplitPane();
        smsPanel = new esmska.gui.SMSPanel();
        queuePanel = new esmska.gui.QueuePanel();
        contactPanel = new esmska.gui.ContactPanel();
        statusPanel = new esmska.gui.StatusPanel();
        jSeparator1 = new javax.swing.JSeparator();
        toolBar = new javax.swing.JToolBar();
        compressButton = new javax.swing.JButton();
        undoButton = new javax.swing.JButton();
        redoButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        historyButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        configButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        programMenu = new javax.swing.JMenu();
        configMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        messageMenu = new javax.swing.JMenu();
        undoMenuItem = new javax.swing.JMenuItem();
        redoMenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        compressMenuItem = new javax.swing.JMenuItem();
        sendMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        historyMenuItem = new javax.swing.JMenuItem();
        logMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        importMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Esmska"); // NOI18N
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        horizontalSplitPane.setBorder(null);
        horizontalSplitPane.setResizeWeight(0.5);
        horizontalSplitPane.setContinuousLayout(true);
        horizontalSplitPane.setOneTouchExpandable(true);
        horizontalSplitPane.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        verticalSplitPane.setBorder(null);
        verticalSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
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

        org.openide.awt.Mnemonics.setLocalizedText(programMenu, l10n.getString("MainFrame.programMenu.text")); // NOI18N

        configMenuItem.setAction(configAction);
        programMenu.add(configMenuItem);

        aboutMenuItem.setAction(aboutAction);
        programMenu.add(aboutMenuItem);

        exitMenuItem.setAction(quitAction);
        programMenu.add(exitMenuItem);

        menuBar.add(programMenu);

        org.openide.awt.Mnemonics.setLocalizedText(messageMenu, l10n.getString("MainFrame.messageMenu.text")); // NOI18N

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

        org.openide.awt.Mnemonics.setLocalizedText(toolsMenu, l10n.getString("MainFrame.toolsMenu.text")); // NOI18N

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

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(horizontalSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(toolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(horizontalSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        //if user clicked on close button (event non-null) and notification icon
        //installed or on mac, just hide the main window
        if (evt != null && (NotificationIcon.isInstalled() || OSType.isMac())) {
            if (OSType.isMac()) {
                this.setVisible(false);
            } else {
                NotificationIcon.toggleMainFrameVisibility();
            }
            return;
        }
        
        //save all settings
        try {
            saveConfig();
            saveContacts();
            saveQueue();
            saveHistory();
            saveKeyring();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Serious error during saving", t);
        } finally {
            if (!saveOk) { //some data were not saved
                logger.warning("Some config files were not saved");
                JOptionPane.showMessageDialog(this,
                        l10n.getString("MainFrame.cant_save_config"),
                        null, JOptionPane.WARNING_MESSAGE);
            }
            System.exit(saveOk ? 0 : 1);
        }
    }//GEN-LAST:event_formWindowClosing
    
    /** Notifies about change in sms queue */
    public void smsProcessed(SMS sms) {
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
    
    /** save program configuration */
    private void saveConfig() {
        //save frame layout
        config.setMainDimension(this.getSize());
        config.setHorizontalSplitPaneLocation(horizontalSplitPane.getDividerLocation());
        config.setVerticalSplitPaneLocation(verticalSplitPane.getDividerLocation());
        
        try {
            persistenceManager.saveConfig();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save config", ex);
            saveOk = false;
        }
    }
    
    /** load program configuration */
    private void loadConfig() {
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
    
    /** save contacts */
    private void saveContacts() {
        try {
            persistenceManager.saveContacts();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save contacts", ex);
            saveOk = false;
        }
    }
    
    /** save sms queue */
    private void saveQueue() {
        try {
            persistenceManager.saveQueue();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save queue", ex);
            saveOk = false;
        }
    }
    
    /** save sms history */
    private void saveHistory() {
        //erase old messages from history if demanded
        if (config.isReducedHistory()) {
            ArrayList<Record> records = history.getRecords();
            //computer last acceptable record time
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.DAY_OF_MONTH, -config.getReducedHistoryCount());
            Date limit = limitCal.getTime();
            //traverse through history and erase all older records than the limit time
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
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save history", ex);
            saveOk = false;
        }
    }
    
    /** save keyring */
    private void saveKeyring() {
        try {
            persistenceManager.saveKeyring();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save keyring", ex);
            saveOk = false;
        }
    }
    
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
    
    public QueuePanel getQueuePanel() {
        return queuePanel;
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
            MainFrame.this.formWindowClosing(null);
            System.exit(0);
        }
    }
    
    /** get action used to exit the application */
    public Action getQuitAction() {
        return quitAction;
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
    
    /** get action used to show settings frame */
    public Action getConfigAction() {
        return configAction;
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
            ExportManager.exportContacts(MainFrame.this, contacts);
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
    
    /** get action used to show history frame */
    public Action getHistoryAction() {
        return historyAction;
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
                    contactPanel.clearSelection();
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton compressButton;
    private javax.swing.JMenuItem compressMenuItem;
    private javax.swing.JButton configButton;
    private javax.swing.JMenuItem configMenuItem;
    private esmska.gui.ContactPanel contactPanel;
    private javax.swing.JButton exitButton;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JButton historyButton;
    private javax.swing.JMenuItem historyMenuItem;
    private javax.swing.JSplitPane horizontalSplitPane;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JMenuItem logMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu messageMenu;
    private javax.swing.JMenu programMenu;
    private esmska.gui.QueuePanel queuePanel;
    private javax.swing.JButton redoButton;
    private javax.swing.JMenuItem redoMenuItem;
    private javax.swing.JMenuItem sendMenuItem;
    private esmska.gui.SMSPanel smsPanel;
    private esmska.gui.StatusPanel statusPanel;
    private javax.swing.JToolBar toolBar;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JButton undoButton;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JSplitPane verticalSplitPane;
    // End of variables declaration//GEN-END:variables
}
