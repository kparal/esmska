/*
 * Main.java
 *
 * Created on 24. srpen 2007, 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import esmska.gui.MainFrame;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import esmska.persistence.PersistenceManager;
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
        try {
            if (configPath != null)
                PersistenceManager.setProgramDir(configPath);
            PersistenceManager pm = PersistenceManager.getInstance();
            try {
                pm.loadConfig();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not load config file", ex);
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
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not create program dir or read config files", ex);
            JOptionPane.showMessageDialog(null, "Nepodařilo se vytvořit adresář " +
                    "nebo číst z adresáře s konfigurací!",
                    "Chyba spouštění", JOptionPane.ERROR_MESSAGE);
        }
        
        //set L&F
        try {
            ThemeManager.setLaF();
        } catch (Throwable ex) {
            logger.log(Level.WARNING, "Could not set Look and Feel", ex);
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
