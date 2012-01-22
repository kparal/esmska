package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import esmska.integration.ActionBean;

/**
 * Quit action handler.
 *
 * @author Marian Bouček
 */
public class MacQuitHandler implements QuitHandler {
    private final ActionBean bean;

    public MacQuitHandler(ActionBean bean) {
        this.bean = bean;
    }

    @Override
    public void handleQuitRequestWith(QuitEvent qe, QuitResponse qr) {
        HandlerUtils.performAction(qe, bean.getQuitAction(), "quitSelected");
        qr.performQuit(); // this should not be necessary
    }
}
