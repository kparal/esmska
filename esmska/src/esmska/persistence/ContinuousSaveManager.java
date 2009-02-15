/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.persistence;

import esmska.data.Contacts;
import esmska.data.History;
import esmska.data.Keyring;
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
}
