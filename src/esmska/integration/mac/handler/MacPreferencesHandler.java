package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.PreferencesHandler;
import esmska.integration.ActionBean;

/**
 * Preferences action handler.
 *
 * @author Marian Bouček
 */
public class MacPreferencesHandler implements PreferencesHandler {
    private final ActionBean bean;

    public MacPreferencesHandler(ActionBean bean) {
        this.bean = bean;
    }

    @Override
    public void handlePreferences(PreferencesEvent pe) {
        HandlerUtils.performAction(pe, bean.getConfigAction(), "configSelected");
    }
}
