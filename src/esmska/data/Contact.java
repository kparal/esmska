package esmska.data;

import java.beans.*;
import java.text.Collator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * SMS Contact entity
 *
 * @author ripper
 */
public class Contact extends Object implements Comparable<Contact> {

    private String name;
    /**
     * full phone number including the country code (starting with "+")
     */
    private String number;
    private String gateway;
    private String group;
    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>

    /**
     * Create new contact with properties copied from provided contact
     */
    public Contact(Contact c) {
        this(c.getName(), c.getNumber(), c.getGateway(), c.getGroup());
    }

    /**
     * Create new contact. For detailed parameters restrictions see individual
     * setter methods.
     */
    public Contact(String name, String number, String gateway, String group) {
        setName(name);
        setNumber(number);
        setGateway(gateway);
        setGroup(group);
    }

    /**
     * Copy all contact properties from provided contact to current contact
     */
    public void copyFrom(Contact c) {
        setName(c.getName());
        setNumber(c.getNumber());
        setGateway(c.getGateway());
        setGroup(c.getGroup());
    }

    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    /**
     * Get contact name. Never null.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get valid full phone number including the country code (starting with
     * "+") or empty string. Never null.
     */
    public String getNumber() {
        return this.number;
    }

    /**
     * Get gateway. Never null.
     */
    public String getGateway() {
        return this.gateway;
    }
    // </editor-fold>

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    /**
     * Set contact name.
     *
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

    /**
     * Set full phone number.
     *
     * @param number new contact number. Must be valid (see
     * {@link #isValidNumber}) or an empty string. Null value is changed to an
     * empty string.
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

    /**
     * Set contact gateway
     *
     * @param gateway new gateway. Null value is changed to "unknown" gateway.
     */
    public void setGateway(String gateway) {
        if (gateway == null) {
            gateway = Gateway.UNKNOWN;
        }

        String oldGateway = this.gateway;
        this.gateway = gateway;
        changeSupport.firePropertyChange("gateway", oldGateway, gateway);
    }
    // </editor-fold>

    /**
     * Check validity of phone number
     *
     * @return true if number is in form +[0-9]{2,15} with valid country prefix,
     * false otherwise
     */
    public static boolean isValidNumber(String number) {
        if (number == null) {
            return false;
        }
        String prefix = CountryPrefix.extractCountryPrefix(number);
        if (prefix == null) {
            return false;
        }
        number = number.substring(prefix.length());
        if (number.length() < 1 || number.length() + prefix.length() > 16) {
            return false;
        }
        for (Character c : number.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Modify (phone) number into anonymous one
     *
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

    /**
     * Try to extract valid number from some local format (like "(1) 222 333")
     * and convert it into international number.
     *
     * @param number number in non-standard format; may be null
     * @return parsed valid (international) number or null
     */
    public static String parseNumber(String number) {
        if (StringUtils.isEmpty(number)) {
            return null;
        }
        boolean international = number.startsWith("+");
        number = number.replaceAll("[^0-9]", "");
        if (!international && StringUtils.isNotEmpty(Config.getInstance().getCountryPrefix())) {
            number = Config.getInstance().getCountryPrefix() + number;
        } else {
            number = "+" + number;
        }

        if (isValidNumber(number)) {
            return number;
        } else {
            return null;
        }
    }

    @Override
    public int compareTo(Contact c) {
        Collator collator = Collator.getInstance();

        return new CompareToBuilder().append(name, c.name, collator).
                append(number, c.number, collator).
                append(gateway, c.gateway, collator).toComparison();
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
                append(gateway, c.gateway).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(337, 139).append(name).append(number).
                append(gateway).toHashCode();
    }

}
