/*
 * ExportManager.java
 *
 * Created on 22. srpen 2007, 23:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import persistence.Contact;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    
    /** Creates a new instance of ExportManager */
    private ExportManager() {
    }
    
    public static void exportContacts(Component parent, Collection<Contact> contacts) {
        //show info
        String message =
                "<html>Své kontakty můžete exportovat do CSV souboru. To je<br>" +
                "textový soubor, kde všechna data vidíte v čitelné podobě.<br>" +
                "Pomocí importu můžete data později opět nahrát zpět do Esmsky,<br>" +
                "nebo je využít jinak.<br><br>" +
                "Soubor bude uložen v kódování UTF-8.<br><br>" +
                "Při potřebě úpravy struktury souboru (např. za účelem importu<br>" +
                "do jiného programu) využijte nějaký tabulkový procesor,<br>" +
                "např. zdarma dostupný OpenOffice Calc (www.openoffice.cz).</html>";
        JOptionPane.showMessageDialog(parent,new JLabel(message),"Export kontaktů",
                JOptionPane.INFORMATION_MESSAGE);
        
        //choose file
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Vyberte umístění exportovaného souboru");
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }
            public String getDescription() {
                return "CSV soubory (*.csv)";
            }
        });
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = chooser.getSelectedFile();
        if (file.exists() && !file.canWrite()) {
            JOptionPane.showMessageDialog(parent,"Do souboru " + file.getAbsolutePath() +
                    " nelze zapisovat!","Chyba výběru souboru", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        //save
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file,false),"UTF-8"));
            for (Contact contact : contacts) {
                //escape quotes
                String name = contact.getName().replaceAll("\"","\"\"");
                String operator = contact.getOperator().toString().replaceAll("\"","\"\"");
                writer.write("\"" + name + "\",\"+420" + contact.getNumber() +
                        "\",\"" + operator + "\"");
                writer.newLine();
            }
            writer.close();
            writer = null;
            JOptionPane.showMessageDialog(parent,"Export úspěšně dokončen!","Export hotov",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,"Export selhal!","Chyba při exportu",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
}
