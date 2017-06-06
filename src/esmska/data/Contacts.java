package esmska.data;

import esmska.data.event.ActionEventSupport;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
<<<<<<< HEAD
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Class managing all program contacts
 *
 * @author ripper
 */
public class Contacts {

    /**
     * new contact added
     */
    public static final int ACTION_ADD_CONTACT = 0;
    /**
     * existing contact removed
     */
    public static final int ACTION_REMOVE_CONTACT = 1;
    /**
     * all contacts removed
     */
    public static final int ACTION_CLEAR_CONTACTS = 2;
    /**
     * properties of some contact changed
     */
    public static final int ACTION_CHANGE_CONTACT = 3;

    /**
     * shared instance
     */
    private static final Contacts instance = new Contacts();
    private static final Logger logger = Logger.getLogger(Contacts.class.getName());

    private final SortedSet<Contact> contacts = Collections.synchronizedSortedSet(new TreeSet<Contact>());
    private ContactChangeListener contactChangeListener = new ContactChangeListener();

    private static final Map<String, Integer> groupMap = new TreeMap<String, Integer>();
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    private static boolean changeGroup = false;

    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /**
     * Disabled contructor
     */
    private Contacts() {
    }

    /**
     * Get shared instance
     */
    public static Contacts getInstance() {
        return instance;
    }

    public static Map<String, Integer> getMap() {
        return groupMap;
    }

    public static boolean isChangeGroup() {
        return changeGroup;
    }

    public static void setChangeGroup(boolean changeGroup) {
        Contacts.changeGroup = changeGroup;
    }

    /**
     * Get unmodifiable collection of all contacts sorted by name
     */
    public SortedSet<Contact> getAll() {
        return Collections.unmodifiableSortedSet(contacts);
    }

    /**
     * Add new contact
     *
     * @param contact new contact, not null
     * @return See {@link Collection#add}
     */
    public boolean add(Contact contact) {
        if (contact == null) {

            throw new IllegalArgumentException("contact");
        }
        logger.fine("Adding new contact: " + contact);
        boolean added = false;

        synchronized (contacts) {
            added = contacts.add(contact);
            addGroup(contact.getGroup());
            if (added) {
                contact.addPropertyChangeListener(contactChangeListener);
            }
        }

        if (added) {
            actionSupport.fireActionPerformed(ACTION_ADD_CONTACT, null);
        }
        return added;
    }

    /**
     * Add new contacts
     *
     * @param contacts collection of contacts, not null
     * @return See {@link Collection#addAll}
     */
    public boolean addAll(Collection<Contact> contacts) {
        if (contacts == null) {
            throw new IllegalArgumentException("contacts");
        }
        logger.fine("Adding " + contacts.size() + " contacts: " + contacts);
        boolean changed = false;

        synchronized (this.contacts) {
            for (Contact contact : contacts) {
                addGroup(contact.getGroup());
                if (!this.contacts.contains(contact)) {
                    contact.addPropertyChangeListener(contactChangeListener);
                }
            }
            changed = this.contacts.addAll(contacts);
        }

        if (changed) {
            actionSupport.fireActionPerformed(ACTION_ADD_CONTACT, null);
        }
        return changed;
    }

    /**
     * Remove existing contact
     *
     * @param contact contact to be removed, not null
     * @return See {@link Collection#remove}
     */
    public boolean remove(Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact");
        }
        logger.fine("Removing contact: " + contact);
        boolean removed = false;

        synchronized (contacts) {
            removed = contacts.remove(contact);
            removeGroup(contact.getGroup());
            if (removed) {
                contact.removePropertyChangeListener(contactChangeListener);
            }
        }

        if (removed) {
            actionSupport.fireActionPerformed(ACTION_REMOVE_CONTACT, null);
        }
        return removed;
    }

    /**
     * Remove existing contacts
     *
     * @param contacts collection of contacts to be removed, not null
     * @return See {@link Collection#removeAll}
     */
    public boolean removeAll(Collection<Contact> contacts) {
        if (contacts == null) {
            throw new IllegalArgumentException("contacts");
        }
        logger.fine("Removing " + contacts.size() + " contacts: " + contacts);
        boolean changed = false;

        synchronized (this.contacts) {
            for (Contact contact : contacts) {
                removeGroup(contact.getGroup());
                if (this.contacts.contains(contact)) {
                    contact.removePropertyChangeListener(contactChangeListener);
                }
            }
            changed = this.contacts.removeAll(contacts);
        }

        if (changed) {
            actionSupport.fireActionPerformed(ACTION_REMOVE_CONTACT, null);
        }
        return changed;
    }

    /**
     * Remove all contacts
     */
    public void clear() {
        logger.fine("Removing all contacts");

        synchronized (contacts) {
            for (Contact contact : contacts) {
                contact.removePropertyChangeListener(contactChangeListener);
            }
            contacts.clear();
        }

        actionSupport.fireActionPerformed(ACTION_CLEAR_CONTACTS, null);
    }

    /**
     * Search for an existing contact
     *
     * @param contact contact to be searched, not null
     * @return See {@link Collection#contains}
     */
    public boolean contains(Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact");
        }
        return contacts.contains(contact);
    }

    /**
     * Return number of contacts
     *
     * @return See {@link Collection#size}
     */
    public int size() {
        return contacts.size();
    }

    /**
     * Return if there are no contacts
     *
     * @return See {@link Collection#isEmpty}
     */
    public boolean isEmpty() {
        return contacts.isEmpty();
    }

    public void edtitGroup(String oldGroup, String newGroup) {

        if (!oldGroup.equals(newGroup)) {
            addGroup(newGroup);
            removeGroup(oldGroup);
        }
    }

    private void addGroup(String group) {
        if (!group.equals("")) {
            changeGroup = true;
            Integer value = (Integer) groupMap.get(group);
            if (value == null) {
                groupMap.put(group, 1);
            } else {
                groupMap.replace(group, value + 1);
            }
        }
    }

    private void removeGroup(String group) {
        if (!group.equals("")) {
            changeGroup = true;
            Integer value = (Integer) groupMap.get(group) - 1;
            if (value == 0) {
                groupMap.remove(group);
            } else {
                groupMap.replace(group, value);
            }
        }
    }

    /**
     * Listener for changes in individual contacts notifying this class'
     * listeners that some contact's properties have changed.
     */
    private class ContactChangeListener implements PropertyChangeListener {

=======
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/** Class managing all program contacts
 * @author ripper
 */
public class Contacts {

    /** new contact added */
    public static final int ACTION_ADD_CONTACT = 0;
    /** existing contact removed */
    public static final int ACTION_REMOVE_CONTACT = 1;
    /** all contacts removed */
    public static final int ACTION_CLEAR_CONTACTS = 2;
    /** properties of some contact changed */
    public static final int ACTION_CHANGE_CONTACT = 3;

    /** shared instance */
    private static final Contacts instance = new Contacts();
    private static final Logger logger = Logger.getLogger(Contacts.class.getName());
    private final SortedSet<Contact> contacts = Collections.synchronizedSortedSet(new TreeSet<Contact>());
    private ContactChangeListener contactChangeListener = new ContactChangeListener();

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** Disabled contructor */
    private Contacts() {
    }

    /** Get shared instance */
    public static Contacts getInstance() {
        return instance;
    }

    /** Get unmodifiable collection of all contacts sorted by name */
    public SortedSet<Contact> getAll() {
        return Collections.unmodifiableSortedSet(contacts);
    }

    /** Add new contact
     * @param contact new contact, not null
     * @return See {@link Collection#add}
     */
    public boolean add(Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact");
        }
        logger.fine("Adding new contact: "+ contact);
        boolean added = false;

        synchronized(contacts) {
            added = contacts.add(contact);
            if (added) {
                contact.addPropertyChangeListener(contactChangeListener);
            }
        }

        if (added) {
            actionSupport.fireActionPerformed(ACTION_ADD_CONTACT, null);
        }
        return added;
    }

    /** Add new contacts
     * @param contacts collection of contacts, not null
     * @return See {@link Collection#addAll}
     */
    public boolean addAll(Collection<Contact> contacts) {
        if (contacts == null) {
            throw new IllegalArgumentException("contacts");
        }
        logger.fine("Adding " + contacts.size() + " contacts: " + contacts);
        boolean changed = false;

        synchronized(this.contacts) {
            for (Contact contact : contacts) {
                if (!this.contacts.contains(contact)) {
                    contact.addPropertyChangeListener(contactChangeListener);
                }
            }
            changed = this.contacts.addAll(contacts);
        }

        if (changed) {
            actionSupport.fireActionPerformed(ACTION_ADD_CONTACT, null);
        }
        return changed;
    }

    /** Remove existing contact
     * @param contact contact to be removed, not null
     * @return See {@link Collection#remove}
     */
    public boolean remove(Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact");
        }
        logger.fine("Removing contact: " + contact);
        boolean removed = false;

        synchronized(contacts) {
            removed = contacts.remove(contact);
            if (removed) {
                contact.removePropertyChangeListener(contactChangeListener);
            }
        }

        if (removed) {
            actionSupport.fireActionPerformed(ACTION_REMOVE_CONTACT, null);
        }
        return removed;
    }

    /** Remove existing contacts
     * @param contacts collection of contacts to be removed, not null
     * @return See {@link Collection#removeAll}
     */
    public boolean removeAll(Collection<Contact> contacts) {
        if (contacts == null) {
            throw new IllegalArgumentException("contacts");
        }
        logger.fine("Removing " + contacts.size() + " contacts: " + contacts);
        boolean changed = false;

        synchronized(this.contacts) {
            for (Contact contact : contacts) {
                if (this.contacts.contains(contact)) {
                    contact.removePropertyChangeListener(contactChangeListener);
                }
            }
            changed = this.contacts.removeAll(contacts);
        }

        if (changed) {
            actionSupport.fireActionPerformed(ACTION_REMOVE_CONTACT, null);
        }
        return changed;
    }

    /** Remove all contacts */
    public void clear() {
        logger.fine("Removing all contacts");

        synchronized(contacts) {
            for (Contact contact : contacts) {
                contact.removePropertyChangeListener(contactChangeListener);
            }
            contacts.clear();
        }

        actionSupport.fireActionPerformed(ACTION_CLEAR_CONTACTS, null);
    }

    /** Search for an existing contact
     * @param contact contact to be searched, not null
     * @return See {@link Collection#contains}
     */
    public boolean contains(Contact contact) {
        if (contact == null) {
            throw new IllegalArgumentException("contact");
        }
        return contacts.contains(contact);
    }

    /** Return number of contacts
     * @return See {@link Collection#size}
     */
    public int size() {
        return contacts.size();
    }

    /** Return if there are no contacts
     * @return See {@link Collection#isEmpty}
     */
    public boolean isEmpty() {
        return contacts.isEmpty();
    }

    /** Listener for changes in individual contacts notifying this class'
     * listeners that some contact's properties have changed.
     */
    private class ContactChangeListener implements PropertyChangeListener {
>>>>>>> origin/work
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            actionSupport.fireActionPerformed(ACTION_CHANGE_CONTACT, null);
        }
    }
}
