package esmska.persistence;

import esmska.Context;
import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.Keyring;
import esmska.data.Queue;
import esmska.data.Queue.Events;
import esmska.data.SMS;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class providing continuous saving of user data to disk
 *
 * @author ripper
 */
public class ContinuousSaveManager {
    private static final Logger logger = Logger.getLogger(ContinuousSaveManager.class.getName());

    private static ActionListener historySaveListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Context.persistenceManager.saveHistory();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not save history", ex);
            }
        }
    };

    private static ActionListener keyringSaveListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Context.persistenceManager.saveKeyring();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not save keyring", ex);
            }
        }
    };

    private static ActionListener contactsSaveListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Context.persistenceManager.saveContacts();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not save contacts", ex);
            }
        }
    };

    private static ValuedListener<Queue.Events, SMS> queueValuedListener = new ValuedListener<Queue.Events, SMS>() {
        @Override
        public void eventOccured(ValuedEvent<Events, SMS> e) {
            switch (e.getEvent()) {
                case QUEUE_CLEARED:
                case SMS_ADDED:
                case SMS_POSITION_CHANGED:
                case SMS_REMOVED:
                    try {
                        Context.persistenceManager.saveQueue();
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Could not save queue", ex);
                    }
            }
        }
    };

    /** Enable automatic saving of history when changed */
    public static void enableHistory() {
        History.getInstance().addActionListener(historySaveListener);
    }

    /** Enable automatic saving of keyring when changed */
    public static void enableKeyring() {
        Keyring.getInstance().addActionListener(keyringSaveListener);
    }

    /** Enable automatic saving of contacts when changed */
    public static void enableContacts() {
        Contacts.getInstance().addActionListener(contactsSaveListener);
    }

    /** Enable automatic saving of queue when changed */
    public static void enableQueue() {
        Queue.getInstance().addValuedListener(queueValuedListener);
    }

    /** Disable automatic saving of history when changed */
    public static void disableHistory() {
        History.getInstance().removeActionListener(historySaveListener);
    }

    /** Disable automatic saving of keyring when changed */
    public static void disableKeyring() {
        Keyring.getInstance().removeActionListener(keyringSaveListener);
    }

    /** Disable automatic saving of contacts when changed */
    public static void disableContacts() {
        Contacts.getInstance().removeActionListener(contactsSaveListener);
    }

    /** Disable automatic saving of queue when changed */
    public static void disableQueue() {
        Queue.getInstance().removeValuedListener(queueValuedListener);
    }
}
