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

/** Support for firing ActionEvents in classes.
 *
 * @author ripper
 */
public class ActionEventSupport {
    /** A container wants to be resized, possibly by frame.pack() */
    public static final int ACTION_NEED_RESIZE = 0;

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
    public synchronized void addActionListener(ActionListener actionListener) {
        listeners.add(actionListener);
    }
    
    /** Remove existing ActionListener */
    public synchronized void removeActionListener(ActionListener actionListener) {
        listeners.remove(actionListener);
    }

    /** Fire new ActionEvent */
    public void fireActionPerformed(int id, String command) {
        ActionEvent event = new ActionEvent(source, id, command);
        // clone the list of the listeners to allow the original list to be modified
        // while firing up events
        ArrayList<ActionListener> list = new ArrayList<ActionListener>(listeners);
        for (ActionListener listener : list) {
            listener.actionPerformed(event);
        }
    }
    
}
