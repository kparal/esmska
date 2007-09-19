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
    private static boolean portable;
    private static String config;
    
    /** Program starter class
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //parse commandline arguments
        parseArgs(args);
        
        //portable mode
        if (portable && config == null) {
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
                config = chooser.getSelectedFile().getPath();
        }
        
        //load user files
        try {
            if (config != null)
                PersistenceManager.setProgramDir(config);
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
    
    /** Parse commandline arguments */
    private static void parseArgs(String[] args) {
        List<String> arguments = Arrays.asList(args);
        
        for (Iterator it = arguments.iterator(); it.hasNext(); ) {
            String arg = (String) it.next();
            if (arg.equals("-h") || arg.equals("--help")) {
                printUsage();
                System.exit(1);
            } else if (arg.equals("-p") || arg.equals("--portable")) {
                portable = true;
            } else if (arg.equals("-c") || arg.equals("--config")) {
                if (!it.hasNext()) {
                    System.err.println("Chybí cesta!");
                    printUsage();
                    System.exit(1);
                }
                config = (String) it.next();
            } else {
                System.err.println("Neznámá volba!");
                printUsage();
                System.exit(1);
            }
        }
    }
    
    /** Print usage help */
    private static void printUsage() {
        String usage =
                "Použití: java -jar esmska.jar [VOLBY]\n" +
                "\n" +
                "Dostupné volby:\n" +
                "   -h, --help                  Zobrazí tuto nápovědu\n" +
                "   -p, --portable              Zapnutí přenosného módu, " +
                "zeptá se na umístění konfiguračních souborů, pokud je nenajde v aktuálním adresáři\n" +
                "   -c, --config <cesta>        Nastavení cesty ke konfiguračním souborům";
        System.out.println(usage);
    }
}
