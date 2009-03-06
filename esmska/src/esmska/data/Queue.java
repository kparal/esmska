/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.apache.commons.lang.Validate;

/** Class representing queue of SMS
 *
 * @author ripper
 */
public class Queue {

    public static enum Events {
        /** New sms added.
         * Event value: added sms. */
        SMS_ADDED,
        /** Existing sms removed.
         * Event value: removed sms. */
        SMS_REMOVED,
        /** All sms's removed.
         * Event value: null. */
        QUEUE_CLEARED,
        /** The postition of sms in the queue changed.
         * Event value: moved sms. */
        SMS_POSITION_CHANGED,
        /** Existing sms is now ready for sending.
         * Event value: ready sms. */
        NEW_SMS_READY,
        /** Existing sms is now being sent.
         * Event value: sms being sent. */
        SENDING_SMS,
        /** Existing sms has been sent.
         * Event value: sent sms. */
        SMS_SENT,
        /** Existing sms failed to be sent.
         * Event value: failed sms. */
        SMS_SENDING_FAILED,
        /** Queue has been paused.
         * Event value: null. */
        QUEUE_PAUSED,
        /** Queue has been resumed.
         * Event value: null. */
        QUEUE_RESUMED
    }

    /** Internal tick interval of the queue in milliseconds.
     After each tick the current delay of all messages is recomputed. */
    public static final int TIMER_TICK = 250;

    /** shared instance */
    private static final Queue instance = new Queue();
    private static final Logger logger = Logger.getLogger(Queue.class.getName());
    private static final History history = History.getInstance();

    /** map of [operator name;SMS[*]] */
    private final SortedMap<String,List<SMS>> queue = Collections.synchronizedSortedMap(new TreeMap<String,List<SMS>>());
    private final AtomicBoolean paused = new AtomicBoolean();
    //map of <operator name, current delay in seconds>
    private final Map<String, Long> operatorDelay = Collections.synchronizedMap(new HashMap<String, Long>());
    //every second check the queue
    private final Timer timer = new Timer(TIMER_TICK, new TimerListener());

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, SMS> valuedSupport = new ValuedEventSupport<Events, SMS>(this);
    public void addValuedListener(ValuedListener<Events, SMS> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, SMS> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    /** Disabled contructor */
    private Queue() {
    }

    /** Get shared instance */
    public static Queue getInstance() {
        return instance;
    }

    /** Get all SMS in the queue.
     * This is a shortcut for getAll(null). */
    public List<SMS> getAll() {
        return getAll(null);
    }

    /** Get all SMS in the queue for specified operator.
     * The queue is always sorted by the operator name, the messages are not sorted.
     * @param operatorName name of the operator. May be null for any operator.
     * @return unmodifiable list of SMS for specified operator.
     */
    public List<SMS> getAll(String operatorName) {
        List<SMS> list = new ArrayList<SMS>();

        synchronized(queue) {
            if (operatorName == null) { //take all messages
                for (Collection<SMS> col : queue.values()) {
                    list.addAll(col);
                }
            } else if (queue.containsKey(operatorName)) { //take messages of that operator
                list = queue.get(operatorName);
            } else {
                //operator not found, therefore empty list
            }
        }

        return Collections.unmodifiableList(list);
    }

    /** Get a collection of SMS with particular status.
     * This a shortcut for getAllWithStatus(status, null).
     */
    public List<SMS> getAllWithStatus(SMS.Status status) {
        return getAllWithStatus(status, null);
    }

    /** Get a collection of SMS with particular status and operator.
     * The queue is always sorted by the operator name, the messages are not sorted.
     * @param status SMS status, not null
     * @param operatorName name of the operator of the SMS, may be null for any operator
     * @return unmodifiable list of SMS with that status in the queue
     */
    public List<SMS> getAllWithStatus(SMS.Status status, String operatorName) {
        Validate.notNull(status, "status is null");

        List<SMS> list = new ArrayList<SMS>();

        synchronized(queue) {
            if (operatorName == null) { //take every operator
                for (Collection<SMS> col : queue.values()) {
                    for (SMS sms : col) {
                        if (sms.getStatus() == status) {
                            list.add(sms);
                        }
                    }
                }
            } else if (queue.containsKey(operatorName)) { //only one operator
                for (SMS sms : queue.get(operatorName)) {
                    if (sms.getStatus() == status) {
                        list.add(sms);
                    }
                }
            } else {
                //operator not found, therefore empty list
            }
        }

        return Collections.unmodifiableList(list);
    }

    /** Add new SMS to the queue. May not be null.
     * @return See {@link Collection#add}.
     */
    public boolean add(SMS sms) {
        Validate.notNull(sms);

        sms.setStatus(SMS.Status.WAITING);
        String operator = sms.getOperator();
        boolean added = false;

        synchronized(queue) {
            if (queue.containsKey(operator)) { //this operator was already in the queue
                if (!queue.get(operator).contains(sms)) { //sms not already present
                    added = queue.get(operator).add(sms);
                }
            } else { //new operator
                List<SMS> list = new ArrayList<SMS>();
                list.add(sms);
                queue.put(operator, list);
                added = true;
            }
        }

        if (added) {
            logger.fine("Added new SMS to queue: " + sms.toDebugString());
            valuedSupport.fireEventOccured(Events.SMS_ADDED, sms);
            markIfReady(sms);
            timer.start();
        }
        return added;
    }

    /** Add collection of new SMS to the queue.
     * @param collection Collection of SMS. May not be null, may not contain null element.
     * @return See {@link Collection#addAll(java.util.Collection)}
     */
    public boolean addAll(Collection<SMS> collection) {
        Validate.notNull(collection, "collection is null");
        Validate.noNullElements(collection);

        logger.fine("Adding " + collection.size() + " new SMS to the queue");
        boolean added = false;
        for (SMS sms : collection) {
            if (add(sms)) {
                added = true;
            }
        }
        return added;
    }

    /** Remove SMS from the queue. If the SMS is not present nothing happens.
     * @param sms SMS to be removed. Not null.
     * @return See {@link Collection#remove(java.lang.Object) }
     */
    public boolean remove(SMS sms) {
        Validate.notNull(sms);

        String operator = sms.getOperator();
        boolean removed = false;

        synchronized(queue) {
            if (queue.containsKey(operator)) { //only if we have this operator
                removed = queue.get(operator).remove(sms);
            }
            if (removed && queue.get(operator).size() == 0) {
                //if there are no more sms from that operator, delete it from map
                queue.remove(operator);
                operatorDelay.remove(operator);
            }
        }

        if (removed) {
            logger.fine("Removed SMS from queue: " + sms.toDebugString());
            valuedSupport.fireEventOccured(Events.SMS_REMOVED, sms);
            markAllIfReady();
        }
        return removed;
    }

    /** Remove all SMS from the queue. */
    public void clear() {
        logger.fine("Clearing the queue.");

        synchronized(queue) {
            queue.clear();
            operatorDelay.clear();
        }

        valuedSupport.fireEventOccured(Events.QUEUE_CLEARED, null);
    }

    /** Checks whether the SMS is in the queue.
     * @param sms SMS, not null
     * @return See {@link Collection#contains(java.lang.Object) }
     */
    public boolean contains(SMS sms) {
        Validate.notNull(sms);

        String operator = sms.getOperator();

        synchronized(queue) {
            if (queue.containsKey(operator)) {
                return queue.get(operator).contains(sms);
            } else {
                //nowhere in the queue
                return false;
            }
        }
    }

    /** Get the number of SMS in the queue */
    public int size() {
        int size = 0;

        synchronized(queue) {
            for (Collection<SMS> col : queue.values()) {
                size += col.size();
            }
        }

        return size;
    }

    /** Check if the queue is empty */
    public boolean isEmpty() {
        synchronized(queue) {
            for (Collection<SMS> col : queue.values()) {
                if (col.size() > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Whether queue is currently paused */
    public boolean isPaused() {
        return paused.get();
    }

    /** Sets whether queue is currently paused */
    public void setPaused(boolean paused) {
        this.paused.set(paused);
        if (paused) {
            logger.fine("Queue is now paused");
            valuedSupport.fireEventOccured(Events.QUEUE_PAUSED, null);
        } else {
            logger.fine("Queue is now resumed");
            valuedSupport.fireEventOccured(Events.QUEUE_RESUMED, null);
        }
    }

    /** Move SMS in the queue to another position.
     * Queue is always sorted by operator, therefore SMS may be moved only within
     * section of its operator.
     * @param sms sms to be moved, not null
     * @param positionDelta direction and amount of movement. Positive number moves
     * to the back of the queue, negative number moves to the front of the queue.
     * The number corresponds to the number of positions to change. If the number
     * is larger than current queue dimensions, the element will simply stop as the
     * first or as the last element.
     */
    public void movePosition(SMS sms, int positionDelta) {
        Validate.notNull(sms, "sms is null");

        String operator = sms.getOperator();

        synchronized(queue) {
            if (positionDelta == 0 || !queue.containsKey(operator) ||
                    !queue.get(operator).contains(sms)) {
                //nothing to move
                return;
            }
            logger.fine("Moving sms " + sms.toDebugString() + "with delta " + positionDelta);

            List<SMS> list = queue.get(operator);
            int currentPos = list.indexOf(sms);
            int newPos = currentPos + positionDelta;

            //check the boundaries of the queue
            if (newPos < 0) {
                newPos = 0;
            }
            if (newPos > list.size() - 1) {
                newPos = list.size() - 1;
            }
            if (currentPos == newPos) {
                return;
            }

            list.remove(currentPos);
            list.add(newPos, sms);
        }

        valuedSupport.fireEventOccured(Events.SMS_POSITION_CHANGED, sms);

        //if sms is currently ready, reset it to waiting and do another search
        //(now different sms may be ready instead of this)
        if (sms.getStatus() == SMS.Status.READY) {
            sms.setStatus(SMS.Status.WAITING);
            markAllIfReady();
        }
    }

    /** Return current delay for specified operator.
     * @param operatorName name of the operator. May be null.
     * @return number of milliseconds next message from the operator must wait.
     *  If no such operator found, return 0.
     */
    public long getOperatorDelay(String operatorName) {

        Long del = operatorDelay.get(operatorName);
        if (del != null) {
            return del;
        }

        Operator operator = Operators.getOperator(operatorName);
        long delay = 0;

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
                long difference = (new Date().getTime() - record.getDate().getTime()); //in milliseconds
                delay = Math.max(operator.getDelayBetweenMessages() * 1000 - difference, 0);
            }
        }

        operatorDelay.put(operatorName, delay);
        return delay;
    }

    /** Return current delay for specified sms.
     * The delay is taking into account all previous messages from the same operator
     * which are waiting to be sent. If sms is not found in the queue, it is
     * considered to be at the end of the queue.
     * @param sms sms, not null
     * @return number of milliseconds a message must wait
     */
    public long getSMSDelay(SMS sms) {
        Validate.notNull(sms);

        String operatorName = sms.getOperator();
        long delay = getOperatorDelay(operatorName);
        List<SMS> list = queue.get(operatorName);
        if (list == null) { //no such operator in the queue
            return delay; //therefore operator delay is sms delay
        }
        int index = list.indexOf(sms);
        Operator operator = Operators.getOperator(operatorName);
        int opDelay = operator != null ? operator.getDelayBetweenMessages() * 1000 : 0;
        if (index >= 0) { //in the queue
            delay = delay + (index * opDelay);
        } else { //not in the queue, therefore after all sms's in the queue
            delay = delay + (list.size() * opDelay);
        }
        return delay;
    }

    /** Mark the SMS as successfully sent.
     * @param sms sent SMS, not null
     */
    public void setSMSSent(SMS sms) {
        Validate.notNull(sms);

        logger.fine("Marking sms as successfully sent: " + sms.toDebugString());
        sms.setStatus(SMS.Status.SENT);
        valuedSupport.fireEventOccured(Events.SMS_SENT, sms);

        updateOperatorDelay(sms.getOperator());
        remove(sms); //remove it from the queue
        timer.start();
    }

     /** Mark SMS as currently being sent.
     * @param sms SMS that is currently being sent, not null
     */
    public void setSMSSending(SMS sms) {
        Validate.notNull(sms);

        logger.fine("Marking SMS as currently being sent: " + sms.toDebugString());
        sms.setStatus(SMS.Status.SENDING);
        valuedSupport.fireEventOccured(Events.SENDING_SMS, sms);
    }

    /** Mark SMS as failed during sending. Pauses the queue.
     * @param sms SMS that has failed, not null
     */
    public void setSMSFailed(SMS sms) {
        Validate.notNull(sms);

        logger.fine("Marking SMS as failed during sending: " + sms.toDebugString());
        //pause the queue when sms fail
        setPaused(true);
        //set sms to be waiting again
        sms.setStatus(SMS.Status.WAITING);
        valuedSupport.fireEventOccured(Events.SMS_SENDING_FAILED, sms);
        
        updateOperatorDelay(sms.getOperator());
        timer.start();
        markIfReady(sms);
    }

    /** Check if sms is ready and set status if it is */
    private void markIfReady(SMS sms) {
        Validate.notNull(sms);

        long delay = getSMSDelay(sms);
        if (sms.getStatus() == SMS.Status.WAITING && delay <= 0) {
            logger.finer("Marking SMS as ready: " + sms.toDebugString());
            sms.setStatus(SMS.Status.READY);
            valuedSupport.fireEventOccured(Events.NEW_SMS_READY, sms);
        }
    }

    /** Check all sms for that which are ready and set their status */
    private void markAllIfReady() {
        ArrayList<SMS> ready = new ArrayList<SMS>();

        synchronized(queue) {
            for (String operator : queue.keySet()) {
                long delay = getOperatorDelay(operator);
                if (delay > 0) { //any new sms can't be ready
                    continue;
                }
                for (SMS sms : queue.get(operator)) {
                    long smsDelay = getSMSDelay(sms);
                    if (smsDelay > 0) {
                        break;
                    }
                    if (sms.getStatus() == SMS.Status.WAITING) {
                        logger.finer("Marking SMS as ready: " + sms.toDebugString());
                        sms.setStatus(SMS.Status.READY);
                        ready.add(sms);
                    }
                }
            }
        }

        for (SMS sms : ready) {
            valuedSupport.fireEventOccured(Events.NEW_SMS_READY, sms);
        }
    }

    /** Remove operator from delay cache and compute its delay again */
    private void updateOperatorDelay(String operatorName) {
        Validate.notEmpty(operatorName);

        operatorDelay.remove(operatorName);
        getOperatorDelay(operatorName);
    }

    /** Update the information about current message delays */
    private class TimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean timerNeeded = false;
            boolean checkSMSReady = false;

            synchronized(operatorDelay) {
            //for every delay substract one second
                for (Iterator<Entry<String, Long>> iter = operatorDelay.entrySet().iterator(); iter.hasNext(); ) {
                    Entry<String, Long> delay = iter.next();
                    if (!queue.containsKey(delay.getKey())) {
                        //if there is some operator which is no longer in the queue, we don't need it anymore
                        iter.remove();
                        continue;
                    }
                    if (delay.getValue() > 0) {
                        long newDelay = Math.max(delay.getValue() - TIMER_TICK, 0);
                        delay.setValue(newDelay);
                        timerNeeded = true; //stil counting down for someone
                        if (delay.getValue() <= 0) {
                            //new operator delay just dropped to 0
                            checkSMSReady = true;
                        }
                    }
                }
            }

            if (!timerNeeded) {
                //when everything is on 0, no need for timer to run, let's stop it
                timer.stop();
            }
            if (checkSMSReady) {
                //we may have new ready sms, check it
                markAllIfReady();
            }
        }
    }
}
