/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import esmska.data.Icons;
import esmska.utils.L10N;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.DefaultEditorKit;

/** Popup menu with clipboard actions
 *
 * @author ripper
 */
public class ClipboardPopupMenu extends JPopupMenu {

    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static ClipboardPopupMenu instance;
    private static PopupListener popupListener;

    private ClipboardPopupMenu() {
        JMenuItem menuItem = null;

        //cut action
        menuItem = new JMenuItem(new DefaultEditorKit.CutAction());
        menuItem.setText(l10n.getString("ClipboardPopupMenu.Cut"));
        menuItem.setIcon(Icons.get("cut-16.png"));
        this.add(menuItem);

        //copy action
        menuItem = new JMenuItem(new DefaultEditorKit.CopyAction());
        menuItem.setText(l10n.getString("ClipboardPopupMenu.Copy"));
        menuItem.setIcon(Icons.get("copy-16.png"));
        this.add(menuItem);

        //paste action
        menuItem = new JMenuItem(new DefaultEditorKit.PasteAction());
        menuItem.setText(l10n.getString("ClipboardPopupMenu.Paste"));
        menuItem.setIcon(Icons.get("paste-16.png"));
        this.add(menuItem);
        
        //mouse listener
        popupListener = new PopupListener();
    }

    /** Register a clipboard popup menu onto this component 
     * @param component component which should have clipboard popup menu
     */
    public static void register(JComponent component) {
        if (instance == null) {
            instance = new ClipboardPopupMenu();
        }
        component.addMouseListener(popupListener);
    }

    /** Mouse listener for showing the popup menu */
    private class PopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Component comp = e.getComponent();
                //transfer focus to originator of the event
                //(e.g. not to paste text to different textfield)
                if (comp != null && !comp.isFocusOwner()) {
                    comp.requestFocusInWindow();
                }
                //show the menu
                ClipboardPopupMenu.this.show(comp,
                        e.getX(), e.getY());
            }
        }
    }
}
