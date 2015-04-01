package esmska;

import esmska.gui.ThemeManager;
import esmska.update.LegacyUpdater;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;


import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.gui.MainFrame;
import esmska.gui.ExceptionDialog;
import esmska.gui.InitWizardDialog;
import esmska.persistence.PersistenceManager;
import esmska.transfer.ProxyManager;
import esmska.update.Statistics;
import esmska.utils.L10N;
import esmska.utils.LogSupport;
import esmska.utils.RuntimeUtils;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.apache.commons.lang.ArrayUtils;

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
        //initialize logging
        LogSupport.init();
        //store records for pushing it to logfile later
        LogSupport.storeRecords(true);

        //handle uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(ExceptionDialog.getExceptionHandler());
        //workaround for Java 6 to process uncaught exceptions from modal dialogs
        //read more at http://code.google.com/p/esmska/issues/detail?id=256
        System.setProperty("sun.awt.exception.handler",
                ExceptionDialog.AwtHandler.class.getName());

        //detect JVM and warn if not not supported
        if (!RuntimeUtils.isSupportedJava()) {
            logger.warning("You are probably running the program on an unsupported "
                    + "Java version! Program might not work correctly with it! "
                    + "The tested Java versions are: Oracle Java 6 and 7, OpenJDK 6 and 7, Apple Java 6 and 7.");
        }

        //parse commandline arguments
        CommandLineParser clp = new CommandLineParser();
        if (! clp.parseArgs(args)) {
            System.exit(1);
        }

        //log some basic stuff for debugging
        logger.log(Level.FINE, "Esmska {0} starting...", Config.getLatestVersion());
        logger.log(Level.FINER, "System info: {0}", RuntimeUtils.getSystemInfo());

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
                            logger.log(Level.CONFIG, "New config path: {0}", configPath);
                        }
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display file chooser for portable mode", e);
            }
        }

        //get persistence manager
        try {
            if (configPath != null) {
                PersistenceManager.setCustomDirs(configPath, configPath);
            }
            PersistenceManager.instantiate();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not create program dir or read config files", ex);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null,
                                new JLabel(MessageFormat.format(l10n.getString("Main.cant_read_config"),
                                PersistenceManager.getConfigDir().getAbsolutePath(),
                                PersistenceManager.getDataDir().getAbsolutePath())),
                                null, JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display error message", e);
            } finally {
                //if it is not possible to access config dir, we don't want to
                //run anymore
                System.exit(5);
            }
        }
        PersistenceManager pm = Context.persistenceManager;

        //backup files
        try {
            pm.backupConfigFiles();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not back up configuration", ex);
        }

        //initialize file logging
        File logFile = pm.getLogFile();
        try {
            LogSupport.initFileHandler(logFile);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not start logging into " + logFile.getAbsolutePath(), ex);
        } finally {
            //no need to store records anymore
            LogSupport.storeRecords(false);
        }

        //load config file
        try {
            pm.loadConfig();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not load config file", ex);
        }
        
        //load rest of user files
        try {
            pm.loadContacts();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not load contacts file", ex);
        }
        try {
            pm.loadQueue();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not load queue file", ex);
        }
        try {
            pm.loadHistory();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not load history file", ex);
        }
        try {
            pm.loadKeyring();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Could not load keyring file", ex);
        }

        //initialize logging if set from Config
        if (Config.getInstance().isDebugMode()) {
            //set "full" logging, but don't log to console
            LogSupport.enableHttpClientLogging();
            LogSupport.getEsmskaLogger().setLevel(Level.ALL);
        }
        
        //quit if other program instance is already running
        if (!pm.isFirstInstance()) {
            logger.warning("Esmska is already running. Quitting.");
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        String quitOption = l10n.getString("Quit");
                        String[] options = new String[]{quitOption};
                        JOptionPane.showOptionDialog(null, l10n.getString("Main.already_running"),
                            null, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                            options, quitOption);
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display error message", e);
            } finally {
                System.exit(15);
            }
        }

        //warn if configuration files are newer than program version (user has
        //executed an older program version)
        Config config = Config.getInstance();
        final String dataVersion = config.getVersion();
        final String programVersion = Config.getLatestVersion();
        if (Config.compareProgramVersions(dataVersion, programVersion) > 0) {
            logger.log(Level.WARNING,"Configuration files are newer ({0}) then " +
                    "current program version ({1})! Data corruption may occur!", 
                    new Object[]{dataVersion, programVersion});
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        String runOption = l10n.getString("Main.run_anyway");
                        String quitOption = l10n.getString("Quit");
                        String[] options = new String[]{runOption, quitOption};
                        options = RuntimeUtils.sortDialogOptions(options);
                        int result = JOptionPane.showOptionDialog(null,
                                new JLabel(MessageFormat.format(l10n.getString("Main.configsNewer"),
                                dataVersion, programVersion)),
                                null, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                                options, quitOption);
                        if (result != ArrayUtils.indexOf(options, runOption)) {
                            System.exit(0);
                        }
                    }
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't display error message", e);
            }
        }

        //do some initialization if this is the first run
        if (config.isFirstRun()) {
            logger.fine("First run, doing initialization...");
            //set country prefix from locale
            config.setCountryPrefix(
                    CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            //set suggested LaF for this platform
            config.setLookAndFeel(ThemeManager.suggestBestLAF());
            //show first run wizard
            InitWizardDialog dialog = new InitWizardDialog(null, true);
            dialog.setVisible(true);
        }
        
        //update from older versions
        if (!config.isFirstRun()) {
            try {
                LegacyUpdater.update();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Updating to a newer version failed", ex);
            }
        }
        
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
        if (config.isUseProxy()) {
            ProxyManager.setProxy(config.getHttpProxy(),
                    config.getHttpsProxy(), config.getSocksProxy());
        }

        //do some changes for unstable version
        if (!Config.isStableVersion()) {
            config.setAnnounceUnstableUpdates(true);
        }
        
        // refresh UUID if needed
        Statistics.refreshUUID();
        
        //start main frame
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame.instantiate();
                Context.mainFrame.startAndShow();
            }
        });
    }
    
}
