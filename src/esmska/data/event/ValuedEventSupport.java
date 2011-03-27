/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data.event;

import java.util.ArrayList;

/** Support for firing ValuedEvents in classes.
 * @author ripper
 * @param <E> Enum describing the possible event types
 * @param <V> Type of associated value
 */
public class ValuedEventSupport<E extends Enum<E>, V> {
    Object source;
    private ArrayList<ValuedListener<E,V>> listeners = new ArrayList<ValuedListener<E,V>>();

    /** Creates a new instance of ActionEventSupport
     * @param source Source object, for which the ActionEventSupport should work. May not be null.
     */
    public ValuedEventSupport(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        this.source = source;
    }

    /** Add new ActionListener */
    public synchronized void addValuedListener(ValuedListener<E,V> valuedListener) {
        listeners.add(valuedListener);
    }

    /** Remove existing ActionListener */
    public synchronized void removeValuedListener(ValuedListener<E,V> valuedListener) {
        listeners.remove(valuedListener);
    }

    /** Fire new ActionEvent
     * @param event type of event, not null
     * @param value associated value, may be null
     */
    public void fireEventOccured(E event, V value) {
        ValuedEvent<E,V> ve = new ValuedEvent<E,V>(source, event, value);
        // clone the list of the listeners to allow the original list to be modified
        // while firing up events
        ArrayList<ValuedListener<E,V>> list = new ArrayList<ValuedListener<E, V>>(listeners);
        for (ValuedListener<E,V> listener : list) {
            listener.eventOccured(ve);
        }
    }

}
