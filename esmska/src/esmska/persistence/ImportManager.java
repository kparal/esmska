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
import java.text.ParseException;
import java.util.ArrayList;
import esmska.operators.O2;
import esmska.operators.Operator;
import esmska.operators.OperatorEnum;
import esmska.operators.Vodafone;
import esmska.data.Contact;
import esmska.data.SMS;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

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
    
    /** Import sms history from file */
    public static ArrayList<History> importHistory(File file)
            throws IOException, ParseException {
        CsvReader reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
        reader.setUseComments(true);
        ArrayList<History> history = new ArrayList<History>();
        while (reader.readRecord()) {
            String dateString = reader.get(0);
            String name = reader.get(1);
            String number = reader.get(2);
            String operator = reader.get(3);
            String text = reader.get(4);
            String senderName = reader.get(5);
            String senderNumber = reader.get(6);

            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, 
                    DateFormat.LONG, Locale.ROOT);
            Date date = df.parse(dateString);
            
            History hist = new History();
            hist.setDate(date);
            hist.setName(name);
            hist.setNumber(number);
            hist.setOperator(operator);
            hist.setText(text);
            hist.setSenderName(senderName);
            hist.setSenderNumber(senderNumber);
            
            history.add(hist);
        }
        return history;
    }
    
}
