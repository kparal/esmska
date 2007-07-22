/*
 * ConfigBean.java
 *
 * Created on 19. červenec 2007, 20:56
 */

package persistence;

import esmska.SMS;
import java.beans.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Config properties of the whole program
 * @author ripper
 */
public class ConfigBean extends Object implements Serializable {
    
    private boolean rememberQueue = true;
    private String senderName = "";
    private String senderNumber = "";
    private List<SMS> smsQueue = Collections.synchronizedList(new ArrayList<SMS>());
    private boolean useSenderID = false;
    
    private PropertyChangeSupport propertySupport;
    
    public ConfigBean() {
        propertySupport = new PropertyChangeSupport(this);
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        String old = this.senderName;
        this.senderName = senderName;
        propertySupport.firePropertyChange("senderName", old, senderName);
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public void setSenderNumber(String senderNumber) {
        String old = this.senderNumber;
        this.senderNumber = senderNumber;
        propertySupport.firePropertyChange("senderNumber", old, senderNumber);
    }
    
    public boolean isRememberQueue() {
        return rememberQueue;
    }
    
    public void setRememberQueue(boolean rememberQueue) {
        boolean old = this.rememberQueue;
        this.rememberQueue = rememberQueue;
        propertySupport.firePropertyChange("rememberQueue", old, rememberQueue);
    }
    
    public List<SMS> getSmsQueue() {
        return smsQueue;
    }
    
    public void setSmsQueue(List<SMS> smsQueue) {
        List<SMS> old = this.smsQueue;
        this.smsQueue = smsQueue;
        propertySupport.firePropertyChange("smsQueue", old, smsQueue);
    }
    
    
    /**
     * Getter for property useSenderID.
     * @return Value of property useSenderID.
     */
    public boolean isUseSenderID() {
        return this.useSenderID;
    }
    
    /**
     * Setter for property useSenderID.
     * @param useSenderID New value of property useSenderID.
     */
    public void setUseSenderID(boolean useSenderID) {
        boolean oldUseSenderID = this.useSenderID;
        this.useSenderID = useSenderID;
        propertySupport.firePropertyChange("useSenderID", new Boolean(oldUseSenderID), new Boolean(useSenderID));
    }
    
}
