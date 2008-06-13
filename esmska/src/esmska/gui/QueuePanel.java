/*
 * QueuePanel.java
 *
 * Created on 3. říjen 2007, 22:05
 */

package esmska.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jvnet.substance.SubstanceLookAndFeel;

import esmska.data.Config;
import esmska.data.History;
import esmska.data.Icons;
import esmska.data.SMS;
import esmska.operators.Operator;
import esmska.operators.OperatorUtil;
import esmska.persistence.PersistenceManager;
import esmska.utils.ActionEventSupport;
import esmska.integration.IntegrationAdapter;
import esmska.utils.Nullator;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** SMS queue panel
 *
 * @author  ripper
 */
public class QueuePanel extends javax.swing.JPanel {
    /** A message was requested to be edited */
    public static final int ACTION_REQUEST_EDIT_SMS = 0;
    /** Queue was paused or unpaused */
    public static final int ACTION_QUEUE_PAUSE_CHANGED = 1;
    /** A new message is ready to be sent*/
    public static final int ACTION_NEW_SMS_READY = 2;
    
    private static final String RES = "/esmska/resources/";
    private static final List<SMS> smsQueue = PersistenceManager.getQueue();
    private static final Config config = PersistenceManager.getConfig();
    private static final History history = PersistenceManager.getHistory();
    
    private SMSQueuePauseAction smsQueuePauseAction = new SMSQueuePauseAction();
    private Action deleteSMSAction = new DeleteSMSAction();
    private Action editSMSAction = new EditSMSAction();
    private Action smsUpAction = new SMSUpAction();
    private Action smsDownAction = new SMSDownAction();
    
    private QueueListModel queueListModel = new QueueListModel();
    //every second check the queue
    private Timer timer = new Timer(1000, new DelayListener());
    //map of <operator name, current delay in seconds>
    private HashMap<String, Integer> operatorDelay = new HashMap<String, Integer>();
    //map of <sms, current delay in seconds> - the delay can be different for sms of same operator
    private HashMap<SMS, Integer> smsDelay = new HashMap<SMS, Integer>();
    //collection of sms ready and waiting to be sent
    private LinkedHashSet<SMS> readySMS = new LinkedHashSet<SMS>();
    //collection of sms currently being sent
    private HashSet<SMS> sendingSMS = new HashSet<SMS>();
    //sms which have been requested to be edited
    private SMS editRequestedSMS;
    private QueuePopupMenu popup = new QueuePopupMenu();
    private QueueMouseListener mouseListener;
    
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Creates new form QueuePanel */
    public QueuePanel() {
        initComponents();
        
        //add mouse listeners to the queue list
        mouseListener = new QueueMouseListener(queueList, popup);
        queueList.addMouseListener(mouseListener);
        queueList.addMouseWheelListener(mouseListener);

        //if there are some messages in queue, handle all needed routines
        if (!smsQueue.isEmpty()) {
            handleTimerStatus();
            findNewReadySMS();
        }
        
        //update integration sms count
        IntegrationAdapter.getInstance().setSMSCount(smsQueue.size());
    }
    
    /** Get SMS which was requested to be edited */
    public SMS getEditRequestedSMS() {
        return editRequestedSMS;
    }
    
    /** get action used to pause/unpause the sms queue */
    public Action getSMSQueuePauseAction() {
        return smsQueuePauseAction;
    }
    
    /** Whether queue is currently paused */
    public boolean isPaused() {
        return smsQueuePauseAction.isPaused();
    }
    
    /** Sets whether queue is currently paused */
    public void setPaused(boolean paused) {
        smsQueuePauseAction.setPaused(paused);
    }
    
    /** Updates status of selected SMS */
    public void smsProcessed(SMS sms) {
        sendingSMS.remove(sms);
        
        if (sms.getStatus() == SMS.Status.SENT_OK) {
            queueListModel.remove(sms);
        }
        if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
            int index = queueListModel.indexOf(sms);
            queueListModel.fireContentsChanged(
                    queueListModel, index, index);
        }
        
        //update integration sms count
        IntegrationAdapter.getInstance().setSMSCount(smsQueue.size());
        
        findNewReadySMS();
    }
    
    /** Add new SMS to the queue. The SMS is added after the last SMS of the
     * same type of operator.
     */
    public void addSMS(SMS sms) {
        queueListModel.add(sms);
        IntegrationAdapter.getInstance().setSMSCount(smsQueue.size());
        findNewReadySMS();
    }
    
    /** Get a collection of messages which are ready and waiting to be sent
     * @return a collection of messages which are ready and waiting to be sent.
     *  May be empty.
     */
    public Set<SMS> getReadySMS() {
        if (isPaused()) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(readySMS);
        }
    }
    
    /** Mark SMS as currently being sent. This SMS will be distinguished in the queue.
     * @param sms SMS that is currently being sent
     */
    public void markSMSSending(SMS sms) {
        sendingSMS.add(sms);
        int index = queueListModel.indexOf(sms);
        if (index >= 0) {
            queueListModel.fireContentsChanged(
                    queueListModel, index, index);
        }
    }
    
    /** Return current delay for specified operator and set the value in operator delays map.
     * @param operatorName name of the operator
     * @return number of seconds a message from the operator must wait.
     *  If no operator found, return 0.
     */
    private int findCurrentDelay(String operatorName) {
        if (operatorDelay.containsKey(operatorName)) {
            return operatorDelay.get(operatorName);
        }
        
        Operator operator = OperatorUtil.getOperator(operatorName);
        int delay = 0;
        
        if (operator == null) { //unknown operator
            delay = 0;
        } else if (operator.getDelayBetweenMessages() <= 0) { //operator without delay
            delay = 0;
        } else { //search in history
            History.Record record = history.findLastRecord(operatorName);
            if (record == null) { //no previous record
                delay = 0;
            } else { //compute the delay
                //FIXME: does not take various daylight saving time etc into account
                //A more complex library (eg. Joda Time) is needed to calculate true time differences.
                long difference = (new Date().getTime() - record.getDate().getTime()) / 1000; //in seconds
                delay = (int) Math.max(operator.getDelayBetweenMessages() - difference, 0);
                if (timer.isRunning() && delay > 0) {
                    //delay +1 bcz the timer is currently running, 
                    //it's not exactly on a precise second's boundary
                    delay++;
                }
            }
        }

        operatorDelay.put(operatorName, delay);
        return delay;
    }
    
    /** Return current delay for specified sms and set the value in sms delays map.
     * The delay is taking into account all previous messages from the same operator
     * which are waiting to be sent.
     * @param sms sms in queue
     * @return number of seconds a message must wait
     * @throws IllegalArgumentException when sms is null or no such sms is found
     *  in the queue
     */
    private int findCurrentDelay(SMS sms) {
        if (sms == null) {
            throw new IllegalArgumentException("sms can't be null");
        }
        if (smsDelay.containsKey(sms)) {
            return smsDelay.get(sms);
        }
        Operator operator = OperatorUtil.getOperator(sms.getOperator());
        if (operator == null || operator.getDelayBetweenMessages() <= 0) {
            smsDelay.put(sms, 0);
            return 0;
        }
        
        int index = smsQueue.indexOf(sms);
        if (index < 0) {
            throw new IllegalArgumentException("sms not present in sms queue");
        }
        ListIterator<SMS> iter = smsQueue.listIterator(index);
        while (iter.hasPrevious()) {
            SMS message = iter.previous();
            if (Nullator.isEqual(sms.getOperator(), message.getOperator())) {
                int delay = findCurrentDelay(message);
                delay += operator.getDelayBetweenMessages();
                smsDelay.put(sms, delay);
                return delay;
            }
        }
        
        int delay = findCurrentDelay(sms.getOperator());
        smsDelay.put(sms, delay);
        return delay;
    }
    
    /** Convert integer message delay to more human readable string delay.
     * @param delay number of seconds of the delay
     * @return human readable string of the delay, eg: "3h 15m 47s"
     */
    private String convertDelayToHumanString(int delay) {
        int seconds = delay % 60;
        int minutes = (delay / 60) % 60;
        int hours = delay / 3600;
        
        StringBuilder builder = new StringBuilder();
        builder.append(seconds);
        builder.append("s");
        if (minutes > 0) {
            builder.insert(0, "m ");
            builder.insert(0, minutes);
        }
        if (hours > 0) {
            builder.insert(0, "h ");
            builder.insert(0, hours);
        }
        
        return builder.toString();
    }
    
    /** Update timer status according to current queue status. This method stops
     the timer when possible to increase program performance or restarts the timer
     when it's needed. When timer is stopped, the delays map are also cleared. */
    private void handleTimerStatus() {
        if (smsQueue.isEmpty()) {
            timer.stop();
            operatorDelay.clear();
        } else {
            timer.start();
        }
    }
    
    /** Search through the sms in queue, find those which are ready to be sent,
     add them to the collection of ready sms and inform listeners. */
    private void findNewReadySMS() {
        int waitingBefore = readySMS.size();
        
        //traverse sms queue and look for new ready sms
        for (SMS sms : smsQueue) {
            int delay = findCurrentDelay(sms);
            if (delay <= 0) {
                readySMS.add(sms);
            }
        }
        
        //if found some new ready sms and queue is not paused, inform the listeners
        if (readySMS.size() > waitingBefore && !isPaused()) {
            actionSupport.fireActionPerformed(ACTION_NEW_SMS_READY, null);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        smsUpButton = new javax.swing.JButton();
        smsDownButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        queueList = new javax.swing.JList();
        editButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        pauseButton = new javax.swing.JToggleButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Fronta"));
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        smsUpButton.setAction(smsUpAction);
        smsUpButton.setText("");
        smsUpButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        smsUpButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        smsDownButton.setAction(smsDownAction);
        smsDownButton.setText("");
        smsDownButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        smsDownButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        queueList.setModel(queueListModel);
        queueList.setCellRenderer(new SMSQueueListRenderer());
        queueList.setVisibleRowCount(4);
        queueList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                queueListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(queueList);

        editButton.setAction(editSMSAction);
        editButton.setText("");
        editButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        editButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        deleteButton.setAction(deleteSMSAction);
        deleteButton.setText("");
        deleteButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        deleteButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        pauseButton.setAction(smsQueuePauseAction);
        pauseButton.setText("");
        pauseButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        pauseButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(smsDownButton)
                    .addComponent(smsUpButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(deleteButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pauseButton))
                    .addComponent(editButton))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pauseButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(smsUpButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(smsDownButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addComponent(editButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteButton)
                        .addGap(0, 49, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});

    }// </editor-fold>//GEN-END:initComponents
    
    private void queueListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_queueListValueChanged
        //update form components
        if (!evt.getValueIsAdjusting()) {
            int queueSize = queueListModel.getSize();
            int selectedItems = queueList.getSelectedIndices().length;
            deleteSMSAction.setEnabled(queueSize != 0 && selectedItems != 0);
            editSMSAction.setEnabled(queueSize != 0 && selectedItems == 1);
            smsUpAction.setEnabled(queueSize != 0 && selectedItems == 1);
            smsDownAction.setEnabled(queueSize != 0 && selectedItems == 1);
        }
}//GEN-LAST:event_queueListValueChanged

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        pauseButton.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained
    
    /** Erase sms from queue list */
    private class DeleteSMSAction extends AbstractAction {
        public DeleteSMSAction() {
            super("Odstranit zprávy", 
                    new ImageIcon(QueuePanel.class.getResource(RES + "delete-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Odstranit označené zprávy");
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "delete-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Object[] smsArray = queueList.getSelectedValues();
            for (Object o : smsArray) {
                SMS sms = (SMS) o;
                queueListModel.remove(sms);
            }
            IntegrationAdapter.getInstance().setSMSCount(smsQueue.size());
            
            //transfer focus
            if (queueListModel.getSize() > 0) {
                queueList.requestFocusInWindow();
            } else {
                pauseButton.requestFocusInWindow();
            }
        }
    }
    
    /** Edit sms from queue */
    private class EditSMSAction extends AbstractAction {
        public EditSMSAction() {
            super("Upravit zprávu",
                    new ImageIcon(QueuePanel.class.getResource(RES + "edit-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Upravit označenou zprávu");
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "edit-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            SMS sms = (SMS) queueList.getSelectedValue();
            if (sms == null) {
                return;
            }
            
            editRequestedSMS = sms;
            queueListModel.remove(sms);
            
            IntegrationAdapter.getInstance().setSMSCount(smsQueue.size());
            
            //fire event
            actionSupport.fireActionPerformed(ACTION_REQUEST_EDIT_SMS, null);
        }
    }
    
    /** move sms up in sms queue */
    private class SMSUpAction extends AbstractAction {
        public SMSUpAction() {
            super("Přesunout výš",
                    new ImageIcon(QueuePanel.class.getResource(RES + "up-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě výše");
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "up-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int index = queueList.getSelectedIndex();
            if (index <= 0) {
                return;
            }
            synchronized(smsQueue) {
                Collections.swap(smsQueue,index,index-1);
            }
            //update collections
            readySMS.clear();
            smsDelay.clear();
            findNewReadySMS();
            //fire event
            queueListModel.fireContentsChanged(
                    queueListModel, index-1, index);
            queueList.setSelectedIndex(index-1);
            queueList.ensureIndexIsVisible(index-1);
        }
    }
    
    /** move sms down in sms queue */
    private class SMSDownAction extends AbstractAction {
        public SMSDownAction() {
            super("Přesunout níž",
                    new ImageIcon(QueuePanel.class.getResource(RES + "down-16.png")));
            this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě níže");
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "down-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            int index = queueList.getSelectedIndex();
            if (index < 0 || index >= queueListModel.getSize() - 1) {
                return;
            }
            synchronized(smsQueue) {
                Collections.swap(smsQueue,index,index+1);
            }
            //update collections
            readySMS.clear();
            smsDelay.clear();
            findNewReadySMS();
            //fire event
            queueListModel.fireContentsChanged(
                    queueListModel, index, index+1);
            queueList.setSelectedIndex(index+1);
            queueList.ensureIndexIsVisible(index+1);
        }
    }
    
    /** Pause/unpause the sms queue */
    private class SMSQueuePauseAction extends AbstractAction {
        private boolean paused = false;
        private final String nameRunning = "Pozastavit frontu";
        private final String nameStopped = "Spustit frontu";
        private final String descRunning = "Pozastavit odesílání sms ve frontě (Alt+P)";
        private final String descStopped = "Pokračovat v odesílání sms ve frontě (Alt+P)";
        private final ImageIcon pauseIcon = new ImageIcon(QueuePanel.class.getResource(RES + "pause-22.png"));
        private final ImageIcon pauseIconSmall = new ImageIcon(QueuePanel.class.getResource(RES + "pause-16.png"));
        private final ImageIcon startIcon = new ImageIcon(QueuePanel.class.getResource(RES + "start-22.png"));
        private final ImageIcon startIconSmall = new ImageIcon(QueuePanel.class.getResource(RES + "start-16.png"));
        public SMSQueuePauseAction() {
            super();
            putValue(NAME, nameRunning);
            putValue(SHORT_DESCRIPTION, descRunning);
            putValue(SMALL_ICON, pauseIconSmall);
            putValue(LARGE_ICON_KEY, pauseIcon);
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            putValue(SELECTED_KEY, false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (paused) {
                putValue(NAME, nameRunning);
                putValue(SMALL_ICON, pauseIconSmall);
                putValue(LARGE_ICON_KEY, pauseIcon);
                putValue(SHORT_DESCRIPTION, descRunning);
                putValue(SELECTED_KEY, false);
            } else {
                putValue(NAME, nameStopped);
                putValue(SMALL_ICON, startIconSmall);
                putValue(LARGE_ICON_KEY, startIcon);
                putValue(SHORT_DESCRIPTION, descStopped);
                putValue(SELECTED_KEY, true);
            }
            paused = !paused;
            
            //clear text on pause button
            pauseButton.setText(null);
            
            //fire event
            actionSupport.fireActionPerformed(ACTION_QUEUE_PAUSE_CHANGED, null);
            if (!paused && !readySMS.isEmpty()) {
                actionSupport.fireActionPerformed(ACTION_NEW_SMS_READY, null);
            }
        }
        public boolean isPaused() {
            return paused;
        }
        public void setPaused(boolean paused) {
            //set opposite because actionPerformed will revert it
            this.paused = !paused;
            actionPerformed(null);
        }
    }
    
    /** Model for SMSQueueList */
    private class QueueListModel extends AbstractListModel {
        @Override
        public SMS getElementAt(int index) {
            return smsQueue.get(index);
        }
        @Override
        public int getSize() {
            return smsQueue.size();
        }
        public int indexOf(SMS element) {
            return smsQueue.indexOf(element);
        }
        public void add(SMS element) {
            Integer position = null; //where to put the new sms
            //iterate from the end and find last sms of the same operator
            ListIterator<SMS> iter = smsQueue.listIterator(smsQueue.size());
            while (iter.hasPrevious()) {
                SMS sms = iter.previous();
                if (Nullator.isEqual(sms.getOperator(), element.getOperator())) {
                    position = smsQueue.indexOf(sms) + 1; //set the position after this sms
                    break;
                }
            }
            
            boolean added = false;
            //not found any sms of same operator or it was the last sms in the queue
            if (position == null || position >= smsQueue.size()) {
                added = smsQueue.add(element);
            } else { //found position where to put it
                smsQueue.add(position, element);
                added = true; //above method does not provide boolean return value
            }
            if (added) {
                //update queue collections
                smsDelay.clear();
                //update the timer
                handleTimerStatus();
            
                int index = smsQueue.indexOf(element);
                fireIntervalAdded(this, index, index);
            }
        }
        public boolean contains(SMS element) {
            return smsQueue.contains(element);
        }
        public boolean remove(SMS element) {
            int index = smsQueue.indexOf(element);
            boolean removed = smsQueue.remove(element);
            if (removed) {
                //update queue collections
                readySMS.remove(element);
                sendingSMS.remove(element);
                operatorDelay.remove(element.getOperator()); //TODO: remove only when it's the last message of that operator //edit: really?
                smsDelay.clear();
                handleTimerStatus();
                findNewReadySMS();
            
                fireIntervalRemoved(this, index, index);
            }
            return removed;
        }
        @Override
        protected void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1);
        }
        @Override
        protected void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1);
        }
        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }
    }
    
    /** Renderer for items in queue list */
    private class SMSQueueListRenderer extends DefaultListCellRenderer {
        private final JLabel delayLabel = new JLabel("", SwingConstants.TRAILING);
        private final JPanel panel = new JPanel(new BorderLayout());
        private final ImageIcon sendIcon = new ImageIcon(getClass().getResource(RES + "send-16.png"));

        public SMSQueueListRenderer() {
            panel.add(delayLabel, BorderLayout.LINE_END);
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list,value,index,
                    isSelected,false); //looks better without cell focus
            JLabel label = (JLabel) c;
            SMS sms = (SMS)value;
            
            //set text
            String text = sms.toString();
            if (text.startsWith(config.getCountryPrefix())) {
                text = text.substring(config.getCountryPrefix().length());
            }
            label.setText(text);
            //tweak colors on different look and feels
            label.setOpaque(false);
            delayLabel.setForeground(label.getForeground());
            //problematic sms colored
            if ((sms.getStatus() == SMS.Status.PROBLEMATIC) && !isSelected) {
                panel.setBackground(Color.RED);
            } else {
                panel.setBackground(label.getBackground());
            }
            //add operator logo
            Operator operator = OperatorUtil.getOperator(sms.getOperator());
            label.setIcon(operator != null ? operator.getIcon() : Icons.OPERATOR_BLANK);
            //set tooltip
            panel.setToolTipText(wrapToHTML(sms.getText()));
            //set delay label
            if (sendingSMS.contains(sms)) {
                delayLabel.setIcon(sendIcon);
                delayLabel.setText(null);
            } else {
                delayLabel.setIcon(null);
                delayLabel.setText(convertDelayToHumanString(
                        findCurrentDelay(sms)));
            }
            //add to panel
            panel.add(label, BorderLayout.CENTER);
            
            return panel;
        }
        /** transform string to html with linebreaks */
        private String wrapToHTML(String text) {
            StringBuilder output = new StringBuilder();
            output.append("<html>");
            int from = 0;
            while (from < text.length()) {
                int to = from + 50;
                to = text.indexOf(' ',to);
                if (to < 0) {
                    to = text.length();
                }
                output.append(text.substring(from, to));
                output.append("<br>");
                from = to + 1;
            }
            output.append("</html>");
            return output.toString();
        }
    }
    
    /** Every second update the information about current message delays */
    private class DelayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean timerNeeded = false;
            boolean newSMSReady = false;
            
            //for every delay substract one second
            for (Entry<String, Integer> delay : operatorDelay.entrySet()) {
                if (delay.getValue() > 0) {
                    delay.setValue(delay.getValue() - 1);
                    timerNeeded = true;
                    //new operator delay just dropped to 0
                    if (delay.getValue() <= 0) {
                        newSMSReady = true;
                    }
                }
            }
            
            if (timerNeeded) {
                //delays changed, clear current sms delays
                smsDelay.clear();
            } else {
                //when everything is on 0, no need for timer to run, let's stop it
                timer.stop();
            }
            //we have found new ready sms, update the collection
            if (newSMSReady) {
                findNewReadySMS();
            }
            //update the gui
            if (queueListModel.getSize() > 0) {
                queueListModel.fireContentsChanged(queueListModel, 0, queueListModel.getSize() - 1);
            }
        }
    }
    
    /** Popup menu in the queue list */
    private class QueuePopupMenu extends JPopupMenu {

        public QueuePopupMenu() {
            JMenuItem menuItem = null;

            //edit sms action
            menuItem = new JMenuItem(editSMSAction);
            this.add(menuItem);

            //delete sms action
            menuItem = new JMenuItem(deleteSMSAction);
            this.add(menuItem);

            //move sms up action
            menuItem = new JMenuItem(smsUpAction);
            this.add(menuItem);
            
            //move sms down action
            menuItem = new JMenuItem(smsDownAction);
            this.add(menuItem);
            
            this.addSeparator();
            
            //queue pause action
            menuItem = new JMenuItem(smsQueuePauseAction);
            this.add(menuItem);
        }
    }
    
    /** Mouse listener on the queue list */
    private class QueueMouseListener extends ListPopupMouseListener {

        public QueueMouseListener(JList list, JPopupMenu popup) {
            super(list, popup);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            //transfer on left button doubleclick
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                editButton.doClick();
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editButton;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JToggleButton pauseButton;
    private javax.swing.JList queueList;
    private javax.swing.JButton smsDownButton;
    private javax.swing.JButton smsUpButton;
    // End of variables declaration//GEN-END:variables
    
}
