package esmska.transfer;

import esmska.data.Links;
import esmska.data.Queue.Events;
import esmska.data.event.ValuedEvent;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import esmska.data.Queue;
import esmska.data.SMS;
import esmska.utils.L10N;
import esmska.data.event.ValuedListener;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sender of SMS
 *
 * @author ripper
 */
public class SMSSender {
    private static final Logger logger = Logger.getLogger(SMSSender.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Queue queue = Queue.getInstance();
    private static final String NO_REASON_ERROR = l10n.getString("SMSSender.NO_REASON_ERROR");
    private static final String SENDING_CRASHED_ERROR = 
            MessageFormat.format(l10n.getString("SMSSender.SENDING_CRASHED_ERROR"), Links.ISSUES);
    
    /** map of <gateway,worker>; it shows whether some gateway has currently assigned
    a background worker (therefore is sending at the moment) */
    private HashMap<String,SMSWorker> workers = new HashMap<String, SMSWorker>();

    /** Custom executor for executing SwingWorkers. Needed because of bug in Java 6u18:
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6880336
     * http://forums.sun.com/thread.jspa?threadID=5424356
     */
    private static final ExecutorService executor = Executors.newCachedThreadPool();

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
     * @param gatewayName gateway for which to look for new ready sms;
     * use null for any gateway
     */
    private void sendNew(String gatewayName) {
        if (queue.isPaused()) {
            //don't send anything while queue is paused
            return;
        }
        List<SMS> readySMS = queue.getAllWithStatus(SMS.Status.READY, gatewayName);

        for (SMS sms : readySMS) {
            String gateway = sms.getGateway();
            if (workers.containsKey(gateway)) {
                //there's already some message from this gateway being sent,
                //skip this message
                continue;
            }
            
            logger.log(Level.FINE, "Sending new SMS: {0}", sms.toDebugString());
            queue.setSMSSending(sms);
            
            SMSWorker worker = new SMSWorker(sms);
            workers.put(gateway, worker);
            
            //send in worker thread
            executor.execute(worker);
        }
    }
    
    /** Handle processed SMS */
    private void finishedSending(SMS sms, boolean success) {
        logger.log(Level.FINE, "Finished sending SMS: {0}", sms.toDebugString());
        workers.remove(sms.getGateway());
        if (success) {
            queue.setSMSSent(sms);
        } else {
            queue.setSMSFailed(sms);
        }
        //look for another sms to send
        sendNew(sms.getGateway());
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
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Sending of SMS crashed", t);
                sms.setErrMsg(SENDING_CRASHED_ERROR);
            }
            finishedSending(sms, success);
        }
        @Override
        protected Boolean doInBackground() {
            boolean success = false;
            try {
                GatewayInterpreter interpreter = new GatewayInterpreter();
                success = interpreter.sendMessage(sms);
                sms.setGatewayMsg(interpreter.getGatewayMessage());
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
