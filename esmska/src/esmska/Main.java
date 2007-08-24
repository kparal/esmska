/*
 * Main.java
 *
 * Created on 24. srpen 2007, 22:20
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import persistence.PersistenceManager;

/**
 *
 * @author ripper
 */
public class Main {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //load user files
        try {
            PersistenceManager pm = PersistenceManager.getInstance();
            try {
                pm.loadConfig();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                pm.loadContacts();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "Nepodařilo se načíst konfiguraci!",
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
