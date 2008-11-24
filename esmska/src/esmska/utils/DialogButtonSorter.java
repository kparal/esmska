/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

import esmska.ThemeManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Sort dialog buttons according to current look and feel. Some L&Fs are
 * reversing order of the buttons, which is an unwanted behaviour. This
 * class reverts it again, so they are in original order. In every case
 * be sure never to rely on exact button position.
 *
 * @author ripper
 */
public class DialogButtonSorter {

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
     * @return options adjusted to current environment and LaF
     */
    public static Object[] sortOptions(Object... options) {
        if (OSType.isWindows()) {
            return reverse(options);
        }
        if (OSType.isKDEDesktop() && !ThemeManager.isGTKCurrentLaF() &&
                !ThemeManager.isNimbusCurrentLaF()) {
            return reverse(options);
        }
        if (ThemeManager.isGTKCurrentLaF() && !OSType.isKDEDesktop()) {
            return reverse(options);
        }
        if (ThemeManager.isAquaCurrentLaF()) {
            return reverse(options);
        }
        if (ThemeManager.isNimbusCurrentLaF() && !OSType.isKDEDesktop()) {
            return reverse(options);
        }

        //no change needed
        return options;
    }

    /** Reverses array of objects */
    private static Object[] reverse(Object... options) {
        List<Object> list = Arrays.asList(options);
        Collections.reverse(list);
        return list.toArray();
    }
}
