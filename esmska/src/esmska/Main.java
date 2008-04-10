/*
 * Main.java
 *
 * Created on 24. srpen 2007, 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.gui.MainFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import esmska.persistence.PersistenceManager;
import esmska.transfer.ProxyManager;
import esmska.utils.Nullator;
import java.beans.IntrospectionException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jvnet.lafwidget.LafWidget;

/** Starter class for the whole program
 *
 * @author ripper
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static String configPath; //path to config files
    
    /** Program starter method
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //parse commandline arguments
        CommandLineParser clp = new CommandLineParser();
        if (! clp.parseArgs(args))
            System.exit(1);
        configPath = clp.getConfigPath();
        
        //portable mode
        if (clp.isPortable() && configPath == null) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not set system Look and Feel", ex);
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setApproveButtonText("Vybrat");
            chooser.setDialogTitle("Zvolte umístění konfiguračních souborů");
            chooser.setFileHidingEnabled(false);
            chooser.setMultiSelectionEnabled(false);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION)
                configPath = chooser.getSelectedFile().getPath();
        }
        
        //load user files
        PersistenceManager pm = null;
        try {
            if (configPath != null)
                PersistenceManager.setUserDir(configPath);
            pm = PersistenceManager.getInstance();
            try {
                pm.loadConfig();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load config file", ex);
            }
            try {
                pm.loadOperators();
            } catch (IntrospectionException ex) { //OpenJDK 6 seems not to support JavaScript
                logger.log(Level.SEVERE, "Current JRE doesn't support JavaScript execution", ex);
                JOptionPane.showMessageDialog(null, 
                    "<html>Vaše aktuální Java nepodporuje vykonávání JavaScriptových souborů,<br>" +
                    "což je ke správné funkci programu nezbytné. Je doporučené používat<br>" +
                    "Javu od firmy Sun. Program se nyní ukončí.",
                    null, JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, "Nepodařilo se vytvořit adresář " +
                    "nebo číst z adresáře s konfigurací!",
                    null, JOptionPane.ERROR_MESSAGE);
        }
        
        //warn if other program instance is already running
        if (pm != null && !pm.isFirstInstance()) {
            logger.warning("Some other instance of the program is already running");
            String runOption = "Přesto spustit";
            String quitOption = "Ukončit";
            String[] options = new String[]{quitOption, runOption};
            int result = JOptionPane.showOptionDialog(null,
                    "<html><h2>Esmska již jednou běží!</h2>" +
                    "Esmska je již jednou spuštěna. Důrazně nedoporučujeme spouštět další<br>" +
                    "instance programu, neboť může dojít ke ztrátě uživatelských dat.<br>",
                    null, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    options, quitOption);
            if (result != 1)
                System.exit(0);
        }

        //do some incialization if this is the first run
        if (Nullator.isEmpty(PersistenceManager.getConfig().getVersion())) { //first run means version is empty
            //set country prefix from locale
            PersistenceManager.getConfig().setCountryPrefix(
                    CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
        }
        
        //update from older versions
        LegacyUpdater.update();
        
        //set L&F
        try {
            ThemeManager.setLaF();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Could not set Look and Feel", ex);
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
        
        //set Substance specific addons
        UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);

        //start main frame
        java.awt.EventQueue.invokeLater(new java.lang.Runnable() {
            public void run() {
                MainFrame.getInstance().setVisible(true);
            }
        });
    }
    
}
