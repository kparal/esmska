/*
 * ActionEventSupport.java
 *
 * Created on 3. říjen 2007, 17:45
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

/** Support for firing ActionEvents in classes.
 *
 * @author ripper
 */
public class ActionEventSupport {
    Object source;
    private HashSet<ActionListener> listeners = new HashSet<ActionListener>();
    
    /** Creates a new instance of ActionEventSupport */
    public ActionEventSupport(Object source) {
        if (source == null)
            throw new NullPointerException("source");
        this.source = source;
    }
    
    public void addActionListener(ActionListener actionListener) {
        listeners.add(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        listeners.remove(actionListener);
    }

    public void fireActionPerformed(int id, String command) {
        ActionEvent event = new ActionEvent(source, id, command);
        for (ActionListener al : listeners) {
            al.actionPerformed(event);
        }
    }
    
    public void fireActionPerformed(ActionEvent event) {
        for (ActionListener al : listeners) {
            al.actionPerformed(event);
        }
    }
}
