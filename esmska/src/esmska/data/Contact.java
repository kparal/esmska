/*
 * Contact.java
 *
 * Created on 21. červenec 2007, 0:57
 */

package esmska.data;

import esmska.utils.Nullator;
import java.beans.*;
import java.text.Collator;

/** SMS Contact entity
 * @author ripper
 */
public class Contact extends Object implements Comparable<Contact> {
    
    private String name;
    /** full phone number including the country code (starting with "+") */
    private String number;
    private String operator;
    
    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>

    /** Create an empty contact */
    public Contact() {
        this(null,null,null);
    }

    /** Create new contact with properties copied from provided contact */
    public Contact(Contact c) {
        this(c.getName(), c.getNumber(), c.getOperator());
    }

    /** Create new contact with all properties (may be null) */
    public Contact(String name, String number, String operator) {
        setName(name);
        setNumber(number);
        setOperator(operator);
    }

    /** Copy all contact properties from provided contact to current contact */
    public void copyFrom(Contact c) {
        setName(c.getName());
        setNumber(c.getNumber());
        setOperator(c.getOperator());
    }
    
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    public String getName() {
        return this.name;
    }

    /** Get full phone number including the country code (starting with "+") */
    public String getNumber() {
        return this.number;
    }
    
    public String getOperator() {
        return this.operator;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        if (!Nullator.isEqual(oldName, name)) {
            changeSupport.firePropertyChange("name", oldName, name);
        }
    }

    /** Set full phone number including the country code (starting with "+")
     * @param number new contact number. Must start with "+". May be null.
     */
    public void setNumber(String number) {
        if (number != null && !number.startsWith("+")) {
            throw new IllegalArgumentException("Number does not start with '+': " + number);
        }
        String oldNumber = this.number;
        this.number = number;
        if (!Nullator.isEqual(oldNumber, number)) {
            changeSupport.firePropertyChange("number", oldNumber, number);
        }
    }
    
    public void setOperator(String operator) {
        String oldOperator = this.operator;
        this.operator = operator;
        if (!Nullator.isEqual(oldOperator, operator)) {
            changeSupport.firePropertyChange("operator", oldOperator, operator);
        }
    }
    // </editor-fold>
    
    @Override
    public int compareTo(Contact c) {
        int result = 0;
        Collator collator = Collator.getInstance();
        
        //name
        result = collator.compare(this.getName(), c.getName());
        if (result != 0) {
            return result;
        }
        //number
        result = collator.compare(this.getNumber(), c.getNumber());
        if (result != 0) {
            return result;
        }
        //operator
        if (this.getOperator() == null) {
            if (c.getOperator() != null) {
                result = -1;
            }
        } else {
            result = this.getOperator().compareTo(c.getOperator());
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Contact)) {
            return false;
        }
        Contact c = (Contact) obj;
        
        return getName().equals(c.getName()) && getNumber().equals(c.getNumber()) 
                && getOperator().equals(c.getOperator());
    }
    
    @Override
    public int hashCode() {
        return (getName() == null ? 13 : getName().hashCode()) *
                (getNumber() == null ? 23 : getNumber().hashCode()) *
                (getOperator() == null ? 31 : getOperator().hashCode());
    }
    
    
}
