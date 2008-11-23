/*
 * SMSSender.java
 *
 * Created on 6. červenec 2007, 17:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.transfer;

import esmska.data.Config;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import esmska.data.Icons;
import esmska.data.Keyring;
import esmska.data.SMS;
import esmska.gui.MainFrame;
import esmska.gui.QueuePanel;
import esmska.operators.OperatorInterpreter;
import esmska.operators.OperatorUtil;
import esmska.operators.OperatorVariable;
import esmska.persistence.PersistenceManager;
import esmska.utils.L10N;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;

/** Sender of SMS
 *
 * @author ripper
 */
public class SMSSender {
    private static final Logger logger = Logger.getLogger(SMSSender.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Keyring keyring = PersistenceManager.getKeyring();
    private static final Config config = PersistenceManager.getConfig();
    private static final String NO_REASON_ERROR = l10n.getString("SMSSender.NO_REASON_ERROR");
    
    private MainFrame mainFrame = MainFrame.getInstance(); //reference to main form
    //map of <operator,worker>; it show's whether some operator has currently assigned
    //a background worker (therefore is sending at the moment)
    private HashMap<String,SMSWorker> workers = new HashMap<String, SMSWorker>();

    /** Creates a new instance of SMSSender */
    public SMSSender() {
        mainFrame.getQueuePanel().addActionListener(new QueueListener());
    }
    
    /** Return whether there is currently some message being sent.
     * @return true is some message is just being sent; false otherwise
     */
    public boolean isRunning() {
        return !workers.isEmpty();
    }
    
    /** Send new ready SMS */
    private void sendNew() {
        Set<SMS> readySMS = mainFrame.getQueuePanel().getReadySMS();

        for (SMS sms : readySMS) {
            String operator = sms.getOperator();
            if (workers.containsKey(operator)) {
                //there's already some message from this operator being sent,
                //skip this message
                continue;
            }
            
            logger.fine("Sending new SMS: " + sms);
            mainFrame.getStatusPanel().setTaskRunning(true);
            mainFrame.getStatusPanel().setStatusMessage(
                    MessageFormat.format(l10n.getString("SMSSender.sending_message"),
                    sms, (operator == null ? l10n.getString("SMSSender.no_operator") : operator)),
                    true, Icons.STATUS_INFO, true);
            mainFrame.getQueuePanel().markSMSSending(sms);
            
            SMSWorker worker = new SMSWorker(sms);
            workers.put(operator, worker);
            
            //send in worker thread
            worker.execute();
        }
    }
    
    /** Handle processed SMS */
    private void finishedSending(SMS sms) {
        logger.fine("Finished sending SMS: " + sms);
        workers.remove(sms.getOperator());
        mainFrame.smsProcessed(sms);
        //look for another sms to send
        sendNew();
    }
    
    /** send sms over internet */
    private class SMSWorker extends SwingWorker<Void, Void> {
        private SMS sms;
        
        public SMSWorker(SMS sms) {
            super();
            this.sms = sms;
        }
        
        @Override
        protected void done() {
            finishedSending(sms);
        }
        
        @Override
        protected Void doInBackground() {
            boolean success = false;
            try {
                OperatorInterpreter interpreter = new OperatorInterpreter();
                success = interpreter.sendMessage(OperatorUtil.getOperator(sms.getOperator()),
                        extractVariables(sms));
                sms.setOperatorMsg(interpreter.getOperatorMessage());
                if (!success) {
                    sms.setErrMsg(interpreter.getErrorMessage() != null ?
                        interpreter.getErrorMessage() : NO_REASON_ERROR);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while sending sms", ex);
            } finally {
                sms.setStatus(success ? SMS.Status.SENT_OK : SMS.Status.PROBLEMATIC);
            }
            
            return null;
        }
    }
    
    /** Extract variables from SMS to a map */
    private static HashMap<OperatorVariable,String> extractVariables(SMS sms) {
        HashMap<OperatorVariable,String> map = new HashMap<OperatorVariable, String>();
        map.put(OperatorVariable.NUMBER, sms.getNumber());
        map.put(OperatorVariable.MESSAGE, sms.getText());
        map.put(OperatorVariable.SENDERNAME, sms.getSenderName());
        map.put(OperatorVariable.SENDERNUMBER, sms.getSenderNumber());
        
        String[] key = keyring.getKey(sms.getOperator());
        if (key != null) {
            map.put(OperatorVariable.LOGIN, key[0]);
            map.put(OperatorVariable.PASSWORD, key[1]);
        }
        
        if (config.isDemandDeliveryReport()) {
            map.put(OperatorVariable.DELIVERY_REPORT, "true");
        }
        
        return map;
    }
    
    /** Listen for changes in the sms queue */
    private class QueueListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                //on new sms ready try to send it
                case QueuePanel.ACTION_NEW_SMS_READY:
                    sendNew();
                    break;
            }
        }
    }
}
