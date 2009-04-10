/*
 * ExportManager.java
 *
 * Created on 22. srpen 2007, 23:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import android.provider.Contacts;
import android.syncml.pim.vcard.ContactStruct;
import android.syncml.pim.vcard.VCardComposer;
import android.syncml.pim.vcard.VCardException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Logger;


import com.csvreader.CsvWriter;

import esmska.data.Contact;
import esmska.data.History;
import esmska.data.Keyring;
import esmska.data.SMS;
import esmska.utils.L10N;
import esmska.data.Tuple;
import java.util.ResourceBundle;
import org.apache.commons.io.IOUtils;
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
    
    /** Export contacts to csv format
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
    
    /** Export contacts to vCard format
     * @param contacts contacts, not null
     * @param out output stream, not null
     */
    public static void exportContactsToVCard(Collection<Contact> contacts, OutputStream out)
            throws IOException, VCardException {
        Validate.notNull(contacts);
        Validate.notNull(out);

        logger.finer("Exporting " + contacts.size() + " contacts to vCard");
        
        VCardComposer composer = new VCardComposer();

        for (Contact contact : contacts) {
            ContactStruct struct = new ContactStruct();

            struct.name = contact.getName();
            struct.addPhone(contact.getNumber(),
                    String.valueOf(Contacts.Phones.TYPE_MOBILE), null);

            String vcardString = composer.createVCard(struct, VCardComposer.VERSION_VCARD30_INT);

            IOUtils.write(vcardString, out, "UTF-8");
            //create empty lines between contacts
            IOUtils.write("\n", out, "UTF-8");
        }

        out.flush();
    }

    /** Export sms queue
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
    
    /** Export sms history
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
    
    /** Export keyring
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

    /** Export operator script and icon
     *
     * @param scriptContents operator script contents, not null nor empty
     * @param icon operator icon, may be null
     * @param scriptOut output stream for operator script, not null
     * @param iconOut output stream for operator icon, mustn't be null
     * if <code>icon</code> is not null
     * @throws java.io.IOException if there was some problem with saving data
     */
    public static void exportOperator(String scriptContents, byte[] icon, OutputStream scriptOut, OutputStream iconOut)
            throws IOException {
        Validate.notEmpty(scriptContents);
        Validate.notNull(scriptOut);
        if (icon != null) {
            Validate.notNull(iconOut);
        }

        logger.finer("Exporting operator");

        IOUtils.write(scriptContents, scriptOut, "UTF-8");
        scriptOut.flush();

        if (icon != null) {
            IOUtils.write(icon, iconOut);
            iconOut.flush();
        }
    }
}
