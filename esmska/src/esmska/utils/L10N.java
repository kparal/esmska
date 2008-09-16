/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JTabbedPane;
import org.openide.awt.Mnemonics;

/** Class containing useful methods when doing internacionalization and localization
 *
 * @author ripper
 */
public class L10N {
    /** Resource bundle containing all translations */
    public static final ResourceBundle l10nBundle = ResourceBundle.getBundle("esmska.resources.l10n");
    
    /** Set a name and mnemonics to an Action.
     * 
     * @param action Action to modify
     * @param text localized string containg &amp; before character 
     *             which should be used as a mnemonics
     * @see org.openide.awt.Mnemonics#setLocalizedText(javax.swing.AbstractButton, java.lang.String) 
     */
    public static void setLocalizedText(Action action, String text) {
        int index = Mnemonics.findMnemonicAmpersand(text);
        if (index < 0) {
            action.putValue(Action.NAME, text);
        } else {
            String name = text.substring(0, index) + text.substring(index + 1);
            action.putValue(Action.NAME, name);
            action.putValue(Action.MNEMONIC_KEY,name.codePointAt(index));
        }
    }
    
    /** Set a name and mnemonics to a pane in JTabbedPane.
     * 
     * @param tabbedPane tabbed pane containing the pane
     * @param tabIndex index of the pane tab
     * @param text localized string containg &amp; before character 
     *             which should be used as a mnemonics
     * @see org.openide.awt.Mnemonics#setLocalizedText(javax.swing.AbstractButton, java.lang.String) 
     */
    public static void setLocalizedText(JTabbedPane tabbedPane, int tabIndex, String text) {
        int index = Mnemonics.findMnemonicAmpersand(text);
        if (index < 0) {
            tabbedPane.setTitleAt(tabIndex, text);
        } else {
            String name = text.substring(0, index) + text.substring(index + 1);
            tabbedPane.setTitleAt(tabIndex, name);
            tabbedPane.setMnemonicAt(tabIndex, name.codePointAt(index));
        }
    }
}
