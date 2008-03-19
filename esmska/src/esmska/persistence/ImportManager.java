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
import java.security.PrivilegedActionException;
import java.text.ParseException;
import java.util.ArrayList;
import esmska.operators.Operator;
import esmska.data.Contact;
import esmska.data.SMS;
import esmska.operators.DefaultOperator;
import esmska.operators.Operator;
import java.io.FileFilter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptException;

/** Import program data
 *
 * @author ripper
 */
public class ImportManager {
    private static final Logger logger = Logger.getLogger(ImportManager.class.getName());
        
    /** Disabled constructor */
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
            String operator = reader.get(2);
            String text = reader.get(3);
            String senderName = reader.get(4);
            String senderNumber = reader.get(5);

            SMS sms = new SMS();
            sms.setName(name);
            sms.setNumber(number);
            sms.setText(text);
            sms.setSenderName(senderName);
            sms.setSenderNumber(senderNumber);
            sms.setOperator(operator);
            queue.add(sms);
        }
        return queue;
    }
    
    /** Import sms history from file */
    public static ArrayList<History.Record> importHistory(File file)
            throws IOException, ParseException {
        CsvReader reader = new CsvReader(file.getPath(), ',', Charset.forName("UTF-8"));
        reader.setUseComments(true);
        ArrayList<History.Record> history = new ArrayList<History.Record>();
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
            
            History.Record record = new History.Record();
            record.setDate(date);
            record.setName(name);
            record.setNumber(number);
            record.setOperator(operator);
            record.setText(text);
            record.setSenderName(senderName);
            record.setSenderNumber(senderNumber);
            
            history.add(record);
        }
        return history;
    }
    
    /** Import all operators from directory */
    public static TreeSet<Operator> importOperators(File directory) throws IOException {
        TreeSet<Operator> operators = new TreeSet<Operator>();
        if (!directory.canRead() || !directory.isDirectory())
            throw new IOException("Invalid operator directory.");
        
        File[] files = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getAbsolutePath().endsWith(".operator") && pathname.canRead();
            }
        });
        
        for (File file : files) {
            try {
                DefaultOperator operator = new DefaultOperator(file);
                operators.add(operator);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Problem accessing file " + file.getAbsolutePath(), ex);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Ivalid operator file " + file.getAbsolutePath(), ex);
            }
        }

        return operators;
    }
}
