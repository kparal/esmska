/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.data.event.ActionEventSupport;
import esmska.utils.LogUtils;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** SMS history entity
 *
 * @author ripper
 */
public class History {
    
    /** shared instance */
    private static final History instance = new History();

    /** new record added */
    public static final int ACTION_ADD_RECORD = 0;
    /** existing record removed */
    public static final int ACTION_REMOVE_RECORD = 1;
    /** all records removed */
    public static final int ACTION_CLEAR_RECORDS = 2;

    private static final Logger logger = Logger.getLogger(History.class.getName());
    private final List<Record> records = Collections.synchronizedList(new ArrayList<Record>());

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** disabled contructor */
    private History() {
    }

    /** Get shared instance */
    public static History getInstance() {
        return instance;
    }

    /** get all records in unmodifiable list */
    public List<Record> getRecords() {
        return Collections.unmodifiableList(records);
    }
    
    /** get record at index */
    public Record getRecord(int index) {
        return records.get(index);
    }

    /** add new record */
    public void addRecord(Record record) {
        records.add(record);
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
        logger.finer("New history record added: " + record.toDebugString());
    }
    
    /** add new records */
    public void addRecords(Collection<Record> records) {
        for (Record record : records) {
            this.records.add(record);
        }
        logger.finer(records.size() + " new history records added");
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
    }
    
    /** remove existing record */
    public void removeRecord(Record record) {
        records.remove(record);
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
        logger.finer("A history record removed: " + record.toDebugString());
    }
    
    /** remove existing records */
    public void removeRecords(Collection<Record> records) {
        for (Record record : records) {
            this.records.remove(record);
        }
        logger.finer(records.size() + " history records removed");
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
    }
    
    /** delete all records */
    public void clearRecords() {
        records.clear();
        actionSupport.fireActionPerformed(ACTION_CLEAR_RECORDS, null);
        logger.finer("All history records removed");
    }
    
    /** Find last (as in time) record sent to specified operator.
     * @param operatorName name of the operator
     * @return the last (the most recent) record sent to specified operator.
     *  Null when none found.
     */
    public synchronized Record findLastRecord(String operatorName) {
        ListIterator<Record> iter = records.listIterator(records.size());
        while (iter.hasPrevious()) {
            Record record = iter.previous();
            if (ObjectUtils.equals(record.getOperator(), operatorName)) {
                return record;
            }
        }
        return null;
    }
    
    /** Single history record */
    public static class Record {

        private String number; //recipient number
        private String name; //recipient name
        private String text; //message text
        private String senderNumber;
        private String senderName;
        private String operator;
        private Date date;

        /** Create new Record. For detailed parameters restrictions see individual setter methods.
         * @param number not null nor empty
         * @param text not null
         * @param operator not null nor empty
         * @param name
         * @param senderNumber
         * @param senderName
         * @param date null for current time
         */
        public Record(String number, String text, String operator,
                String name, String senderNumber, String senderName, Date date) {
            setName(name);
            setNumber(number);
            setText(text);
            setSenderNumber(senderNumber);
            setSenderName(senderName);
            setOperator(operator);
            setDate(date);
        }

        // <editor-fold defaultstate="collapsed" desc="Get Methods">
        /** Recepient number in international format (starting with "+").
         * Never null nor empty. */
        public String getNumber() {
            return number;
        }

        /** Name of the recepient. Never null. */
        public String getName() {
            return name;
        }

        /** Text of the message. Never null. */
        public String getText() {
            return text;
        }

        /** Sender number. Never null. */
        public String getSenderNumber() {
            return senderNumber;
        }

        /** Sender name. Never null. */
        public String getSenderName() {
            return senderName;
        }

         /** Operator of the message. Never null nor empty. */
        public String getOperator() {
            return operator;
        }

        /** Date of the sending. Never null. */
        public Date getDate() {
            return date;
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Set Methods">
        /** Recepient number in international format (starting with "+").
         * May not be null nor empty. */
        public void setNumber(String number) {
            Validate.notEmpty(number);
            if (!number.startsWith("+")) {
                throw new IllegalArgumentException("Number does not start with '+': " + number);
            }
            this.number = number;
        }

        /** Name of the recepient. Null value is changed to empty string. */
        public void setName(String name) {
            this.name = StringUtils.defaultString(name);
        }


        /** Text of the message. May not be null. */
        public void setText(String text) {
            Validate.notNull(text);
            this.text = text;
        }

        /** Sender number. Null value is changed to empty string. */
        public void setSenderNumber(String senderNumber) {
            this.senderNumber = StringUtils.defaultString(senderNumber);
        }

        /** Sender name. Null value is changed to empty string. */
        public void setSenderName(String senderName) {
            this.senderName = StringUtils.defaultString(senderName);
        }

        /** Operator of the message. May not be null nor empty. */
        public void setOperator(String operator) {
            Validate.notEmpty(operator);
            this.operator = operator;
        }

        /** Date of the sending. Null value is inicialized with current time. */
        public void setDate(Date date) {
            if (date == null) {
                date = new Date();
            }
            this.date = date;
        }
        // </editor-fold>

        public String toDebugString() {
            return "[name=" + name + ", number=" + LogUtils.anonymizeNumber(number) +
                    ", operator=" + operator + "]";
         }
    }
}
