/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.integration.mac;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Window listener that counts how many we have opened modal dialogs
 *
 * @author Marian Bouček
 */
public class ModalSheetCounter extends WindowAdapter {

    private int visibleSheets;

    @Override
    public void windowOpened(WindowEvent e) {
        visibleSheets++;
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
        visibleSheets--;
    }

    public boolean isModalSheetVisible() {
        return visibleSheets > 0;
    }
}
