/*
 * ContactParser.java
 *
 * Created on 11. září 2007, 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import com.csvreader.CsvReader;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import javax.swing.SwingWorker;
import operators.O2;
import operators.Operator;
import operators.OperatorEnum;
import operators.Vodafone;
import persistence.Contact;

/** Parse contacts from csv file of different programs
 *
 * @author ripper
 */
public class ContactParser extends SwingWorker<ArrayList<Contact>, Void> {
    public static final int ESMSKA_FILE = 0;
    public static final int KUBIK_DREAMCOM_FILE = 1;
    public static final int DREAMCOM_SE_FILE = 2;
    
    private File file;
    private Integer type;
    
    public ContactParser(File file, Integer type) {
        super();
        this.file = file;
        this.type = type;
    }
    protected ArrayList<Contact> doInBackground() throws Exception {
        Charset charset = Charset.forName("UTF-8");
        char separator = ',';
        switch (type) {
            case ContactParser.KUBIK_DREAMCOM_FILE:
            case ContactParser.DREAMCOM_SE_FILE:
                charset = Charset.forName("windows-1250");
                separator = ';';
        }
        CsvReader reader = new CsvReader(file.getPath(), separator, charset);
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        while (reader.readRecord()) {
            Contact c = new Contact();
            String name = "";
            String number = "";
            String operatorString = "";
            
            switch (type) {
                case ContactParser.KUBIK_DREAMCOM_FILE:
                    name = reader.get(5);
                    number = reader.get(6).substring(4);
                    operatorString = reader.get(20).equals("") ?
                        reader.get(21) : reader.get(20);
                    break;
                case ContactParser.DREAMCOM_SE_FILE:
                case ContactParser.ESMSKA_FILE:
                    name = reader.get(0);
                    number = reader.get(1).substring(4);
                    operatorString = reader.get(2);
            }
            c.setName(name);
            c.setNumber(number);
            Operator operator = null;
            if (operatorString.equals("Vodafone"))
                operator = new Vodafone();
            else if (operatorString.equals("O2"))
                operator = new O2();
            if (operator == null)
                operator = OperatorEnum.getOperator(c.getNumber());
            if (operator == null)
                continue;
            c.setOperator(operator);
            contacts.add(c);
        }
        return contacts;
    }
}
