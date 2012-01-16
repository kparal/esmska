package esmska.integration.mac.handler;

import com.apple.eawt.AppEvent;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import javax.swing.Action;

/**
 * Utility class for action handlers.
 *
 * @author Marian Bouček
 */
public class HandlerUtils {

    /**
     * Perform action with AppEvent and message. AppEvent is converted to standard
     * ActionEvent object and then it is sent to desired Action.
     *
     * @param ae event
     * @param action action
     * @param message message
     */
    public static void performAction(AppEvent ae, Action action, String message) {
        ActionEvent event = createActionEvent(ae, message);
        action.actionPerformed(event);
    }

    private static ActionEvent createActionEvent(EventObject eo, String message) {
        return new ActionEvent(eo.getSource(), ActionEvent.ACTION_PERFORMED, message);
    }
}
