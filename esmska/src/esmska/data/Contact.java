/*
 * Contact.java
 *
 * Created on 21. červenec 2007, 0:57
 */

package esmska.data;

import java.beans.*;
import java.text.Collator;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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

    /** Create new contact with properties copied from provided contact */
    public Contact(Contact c) {
        this(c.getName(), c.getNumber(), c.getOperator());
    }

    /** Create new contact. For detailed parameters restrictions see individual setter methods. */
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
    /** Get contact name. Never null. */
    public String getName() {
        return this.name;
    }

    /** Get valid full phone number including the country code (starting with "+")
     or empty string. Never null. */
    public String getNumber() {
        return this.number;
    }

    /** Get operator. Never null. */
    public String getOperator() {
        return this.operator;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    /** Set contact name.
     * @param name contact name. Null value is changed to empty string.
     */
    public void setName(String name) {
        if (name == null) {
            name = "";
        }

        String oldName = this.name;
        this.name = name;
        changeSupport.firePropertyChange("name", oldName, name);
    }

    /** Set full phone number.
     * @param number new contact number. Must be valid (see {@link #isValidNumber})
     * or an empty string. Null value is changed to an empty string.
     */
    public void setNumber(String number) {
        if (number == null) {
            number = "";
        }
        if (number.length() > 0 && !isValidNumber(number)) {
            throw new IllegalArgumentException("Number is not valid: " + number);
        }

        String oldNumber = this.number;
        this.number = number;
        changeSupport.firePropertyChange("number", oldNumber, number);
    }

    /** Set contact operator
     * @param operator new operator. Null value is changed to "unknown" operator.
     */
    public void setOperator(String operator) {
        if (operator == null) {
            operator = Operator.UNKNOWN;
        }

        String oldOperator = this.operator;
        this.operator = operator;
        changeSupport.firePropertyChange("operator", oldOperator, operator);
    }
    // </editor-fold>

    /** Check validity of phone number
     * @return true if number is in form +[0-9]{2,15}, false otherwise
     */
    public static boolean isValidNumber(String number) {
        if (number == null) {
            return false;
        }
        if (!number.startsWith("+")) {
            return false;
        }
        number = number.substring(1); //strip the "+"
        if (number.length() < 2 || number.length() > 15) {
            return false;
        }
        for (Character c : number.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /** Modify (phone) number into anonymous one
     * @param number (phone) number, may be null
     * @return the same string with all the numbers replaced by 'N'
     */
    public static String anonymizeNumber(String number) {
        if (number == null) {
            return number;
        } else {
            return number.replaceAll("\\d", "N");
        }
    }

    @Override
    public int compareTo(Contact c) {
        Collator collator = Collator.getInstance();

        return new CompareToBuilder().append(name, c.name, collator).
                append(number, c.number, collator).
                append(operator, c.operator, collator).toComparison();
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

        return new EqualsBuilder().append(name, c.name).append(number, c.number).
                append(operator, c.operator).isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(337, 139).append(name).append(number).
                append(operator).toHashCode();
    }
    
}
