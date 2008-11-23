/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.utils.ActionEventSupport;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/** Class for collecting log messages
 *
 * @author ripper
 */
public class Log {
    /** new record added */
    public static final int ACTION_ADD_RECORD = 0;
    /** existing record removed */
    public static final int ACTION_REMOVE_RECORD = 1;
    /** all records deleted */
    public static final int ACTION_CLEAR_RECORDS = 2;

    private static final Logger logger = Logger.getLogger(Log.class.getName());
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

    /** add new record */
    public void addRecord(Record record) {
        records.add(record);
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
        logger.finer("A new log record added: " + record);
    }
    
    /** remove existing record */
    public void removeRecord(Record record) {
        records.remove(record);
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
        logger.finer("A log record removed: " + record);
    }
    
    /** delete all records */
    public void clearRecords() {
        records.clear();
        actionSupport.fireActionPerformed(ACTION_CLEAR_RECORDS, null);
        logger.finer("All log records removed");
    }
    
    /** Single log record
     */
    public static class Record {

        private String message;
        private ImageIcon icon;
        private Date time;

        /** Creates new Record
         * 
         * @param message message of the record
         * @param time time when event happened
         * @param icon optional icon to display or null
         */
        public Record(String message, Date time, ImageIcon icon) {
            this.message = message;
            this.time = time;
            this.icon = icon;
        }
        
        // <editor-fold defaultstate="collapsed" desc="Get Methods">
        public ImageIcon getIcon() {
            return icon;
        }

        public String getMessage() {
            return message;
        }

        public Date getTime() {
            return time;
        }
        // </editor-fold>

        @Override
        public String toString() {
            return "[time=" + time + "]";
        }
    }
}
