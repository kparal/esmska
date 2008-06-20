/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

import esmska.ThemeManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.UIManager;

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
     * @param options options to revert for certain L&Fs
     * @return original or reverted array, according to current L&F
     */
    public static Object[] sortOptions(Object... options) {
        //GTK and Mac L&Fs are reverting buttons (Java bug?), so revert it back
        //Windows users are used to reversed button order, do it for them as well
        if (ThemeManager.isGTKCurrentLaF() ||
                ThemeManager.isAquaCurrentLaF() ||
                OSType.isWindows()) {
            List<Object> list = Arrays.asList(options);
            Collections.reverse(list);
            return list.toArray();
        }
        
        //other L&Fs are ok, no change needed
        return options;
    }
}
