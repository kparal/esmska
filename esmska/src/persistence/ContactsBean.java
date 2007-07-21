/*
 * ContactsBean.java
 *
 * Created on 21. červenec 2007, 1:05
 */

package persistence;

import java.beans.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author ripper
 */
public class ContactsBean extends Object implements Serializable {
    
    private ArrayList<Contact> contacts = new ArrayList<Contact>();
    
    private PropertyChangeSupport propertySupport;
    
    public ContactsBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Getter for property contacts.
     * @return Value of property contacts.
     */
    public ArrayList<Contact> getContacts() {
        return this.contacts;
    }
    
    /**
     * Setter for property contacts.
     * @param contacts New value of property contacts.
     */
    public void setContacts(ArrayList<Contact> contacts) {
        ArrayList<Contact> oldContacts = this.contacts;
        this.contacts = contacts;
        propertySupport.firePropertyChange("contacts", oldContacts, contacts);
    }
    
    public void sortContacts() {
        Collections.sort(contacts);
    }
    
}
