/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import java.util.ArrayList;
import java.util.ListIterator;

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
    public void addValuedListener(ValuedListener<E,V> valuedListener) {
        listeners.add(valuedListener);
    }

    /** Remove existing ActionListener */
    public void removeValuedListener(ValuedListener<E,V> valuedListener) {
        listeners.remove(valuedListener);
    }

    /** Fire new ActionEvent
     * @param event type of event, not null
     * @param value associated value, may be null
     */
    public void fireEventOccured(E event, V value) {
        ValuedEvent<E,V> ve = new ValuedEvent<E,V>(source, event, value);
        for (ListIterator<ValuedListener<E, V>> it = listeners.listIterator(listeners.size()); it.hasPrevious(); ) {
            it.previous().eventOccured(ve);
        }
    }

}
