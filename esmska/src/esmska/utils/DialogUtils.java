/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

import esmska.ThemeManager;
import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;
import org.apache.commons.lang.ArrayUtils;

/** Sort dialog buttons according to current look and feel. Some L&Fs are
 * reversing order of the buttons, which is an unwanted behaviour. This
 * class reverts it again, so they are in original order. In every case
 * be sure never to rely on exact button position.
 *
 * @author ripper
 */
public class DialogUtils {

    /** Sorts options provided to dialog as a buttons according to current look
     * and feel. Some L&Fs are reversing order of the buttons, which is an 
     * unwanted behaviour. This method reverts it again for such L&Fs, so options
     * are in original order. In every case be sure never rely on exact 
     * option position.
     *
     * More detailed explanation:
     * There are two ways to display options:
     * 1. Standard (default option rightmost): Cancel | Action
     * 2. Windows-like (default option leftmost): Action | Cancel
     *
     * In Esmska all dialogs are written using the standard notation.
     *
     * Desktop environments divided by the way of displaying options:
     * 1. Standard: Gnome, Mac OS
     * 2. Windows like: Windows, KDE
     *
     * In following LAFs and OSs Java displays the options in wrong order:
     * 1. Metal: Windows, KDE
     * 2. GTK: Gnome, Mac OS
     * 3. Windows: Windows (available nowhere else)
     * 4. Aqua: Mac OS (available nowhere else)
     * 5. JGoodies: Windows, KDE
     * 6. Substance: Windows, KDE
     * 7. Nimbus: Windows, Gnome, Mac
     *
     * In these cases this method will revert the button order to display
     * it like native application.
     *
     * @param options options written in the standard notation (default option rightmost)
     * @return options adjusted to current environment and LaF;
     * null if <code>options</code> was null
     */
    public static Object[] sortOptions(Object... options) {
        Object[] reversed = ArrayUtils.clone(options);
        ArrayUtils.reverse(reversed);
        
        if (OSType.isWindows()) {
            return reversed;
        }
        if (OSType.isKDEDesktop() && !ThemeManager.isGTKCurrentLaF() &&
                !ThemeManager.isNimbusCurrentLaF()) {
            return reversed;
        }
        if (ThemeManager.isGTKCurrentLaF() && !OSType.isKDEDesktop()) {
            return reversed;
        }
        if (ThemeManager.isAquaCurrentLaF()) {
            return reversed;
        }
        if (ThemeManager.isNimbusCurrentLaF() && !OSType.isKDEDesktop()) {
            return reversed;
        }

        //no change needed
        return options;
    }

    /** Set dialog to be document modal and set property so it will look as
     * native modal dialog on Mac.
     *
     * @param dialog dialog which should be document modal and Mac native looking
     */
    public static void setDocumentModalDialog(JDialog dialog) {
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);
    }
}
