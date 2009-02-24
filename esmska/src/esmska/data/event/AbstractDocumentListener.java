/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data.event;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Abstract class for DocumentListener. For all DocumentListener's mandatory methods
 * executes method onUpdate(DocumentEvent e).
 *
 * @author ripper
 */
public abstract class AbstractDocumentListener implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e) {
        onUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        onUpdate(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        onUpdate(e);
    }

    /** Method executed on all document updates */
    public abstract void onUpdate(DocumentEvent e);
}
