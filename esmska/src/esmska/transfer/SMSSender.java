/*
 * SMSSender.java
 *
 * Created on 6. červenec 2007, 17:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.transfer;

import esmska.gui.MainFrame;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import esmska.operators.Operator;
import esmska.data.SMS;
import esmska.gui.StatusPanel;

/** Sender of SMS
 *
 * @author ripper
 */
public class SMSSender {
    private List<SMS> smsQueue;
    private boolean running; // sending sms in this moment
    private boolean paused; // queue paused
    private boolean delayed; //waiting for delay to send another sms
    private SMSWorker smsWorker; //worker for background thread
    private MainFrame mainFrame; //reference to main form

    /** Creates a new instance of SMSSender */
    public SMSSender(List<SMS> smsQueue) {
        if (smsQueue == null)
            throw new NullPointerException("smsQueue");
        this.smsQueue = smsQueue;
        this.mainFrame = MainFrame.getInstance();
    }
    
    /** notify about new sms */
    public void announceNewSMS() {
        prepareSending();
    }
    
    private void prepareSending() {
        if (!isDelayed() && !isPaused() && !running && !smsQueue.isEmpty()) {
            running = true;
            SMS sms = smsQueue.get(0);
            mainFrame.setTaskRunning(true);
            mainFrame.printStatusMessage("Posílám zprávu pro " + sms
            + " (" + sms.getOperator() + ") ...", true, StatusPanel.ICON_INFO);
            
            //send in worker thread
            smsWorker = new SMSWorker(sms);
            smsWorker.execute();
        }
    }
    
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
        
        protected Void doInBackground() throws Exception {
            Operator operator = sms.getOperator();
            sms.setImage(operator.getSecurityImage());
            
            //have the user resolve the code from the image
            if (sms.getImage() != null) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JPanel panel = new JPanel();
                        JLabel label = new JLabel("Opište kód z obrázku:",
                                sms.getImage(), JLabel.CENTER);
                        label.setHorizontalTextPosition(JLabel.CENTER);
                        label.setVerticalTextPosition(JLabel.TOP);
                        panel.add(label);
                        String imageCode = JOptionPane.showInputDialog(mainFrame, panel, "Kontrolní kód",
                                JOptionPane.QUESTION_MESSAGE);
                        sms.setImageCode(imageCode);
                    }
                });
            }
            
            //send sms
            boolean success = operator.send(sms);
            sms.setStatus(success?SMS.Status.SENT_OK:SMS.Status.PROBLEMATIC);
            
            return null;
        }
        
    }
    
    /** Whether queue is paused */
    public boolean isPaused() {
        return paused;
    }
    
    /** Pause/unpause queue */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused == false)
            prepareSending();
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
