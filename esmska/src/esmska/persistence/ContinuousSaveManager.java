/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.persistence;

import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.Keyring;
import esmska.data.Queue;
import esmska.data.Queue.Events;
import esmska.data.SMS;
import esmska.utils.ValuedEvent;
import esmska.utils.ValuedListener;
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
                PersistenceManager.getInstance().saveHistory();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Could not save history", ex);
            }
        }
    };

    private static ActionListener keyringSaveListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                PersistenceManager.getInstance().saveKeyring();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not save keyring", ex);
            }
        }
    };

    private static ActionListener contactsSaveListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                PersistenceManager.getInstance().saveContacts();
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
                        PersistenceManager.getInstance().saveQueue();
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Could not save queue", ex);
                    }
            }
        }
    };

    /** Enable automatic saving of history when changed */
    public static void enable(History history) {
        history.addActionListener(historySaveListener);
    }

    /** Enable automatic saving of keyring when changed */
    public static void enable(Keyring keyring) {
        keyring.addActionListener(keyringSaveListener);
    }

    /** Enable automatic saving of contacts when changed */
    public static void enable(Contacts contacts) {
        contacts.addActionListener(contactsSaveListener);
    }

    /** Enable automatic saving of queue when changed */
    public static void enable(Queue queue) {
        queue.addValuedListener(queueValuedListener);
    }

    /** Disable automatic saving of history when changed */
    public static void disable(History history) {
        history.removeActionListener(historySaveListener);
    }

    /** Disable automatic saving of keyring when changed */
    public static void disable(Keyring keyring) {
        keyring.removeActionListener(keyringSaveListener);
    }

    /** Disable automatic saving of contacts when changed */
    public static void disable(Contacts contacts) {
        contacts.removeActionListener(contactsSaveListener);
    }

    /** Disable automatic saving of queue when changed */
    public static void disable(Queue queue) {
        queue.removeValuedListener(queueValuedListener);
    }
}
