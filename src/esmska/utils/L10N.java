/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JTabbedPane;
import org.openide.awt.Mnemonics;

/** Class containing useful methods when doing internacionalization and localization
 *
 * @author ripper
 */
public class L10N {
    /** Resource bundle containing all translations */
    public static final ResourceBundle l10nBundle = ResourceBundle.getBundle("esmska.resources.l10n");
    //openide Mnemonics class allows to set mnemonics only on buttons,
    //so we use fake button and after copy the settings to other objects
    private static final JButton button = new JButton();

    /** Set a name and mnemonics to an Action.
     * 
     * @param action Action to modify
     * @param text localized string containg &amp; before character 
     *             which should be used as a mnemonics
     * @see org.openide.awt.Mnemonics#setLocalizedText(javax.swing.AbstractButton, java.lang.String) 
     */
    public static void setLocalizedText(Action action, String text) {
        button.setText(null);
        button.setMnemonic(-1);
        button.setDisplayedMnemonicIndex(-1);

        Mnemonics.setLocalizedText(button, text);

        action.putValue(Action.NAME, button.getText());
        action.putValue(Action.MNEMONIC_KEY, button.getMnemonic());
        action.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, button.getDisplayedMnemonicIndex());
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
        button.setText(null);
        button.setMnemonic(-1);
        button.setDisplayedMnemonicIndex(-1);

        Mnemonics.setLocalizedText(button, text);

        tabbedPane.setTitleAt(tabIndex, button.getText());
        tabbedPane.setMnemonicAt(tabIndex, button.getMnemonic());
        tabbedPane.setDisplayedMnemonicIndexAt(tabIndex, button.getDisplayedMnemonicIndex());
    }
}
