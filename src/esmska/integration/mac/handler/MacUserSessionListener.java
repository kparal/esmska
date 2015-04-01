package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.UserSessionEvent;
import com.apple.eawt.UserSessionListener;
import esmska.data.Queue;
import java.util.logging.Logger;

/**
 * User session listener which supports fast user switching. When user
 * deactivates session by switching to another user, turn off queue. After he
 * cames back, it will activate queue back.
 *
 * <p>The reason of doing this is possible loss of internet connection while
 * switching users. Deactivation and reactivation happens only if quueue was
 * active before those events happens.</p>
 *
 * @author Marian Bouček
 */
public class MacUserSessionListener implements UserSessionListener {

    private static final Logger logger = Logger.getLogger(MacUserSessionListener.class.getName());
    private transient boolean queuePausedBefore;

    @Override
    public void userSessionDeactivated(UserSessionEvent use) {
        logger.fine("User session deactivated.");

        // pause queue only if is currently active
        queuePausedBefore = Queue.getInstance().isPaused();
        if (!queuePausedBefore) {
            Queue.getInstance().setPaused(true);
        }
    }

    @Override
    public void userSessionActivated(UserSessionEvent use) {
        logger.fine("User session activated.");

        // unpause queue only if it wasn't paused before deactivating session
        if (!queuePausedBefore) {
            Queue.getInstance().setPaused(false);
        }
    }
}
