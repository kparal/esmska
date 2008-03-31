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
import java.security.GeneralSecurityException;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import esmska.data.Contact;
import esmska.data.SMS;
import java.text.DateFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    private static final Logger logger = Logger.getLogger(ExportManager.class.getName());
    private static final String RES = "/esmska/resources/";

    /** Disabled constructor */
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
                JOptionPane.INFORMATION_MESSAGE, 
                new ImageIcon(ExportManager.class.getResource(RES + "contact-48.png")));
        
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
            logger.log(Level.WARNING, "Could not export contacts to file", ex);
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
                    contact.getNumber(),
                    contact.getOperator()
                });
            }
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
                    sms.getName(),
                    sms.getNumber(),
                    sms.getOperator(),
                    sms.getText(),
                    sms.getSenderName(),
                    sms.getSenderNumber()
                });
            }
        } finally {
            writer.close();
        }
    }
    
    /** Export sms history to file */
    public static void exportHistory(Collection<History.Record> history, File file) throws IOException {
        CsvWriter writer = null;
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, 
                DateFormat.LONG, Locale.ROOT);
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment("Historie odeslaných sms");
            for (History.Record record : history) {
                writer.writeRecord(new String[] {
                    df.format(record.getDate()),
                    record.getName(),
                    record.getNumber(),
                    record.getOperator(),
                    record.getText(),
                    record.getSenderName(),
                    record.getSenderNumber()
                });
            }
        } finally {
            writer.close();
        }
    }
    
    /** Export keyring to file.
     * @param keyring Keyring to export.
     * @param file File where to export.
     * @throws java.io.IOException When some error occur during file processing.
     * @throws java.security.GeneralSecurityException When there is problem with
     *         key encryption.
     */
    public static void exportKeyring(Keyring keyring, File file)
            throws IOException, GeneralSecurityException {
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment("Uživatelská jména a hesla k jednotlivým operátorům");
            for (String operatorName : keyring.getOperatorNames()) {
                String[] key = keyring.getKey(operatorName);
                String login = key[0];
                String password = Keyring.encrypt(key[1]);
                writer.writeRecord(new String[] {
                    operatorName,
                    login,
                    password
                });
            }
        } finally {
            writer.close();
        }
    }
}
