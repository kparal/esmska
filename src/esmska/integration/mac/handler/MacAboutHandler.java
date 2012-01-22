package esmska.integration.mac.handler;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import esmska.integration.ActionBean;

/**
 * Handler for About action.
 *
 * @author Marian Boucek
 */
public class MacAboutHandler implements AboutHandler {
    private final ActionBean bean;

    public MacAboutHandler(ActionBean bean) {
        this.bean = bean;
    }

    @Override
    public void handleAbout(AboutEvent ae) {
        HandlerUtils.performAction(ae, bean.getAboutAction(), "aboutSelected");
    }
}
