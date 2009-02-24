/*
 * ActionEventSupport.java
 *
 * Created on 3. říjen 2007, 17:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.data.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ListIterator;

/** Support for firing ActionEvents in classes.
 *
 * @author ripper
 */
public class ActionEventSupport {
    Object source;
    private ArrayList<ActionListener> listeners = new ArrayList<ActionListener>();
    
    /** Creates a new instance of ActionEventSupport
     * @param source Source object, for which the ActionEventSupport should work. May not be null.
     */
    public ActionEventSupport(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        this.source = source;
    }
    
    /** Add new ActionListener */
    public void addActionListener(ActionListener actionListener) {
        listeners.add(actionListener);
    }
    
    /** Remove existing ActionListener */
    public void removeActionListener(ActionListener actionListener) {
        listeners.remove(actionListener);
    }

    /** Fire new ActionEvent */
    public void fireActionPerformed(int id, String command) {
        ActionEvent event = new ActionEvent(source, id, command);
        for (ListIterator<ActionListener> it = listeners.listIterator(listeners.size()); it.hasPrevious(); ) {
            it.previous().actionPerformed(event);
        }
    }
    
}
