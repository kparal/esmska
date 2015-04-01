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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.apache.commons.lang.StringUtils;
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

    /** map of [gateway name;SMS[*]] */
    private final SortedMap<String,List<SMS>> queue = Collections.synchronizedSortedMap(new TreeMap<String,List<SMS>>());
    private final AtomicBoolean paused = new AtomicBoolean();
    //map of <gateway name, current delay in seconds>
    private final Map<String, Long> gatewayDelay = Collections.synchronizedMap(new HashMap<String, Long>());
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

    /** Disabled constructor */
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

    /** Get all SMS in the queue for specified gateway.
     * The queue is always sorted by the gateway name, the messages are not sorted.
     * @param gatewayName name of the gateway. May be null for any gateway.
     * @return unmodifiable list of SMS for specified gateway.
     */
    public List<SMS> getAll(String gatewayName) {
        List<SMS> list = new ArrayList<SMS>();

        synchronized(queue) {
            if (gatewayName == null) { //take all messages
                for (Collection<SMS> col : queue.values()) {
                    list.addAll(col);
                }
            } else if (queue.containsKey(gatewayName)) { //take messages of that gateway
                list = queue.get(gatewayName);
            } else {
                //gateway not found, therefore empty list
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

    /** Get a collection of SMS with particular status and gateway.
     * The queue is always sorted by the gateway name, the messages are not sorted.
     * @param status SMS status, not null
     * @param gatewayName name of the gateway of the SMS, may be null for any gateway
     * @return unmodifiable list of SMS with that status in the queue
     */
    public List<SMS> getAllWithStatus(SMS.Status status, String gatewayName) {
        Validate.notNull(status, "status is null");

        List<SMS> list = new ArrayList<SMS>();

        synchronized(queue) {
            if (gatewayName == null) { //take every gateway
                for (Collection<SMS> col : queue.values()) {
                    for (SMS sms : col) {
                        if (sms.getStatus() == status) {
                            list.add(sms);
                        }
                    }
                }
            } else if (queue.containsKey(gatewayName)) { //only one gateway
                for (SMS sms : queue.get(gatewayName)) {
                    if (sms.getStatus() == status) {
                        list.add(sms);
                    }
                }
            } else {
                //gateway not found, therefore empty list
            }
        }

        return Collections.unmodifiableList(list);
    }

    /** Get all SMS (fragments) in the queue with a specified ID.
     * The queue is always sorted by the gateway name, the messages are not sorted.
     * @param id message ID. Must not be empty.
     * @return unmodifiable list of SMS with the specified ID.
     */
    public List<SMS> getAllWithId(String id) {
        Validate.notEmpty(id);
        List<SMS> list = new ArrayList<SMS>();
        
        synchronized(queue) {
            for (Collection<SMS> col : queue.values()) {
                for (SMS sms : col) {
                    if (StringUtils.equals(sms.getId(), id)) {
                        list.add(sms);
                    }
                }
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
        String gateway = sms.getGateway();
        boolean added = false;

        synchronized(queue) {
            if (queue.containsKey(gateway)) { //this gateway was already in the queue
                if (!queue.get(gateway).contains(sms)) { //sms not already present
                    added = queue.get(gateway).add(sms);
                }
            } else { //new gateway
                List<SMS> list = new ArrayList<SMS>();
                list.add(sms);
                queue.put(gateway, list);
                added = true;
            }
        }

        if (added) {
            logger.log(Level.FINE, "Added new SMS to queue: {0}", sms);
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

        logger.log(Level.FINE, "Adding {0} new SMS to the queue", collection.size());
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

        String gateway = sms.getGateway();
        boolean removed = false;

        synchronized(queue) {
            if (queue.containsKey(gateway)) { //only if we have this gateway
                removed = queue.get(gateway).remove(sms);
            }
            if (removed && queue.get(gateway).isEmpty()) {
                //if there are no more sms from that gateway, delete it from map
                queue.remove(gateway);
                gatewayDelay.remove(gateway);
            }
        }

        if (removed) {
            logger.log(Level.FINE, "Removed SMS from queue: {0}", sms);
            valuedSupport.fireEventOccured(Events.SMS_REMOVED, sms);
            markAllIfReady();
        }
        return removed;
    }
    
    /** Remove all SMS with a specified ID from the queue.
     * @param id SMS to be removed. Not null.
     */
    public void remove(String id) {
        Validate.notEmpty(id);

        synchronized(queue) {
            for (SMS sms : getAllWithId(id)) {
                remove(sms);
            }
        }
    }

    /** Remove all SMS from the queue. */
    public void clear() {
        logger.fine("Clearing the queue.");

        synchronized(queue) {
            queue.clear();
            gatewayDelay.clear();
        }

        valuedSupport.fireEventOccured(Events.QUEUE_CLEARED, null);
    }

    /** Checks whether the SMS is in the queue.
     * @param sms SMS, not null
     * @return See {@link Collection#contains(java.lang.Object) }
     */
    public boolean contains(SMS sms) {
        Validate.notNull(sms);

        String gateway = sms.getGateway();

        synchronized(queue) {
            if (queue.containsKey(gateway)) {
                return queue.get(gateway).contains(sms);
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
     * Queue is always sorted by gateway, therefore SMS may be moved only within
     * section of its gateway.
     * @param sms sms to be moved, not null
     * @param positionDelta direction and amount of movement. Positive number moves
     * to the back of the queue, negative number moves to the front of the queue.
     * The number corresponds to the number of positions to change. If the number
     * is larger than current queue dimensions, the element will simply stop as the
     * first or as the last element.
     */
    public void movePosition(SMS sms, int positionDelta) {
        Validate.notNull(sms, "sms is null");

        String gateway = sms.getGateway();

        synchronized(queue) {
            if (positionDelta == 0 || !queue.containsKey(gateway) ||
                    !queue.get(gateway).contains(sms)) {
                //nothing to move
                return;
            }
            logger.log(Level.FINE, "Moving sms {0} with delta {1}", new Object[]{sms, positionDelta});

            List<SMS> list = queue.get(gateway);
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

        // we have moved an sms in the queue, that may have changed which sms
        // should be waiting and which should be ready now
        // mark all ready sms from this gateway as waiting and then compute
        // a new ready one
        List<SMS> messages = getAll(sms.getGateway());
        for (SMS message : messages) {
            if (message.getStatus() == SMS.Status.READY) {
                message.setStatus(SMS.Status.WAITING);
            }
        }
        markAllIfReady();
    }

    /** Return current delay for specified gateway.
     * @param gatewayName name of the gateway. May be null.
     * @return number of milliseconds next message from the gateway must wait.
     *  If no such gateway found, return 0.
     */
    public long getGatewayDelay(String gatewayName) {

        Long del = gatewayDelay.get(gatewayName);
        if (del != null) {
            return del;
        }

        Gateway gateway = Gateways.getInstance().get(gatewayName);
        long delay = 0;

        if (gateway == null) { //unknown gateway
            delay = 0;
        } else if (gateway.getDelayBetweenMessages() <= 0) { //gateway without delay
            delay = 0;
        } else { //search in history
            History.Record record = history.findLastRecord(gatewayName);
            if (record == null) { //no previous record
                delay = 0;
            } else { //compute the delay
                //FIXME: does not take various daylight saving time etc into account
                //A more complex library (eg. Joda Time) is needed to calculate true time differences.
                long difference = (new Date().getTime() - record.getDate().getTime()); //in milliseconds
                if (difference < 0) {
                    //last message was sent in the future
                    //that's clearly wrong, the user probably messed up system time
                    //the best thing we can do is just try to send the message without a delay
                    delay = 0;
                } else {
                    //let's compute the real remaining delay
                    delay = Math.max(gateway.getDelayBetweenMessages() * 1000 - difference, 0);
                }
            }
        }

        gatewayDelay.put(gatewayName, delay);
        return delay;
    }

    /** Return current delay for specified sms.
     * The delay is taking into account all previous messages from the same gateway
     * which are waiting to be sent. If sms is not found in the queue, it is
     * considered to be at the end of the queue.
     * @param sms sms, not null
     * @return number of milliseconds a message must wait
     */
    public long getSMSDelay(SMS sms) {
        Validate.notNull(sms);

        String gatewayName = sms.getGateway();
        long delay = getGatewayDelay(gatewayName);
        List<SMS> list = queue.get(gatewayName);
        if (list == null) { //no such gateway in the queue
            return delay; //therefore gateway delay is sms delay
        }
        int index = list.indexOf(sms);
        Gateway gateway = Gateways.getInstance().get(gatewayName);
        int opDelay = gateway != null ? gateway.getDelayBetweenMessages() * 1000 : 0;
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

        logger.log(Level.FINE, "Marking sms as successfully sent: {0}", sms);
        sms.setStatus(SMS.Status.SENT);
        valuedSupport.fireEventOccured(Events.SMS_SENT, sms);

        updateGatewayDelay(sms.getGateway());
        remove(sms); //remove it from the queue
        timer.start();
    }

     /** Mark SMS as currently being sent.
     * @param sms SMS that is currently being sent, not null
     */
    public void setSMSSending(SMS sms) {
        Validate.notNull(sms);

        logger.log(Level.FINE, "Marking SMS as currently being sent: {0}", sms);
        sms.setStatus(SMS.Status.SENDING);
        valuedSupport.fireEventOccured(Events.SENDING_SMS, sms);
    }

    /** Mark SMS as failed during sending. Pauses the queue.
     * @param sms SMS that has failed, not null
     */
    public void setSMSFailed(SMS sms) {
        Validate.notNull(sms);

        logger.log(Level.FINE, "Marking SMS as failed during sending: {0}", sms);
        //pause the queue when sms fail
        setPaused(true);
        //set sms to be waiting again
        sms.setStatus(SMS.Status.WAITING);
        valuedSupport.fireEventOccured(Events.SMS_SENDING_FAILED, sms);
        
        updateGatewayDelay(sms.getGateway());
        timer.start();
        markIfReady(sms);
    }
    
    /**
    * Extract all message fragments (according to ID) from the queue and join them
    * into a full message.
    * @param id id of all the message fragments; not empty
    * @param remove whether to remove all the fragments from the queue in the process
    * @return sms the whole message with concatenated fragments' text; or null if
    *             no such ID was present
    */
    public SMS extractSMS(String id, boolean remove) {
        Validate.notEmpty(id);
        SMS sms;
        
        synchronized(queue) {
            List<SMS> fragments = getAllWithId(id);
            if (fragments.isEmpty()) {
                return null;
            }

            SMS head = fragments.get(0);
            sms = new SMS(head.getNumber(), "", head.getGateway(), head.getName(), null);
            for (SMS fragment : fragments) {
                sms.setText(sms.getText() + fragment.getText());
            }

            if (remove) {
                remove(id);
            }
        }
        
        return sms;
    }

    /** Check if sms is ready and set status if it is */
    private void markIfReady(SMS sms) {
        Validate.notNull(sms);

        long delay = getSMSDelay(sms);
        if (sms.getStatus() == SMS.Status.WAITING && delay <= 0) {
            logger.log(Level.FINER, "Marking SMS as ready: {0}", sms);
            sms.setStatus(SMS.Status.READY);
            valuedSupport.fireEventOccured(Events.NEW_SMS_READY, sms);
        }
    }

    /** Check all sms for that which are ready and set their status */
    private void markAllIfReady() {
        ArrayList<SMS> ready = new ArrayList<SMS>();

        synchronized(queue) {
            for (String gateway : queue.keySet()) {
                long delay = getGatewayDelay(gateway);
                if (delay > 0) { //any new sms can't be ready
                    continue;
                }
                for (SMS sms : queue.get(gateway)) {
                    long smsDelay = getSMSDelay(sms);
                    if (smsDelay > 0) {
                        break;
                    }
                    if (sms.getStatus() == SMS.Status.WAITING) {
                        logger.log(Level.FINER, "Marking SMS as ready: {0}", sms);
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

    /** Remove gateway from delay cache and compute its delay again */
    private void updateGatewayDelay(String gatewayName) {
        Validate.notEmpty(gatewayName);

        gatewayDelay.remove(gatewayName);
        getGatewayDelay(gatewayName);
    }

    /** Update the information about current message delays */
    private class TimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean timerNeeded = false;
            boolean checkSMSReady = false;

            synchronized(gatewayDelay) {
            //for every delay substract one second
                for (Iterator<Entry<String, Long>> iter = gatewayDelay.entrySet().iterator(); iter.hasNext(); ) {
                    Entry<String, Long> delay = iter.next();
                    if (!queue.containsKey(delay.getKey())) {
                        //if there is some gateway which is no longer in the queue, we don't need it anymore
                        iter.remove();
                        continue;
                    }
                    if (delay.getValue() > 0) {
                        long newDelay = Math.max(delay.getValue() - TIMER_TICK, 0);
                        delay.setValue(newDelay);
                        timerNeeded = true; //stil counting down for someone
                        if (delay.getValue() <= 0) {
                            //new gateway delay just dropped to 0
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
