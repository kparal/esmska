/*
 * Contact.java
 *
 * Created on 21. červenec 2007, 0:57
 */

package esmska.data;

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
    
    public Contact() {
        this(null,null,null);
    }
    
    public Contact(String name, String number, String operator) {
        this.name = name;
        this.number = number;
        this.operator = operator;
    }
    
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    public String getName() {
        return this.name;
    }
    
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
        changeSupport.firePropertyChange("name", oldName, name);
    }
    
    public void setNumber(String number) {
        String oldNumber = this.number;
        this.number = number;
        changeSupport.firePropertyChange("number", oldNumber, number);
    }
    
    public void setOperator(String operator) {
        String oldOperator = this.operator;
        this.operator = operator;
        changeSupport.firePropertyChange("operator", oldOperator, operator);
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
