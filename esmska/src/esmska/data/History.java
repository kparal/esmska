/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.utils.ActionEventSupport;
import esmska.utils.LogUtils;
import esmska.utils.Nullator;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

/** SMS history entity
 *
 * @author ripper
 */
public class History {
    
    /** new record added */
    public static final int ACTION_ADD_RECORD = 0;
    /** existing record removed */
    public static final int ACTION_REMOVE_RECORD = 1;
    /** all records removed */
    public static final int ACTION_CLEAR_RECORDS = 2;

    private static final Logger logger = Logger.getLogger(History.class.getName());
    private ArrayList<Record> records = new ArrayList<Record>();
    
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
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
    public Record findLastRecord(String operatorName) {
        ListIterator<Record> iter = records.listIterator(records.size());
        while (iter.hasPrevious()) {
            Record record = iter.previous();
            if (Nullator.isEqual(record.getOperator(), operatorName)) {
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

        // <editor-fold defaultstate="collapsed" desc="Get Methods">
        public String getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }

        public String getSenderNumber() {
            return senderNumber;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getOperator() {
            return operator;
        }

        public Date getDate() {
            return date;
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="Set Methods">
        public void setNumber(String number) {
            this.number = number;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public void setSenderNumber(String senderNumber) {
            this.senderNumber = senderNumber;
        }
        
        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }
        
        public void setOperator(String operator) {
            this.operator = operator;
        }
        
        public void setDate(Date date) {
            this.date = date;
        }
        // </editor-fold>

        public String toDebugString() {
            return "[name=" + name + ", number=" + LogUtils.anonymizeNumber(number) +
                    ", operator=" + operator + "]";
         }
    }
}
