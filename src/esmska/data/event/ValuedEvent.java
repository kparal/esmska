package esmska.data.event;

import java.util.EventObject;
import org.apache.commons.lang.Validate;

/** An event which carries a value. The value may be null (check the description
 * of the particular event).
 * @author ripper
 * @param <E> Enum describing the possible event types
 * @param <V> Type of associated value
 */
public class ValuedEvent<E extends Enum<E>, V> extends EventObject {

    protected E event;
    protected V value;

    /** Constructs a new ValuedEvent.
     * @param source the object that originated the event, not null
     * @param event enum value determining type of event, not null
     * @param value value associated with the event, may be null
     */
    public ValuedEvent(Object source, E event, V value) {
        super(source);
        Validate.notNull(event);

        this.event = event;
        this.value = value;
    }

    /** Get the event type */
    public E getEvent() {
        return event;
    }

    /** Get the associated value. May be null. */
    public V getValue() {
        return value;
    }

}
