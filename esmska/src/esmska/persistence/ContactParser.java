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

/** Parse contacts from csv file of different programs
 *
 * @author ripper
 */
public class ContactParser extends SwingWorker<ArrayList<Contact>, Void> {
    public static enum ContactType {
        ESMSKA_FILE, KUBIK_DREAMCOM_FILE, DREAMCOM_SE_FILE
    }
    
    private File file;
    private ContactType type;
    
    public ContactParser(File file, ContactType type) {
        super();
        this.file = file;
        this.type = type;
    }
    protected ArrayList<Contact> doInBackground() throws Exception {
        Charset charset = Charset.forName("UTF-8");
        char separator = ',';
        switch (type) {
            case KUBIK_DREAMCOM_FILE:
            case DREAMCOM_SE_FILE:
                charset = Charset.forName("windows-1250");
                separator = ';';
        }
        CsvReader reader = new CsvReader(file.getPath(), separator, charset);
        reader.setUseComments(true);
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        while (reader.readRecord()) {
            Contact c = new Contact();
            String name = "";
            String number = "";
            String operator = "";
            
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
            c.setName(name);
            c.setNumber(number);
            switch (type) {
                case KUBIK_DREAMCOM_FILE:
                case DREAMCOM_SE_FILE:
                case ESMSKA_FILE: //LEGACY: be compatible with Esmska 0.7.0 and older
                    if (operator.equals("Vodafone")) {
                        operator = "[CZ]Vodafone";
                    } else if (operator.equals("O2")) {
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
