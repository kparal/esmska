/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data.event;

import java.util.EventListener;

/** Listener interface for receiving valued events
 * @author ripper
 * @param <E> Enum describing the possible event types
 * @param <V> Type of associated value
 */
public interface ValuedListener<E extends Enum<E>, V> extends EventListener {

    /**
     * Invoked when an event occurs.
     */
    public void eventOccured(ValuedEvent<E,V> e);

}
