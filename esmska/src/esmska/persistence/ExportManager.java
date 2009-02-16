/*
 * ExportManager.java
 *
 * Created on 22. srpen 2007, 23:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.wimpi.pim.Pim;
import net.wimpi.pim.contact.io.ContactMarshaller;
import net.wimpi.pim.contact.model.Communications;
import net.wimpi.pim.contact.model.PersonalIdentity;
import net.wimpi.pim.contact.model.PhoneNumber;
import net.wimpi.pim.factory.ContactIOFactory;
import net.wimpi.pim.factory.ContactModelFactory;

import com.csvreader.CsvWriter;

import esmska.data.Contact;
import esmska.data.History;
import esmska.data.Icons;
import esmska.data.Keyring;
import esmska.data.Log;
import esmska.data.SMS;
import esmska.utils.ConfirmingFileChooser;
import esmska.utils.L10N;
import esmska.utils.Tuple;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    private static final Logger logger = Logger.getLogger(ExportManager.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    private static final FileFilter csvFileFilter = 
            new FileNameExtensionFilter(l10n.getString("ExportManager.csv_filter"), "csv");
    private static final FileFilter vCardFileFilter = 
            new FileNameExtensionFilter(l10n.getString("ExportManager.vcard_filter"), "vcf", "vcard");
    private static ConfirmingFileChooser chooser;

    /** Disabled constructor */
    private ExportManager() {
    }
    
    /** Export contacts with info and file chooser dialog */
    public static void exportContacts(Component parent, Collection<Contact> contacts) {
        logger.finer("About to export " + contacts.size() + " contacts");
        //show info
        String message = l10n.getString("ExportManager.export_info");
        JOptionPane.showMessageDialog(parent,new JLabel(message),l10n.getString("ExportManager.contact_export"),
                JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(ExportManager.class.getResource(RES + "contact-48.png")));

        //choose file
        if (chooser == null) {
            chooser = new ConfirmingFileChooser();
        }
        chooser.setDialogTitle(l10n.getString("ExportManager.choose_export_file"));
        //set dialog type not to erase approve button text later
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setApproveButtonText(l10n.getString("Save"));
        chooser.addChoosableFileFilter(csvFileFilter);
        chooser.addChoosableFileFilter(vCardFileFilter);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(csvFileFilter);
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        File file = chooser.getSelectedFile();
        logger.finer("File chosen for contacts export: " + file.getAbsolutePath());

        //check if file can be written
        if (file.exists() && !file.canWrite()) {
            logger.info("File '" + file.getAbsolutePath() + "' can't be written");
            JOptionPane.showMessageDialog(parent,
                    MessageFormat.format(l10n.getString("ExportManager.cant_write"), file.getAbsolutePath()),
                    null, JOptionPane.ERROR_MESSAGE);
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
            Log.getInstance().addRecord(new Log.Record(
                    l10n.getString("ExportManager.export_failed"), null, Icons.STATUS_ERROR));
            JOptionPane.showMessageDialog(parent,l10n.getString("ExportManager.export_failed"), null,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Log.getInstance().addRecord(new Log.Record(
                    l10n.getString("ExportManager.export_ok"), null, Icons.STATUS_INFO));
        JOptionPane.showMessageDialog(parent,l10n.getString("ExportManager.export_ok!"), null,
                    JOptionPane.INFORMATION_MESSAGE);
    }
    
    /** Export contacts to csv file */
    public static void exportContacts(Collection<Contact> contacts, File file)
    throws IOException {
        logger.finer("Exporting " + contacts.size() + " contacts to CSV file: " + file.getAbsolutePath());
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment(l10n.getString("ExportManager.contact_list"));
            for (Contact contact : contacts) {
                writer.writeRecord(new String[] {
                    contact.getName(),
                    contact.getNumber(),
                    contact.getOperator()
                });
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    /** Export contacts to vCard file */
    public static void exportContactsToVCard(Collection<Contact> contacts, File file)
            throws IOException {
        logger.finer("Exporting " + contacts.size() + " contacts to vCard file: " + file.getAbsolutePath());
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
        logger.finer("Exporting queue of " + queue.size() + " SMSs to file: " + file.getAbsolutePath());
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment(l10n.getString("ExportManager.sms_queue"));
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
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    /** Export sms history to file */
    public static void exportHistory(Collection<History.Record> history, File file) throws IOException {
        logger.finer("Exporting history of " + history.size() + " records to file: " + file.getAbsolutePath());
        CsvWriter writer = null;
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.LONG, Locale.ROOT);
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment(l10n.getString("ExportManager.history"));
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
            if (writer != null) {
                writer.close();
            }
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
        logger.finer("Exporting keyring to file: " + file.getAbsolutePath());
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(file.getPath(), ',', Charset.forName("UTF-8"));
            writer.writeComment(l10n.getString("ExportManager.login"));
            for (String operatorName : keyring.getOperatorNames()) {
                Tuple<String, String> key = keyring.getKey(operatorName);
                String login = key.get1();
                String password = Keyring.encrypt(key.get2());
                writer.writeRecord(new String[] {
                    operatorName,
                    login,
                    password
                });
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
