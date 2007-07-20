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

/** Config properties of the whole program
 * @author ripper
 */
public class ConfigBean extends Object implements Serializable {
    
    public static final String PROP_SENDER_NAME = "senderName";
    public static final String PROP_SENDER_NUMBER = "senderNumber";
    public static final String PROP_REMEMBER_SETTINGS = "rememberSettings";
    public static final String PROP_SMS_QUEUE = "smsQueue";
    
    private boolean rememberSettings = true;
    private String senderName;
    private String senderNumber;
    private ArrayList<SMS> smsQueue;
    
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
        propertySupport.firePropertyChange(PROP_SENDER_NAME, old, senderName);
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public void setSenderNumber(String senderNumber) {
        String old = this.senderNumber;
        this.senderNumber = senderNumber;
        propertySupport.firePropertyChange(PROP_SENDER_NUMBER, old, senderNumber);
    }

    public boolean isRememberSettings() {
        return rememberSettings;
    }

    public void setRememberSettings(boolean rememberSettings) {
        boolean old = this.rememberSettings;
        this.rememberSettings = rememberSettings;
        propertySupport.firePropertyChange(PROP_REMEMBER_SETTINGS, old, rememberSettings);
    }

    public ArrayList<SMS> getSmsQueue() {
        return smsQueue;
    }

    public void setSmsQueue(ArrayList<SMS> smsQueue) {
        ArrayList<SMS> old = this.smsQueue;
        this.smsQueue = smsQueue;
        propertySupport.firePropertyChange(PROP_SMS_QUEUE, old, smsQueue);
    }

}
