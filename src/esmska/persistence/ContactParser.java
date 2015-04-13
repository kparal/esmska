package esmska.persistence;

import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import com.csvreader.CsvReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.swing.SwingWorker;
import esmska.data.Contact;
import esmska.data.Gateways;
import esmska.data.Gateway;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.RandomUtils;

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
    private ArrayList<Contact> parseCSV() throws Exception {
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
                try {
                    String name = "";
                    String number = "";
                    String gateway = "";
                    String group = "";
                    //read record items
                    switch (type) {
                        case KUBIK_DREAMCOM_FILE:
                            name = reader.get(5);
                            number = reader.get(6);
                            gateway = reader.get(20).equals("") ? reader.get(21) : reader.get(20);
                            break;
                        case DREAMCOM_SE_FILE:
                        case ESMSKA_FILE:
                            name = reader.get(0);
                            number = reader.get(1);
                            gateway = reader.get(2);
                            group= reader.get(3);
                    }

                    if (StringUtils.isEmpty(name)) {
                        continue;
                    }
                    if (!Contact.isValidNumber(number)) {
                        continue;
                    }

                    //convert known gateways to our gateways
                    switch (type) {
                        case KUBIK_DREAMCOM_FILE:
                            if (gateway.startsWith("Oskar") || gateway.startsWith("Vodafone")) {
                                gateway = "[CZ]Vodafone";
                            } else if (gateway.startsWith("Eurotel") || gateway.startsWith("O2")) {
                                gateway = "[CZ]O2";
                            } else if (gateway.startsWith("T-Mobile")) {
                                gateway = "[CZ]T-mobile";
                            }
                            break;
                        case DREAMCOM_SE_FILE:
                            if (gateway.startsWith("O2")) {
                                gateway = "[CZ]O2";
                            } else if (gateway.startsWith("Vodafone")) {
                                gateway = "[CZ]Vodafone";
                            } else if (gateway.startsWith("T-Zones")) {
                                gateway = "[CZ]t-zones";
                            }
                            break;
                    }

                    contacts.add(new Contact(name, number, gateway,group));
                } catch (Exception e) {
                    logger.severe("Invalid contact record: " + reader.getRawRecord());
                    throw e;
                }
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
    private ArrayList<Contact> parseVCARD() throws IOException, VCardException {
        logger.finer("Parsing vCard file '" + file + "' as type " + type);
        contacts.clear();

        VCardParser parser = new VCardParser();
        VDataBuilder builder = new VDataBuilder();

        //read whole file to string
        String vcardString = FileUtils.readFileToString(file, "UTF-8");

        boolean parsed = parser.parse(vcardString, "UTF-8", builder);
        if (!parsed) {
            throw new VCardException("Could not parse vCard file: " + file);
        }

        //get all parsed contacts
        List<VNode> pimContacts = builder.vNodeList;

        for (VNode contact : pimContacts) {
            ArrayList<PropertyNode> props = contact.propList;

            //contact name - FN property
            String name = null;
            for (PropertyNode prop : props) {
                if ("FN".equals(prop.propName)) {
                    name = prop.propValue;
                    break;
                }
            }
            //contact name - N property (in case FN wasn't present)
            if (name == null) {
                for (PropertyNode prop : props) {
                    if ("N".equals(prop.propName)) {
                        //replace separators as spaces between name parts
                        name = StringUtils.replace(prop.propValue, ";", " ");
                        break;
                    }
                }
            }
            //contact name - ORG property (in case FN and N wasn't present)
            if (name == null) {
                for (PropertyNode prop : props) {
                    if ("ORG".equals(prop.propName)) {
                        name = prop.propValue;
                        break;
                    }
                }
            }
            //skip contact without name
            if (StringUtils.isEmpty(name)) {
                continue;
            }

            //phone number - TEL property
            String number = null;
            boolean preferred = false, cellular = false;
            for (PropertyNode prop : props) {
                if ("TEL".equals(prop.propName)) {
                    Set<String> types = prop.paramMap_TYPE;
                    if (StringUtils.isEmpty(number)) {
                        //first number
                        number = prop.propValue;
                    }
                    if (!preferred && containsIgnoreCase(types, "PREF")) {
                        //found first preferred number
                        number = prop.propValue;
                        preferred = true;
                    }
                    if (!preferred && !cellular && containsIgnoreCase(types, "CELL")) {
                        //found first cellular and there was no previously preferred number
                        number = prop.propValue;
                        cellular = true;
                    }
                }
            }
            //convert number to valid format (or null)
            number = Contact.parseNumber(number);
            //skip contact without valid number
            if (number == null) {
                continue;
            }

            //guess gateway
            ArrayList<Gateway> gateways = Gateways.getInstance().suggestGateway(number).get1();
            String gatewayName = gateways.isEmpty() ? null :
                gateways.get(RandomUtils.nextInt(gateways.size())).getName();

            //create contact
            contacts.add(new Contact(name, number, gatewayName));
        }

        logger.finer("Parsed " + contacts.size() + " contacts");
        return contacts;
    }

    /** Check if set of strings contains particular string regardless of
     * case sensitivity.
     * @param set not null
     * @param item not null
     * @return true if set contains item case-insensitively
     */
    private boolean containsIgnoreCase(Set<String> set, String item) {
        Validate.notNull(set);
        Validate.notNull(item);

        for (String s : set) {
            if (StringUtils.equalsIgnoreCase(s, item)) {
                return true;
            }
        }
        return false;
    }
}
