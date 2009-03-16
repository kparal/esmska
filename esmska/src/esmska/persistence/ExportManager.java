/*
 * ExportManager.java
 *
 * Created on 22. srpen 2007, 23:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Logger;

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
import esmska.data.Keyring;
import esmska.data.SMS;
import esmska.utils.L10N;
import esmska.data.Tuple;
import java.util.ResourceBundle;
import org.apache.commons.lang.Validate;

/** Export program data
 *
 * @author ripper
 */
public class ExportManager {
    private static final Logger logger = Logger.getLogger(ExportManager.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    /** Disabled constructor */
    private ExportManager() {
    }
    
    /** Export contacts to csv file
     * @param contacts contacts, not null
     * @param out output stream, not null
     */
    public static void exportContacts(Collection<Contact> contacts, OutputStream out)
    throws IOException {
        Validate.notNull(contacts);
        Validate.notNull(out);

        logger.finer("Exporting " + contacts.size() + " contacts to CSV");
        CsvWriter writer = new CsvWriter(out, ',', Charset.forName("UTF-8"));

        writer.writeComment(l10n.getString("ExportManager.contact_list"));
        for (Contact contact : contacts) {
            writer.writeRecord(new String[] {
                contact.getName(),
                contact.getNumber(),
                contact.getOperator()
            });
        }
        writer.flush();
    }
    
    /** Export contacts to vCard file
     * @param contacts contacts, not null
     * @param out output stream, not null
     */
    public static void exportContactsToVCard(Collection<Contact> contacts, OutputStream out)
            throws IOException {
        Validate.notNull(contacts);
        Validate.notNull(out);

        logger.finer("Exporting " + contacts.size() + " contacts to vCard");
        
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
        marshaller.marshallContacts(out,
                conts.toArray(new net.wimpi.pim.contact.model.Contact[0]));
        out.flush();
    }

    /** Export sms queue to file
     * @param queue queue, not null
     * @param out output stream, not null
     */
    public static void exportQueue(Collection<SMS> queue, OutputStream out) throws IOException {
        Validate.notNull(queue);
        Validate.notNull(out);

        logger.finer("Exporting queue of " + queue.size() + " SMSs");
        CsvWriter writer = new CsvWriter(out, ',', Charset.forName("UTF-8"));

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
        writer.flush();
    }
    
    /** Export sms history to file
     * @param history history, not null
     * @param out output stream, not null
     */
    public static void exportHistory(Collection<History.Record> history, OutputStream out) throws IOException {
        Validate.notNull(history);
        Validate.notNull(out);

        logger.finer("Exporting history of " + history.size() + " records");
        CsvWriter writer = new CsvWriter(out, ',', Charset.forName("UTF-8"));
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG,
                DateFormat.LONG, Locale.ROOT);

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
        writer.flush();
    }
    
    /** Export keyring to file.
     * @param keyring keyring to export, not null
     * @param out output stream, not null
     * @throws java.io.IOException When some error occur during file processing.
     * @throws java.security.GeneralSecurityException When there is problem with
     *         key encryption.
     */
    public static void exportKeyring(Keyring keyring, OutputStream out)
            throws IOException, GeneralSecurityException {
        Validate.notNull(keyring);
        Validate.notNull(out);

        logger.finer("Exporting keyring");
        CsvWriter writer = new CsvWriter(out, ',', Charset.forName("UTF-8"));

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
        writer.flush();
    }
}
