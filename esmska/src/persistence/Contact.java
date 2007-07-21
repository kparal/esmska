/*
 * Contact.java
 *
 * Created on 21. červenec 2007, 0:57
 */

package persistence;

import java.beans.*;
import java.io.Serializable;
import operators.Operator;

/** SMS Contact
 * @author ripper
 */
public class Contact extends Object implements Serializable {
    
    private String name;
    private String number;
    private Operator operator;
    
    private PropertyChangeSupport propertySupport;
    
    public Contact() {
        this(null,null,null);
    }
    
    public Contact(String name, String number, Operator operator) {
        this.name = name;
        this.number = number;
        this.operator = operator;
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    
    /**
     * Getter for property name.
     * @return Value of property name.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * Setter for property name.
     * @param name New value of property name.
     */
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        propertySupport.firePropertyChange("name", oldName, name);
    }
    
    /**
     * Getter for property number.
     * @return Value of property number.
     */
    public String getNumber() {
        return this.number;
    }
    
    /**
     * Setter for property number.
     * @param number New value of property number.
     */
    public void setNumber(String number) {
        String oldNumber = this.number;
        this.number = number;
        propertySupport.firePropertyChange("number", oldNumber, number);
    }
    
    /**
     * Getter for property operator.
     * @return Value of property operator.
     */
    public Operator getOperator() {
        return this.operator;
    }
    
    /**
     * Setter for property operator.
     * @param operator New value of property operator.
     */
    public void setOperator(Operator operator) {
        Operator oldOperator = this.operator;
        this.operator = operator;
        propertySupport.firePropertyChange("operator", oldOperator, operator);
    }
    
}
