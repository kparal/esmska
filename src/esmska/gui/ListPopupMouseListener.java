package esmska.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JList;
import javax.swing.JPopupMenu;

/** Mouse listener for intelligent showing popups on JList. When triggering popup
 * on unselected item, selects this item before showing popup (and unselects
 * previous). Also listens for mouse wheel events and scrolls list selections
 * according to mouse wheel scrolling.
 * 
 * @author ripper
 */
public class ListPopupMouseListener extends MouseAdapter {

    protected JList list;
    protected JPopupMenu popup;

    /** Constructor.
     * 
     * @param list JList on which to listen for events. May not be null.
     * @param popup a popup to show on popup events. Use null for no popup.
     */
    public ListPopupMouseListener(JList list, JPopupMenu popup) {
        if (list == null) {
            throw new IllegalArgumentException("list may not be null");
        }
        this.list = list;
        this.popup = popup;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybePopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybePopup(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int index = list.getSelectedIndex();
        if (e.getWheelRotation() >= 0) { //mouse wheel down
            if (index < list.getModel().getSize() - 1) {
                list.setSelectedIndex(index + 1);
                list.ensureIndexIsVisible(index + 1);
            }
        } else { //mouse wheel up
            if (index > 0) {
                list.setSelectedIndex(index - 1);
                list.ensureIndexIsVisible(index - 1);
            }
        }
    }

    /** handle popup requests */
    protected void maybePopup(MouseEvent e) {
        if (!e.isPopupTrigger() || popup == null) {
            return;
        }

        //if user clicked on unselected item, select it
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0 && !list.isSelectedIndex(index)) {
            list.setSelectedIndex(index);
        }

        //show popup
        popup.show(list, e.getX(), e.getY());
    }
}
