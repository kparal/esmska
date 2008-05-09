/*
 * MainFrame.java
 *
 * Created on 6. červenec 2007, 15:37
 */

package esmska.gui;

import esmska.data.Envelope;
import esmska.persistence.ExportManager;
import esmska.transfer.SMSSender;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import esmska.data.Config;
import esmska.data.Contact;
import esmska.persistence.PersistenceManager;
import esmska.data.SMS;
import esmska.UpdateChecker;
import esmska.data.History;
import esmska.data.Icons;
import esmska.utils.Nullator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * MainFrame form
 *
 * @author ripper
 */
public class MainFrame extends javax.swing.JFrame {
    private static MainFrame instance;
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    private static final String RES = "/esmska/resources/";

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
    /** box for messages */
    private Envelope envelope;
    /** timer to send another sms after defined delay */
    private Timer smsDelayTimer = new Timer(1000,new SMSDelayActionListener());
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

        //set tooltip delay
        ToolTipManager.sharedInstance().setInitialDelay(750);
        ToolTipManager.sharedInstance().setDismissDelay(60000);
        
        //init custom components
        smsSender = new SMSSender(smsQueue);
        envelope = new Envelope();
        smsPanel.setEnvelope(envelope);
        
        //load config
        try {
            persistenceManager = PersistenceManager.getInstance();
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not create program dir with config files", ex);
            statusPanel.setStatusMessage("Nepovedlo se vytvořit adresář s nastavením programu!",
                    false, Icons.STATUS_ERROR, true);
        }
        loadConfig();
        if (smsQueue.size() > 0)
            pauseSMSQueue(true);
        
        //setup components
        smsDelayTimer.setInitialDelay(0);
        contactPanel.requestFocusInWindow();
        contactPanel.ensureContactSelected();
        
        //check for valid operators
        if (PersistenceManager.getOperators().size() <= 0) {
            JOptionPane.showMessageDialog(null,
                    "<html><h2>Nepodařilo se nalézt žádné operátory!</h2>" +
                    "Bez operátorů je program nepoužitelný. Problém může pramenit " +
                    "z těchto příčin:<br>" +
                    "<ul><li>Váš program je nekorektně nainstalován a chybí mu některé<br>" +
                    "soubory nebo jsou poškozeny. Zkuste jej stáhnout znovu.</li>" +
                    "<li>Operační systém špatně nastavil cestu k programu.<br>" +
                    "Zkuste místo poklikání na <i>esmska.jar</i> raději program spustit pomocí<br>" +
                    "souboru <i>esmska.sh</i> (v Linuxu, apod) nebo <i>esmska.bat</i> (ve Windows).</li>" +
                    "</ul></html>",
                    null, JOptionPane.ERROR_MESSAGE);
        }
        
        //use bindings
        Binding bind = Bindings.createAutoBinding(UpdateStrategy.READ, config, 
                BeanProperty.create("toolbarVisible"), toolBar, BeanProperty.create("visible"));
        bindGroup.addBinding(bind);
        bindGroup.bind();
        
        //show tip of the day
        showTipOfTheDay();
        
        //check for updates
        if (config.isCheckForUpdates()) {
            UpdateChecker updateChecker = new UpdateChecker();
            updateChecker.addActionListener(new UpdateListener());
            updateChecker.checkForUpdates();
        }
    }
    
    /** Get current instance */
    public static MainFrame getInstance() {
        if (instance == null)
            instance = new MainFrame();
        return instance;
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
        compressMenuItem = new javax.swing.JMenuItem();
        sendMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        historyMenuItem = new javax.swing.JMenuItem();
        logMenuItem = new javax.swing.JMenuItem();
        importMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Esmska");
        setIconImage(new ImageIcon(getClass().getResource(RES + "esmska.png")).getImage());
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
        compressButton.setToolTipText("Zkomprimovat zprávu (Ctrl+K)");
        compressButton.setFocusable(false);
        compressButton.setHideActionText(true);
        toolBar.add(compressButton);

        undoButton.setAction(smsPanel.getUndoAction());
        undoButton.setToolTipText("Zpět (Ctrl+Z)");
        undoButton.setFocusable(false);
        undoButton.setHideActionText(true);
        toolBar.add(undoButton);

        redoButton.setAction(smsPanel.getRedoAction());
        redoButton.setToolTipText("Vpřed (Ctrl+Y)");
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

        //disable mnemonics for buttons
        for (Component comp : toolBar.getComponents()) {
            if (comp instanceof JButton) {
                ((JButton)comp).setMnemonic(0);
            }
        }

        programMenu.setMnemonic('r');
        programMenu.setText("Program");

        configMenuItem.setAction(configAction);
        programMenu.add(configMenuItem);

        aboutMenuItem.setAction(aboutAction);
        programMenu.add(aboutMenuItem);

        exitMenuItem.setAction(quitAction);
        programMenu.add(exitMenuItem);

        menuBar.add(programMenu);

        messageMenu.setMnemonic('z');
        messageMenu.setText("Zpráva");

        undoMenuItem.setAction(smsPanel.getUndoAction());
        messageMenu.add(undoMenuItem);

        redoMenuItem.setAction(smsPanel.getRedoAction());
        messageMenu.add(redoMenuItem);

        compressMenuItem.setAction(smsPanel.getCompressAction());
        messageMenu.add(compressMenuItem);

        sendMenuItem.setAction(smsPanel.getSendAction());
        messageMenu.add(sendMenuItem);

        menuBar.add(messageMenu);

        toolsMenu.setMnemonic('n');
        toolsMenu.setText("Nástroje");

        historyMenuItem.setAction(historyAction);
        toolsMenu.add(historyMenuItem);

        logMenuItem.setAction(statusPanel.getLogAction());
        toolsMenu.add(logMenuItem);

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
        //installed, just hide the main window
        if (evt != null && NotificationIcon.isInstalled()) {
            NotificationIcon.toggleMainFrameVisibility();
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
                        "Některé konfigurační soubory nemohly být uloženy!",
                        null, JOptionPane.WARNING_MESSAGE);
            }
            System.exit(saveOk ? 0 : 1);
        }
    }//GEN-LAST:event_formWindowClosing
    
    /** Notifies about change in sms queue */
    public void smsProcessed(SMS sms) {
        if (sms.getStatus() == SMS.Status.SENT_OK) {
            statusPanel.setStatusMessage("Zpráva pro " + sms + " odeslána.",
                    true, Icons.STATUS_MESSAGE, true);
            setSMSDelay();
            createHistory(sms);
            
            if (smsPanel.getText().length() > 0)
                smsPanel.requestFocusInWindow();
            else
                contactPanel.requestFocusInWindow();
        } else if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
            logger.info("Message for " + sms + " could not be sent");
            pauseSMSQueue(true);
            statusPanel.setStatusMessage("Zprávu pro " + sms + " se nepodařilo odeslat!",
                    true, Icons.STATUS_WARNING, true);
            
            //prepare dialog
            JLabel label = new JLabel("<html>"
                    + "<h2>Zprávu pro '" + sms + "' se nepovedlo odeslat!</h2>" + 
                    (sms.getErrMsg() != null ? sms.getErrMsg().trim() : "")
                    + "</html>");
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
            dialog.setResizable(true);
            dialog.setVisible(true);
            
            //transfer focus
            if (smsPanel.getText().length() > 0)
                smsPanel.requestFocusInWindow();
            else
                queuePanel.requestFocusInWindow();
        }

        if (!Nullator.isEmpty(sms.getOperatorMsg())) {
            statusPanel.setStatusMessage(sms.getOperator() + ": " + sms.getOperatorMsg(),
                    true, Icons.STATUS_MESSAGE, true);
        }
        
        statusPanel.setTaskRunning(false);
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
    
    /** Pauses sms queue */
    public void pauseSMSQueue(boolean pause) {
        queuePanel.setPaused(pause);
    }
    
    /** Forces delay before sending another sms */
    public void setSMSDelay() {
        smsSender.setDelayed(true);
        smsDelayTimer.start();
    }
    
    /** Display random tip from the collection of tips */
    public void showTipOfTheDay() {
        try {
            List tips = IOUtils.readLines(
                    getClass().getResourceAsStream(RES + "tips.txt"), "UTF-8");
            int random = new Random().nextInt(tips.size());
            statusPanel.setStatusMessage("Tip: " + tips.get(random), false, null, false);
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
        if (config.isRememberLayout()) {
            Dimension mainDimension = config.getMainDimension();
            Integer horizontalSplitPaneLocation = config.getHorizontalSplitPaneLocation();
            Integer verticalSplitPaneLocation = config.getVerticalSplitPaneLocation();
            if (mainDimension != null)
                this.setSize(mainDimension);
            if (horizontalSplitPaneLocation != null)
                horizontalSplitPane.setDividerLocation(horizontalSplitPaneLocation);
            if (verticalSplitPaneLocation != null)
                verticalSplitPane.setDividerLocation(verticalSplitPaneLocation);
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
        if (!config.isRememberQueue())
            smsQueue.clear();
        try {
            persistenceManager.saveQueue();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save queue", ex);
            saveOk = false;
        }
    }
    
    /** save sms history */
    private void saveHistory() {
        if (!config.isRememberHistory())
            history.clearRecords();
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
            super("O programu", new ImageIcon(MainFrame.class.getResource(RES + "about-16.png")));
            putValue(SHORT_DESCRIPTION,"Zobrazit informace o programu");
            putValue(MNEMONIC_KEY,KeyEvent.VK_O);
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
            super("Ukončit");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "exit-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "exit-32.png")));
            putValue(SHORT_DESCRIPTION,"Ukončit program");
            putValue(MNEMONIC_KEY,KeyEvent.VK_U);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, 
                    KeyEvent.CTRL_DOWN_MASK));
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
            super("Nastavení");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "config-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "config-32.png")));
            putValue(SHORT_DESCRIPTION,"Nastavit chování programu");
            putValue(MNEMONIC_KEY,KeyEvent.VK_N);
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
            super("Import kontaktů", new ImageIcon(MainFrame.class.getResource(RES + "contact-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Importovat kontakty z jiných aplikací");
            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
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
            super("Export kontaktů", new ImageIcon(MainFrame.class.getResource(RES + "contact-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Exportovat kontakty do souboru");
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
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
            super("Historie zpráv");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "history-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "history-32.png")));
            this.putValue(SHORT_DESCRIPTION,"Zobrazit historii odeslaných zpráv");
            putValue(MNEMONIC_KEY, KeyEvent.VK_H);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, 
                    KeyEvent.CTRL_DOWN_MASK));
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
    
    /** Progress bar action listener after sending sms */
    private class SMSDelayActionListener implements ActionListener {
        private final int DELAY = 15;
        private int seconds = 0;
        @Override
        public void actionPerformed(ActionEvent e) {
            if (seconds <= DELAY) { //still waiting
                statusPanel.setProgress(seconds, "Další sms za: " + (DELAY-seconds) + "s",
                        null, null);
                if (seconds == 0)
                    statusPanel.setProgress(null, null, null, true);
                seconds++;
            } else { //delay finished
                smsDelayTimer.stop();
                statusPanel.setProgress(null, null, null, false);
                seconds = 0;
                smsSender.setDelayed(false);
                smsSender.announceNewSMS();
            }
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
                    if (sms == null)
                        return;
                    contactPanel.clearSelection();
                    smsPanel.setSMS(sms);
                    break;
                case QueuePanel.ACTION_QUEUE_PAUSE_CHANGED:
                    smsSender.setPaused(queuePanel.isPaused());
                    break;
                default: 
                    logger.severe("Uknown queue event type: " + e.getID());
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
                    statusPanel.setStatusMessage("Import kontaktů úspěšně dokončen",
                            true, Icons.STATUS_INFO, true);
                    break;
                default:
                    logger.severe("Unknown import event type: " + e.getID());
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
                default:
                    logger.severe("Unknown contact event type: " + e.getID());
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
                    for (SMS sms : envelope.generate()) {
                        queuePanel.addSMS(sms);
                        smsSender.announceNewSMS();
                    }
                    break;
                default: 
                    logger.severe("Uknown sms event type: " + e.getID());
            }
        }
    }
    
    /** Listens for events from update checker */
    private class UpdateListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            statusPanel.setStatusMessage("Byla vydána nová verze programu!", 
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
