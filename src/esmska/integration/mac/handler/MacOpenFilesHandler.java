package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.OpenFilesHandler;
import esmska.Context;
import esmska.gui.ImportFrame;
import java.io.File;

/**
 * Open file handler.
 *
 * @author Marian Bouček
 */
public class MacOpenFilesHandler implements OpenFilesHandler {

    @Override
    public void openFiles(OpenFilesEvent ofe) {
        for (File f : ofe.getFiles()) {
            ImportFrame importFrame = new ImportFrame();
            importFrame.setLocationRelativeTo(Context.mainFrame);
            importFrame.importVCardFile(f.getAbsolutePath());
            importFrame.setVisible(true);
        }
    }
}
