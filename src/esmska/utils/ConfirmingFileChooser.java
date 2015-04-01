/*
 * @(#) ConfirmingFileChooser.java
 */
package esmska.utils;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Extended dialog for choosing files. When saving file to an already existing file,
 * a confirm overwrite dialog is shown.
 *
 * @author  Marian Bouček
 */
public class ConfirmingFileChooser extends JFileChooser {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    private static String overwriteOption = l10n.getString("Replace");
    private static String cancelOption = l10n.getString("Cancel");
    private static Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, overwriteOption);
    
    @Override
    public void approveSelection() {
        //confirm overwrite when saving to existing file
        if (getDialogType() == SAVE_DIALOG && fileExists()) {
            boolean overwrite = showConfirmOverwriteDialog();
            if (overwrite) {
                super.approveSelection();
            }
        } else {
            super.approveSelection();
        }
    }
    
    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);
        RuntimeUtils.setDocumentModalDialog(dialog);
        return dialog;
    }
    
    /** Show dialog for confirming overwrite of the file previously selected
     * in the save dialog.
     * 
     * @return true if the file should be overwritten; else otherwise
     */
    private boolean showConfirmOverwriteDialog() {
        String message = MessageFormat.format(l10n.getString("ConfirmingFileChooser.sure_to_replace"),
                getSelectedFile().getName(), getSelectedFile().getParent());
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, overwriteOption);
        
        JDialog confirmDialog = pane.createDialog(getParent(), null);
        RuntimeUtils.setDocumentModalDialog(confirmDialog);
        confirmDialog.setResizable(true);
        confirmDialog.pack();

        confirmDialog.setVisible(true);
        confirmDialog.dispose();

        Object value = pane.getValue();
        return overwriteOption.equals(value);
    }
    
    /** Check if selected file exists. Append correct extension if neccessary. */
    private boolean fileExists() {
        File file = getSelectedFile();
        
        //if file already exists, allow not appending the right extension
        if (file.exists()) {
            return true;
        }
        
    	//if filter does not have extensions, there's nothing to check
    	FileNameExtensionFilter choosedFilter = (FileNameExtensionFilter) getFileFilter();
    	if (choosedFilter.getExtensions().length == 0) {
    		return file.exists();
    	}
    	
        //check if file has correct extension
    	boolean hasExtension = false;
    	String fileName = file.getAbsolutePath();
    	
    	for (String ext : choosedFilter.getExtensions()) {
    		if (fileName.endsWith("." + ext)) {
    			hasExtension = true;
    			break;
    		}
    	}
    	
        //if not, append it
    	if (!hasExtension) {
                file = new File(fileName + "." + choosedFilter.getExtensions()[0]);
    		setSelectedFile(file);
    	}
    	
        //check existence
    	return file.exists();
    }
}
  