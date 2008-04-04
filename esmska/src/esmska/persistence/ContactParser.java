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
import esmska.data.*;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.swing.SwingWorker;
import esmska.data.Contact;
import esmska.gui.FormChecker;

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
        DREAMCOM_SE_FILE
    }
    
    private File file;
    private ContactType type;
    
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
    
    protected ArrayList<Contact> doInBackground() throws Exception {
        Charset charset = Charset.forName("UTF-8");
        char separator = ',';
        //set charset
        switch (type) {
            case KUBIK_DREAMCOM_FILE:
            case DREAMCOM_SE_FILE:
                charset = Charset.forName("windows-1250");
                separator = ';';
        }
        CsvReader reader = new CsvReader(file.getPath(), separator, charset);
        reader.setUseComments(true);
        ArrayList<Contact> contacts = new ArrayList<Contact>();
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
                    operator = reader.get(20).equals("") ?
                        reader.get(21) : reader.get(20);
                    break;
                case DREAMCOM_SE_FILE:
                case ESMSKA_FILE:
                    name = reader.get(0);
                    number = reader.get(1);
                    operator = reader.get(2);
            }
            
            if (!FormChecker.checkContactName(name))
                continue;
            c.setName(name);
            if (!FormChecker.checkSMSNumber(number))
                continue;
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
        return contacts;
    }
}
