/*
 * ImportManager.java
 *
 * Created on 15. září 2007, 13:02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.persistence;

import com.csvreader.CsvReader;
import esmska.*;
import esmska.data.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import esmska.operators.O2;
import esmska.operators.Operator;
import esmska.operators.OperatorEnum;
import esmska.operators.Vodafone;
import esmska.data.Contact;
import esmska.data.SMS;

/** Import program data
 *
 * @author ripper
 */
public class ImportManager {
    
    /** Creates a new instance of ImportManager */
    private ImportManager() {
    }
    
    /** Import contacts from file */
    public static ArrayList<Contact> importContacts(File file, ContactParser.ContactType type)
    throws Exception {
        ContactParser parser = new ContactParser(file, type);
        parser.execute();
        return parser.get();
    }
    
    /** Import sms queue from file */
    public static ArrayList<SMS> importQueue(File file) throws IOException {
        CsvReader reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
        reader.setUseComments(true);
        ArrayList<SMS> queue = new ArrayList<SMS>();
        while (reader.readRecord()) {
            String name = reader.get(0);
            String number = reader.get(1);
            String operatorString = reader.get(2);
            String text = reader.get(3);
            String senderName = reader.get(4);
            String senderNumber = reader.get(5);

            SMS sms = new SMS();
            sms.setName(name);
            sms.setNumber(number);
            sms.setText(text);
            sms.setSenderName(senderName);
            sms.setSenderNumber(senderNumber);
            Operator operator = null;
            if (operatorString.equals("Vodafone"))
                operator = new Vodafone();
            else if (operatorString.equals("O2"))
                operator = new O2();
            if (operator == null)
                operator = OperatorEnum.getOperator(sms.getNumber());
            if (operator == null)
                continue;
            sms.setOperator(operator);
            queue.add(sms);
        }
        return queue;
    }
    
}
