/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.utils.ActionEventSupport;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/** SMS history entity
 *
 * @author ripper
 */
public class History {
    
     /** new record added */
    public static final int ACTION_ADD_RECORD = 0;
    /** existing record removed */
    public static final int ACTION_REMOVE_RECORD = 1;
    /** all records deleted */
    public static final int ACTION_CLEAR_RECORDS = 2;
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
    
    /** get all records */
    public ArrayList<Record> getRecords() {
        return records;
    }
    
    /** get record at index */
    public Record getRecord(int index) {
        return records.get(index);
    }

    /** add new record */
    public void addRecord(Record record) {
        records.add(record);
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
    }
    
    /** add new records */
    public void addRecords(Collection<Record> records) {
        for (Record record : records)
            this.records.add(record);
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
    }
    
    /** remove existing record */
    public void removeRecord(Record record) {
        records.remove(record);
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
    }
    
    /** remove existing records */
    public void removeRecords(Collection<Record> records) {
        for (Record record : records)
            this.records.remove(record);
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
    }
    
    /** delete all records */
    public void clearRecords() {
        records.clear();
        actionSupport.fireActionPerformed(ACTION_CLEAR_RECORDS, null);
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

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSenderNumber() {
            return senderNumber;
        }

        public void setSenderNumber(String senderNumber) {
            this.senderNumber = senderNumber;
        }

        public String getSenderName() {
            return senderName;
        }

        public void setSenderName(String senderName) {
            this.senderName = senderName;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
