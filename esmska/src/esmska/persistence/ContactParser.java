/*
 * ContactParser.java
 *
 * Created on 11. září 2007, 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import com.csvreader.CsvReader;
import esmska.data.Config;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.swing.SwingWorker;
import esmska.data.Contact;
import esmska.gui.FormChecker;
import esmska.operators.Operator;
import esmska.operators.OperatorUtil;
import esmska.utils.Nullator;
import java.io.FileInputStream;
import java.util.logging.Logger;
import net.wimpi.pim.Pim;
import net.wimpi.pim.contact.io.ContactUnmarshaller;
import net.wimpi.pim.contact.model.Communications;
import net.wimpi.pim.contact.model.PersonalIdentity;
import net.wimpi.pim.contact.model.PhoneNumber;
import net.wimpi.pim.factory.ContactIOFactory;

/** Parse contacts from csv file of different programs. Works in background thread.
 * Returns collection of parsed contacts.
 * @author ripper
 */
public class ContactParser extends SwingWorker<ArrayList<Contact>, Void> {
    /** Types of parseable file formats */
    public static enum ContactType {
        /** Native file format for Esmska */
        ESMSKA_FILE,
        /** File format of the Kubík SMS DreamCom */
        KUBIK_DREAMCOM_FILE,
        /** File format of the DreamCom SE */
        DREAMCOM_SE_FILE,
        /** vCard file format (.vcard, .vcf) */
        VCARD_FILE
    }

    private static final Logger logger = Logger.getLogger(ContactParser.class.getName());
    private static final Config config = PersistenceManager.getConfig();
    private File file;
    private ContactType type;
    private ArrayList<Contact> contacts = new ArrayList<Contact>();
    
    /** Creates new ContactParser.
     * 
     * @param file File to parse.
     * @param type Which program was used to save the file.
     */
    public ContactParser(File file, ContactType type) {
        super();
        this.file = file;
        this.type = type;
    }
    
    @Override
    protected ArrayList<Contact> doInBackground() throws Exception {
        switch(type) {
            case VCARD_FILE:
                return parseVCARD();
            case ESMSKA_FILE:
            case KUBIK_DREAMCOM_FILE:
            case DREAMCOM_SE_FILE:
                return parseCSV();
            default:
                throw new UnsupportedOperationException("Enum element " + type +
                        " not currently supported");
        }
    }
    
    /** parse csv file and return contacts */
    private ArrayList<Contact> parseCSV() throws IOException {
        logger.finer("Parsing CSV file '" + file + "' as type " + type);
        contacts.clear();
        
        Charset charset = Charset.forName("UTF-8");
        char separator = ',';
        //set charset
        switch (type) {
            case KUBIK_DREAMCOM_FILE:
            case DREAMCOM_SE_FILE:
                charset = Charset.forName("windows-1250");
                separator = ';';
        }
        CsvReader reader = null;
        
        try {
            reader = new CsvReader(file.getPath(), separator, charset);
            reader.setUseComments(true);

            //read all the records
            while (reader.readRecord()) {
                Contact c = new Contact();
                String name = "";
                String number = "";
                String operator = "";

                //read record items
                switch (type) {
                    case KUBIK_DREAMCOM_FILE:
                        name = reader.get(5);
                        number = reader.get(6);
                        operator = reader.get(20).equals("") ? reader.get(21) : reader.get(20);
                        break;
                    case DREAMCOM_SE_FILE:
                    case ESMSKA_FILE:
                        name = reader.get(0);
                        number = reader.get(1);
                        operator = reader.get(2);
                }

                if (!FormChecker.checkContactName(name)) {
                    continue;
                }
                c.setName(name);
                if (!FormChecker.checkSMSNumber(number)) {
                    continue;
                }
                c.setNumber(number);
                //convert known operators to our operators
                switch (type) {
                    case KUBIK_DREAMCOM_FILE:
                        if (operator.startsWith("Oskar") || operator.startsWith("Vodafone")) {
                            operator = "[CZ]Vodafone";
                        } else if (operator.startsWith("Eurotel") || operator.startsWith("O2")) {
                            operator = "[CZ]O2";
                        } else if (operator.startsWith("T-Mobile")) {
                            operator = "[CZ]t-zones";
                        }
                        break;
                    case DREAMCOM_SE_FILE:
                        if (operator.startsWith("O2")) {
                            operator = "[CZ]O2";
                        } else if (operator.startsWith("Vodafone")) {
                            operator = "[CZ]Vodafone";
                        } else if (operator.startsWith("T-Zones")) {
                            operator = "[CZ]t-zones";
                        }
                        break;
                    case ESMSKA_FILE: //LEGACY: be compatible with Esmska 0.7.0 and older
                        if ("Vodafone".equals(operator)) {
                            operator = "[CZ]Vodafone";
                        } else if ("O2".equals(operator)) {
                            operator = "[CZ]O2";
                        }
                        break;
                }
                c.setOperator(operator);

                contacts.add(c);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        logger.finer("Parsed " + contacts.size() + " contacts");
        return contacts;
    }
    
    /** parse vcard file and return contacts */
    private ArrayList<Contact> parseVCARD() throws IOException {
        logger.finer("Parsing CSV file '" + file + "' as type " + type);
        contacts.clear();
        
        ContactIOFactory ciof = Pim.getContactIOFactory();
        ContactUnmarshaller unmarshaller = ciof.createContactUnmarshaller();
        unmarshaller.setStrict(false); //in order to parse older vCard 2.1 files
        unmarshaller.setEncoding("UTF-8");
        
        FileInputStream input = null;
        
        try {
            input = new FileInputStream(file);
            net.wimpi.pim.contact.model.Contact[] pimContacts =
                    unmarshaller.unmarshallContacts(input);
            for (net.wimpi.pim.contact.model.Contact pimContact : pimContacts) {
                PersonalIdentity pi = pimContact.getPersonalIdentity();
                //FN (formatted name) should be there
                String name = pi.getFormattedName();
                //if not, read N (name)
                if (Nullator.isEmpty(name)) {
                    String firstName = pi.getFirstname();
                    String middleName = "";
                    for (int i = 0; i < pi.getAdditionalNameCount(); i++) {
                        middleName += pi.getAdditionalName(i);
                        if (i < pi.getAdditionalNameCount() - 1) {
                            middleName += " ";
                        }
                    }
                    String lastName = pi.getLastname();
                    name = (Nullator.isEmpty(firstName) ? "" : firstName + " ") +
                            (Nullator.isEmpty(middleName) ? "" : middleName + " ") +
                            (Nullator.isEmpty(lastName) ? "" : lastName);
                    name = name.trim();
                }
                //if no FN nor N, skip contact
                if (Nullator.isEmpty(name)) {
                    continue;
                }
                Communications co = pimContact.getCommunications();
                String number = "";
                //select best phone number if available
                if (co != null && co.getPhoneNumberCount() > 0) {
                    PhoneNumber preffered = co.getPreferredPhoneNumber();
                    PhoneNumber[] cellulars = co.listPhoneNumbersByType(PhoneNumber.TYPE_CELLULAR);
                    PhoneNumber[] messengers = co.listPhoneNumbersByType(PhoneNumber.TYPE_MESSAGING);
                    PhoneNumber first = (PhoneNumber) co.getPhoneNumbers().next();
                    //priority: preffered > cellular > messaging > first listed
                    if (preffered != null) {
                        number = preffered.getNumber();
                    } else if (cellulars.length > 0) {
                        number = cellulars[0].getNumber();
                    } else if (messengers.length > 0) {
                        number = messengers[0].getNumber();
                    } else {
                        number = first.getNumber();
                    }
                }
                //convert to international format
                if (!Nullator.isEmpty(number)) {
                    boolean international = number.startsWith("+");
                    number = number.replaceAll("[^0-9]", "");
                    if (!international && !Nullator.isEmpty(config.getCountryPrefix())) {
                        number = config.getCountryPrefix() + number;
                    } else {
                        number = "+" + number;
                    }
                }

                //create contact
                Contact contact = new Contact();
                contact.setName(name);
                contact.setNumber(number);
                Operator operator = OperatorUtil.suggestOperator(number, null);
                contact.setOperator(operator != null ? operator.getName() : null); //guess operator

                contacts.add(contact);
            }
        } finally {
            if (input != null) {
                input.close();
            }
        }

        logger.finer("Parsed " + contacts.size() + " contacts");
        return contacts;
    }
}
