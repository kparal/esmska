/*
 * ExportManager.java
 *
 * Created on 22. srpen 2007, 23:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import com.csvreader.CsvWriter;
import esmska.data.*;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import esmska.data.Contact;
import esmska.data.SMS;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    
    /** Creates a new instance of ExportManager */
    private ExportManager() {
    }
    
    /** Export contacts with info and file chooser dialog */
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
        if (! file.getName().toLowerCase().endsWith(".csv"))
            file = new File(file.getPath() + ".csv");
        
        if (file.exists() && !file.canWrite()) {
            JOptionPane.showMessageDialog(parent,"Do souboru " + file.getAbsolutePath() +
                    " nelze zapisovat!","Chyba výběru souboru", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        //save
        try {
            exportContacts(contacts, file);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,"Export selhal!","Chyba při exportu",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(parent,"Export úspěšně dokončen!","Export hotov",
                    JOptionPane.INFORMATION_MESSAGE);
        
    }
    
    /** Export contacts to file */
    public static void exportContacts(Collection<Contact> contacts, File file)
    throws IOException {
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment("Seznam kontaktů");
            for (Contact contact : contacts) {
                writer.writeRecord(new String[] {
                    contact.getName(),
                    contact.getCountryCode() + contact.getNumber(),
                    contact.getOperator().toString()
                });
            }
        } catch (IOException ex) {
            throw new IOException(ex);
        } finally {
            writer.close();
        }
    }
    
    /** Export sms queue to file */
    public static void exportQueue(Collection<SMS> queue, File file) throws IOException {
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment("Fronta sms k odeslání");
            for (SMS sms : queue) {
                writer.writeRecord(new String[] {
                    sms.getName() != null ? sms.getName() : "",
                    sms.getNumber(),
                    sms.getOperator().toString(),
                    sms.getText(),
                    sms.getSenderName() != null ? sms.getSenderName() : "",
                    sms.getSenderNumber() != null ? sms.getSenderNumber() : ""
                });
            }
        } catch (IOException ex) {
            throw new IOException(ex);
        } finally {
            writer.close();
        }
    }
}
