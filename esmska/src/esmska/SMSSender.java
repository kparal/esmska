/*
 * SMSSender.java
 *
 * Created on 6. červenec 2007, 17:55
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import operators.Operator;
import persistence.SMS;

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
    private Main parent; //reference to main form
    
    /** Creates a new instance of SMSSender */
    public SMSSender(List<SMS> smsQueue, Main parent) {
        if (smsQueue == null)
            throw new NullPointerException("smsQueue");
        this.smsQueue = smsQueue;
        this.parent = parent;
    }
    
    /** notify about new sms */
    public void announceNewSMS() {
        prepareSending();
    }
    
    private void prepareSending() {
        if (!isDelayed() && !isPaused() && !running && !smsQueue.isEmpty()) {
            running = true;
            SMS sms = smsQueue.get(0);
            parent.setTaskRunning(true);
            parent.printStatusMessage("Posílám zprávu pro " + sms
            + " (" + sms.getOperator() + ") ...");
            
            //send in worker thread
            smsWorker = new SMSWorker(sms);
            smsWorker.execute();
        }
    }
    
    private void finishedSending(SMS sms) {
        if (sms.getStatus() == SMS.Status.SENT_OK) {
            parent.printStatusMessage("Zpráva pro " + sms
            + " byla úspěšně odeslána.");
            parent.setSMSDelay();
        }
        if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
            parent.printStatusMessage("Zprávu pro " + sms
            + " se nepodařilo odeslat!");
            parent.pauseSMSQueue();
            
            JOptionPane.showMessageDialog(parent, new JLabel("<html>"
                    + "<h2>Zprávu se nepovedlo odeslat!</h2>Důvod: " + sms.getErrMsg()
                    + "</html>"), "Chyba při odesílání", JOptionPane.WARNING_MESSAGE);
        }
        parent.smsProcessed(sms);
        parent.setTaskRunning(false);
        running = false;
    }
    
    /** send sms over internet */
    private class SMSWorker extends SwingWorker<Void, Void> {
        private SMS sms;
        
        public SMSWorker(SMS sms) {
            super();
            this.sms = sms;
        }
        
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
                        Image image = Toolkit.getDefaultToolkit().getImage(sms.getImage());
                        image.flush();
                        JLabel label = new JLabel("Opište kód z obrázku:",
                                new ImageIcon(image), JLabel.CENTER);
                        label.setHorizontalTextPosition(JLabel.CENTER);
                        label.setVerticalTextPosition(JLabel.TOP);
                        panel.add(label);
                        String imageCode = JOptionPane.showInputDialog(parent, panel, "Kontrolní kód",
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
