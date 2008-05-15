/*
 * SMSSender.java
 *
 * Created on 6. červenec 2007, 17:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.transfer;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import esmska.data.Icons;
import esmska.data.Keyring;
import esmska.data.SMS;
import esmska.gui.MainFrame;
import esmska.operators.OperatorInterpreter;
import esmska.operators.OperatorUtil;
import esmska.operators.OperatorVariable;
import esmska.persistence.PersistenceManager;
import esmska.utils.Nullator;

/** Sender of SMS
 *
 * @author ripper
 */
public class SMSSender {
    private static final Logger logger = Logger.getLogger(SMSSender.class.getName());
    private static final Keyring keyring = PersistenceManager.getKeyring();   
    private static final String NO_REASON_ERROR = "Autor skriptu pro daného operátora neposkytl<br>" +
            "žádné další informace o příčině selhání.";
    
    private List<SMS> smsQueue;
    private boolean running; // sending sms in this moment
    private boolean paused; // queue paused
    private boolean delayed; //waiting for delay to send another sms
    private SMSWorker smsWorker; //worker for background thread
    private MainFrame mainFrame; //reference to main form

    /** Creates a new instance of SMSSender */
    public SMSSender(List<SMS> smsQueue) {
        if (smsQueue == null) {
            throw new NullPointerException("smsQueue");
        }
        this.smsQueue = smsQueue;
        this.mainFrame = MainFrame.getInstance();
    }
    
    /** notify about new sms */
    public void announceNewSMS() {
        prepareSending();
    }
    
    /** Make arrangements needed for sending SMS */
    private void prepareSending() {
        if (!isDelayed() && !isPaused() && !running && !smsQueue.isEmpty()) {
            running = true;
            SMS sms = smsQueue.get(0);
            mainFrame.getStatusPanel().setTaskRunning(true);
            String operator = Nullator.isEmpty(sms.getOperator()) ? 
                "žádný operátor" : sms.getOperator();
            mainFrame. getStatusPanel().setStatusMessage("Posílám zprávu pro " + sms
            + " (" + operator + ") ...", true, Icons.STATUS_INFO, true);
            
            //send in worker thread
            smsWorker = new SMSWorker(sms);
            smsWorker.execute();
        }
    }
    
    /** Handle processed SMS */
    private void finishedSending(SMS sms) {
        mainFrame.smsProcessed(sms);
        running = false;
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
        
        return map;
    }
    
    /** Whether queue is paused */
    public boolean isPaused() {
        return paused;
    }
    
    /** Pause/unpause queue */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            prepareSending();
        }
    }
    
    /** Whether queue is delayed */
    public boolean isDelayed() {
        return delayed;
    }
    
    /** Delay/undelay queue */
    public void setDelayed(boolean delayed) {
        this.delayed = delayed;
    }
}
