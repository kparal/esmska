/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.integration;

import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;

/** Various helper methods to integrate program with Mac OS more closely.
 *
 * @author ripper
 */
public class MacUtils {

    /** Set dialog to be document modal and set property so it will look as
     * native modal dialog.
     * 
     * @param dialog dialog which should be document modal and Mac native looking
     */
    public static void setDocumentModalDialog(JDialog dialog) {
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.getRootPane().putClientProperty("apple.awt.documentModalSheet",
                Boolean.TRUE);
    }
    
}
