/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.data.event.ActionEventSupport;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;
import java.util.List;
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

    /** shared instance */
    private static final Log instance = new Log();
    private static final Logger logger = Logger.getLogger(Log.class.getName());
    private List<Record> records = Collections.synchronizedList(new ArrayList<Record>());

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** Disabled contructor */
    private Log() {
    }

    /** Get shared instance */
    public static Log getInstance() {
        return instance;
    }

    /** get all records in unmodifiable list */
    public synchronized List<Record> getRecords() {
        return Collections.unmodifiableList(records);
    }

    /** add new record */
    public synchronized void addRecord(Record record) {
        records.add(record);
        actionSupport.fireActionPerformed(ACTION_ADD_RECORD, null);
        logger.finer("A new log record added: " + record);
    }
    
    /** remove existing record */
    public synchronized void removeRecord(Record record) {
        records.remove(record);
        actionSupport.fireActionPerformed(ACTION_REMOVE_RECORD, null);
        logger.finer("A log record removed: " + record);
    }
    
    /** delete all records */
    public synchronized void clearRecords() {
        records.clear();
        actionSupport.fireActionPerformed(ACTION_CLEAR_RECORDS, null);
        logger.finer("All log records removed");
    }

    /** Return number of records
     * @return See {@link Collection#size}
     */
    public synchronized int size() {
        return records.size();
    }

    /** Return if there are no records
     * @return See {@link Collection#isEmpty}
     */
    public synchronized boolean isEmpty() {
        return records.isEmpty();
    }

    /** Get lastly added record
     * @return last record or null if log empty
     */
    public synchronized Record getLastRecord() {
        if (records.size() > 0) {
            return records.get(records.size() - 1);
        } else {
            return null;
        }
    }
    
    /** Single log record
     */
    public static class Record {

        private String message;
        private ImageIcon icon;
        private Date time;

        /** Creates new Record with current time and no icon
         * 
         * @param message message of the record, not null
         */
        public Record(String message) {
            this(message, null, null);
        }

        /** Creates new Record
         * 
         * @param message message of the record, not null
         * @param time time when event happened. You can use null for current time.
         * @param icon optional icon to display or null
         */
        public Record(String message, Date time, ImageIcon icon) {
            if (message == null) {
                throw new IllegalArgumentException("message");
            }
            this.message = message;
            this.time = (time != null ? time : new Date());
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
