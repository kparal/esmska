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
import esmska.data.Queue.Events;
import esmska.data.event.ValuedEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import esmska.data.Keyring;
import esmska.data.Operators;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.operators.OperatorInterpreter;
import esmska.operators.OperatorVariable;
import esmska.utils.L10N;
import esmska.utils.Tuple;
import esmska.data.event.ValuedListener;
import java.util.List;
import java.util.ResourceBundle;

/** Sender of SMS
 *
 * @author ripper
 */
public class SMSSender {
    private static final Logger logger = Logger.getLogger(SMSSender.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Keyring keyring = Keyring.getInstance();
    private static final Config config = Config.getInstance();
    private static final Queue queue = Queue.getInstance();
    private static final String NO_REASON_ERROR = l10n.getString("SMSSender.NO_REASON_ERROR");
    
    /** map of <operator,worker>; it shows whether some operator has currently assigned
    a background worker (therefore is sending at the moment) */
    private HashMap<String,SMSWorker> workers = new HashMap<String, SMSWorker>();

    /** Creates a new instance of SMSSender */
    public SMSSender() {
        queue.addValuedListener(new QueueListener());
    }
    
    /** Return whether there is currently some message being sent.
     * @return true is some message is just being sent; false otherwise
     */
    public boolean isRunning() {
        return !workers.isEmpty();
    }
    
    /** Send new ready SMS
     * @param operatorName operator for which to look for new ready sms;
     * use null for any operator
     */
    private void sendNew(String operatorName) {
        if (queue.isPaused()) {
            //don't send anything while queue is paused
            return;
        }
        List<SMS> readySMS = queue.getAllWithStatus(SMS.Status.READY, operatorName);

        for (SMS sms : readySMS) {
            String operator = sms.getOperator();
            if (workers.containsKey(operator)) {
                //there's already some message from this operator being sent,
                //skip this message
                continue;
            }
            
            logger.fine("Sending new SMS: " + sms);
            queue.setSMSSending(sms);
            
            SMSWorker worker = new SMSWorker(sms);
            workers.put(operator, worker);
            
            //send in worker thread
            worker.execute();
        }
    }
    
    /** Handle processed SMS */
    private void finishedSending(SMS sms, boolean success) {
        logger.fine("Finished sending SMS: " + sms);
        workers.remove(sms.getOperator());
        if (success) {
            queue.setSMSSent(sms);
        } else {
            queue.setSMSFailed(sms);
        }
        //look for another sms to send
        sendNew(sms.getOperator());
    }
    
    /** send sms over internet */
    private class SMSWorker extends SwingWorker<Boolean, Void> {
        private SMS sms;
        
        public SMSWorker(SMS sms) {
            super();
            this.sms = sms;
        }
        @Override
        protected void done() {
            boolean success = false;
            try {
                success = get();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Can't get result of sending sms", ex);
            }
            finishedSending(sms, success);
        }
        @Override
        protected Boolean doInBackground() {
            boolean success = false;
            try {
                OperatorInterpreter interpreter = new OperatorInterpreter();
                success = interpreter.sendMessage(Operators.getOperator(sms.getOperator()),
                        extractVariables(sms));
                sms.setOperatorMsg(interpreter.getOperatorMessage());
                sms.setErrMsg(null);
                if (!success) {
                    sms.setErrMsg(interpreter.getErrorMessage() != null ?
                        interpreter.getErrorMessage() : NO_REASON_ERROR);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while sending sms", ex);
                success = false;
            }
            return success;
        }
    }
    
    /** Extract variables from SMS to a map */
    private static HashMap<OperatorVariable,String> extractVariables(SMS sms) {
        HashMap<OperatorVariable,String> map = new HashMap<OperatorVariable, String>();
        map.put(OperatorVariable.NUMBER, sms.getNumber());
        map.put(OperatorVariable.MESSAGE, sms.getText());
        map.put(OperatorVariable.SENDERNAME, sms.getSenderName());
        map.put(OperatorVariable.SENDERNUMBER, sms.getSenderNumber());
        
        Tuple<String, String> key = keyring.getKey(sms.getOperator());
        if (key != null) {
            map.put(OperatorVariable.LOGIN, key.get1());
            map.put(OperatorVariable.PASSWORD, key.get2());
        }
        
        if (config.isDemandDeliveryReport()) {
            map.put(OperatorVariable.DELIVERY_REPORT, "true");
        }
        
        return map;
    }
    
    /** Listen for changes in the sms queue */
    private class QueueListener implements ValuedListener<Queue.Events, SMS> {
        @Override
        public void eventOccured(ValuedEvent<Events, SMS> e) {
            switch (e.getEvent()) {
                //on new sms ready try to send it
                case NEW_SMS_READY:
                case QUEUE_RESUMED:
                    sendNew(null);
                    break;
            }
        }
    }
}
