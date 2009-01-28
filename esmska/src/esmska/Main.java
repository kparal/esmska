/*
 * Main.java
 *
 * Created on 24. srpen 2007, 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import esmska.update.LegacyUpdater;
import java.beans.IntrospectionException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;


import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.gui.MainFrame;
import esmska.persistence.PersistenceManager;
import esmska.transfer.ProxyManager;
import esmska.utils.JavaType;
import esmska.utils.L10N;
import esmska.utils.Nullator;
import esmska.utils.OSType;
import java.awt.EventQueue;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;

/** Starter class for the whole program
 *
 * @author ripper
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static String configPath; //path to config files
    
    /** Program starter method
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //detect JVM and warn if not not supported
        if (!JavaType.isSupported()) {
            logger.severe(l10n.getString("Main.unsupported_java"));
        }

        //parse commandline arguments
        CommandLineParser clp = new CommandLineParser();
        if (! clp.parseArgs(args)) {
            System.exit(1);
        }
        logger.fine("Esmska " + Config.getLatestVersion() + " starting...");

        //remember Mac UI for MenuBar from default l&f
        String macBarUI = UIManager.getString("MenuBarUI");
        
        //portable mode
        configPath = clp.getConfigPath();
        if (clp.isPortable() && configPath == null) {
            logger.fine("Entering portable mode");
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Could not set system Look and Feel", ex);
                        }
                        JFileChooser chooser = new JFileChooser();
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        chooser.setApproveButtonText(l10n.getString("Select"));
                        chooser.setDialogTitle(l10n.getString("Main.choose_config_files"));
                        chooser.setFileHidingEnabled(false);
                        chooser.setMultiSelectionEnabled(false);
                        int result = chooser.showOpenDialog(null);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            configPath = chooser.getSelectedFile().getPath();
                            logger.config("New config path: " + configPath);
                        }
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display file chooser for portable mode", e);
            }
        }

        //load user files
        PersistenceManager pm = null;
        try {
            if (configPath != null) {
                PersistenceManager.setUserDir(configPath);
            }
            pm = PersistenceManager.getInstance();
            try {
                pm.loadConfig();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load config file", ex);
            }
            try {
                pm.loadOperators();
            } catch (IntrospectionException ex) { //it seems there is not JavaScript support
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
                System.exit(1);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Could not load operators", ex);
            }
            try {
                pm.loadContacts();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load contacts file", ex);
            }
            try {
                pm.loadQueue();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load queue file", ex);
            }
            try {
                pm.loadHistory();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load history file", ex);
            }
            try {
                pm.loadKeyring();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load keyring file", ex);
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not create program dir or read config files", ex);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null,
                                MessageFormat.format(l10n.getString("Main.cant_read_config"),
                                PersistenceManager.getUserDir().getAbsolutePath()),
                                null, JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display error message", e);
            }
        }
        
        //warn if other program instance is already running
        if (pm != null && !pm.isFirstInstance()) {
            logger.warning("Some other instance of the program is already running");
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        String runOption = l10n.getString("Main.run_anyway");
                        String quitOption = l10n.getString("Quit");
                        String[] options = new String[]{runOption, quitOption};
                        int result = JOptionPane.showOptionDialog(null, l10n.getString("Main.already_running"),
                                null, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                                options, quitOption);
                        if (result != 0) {
                            System.exit(0);
                        }
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display error message", e);
            }
        }

        //do some initialization if this is the first run
        if (Nullator.isEmpty(PersistenceManager.getConfig().getVersion())) { //first run means version is empty
            logger.fine("First run, doing initialization...");
            //set country prefix from locale
            PersistenceManager.getConfig().setCountryPrefix(
                    CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            //set suggested LaF for this platform
            PersistenceManager.getConfig().setLookAndFeel(ThemeManager.suggestBestLAF());
        }
        
        //update from older versions
        LegacyUpdater.update();
        
        //set L&F
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        ThemeManager.setLaF();
                    } catch (Throwable ex) {
                        logger.log(Level.WARNING, "Could not set Look and Feel", ex);
                    }
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Setting LaF interrupted", e);
        }
        
        //set proxy
        Config config = PersistenceManager.getConfig();
        if (config.isUseProxy()) {
            if (config.isSameProxy()) {
                ProxyManager.setProxy(config.getHttpProxy());
            } else {
                ProxyManager.setProxy(config.getHttpProxy(),
                        config.getHttpsProxy(), config.getSocksProxy());
            }
        }
        
        //set MenuBar usage on Mac OS
        if (macBarUI != null && OSType.isMac()) {
            logger.fine("Setting Mac OS UI");
            UIManager.put("MenuBarUI", macBarUI);
        }
        
        //start main frame
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame.getInstance().startAndShow();
            }
        });
    }
    
}
