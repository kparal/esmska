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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import esmska.persistence.PersistenceManager;

/** Starter class for the whole program
 *
 * @author ripper
 */
public class Main {
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
                ex.printStackTrace();
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
                ex.printStackTrace();
            }
            try {
                pm.loadContacts();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                pm.loadQueue();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Nepodařilo se vytvořit adresář " +
                    "nebo číst z adresáře s konfigurací!",
                    "Chyba spouštění", JOptionPane.ERROR_MESSAGE);
        }
        
        //set L&F
        try {
            ThemeManager.setLaF();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        //start main frame
        java.awt.EventQueue.invokeLater(new java.lang.Runnable() {
            public void run() {
                MainFrame.getInstance().setVisible(true);
            }
        });
    }
    
}
