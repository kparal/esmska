package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppReOpenedListener;
import esmska.Context;

/**
 * Handler for reopen action. It will happen while application has no visible
 * windows and user clicks on dock icon.
 *
 * @author Marian Bouček
 */
public class MacAppReOpenedListener implements AppReOpenedListener {

    @Override
    public void appReOpened(AppReOpenedEvent aroe) {
        Context.mainFrame.setVisible(true);
    }
}
