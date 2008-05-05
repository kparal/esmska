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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import net.wimpi.pim.Pim;
import net.wimpi.pim.contact.io.ContactMarshaller;
import net.wimpi.pim.contact.model.Communications;
import net.wimpi.pim.contact.model.PersonalIdentity;
import net.wimpi.pim.contact.model.PhoneNumber;
import net.wimpi.pim.factory.ContactIOFactory;
import net.wimpi.pim.factory.ContactModelFactory;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    private static final Logger logger = Logger.getLogger(ExportManager.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final FileFilter csvFileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
        }
        @Override
        public String getDescription() {
            return "CSV soubory (*.csv)";
        }
    };
    private static final FileFilter vCardFileFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".vcard") ||
                    f.getName().toLowerCase().endsWith(".vcf");
        }
        @Override
        public String getDescription() {
            return "vCard soubory (*.vcard, *.vcf)";
        }
    };

    /** Disabled constructor */
    private ExportManager() {
    }
    
    /** Export contacts with info and file chooser dialog */
    public static void exportContacts(Component parent, Collection<Contact> contacts) {
        //show info
        String message =
                "<html>Své kontakty můžete exportovat do CSV či vCard souboru. To jsou<br>" +
                "textové soubory, kde všechna data vidíte v čitelné podobě.<br>" +
                "Pomocí importu můžete data později opět nahrát zpět do Esmsky,<br>" +
                "nebo je využít jinak.<br><br>" +
                "Soubor ve formátu vCard je standardizovaný a můžete ho použít<br>" +
                "v mnoha dalších aplikacích. Soubor CSV má velice jednoduchý obsah<br>" +
                "a každý program ho vytváří trochu jinak. Při potřebě úpravy jeho<br>" +
                "struktury pro import v jiném programu využijte nějaký tabulkový<br>" +
                "procesor, např. zdarma dostupný OpenOffice Calc (www.openoffice.cz).<br><br>" +
                "Soubor bude uložen v kódování UTF-8.</html>";
        JOptionPane.showMessageDialog(parent,new JLabel(message),"Export kontaktů",
                JOptionPane.INFORMATION_MESSAGE, 
                new ImageIcon(ExportManager.class.getResource(RES + "contact-48.png")));
        
        //choose file
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Vyberte umístění a typ exportovaného souboru");
        chooser.addChoosableFileFilter(csvFileFilter);
        chooser.addChoosableFileFilter(vCardFileFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(csvFileFilter);
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
            return;
        
        File file = chooser.getSelectedFile();
        //append correct extension
        if (chooser.getFileFilter() == csvFileFilter && 
                !file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getPath() + ".csv");
        } else if (chooser.getFileFilter() == vCardFileFilter &&
                !file.getName().toLowerCase().endsWith(".vcard") &&
                !file.getName().toLowerCase().endsWith(".vcf")) {
            file = new File(file.getPath() + ".vcf");
        }
        
        if (file.exists() && !file.canWrite()) {
            JOptionPane.showMessageDialog(parent,"Do souboru " + file.getAbsolutePath() +
                    " nelze zapisovat!", null, JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        //save
        try {
            if (chooser.getFileFilter() == vCardFileFilter) {
                exportContactsToVCard(contacts, file);
            } else {
                exportContacts(contacts, file);
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not export contacts to file", ex);
            JOptionPane.showMessageDialog(parent,"Export selhal!", null,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JOptionPane.showMessageDialog(parent,"Export úspěšně dokončen!", null,
                    JOptionPane.INFORMATION_MESSAGE);
        
    }
    
    /** Export contacts to csv file */
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
    
    /** Export contacts to vCard file */
    public static void exportContactsToVCard(Collection<Contact> contacts, File file)
            throws IOException {
        OutputStream output = null;
        
        try {
            output = new FileOutputStream(file);
            
            ContactIOFactory ciof = Pim.getContactIOFactory();
            ContactModelFactory cmf = Pim.getContactModelFactory();
            ContactMarshaller marshaller = ciof.createContactMarshaller();
            marshaller.setEncoding("UTF-8");
            ArrayList<net.wimpi.pim.contact.model.Contact> conts =
                    new ArrayList<net.wimpi.pim.contact.model.Contact>();

            //convert contacts to vcard library contacts
            for (Contact contact : contacts) {
                net.wimpi.pim.contact.model.Contact c = cmf.createContact();

                PersonalIdentity pid = cmf.createPersonalIdentity();
                pid.setFormattedName(contact.getName());
                pid.setFirstname("");
                pid.setLastname(contact.getName());
                c.setPersonalIdentity(pid);

                Communications comm = cmf.createCommunications();
                PhoneNumber number = cmf.createPhoneNumber();
                number.setNumber(contact.getNumber());
                comm.addPhoneNumber(number);
                c.setCommunications(comm);

                conts.add(c);
            }

            //do the export
            marshaller.marshallContacts(output,
                    conts.toArray(new net.wimpi.pim.contact.model.Contact[0]));

        } finally {
            if (output != null) {
                output.close();
            }
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
