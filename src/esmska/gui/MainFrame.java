package esmska.gui;

import esmska.Context;
import esmska.data.Queue.Events;
import esmska.data.event.ValuedEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
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
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JToolBar.Separator;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ToolTipManager;

import javax.swing.WindowConstants;
import org.apache.commons.io.IOUtils;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;

import esmska.update.UpdateChecker;
import esmska.data.Config;
import esmska.data.Gateways;
import esmska.data.History;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.integration.ActionBean;
import esmska.integration.IntegrationAdapter;
import esmska.transfer.SMSSender;
import esmska.utils.L10N;
import esmska.data.event.ValuedListener;
import esmska.data.Links;
import esmska.data.event.ActionEventSupport;
import esmska.transfer.ImageCodeManager;
import esmska.update.Statistics;
import esmska.update.UpdateInstaller;
import esmska.utils.MiscUtils;
import esmska.utils.RuntimeUtils;
import java.awt.Image;
import java.awt.SplashScreen;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.Beans;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.openide.awt.Mnemonics;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;

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
    
    /** sender of sms */
    private static final SMSSender smsSender = new SMSSender();
    private static final Config config = Config.getInstance();
    private static final History history = History.getInstance();
    private static final Log log = Log.getInstance();
    private static final Queue queue = Queue.getInstance();
    /** shutdown handler thread */
    private Thread shutdownThread = new ShutdownThread();
    private UpdateChecker updateChecker = UpdateChecker.getInstance();

    /**
     * Creates new form MainFrame
     */
    private MainFrame() {
        instance = this;
        Context.mainFrame = instance;

        // if we are using Aqua L&F, set textured window property
        // must be called before components inicialization
        if (ThemeManager.isAquaCurrentLaF()) {
            getRootPane().putClientProperty("apple.awt.brushMetalLook", "true");
        }

        initComponents();

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(Icons.get("esmska-16.png").getImage());
        images.add(Icons.get("esmska-32.png").getImage());
        images.add(Icons.get("esmska-64.png").getImage());
        images.add(Icons.get("esmska.png").getImage());
        setIconImages(images);
        
        //hide on Ctrl+W
        String command = "hide";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //hide only when notification icon present or on mac
                if (NotificationIcon.isInstalled() || RuntimeUtils.isMac()) {
                    formWindowClosing(new WindowEvent(MainFrame.this, 0));
                }
            }
        });
        
        //on Mac, move program menu items to system menu
        if (RuntimeUtils.isMac()) {
            logger.fine("Running on Mac OS, hiding some menu items...");
            try {
                ActionBean bean = new ActionBean();
                bean.setQuitAction(Actions.getQuitAction());
                bean.setAboutAction(Actions.getAboutAction());
                bean.setConfigAction(Actions.getConfigAction());

                IntegrationAdapter integration = IntegrationAdapter.getInstance();
                integration.setActionBean(bean);
                integration.activateGUI();

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
        
        //add first log record
        log.addRecord(new Log.Record(l10n.getString("Program_start")));

        //load config
        loadConfig();
        if (queue.size() > 0) {
            queue.setPaused(true);
        }
        
        //setup components
        contactPanel.requestFocusInWindow();
        contactPanel.ensureContactSelected();
        queue.addValuedListener(new QueueListener());
        ImageCodeManager.setResolver(new GUIImageCodeResolver());
        
        //use bindings
        Binding bind = Bindings.createAutoBinding(UpdateStrategy.READ, config, 
                BeanProperty.create("toolbarVisible"), toolBar, BeanProperty.create("visible"));
        Binding bind2 = Bindings.createAutoBinding(UpdateStrategy.READ, config,
                BeanProperty.create("notificationIconVisible"), exitButton, BeanProperty.create("visible"));
        bindGroup.addBinding(bind);
        bindGroup.addBinding(bind2);
        bindGroup.bind();
        
        //add shutdown handler, when program is closed externally (logout, SIGTERM, etc)
        //only if really running, not in design mode
        if (!Beans.isDesignTime()) {
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        }

        // listen for changed sizes of smsPanel and queuePanel and adjust splitPane if required
        ActionListener verticalSplitListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getID() == ActionEventSupport.ACTION_NEED_RESIZE) {
                    JComponent comp = (JComponent) e.getSource();
                    int prefHeight = comp.getPreferredSize().height;
                    int dividerLocation = prefHeight;
                    if (verticalSplitPane.getBottomComponent() == comp) {
                    //for bottom component we have to substract the number from full height
                        dividerLocation = verticalSplitPane.getHeight() - prefHeight - verticalSplitPane.getDividerSize();
                    }
                    verticalSplitPane.setDividerLocation(dividerLocation);
                }
            }
        };
        smsPanel.addActionListener(verticalSplitListener);
        queuePanel.addActionListener(verticalSplitListener);
        
        // wait for asynchronous user data loading and then finalize everything
        final AtomicBoolean calledEverythingLoaded = new AtomicBoolean();
        Context.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!StringUtils.equals(evt.getPropertyName(), "everythingLoaded")) {
                    return;
                }
                if (!Context.everythingLoaded()) {
                    return;
                }
                // this may come from non-EDT thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (calledEverythingLoaded.compareAndSet(false, true)) {
                            everythingLoaded();
                        }
                    }
                });
                Context.removePropertyChangeListener(this);
            }
        });
        // the data could have been loaded before setting the listener, check it
        if (Context.everythingLoaded() && calledEverythingLoaded.compareAndSet(false, true)) {
            everythingLoaded();
        }
    }
    
    /** Performs last operations after all user data has been loaded. */
    private void everythingLoaded() {
        //check for valid gateways
        if (Gateways.getInstance().size() <= 0) {
            logger.warning("No usable gateways found");
            JOptionPane.showMessageDialog(this,
                    new JLabel(l10n.getString("MainFrame.no_gateways")),
                    null, JOptionPane.ERROR_MESSAGE);
        }
        
        // send statistics
        Statistics.sendUsageInfo();
        
        // check for updates
        updateChecker.addActionListener(new UpdateListener());
        updateChecker.checkForUpdates();
    }
    
    /** Start loading gatewates asynchronously */
    private void loadGatewaysAsync() {
        logger.fine("Loading gateways asynchronously...");
        Thread loadGwsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // load gateways
                try {
                    Context.persistenceManager.loadGateways();
                } catch (IntrospectionException ex) { //it seems there is no JavaScript support
                    logger.log(Level.SEVERE, "Current JRE doesn't support JavaScript execution", ex);
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null, l10n.getString("Main.no_javascript"),
                                        null, JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Can't display error message", e);
                    }
                    System.exit(2);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Could not load gateways", ex);
                }
                // load gateway properties
                try {
                    Context.persistenceManager.loadGatewayProperties();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Could not load gateway properties file", ex);
                }
                // announce
                Context.setGatewaysLoaded(true);
            }
        });
        loadGwsThread.setDaemon(true);
        loadGwsThread.start();
    }
    
    /** Create an instance of MainFrame. Should be called only for the first
     * initialization, after that the instance is available in the Context.
     */
    public static void instantiate() {
        if (instance == null) {
            instance = new MainFrame();
        } else {
            throw new IllegalStateException("MainFrame is already instantiated");
        }
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
        
        // after all is set, load gateways asynchronously
        // we delayed it to this point, because even running it in a background
        // thread slows the startup of the main window
        loadGatewaysAsync();
    }

    /** Display random tip from the collection of tips */
    public void showTipOfTheDay() {
        try {
            List tips = IOUtils.readLines(
                    getClass().getResourceAsStream(RES + "tips.txt"), "UTF-8");
            int random = new Random().nextInt(tips.size());
            statusPanel.setStatusMessage(l10n.getString("MainFrame.tip") + " " +
                    l10n.getString((String)tips.get(random)), null, null, false);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Can't display tip of the day", ex);
        }
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public ContactPanel getContactPanel() {
        return contactPanel;
    }

    public SMSPanel getSMSPanel() {
        return smsPanel;
    }

    public SMSSender getSMSSender() {
        return smsSender;
    }
    
    public JToolBar getToolbar() {
        return toolBar;
    }

    public QueuePanel getQueuePanel() {
        return queuePanel;
    }

    public JSplitPane getHorizontalSplitPane() {
        return horizontalSplitPane;
    }

    public JSplitPane getVerticalSplitPane() {
        return verticalSplitPane;
    }

    /** Quit the program */
    public void exit() {
        logger.fine("Closing program...");

        //user requested program close, shutdown handler not needed
        Runtime.getRuntime().removeShutdownHook(shutdownThread);

        //save end exit
        boolean saveOk = saveAll();
        if (!saveOk) { //some data were not saved
            JOptionPane.showMessageDialog(this,
                    l10n.getString("MainFrame.cant_save_config"),
                    null, JOptionPane.WARNING_MESSAGE);
        }
        int returnCode = saveOk ? 0 : 3;
        logger.log(Level.FINE, "Exiting program with return code: {0}", returnCode);
        System.exit(returnCode);
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
        donateButton = new JButton();
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
        problemMenuItem = new JMenuItem();
        translateMenuItem = new JMenuItem();
        donateMenuItem = new JMenuItem();
        helpSeparator = new JSeparator();
        aboutMenuItem = new JMenuItem();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Esmska"); // NOI18N
        setLocationByPlatform(true);
        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(WindowEvent evt) {
            }
        });
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

        smsPanel.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        verticalSplitPane.setLeftComponent(smsPanel);

        queuePanel.addValuedListener(new QueuePanelListener());
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

        historyButton.setAction(Actions.getHistoryAction());
        historyButton.setFocusable(false);
        historyButton.setHideActionText(true);
        Mnemonics.setLocalizedText(historyButton, l10n.getString("History"));
        historyButton.setToolTipText(historyButton.getToolTipText() + " (Ctrl+T)");
        toolBar.add(historyButton);
        toolBar.add(jSeparator3);

        configButton.setAction(Actions.getConfigAction());
        configButton.setToolTipText(Actions.getConfigAction().getValue(Action.NAME).toString());
        configButton.setFocusable(false);
        configButton.setHideActionText(true);
        toolBar.add(configButton);

        exitButton.setAction(Actions.getQuitAction());
        exitButton.setToolTipText(Actions.getQuitAction().getValue(Action.NAME).toString() + " (Ctrl+Q)");
        exitButton.setFocusable(false);
        exitButton.setHideActionText(true);
        toolBar.add(exitButton);

        donateButton.setAction(Actions.getBrowseAction(Links.DONATE));
        donateButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/donate-32.png"))); // NOI18N
        Mnemonics.setLocalizedText(donateButton,l10n.getString("MainFrame.donateButton.text")); // NOI18N
        donateButton.setToolTipText(l10n.getString("AboutFrame.supportHyperlink.toolTipText")); // NOI18N
        donateButton.setFocusable(false);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(donateButton);
        for (Component comp : toolBar.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setMnemonic(0);
                button.putClientProperty("JButton.buttonType", "gradient");
            }
        }

        Mnemonics.setLocalizedText(programMenu,l10n.getString("MainFrame.programMenu.text"));

        configMenuItem.setAction(Actions.getConfigAction());
        programMenu.add(configMenuItem);

        exitMenuItem.setAction(Actions.getQuitAction());
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

        Mnemonics.setLocalizedText(toolsMenu,l10n.getString("MainFrame.toolsMenu.text"));

        historyMenuItem.setAction(Actions.getHistoryAction());
        toolsMenu.add(historyMenuItem);

        logMenuItem.setAction(Actions.getLogAction());
        toolsMenu.add(logMenuItem);
        toolsMenu.add(jSeparator4);

        importMenuItem.setAction(Actions.getImportAction());
        toolsMenu.add(importMenuItem);

        exportMenuItem.setAction(Actions.getExportAction());
        toolsMenu.add(exportMenuItem);

        menuBar.add(toolsMenu);
        Mnemonics.setLocalizedText(helpMenu,l10n.getString("MainFrame.helpMenu.text")); // NOI18N

        faqMenuItem.setAction(Actions.getBrowseAction(Links.FAQ));
        Mnemonics.setLocalizedText(faqMenuItem, l10n.getString("MainFrame.faqMenuItem.text"));
        faqMenuItem.setToolTipText(l10n.getString("MainFrame.faqMenuItem.toolTipText")); // NOI18N
        helpMenu.add(faqMenuItem);

        getHelpMenuItem.setAction(Actions.getBrowseAction(Links.FORUM));
        getHelpMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/getHelp-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(getHelpMenuItem,l10n.getString("MainFrame.getHelpMenuItem.text")); // NOI18N
        getHelpMenuItem.setToolTipText(l10n.getString("MainFrame.getHelpMenuItem.toolTipText")); // NOI18N
        helpMenu.add(getHelpMenuItem);

        problemMenuItem.setAction(Actions.getBrowseAction(Links.ISSUES));
        Mnemonics.setLocalizedText(problemMenuItem,l10n.getString("MainFrame.problemMenuItem.text")); // NOI18N
        problemMenuItem.setToolTipText(l10n.getString("MainFrame.problemMenuItem.toolTipText")); // NOI18N
        helpMenu.add(problemMenuItem);

        translateMenuItem.setAction(Actions.getBrowseAction(Links.TRANSLATE));
        Mnemonics.setLocalizedText(translateMenuItem,l10n.getString("MainFrame.translateMenuItem.text")); // NOI18N
        translateMenuItem.setToolTipText(l10n.getString("MainFrame.translateMenuItem.toolTipText")); // NOI18N
        helpMenu.add(translateMenuItem);

        donateMenuItem.setAction(Actions.getBrowseAction(Links.DONATE));
        donateMenuItem.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/donate-16.png"))); // NOI18N
        Mnemonics.setLocalizedText(donateMenuItem,l10n.getString("MainFrame.donateMenuItem.text")); // NOI18N
        donateMenuItem.setToolTipText(l10n.getString("AboutFrame.supportHyperlink.toolTipText")); // NOI18N
        helpMenu.add(donateMenuItem);
        helpMenu.add(helpSeparator);

        aboutMenuItem.setAction(Actions.getAboutAction());
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(statusPanel, GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
            .addComponent(jSeparator1, GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 647, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(toolBar, GroupLayout.DEFAULT_SIZE, 659, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(toolBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(horizontalSplitPane, GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
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
        if (evt != null && (NotificationIcon.isInstalled() || RuntimeUtils.isMac())) {
            logger.fine("Hiding main window");
            if (RuntimeUtils.isMac()) {
                this.setVisible(false);
            } else {
                NotificationIcon.toggleMainFrameVisibility();
            }
            return;
        }
        //otherwise exit
        exit();
    }//GEN-LAST:event_formWindowClosing

    private void formWindowGainedFocus(WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        //work around bug http://code.google.com/p/esmska/issues/detail?id=182
        //but don't do it on Mac (sigh): http://code.google.com/p/esmska/issues/detail?id=301
        if (!RuntimeUtils.isMac()) {
            this.setVisible(true);
        }
    }//GEN-LAST:event_formWindowGainedFocus

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
            saveOk = saveGatewayProperties() && saveOk;
            return saveOk;
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Serious error during saving user data", t);
            return false;
        }
    }
    
    /** Saves history of sent sms */
    private void createHistory(SMS sms) {
        History.Record record = new History.Record(sms.getNumber(), sms.getWholeText(),
                sms.getGateway(), sms.getName(), sms.getSenderNumber(),
                sms.getSenderName(), null);
        if(history.getRecords().isEmpty()){
            history.addRecord(record);
        }else{
            History.Record r = history.getRecord(history.getRecords().size()-1);
            if(!(r.getNumber().equals(sms.getNumber()) && r.getText().equals(sms.getWholeText()) 
                    && r.getGateway().equals(sms.getGateway()) && r.getName().equals(sms.getName()))){
                history.addRecord(record);
            }
        }
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
            Context.persistenceManager.saveConfig();
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
            Context.persistenceManager.saveContacts();
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
            Context.persistenceManager.saveQueue();
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
            //computer last acceptable record time
            Calendar limitCal = Calendar.getInstance();
            limitCal.add(Calendar.DAY_OF_MONTH, -config.getReducedHistoryCount());
            Date limit = limitCal.getTime();
            //remove old records
            history.removeRecordsOlderThan(limit);
        }
        
        try {
            Context.persistenceManager.saveHistory();
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
            Context.persistenceManager.saveKeyring();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save keyring", ex);
            return false;
        }
    }

    /** Save gateway properties.
     * Skips saving if the gateways properties wasn't yet already loaded (they
     * are loaded asynchronously), then there is nothing to save.
     * @return true if saved ok or not even yet loaded; false otherwise
     */
    private boolean saveGatewayProperties() {
        if (!Context.gatewaysLoaded()) {
            logger.log(Level.FINE, "Not saving gateway properties because they " +
                    "were not yet even loaded.");
            return true;
        }
        try {
            Context.persistenceManager.saveGatewayProperties();
            return true;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not save gateway properties", ex);
            return false;
        }
    }

    /** Listen for changes in the queue */
    private class QueueListener implements ValuedListener<Queue.Events, SMS> {
        @Override
        public void eventOccured(ValuedEvent<Events, SMS> e) {
            switch (e.getEvent()) {
                case SENDING_SMS:
                    sendingSMS(e.getValue());
                    break;
                case SMS_SENT:
                    smsSent(e.getValue());
                    break;
                case SMS_SENDING_FAILED:
                    smsFailed(e.getValue());
                    break;
            }
        }
        private void sendingSMS(SMS sms) {
            String gateway = sms.getGateway();
            statusPanel.setTaskRunning(true);
            log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("SMSSender.sending_message"),
                    sms.getRecipient(), (gateway == null ? l10n.getString("SMSSender.no_gateway") : gateway)),
                    null, Icons.STATUS_INFO));
        }
        private void smsSent(SMS sms) {
            log.addRecord(new Log.Record(MessageFormat.format(l10n.getString("MainFrame.sms_sent"), sms.getRecipient()),
                    null, Icons.STATUS_MESSAGE));
            createHistory(sms);
            finish(sms);
        }
        private void smsFailed(SMS sms) {
            logger.log(Level.INFO, "Message could not be sent: {0}\nProblem: {1}",
                    new Object[]{sms, sms.getProblem()});
            log.addRecord(new Log.Record(MessageFormat.format(l10n.getString("MainFrame.sms_failed"), sms.getRecipient()),
                    null, Icons.STATUS_WARNING));

            //show the dialog
            logger.fine("Showing reason why SMS sending failed...");
            GatewayMessageFrame gatewayMessageFrame = GatewayMessageFrame.getInstance();
            gatewayMessageFrame.addErrorMsg(sms);

            finish(sms);
        }
        private void finish(SMS sms) {
            //show gateway message if present
            if (StringUtils.isNotEmpty(sms.getSupplMsg())) {
                log.addRecord(new Log.Record(sms.getGateway() + ": " + sms.getSupplMsg(),
                        null, Icons.STATUS_MESSAGE));
            }
            //disable task indicator
            if (!smsSender.isRunning()) {
                statusPanel.setTaskRunning(false);
            }
        }
    }

    /** Listens for events from queue panel */
    private class QueuePanelListener implements ValuedListener<QueuePanel.Events, SMS> {
        @Override
        public void eventOccured(ValuedEvent<QueuePanel.Events, SMS> e) {
            switch (e.getEvent()) {
                //edit sms in queue
                case SMS_EDIT_REQUESTED:
                    SMS sms = e.getValue();
                    if (sms == null) {
                        return;
                    }
                    //if currently writing some sms then ask whether to overwrite text
                    if (StringUtils.isNotEmpty(smsPanel.getText().trim())) {
                        String replaceOption = l10n.getString("Replace");
                        String cancelOption = l10n.getString("Cancel");
                        String[] options = new String[]{cancelOption, replaceOption};
                        options = RuntimeUtils.sortDialogOptions(options);
                        int result = JOptionPane.showOptionDialog(MainFrame.this, 
                                new JLabel(l10n.getString("QueuePanel.replaceSms")),
                                null, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                options, replaceOption);
                        //if not chosen to replace don't do anything
                        if (result != ArrayUtils.indexOf(options, replaceOption)) {
                            return;
                        }
                    }
                    //edit the message
                    SMS smsToEdit = queue.extractSMS(sms.getId(), true);
                    smsPanel.setSMS(smsToEdit);
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
    
    /** Listens for events from update checker */
    private class UpdateListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int event = e.getID();
            
            switch (event) {
                case UpdateChecker.ACTION_PROGRAM_UPDATE_AVAILABLE:
                    announceProgram();
                    break;
                case UpdateChecker.ACTION_GATEWAY_UPDATE_AVAILABLE:
                    updateGateways();
                    break;
                case UpdateChecker.ACTION_PROGRAM_AND_GATEWAY_UPDATE_AVAILABLE:
                    announceProgram();
                    updateGateways();
                    break;
            }

            //schedule next check
            Timer timer = new Timer(UpdateChecker.AUTO_CHECK_INTERVAL * 1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateChecker.checkForUpdates();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }

        /** Announce program updates available */
        private void announceProgram() {
            String message = MessageFormat.format(l10n.getString("MainFrame.new_program_version"),
                    updateChecker.getLatestProgramVersion());
            log.addRecord(new Log.Record(MiscUtils.stripHtml(message), null, Icons.STATUS_UPDATE_IMPORTANT));
            statusPanel.setStatusMessage(message, null, Icons.STATUS_UPDATE_IMPORTANT, true);
            //on click open program homepage in browser
            statusPanel.installClickHandler(new Runnable() {
                @Override
                public void run() {
                    Action browseAction = Actions.getBrowseAction(Links.DOWNLOAD);
                    browseAction.actionPerformed(null);
                }
            }, l10n.getString("Update.browseDownloads"));
        }

        /** perform gateway update */
        private void updateGateways() {
            UpdateInstaller.getInstance().installNewGateways();
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
    private JButton donateButton;
    private JMenuItem donateMenuItem;
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
