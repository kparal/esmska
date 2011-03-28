package esmska.data.event;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/** Abstract class for ListDataListener. For all ListDataListener's mandatory methods
 * executes method onUpdate(ListDataEvent e).
 *
 * @author ripper
 */
public abstract class AbstractListDataListener implements ListDataListener {

    @Override
    public void contentsChanged(ListDataEvent e) {
         onUpdate(e);
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
         onUpdate(e);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
         onUpdate(e);
    }

    /** Method executed on all list data updates */
    public abstract void onUpdate(ListDataEvent e);
}
