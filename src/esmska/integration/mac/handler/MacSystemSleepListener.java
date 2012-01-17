package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent.SystemSleepEvent;
import com.apple.eawt.SystemSleepListener;
import esmska.data.Queue;
import java.util.logging.Logger;

/**
 * System sleep listener. It deactivates queueu when system is going to sleep
 * and reactivates it when system awake. Deactivation and reactivation happens
 * only if quueue was active before those events happens.
 *
 * @author Marian Bouček
 */
public class MacSystemSleepListener implements SystemSleepListener {

    private static final Logger logger = Logger.getLogger(MacSystemSleepListener.class.getName());
    private transient boolean queuePausedBefore;

    @Override
    public void systemAboutToSleep(SystemSleepEvent sse) {
        logger.fine("System is about to sleep.");

        // pause queue only if is currently active
        queuePausedBefore = Queue.getInstance().isPaused();
        if (!queuePausedBefore) {
            Queue.getInstance().setPaused(true);
        }
    }

    @Override
    public void systemAwoke(SystemSleepEvent sse) {
        logger.fine("System awoke.");

        // unpause queue only if it wasn't paused before deactivating session
        if (!queuePausedBefore) {
            Queue.getInstance().setPaused(false);
        }
    }
}
