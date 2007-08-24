/*
 * ConfigBean.java
 *
 * Created on 19. červenec 2007, 20:56
 */

package persistence;

import esmska.ThemeManager;
import persistence.SMS;
import java.awt.Dimension;
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
    private boolean rememberLayout = true;
    private Dimension mainDimension;
    private Integer horizontalSplitPaneLocation;
    private Integer verticalSplitPaneLocation;
    private String lookAndFeel = ThemeManager.LAF_SYSTEM;
    
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
    
    /**
     * Getter for property rememberLayout.
     * @return Value of property rememberLayout.
     */
    public boolean isRememberLayout() {
        return this.rememberLayout;
    }
    
    /**
     * Setter for property rememberLayout.
     * @param rememberLayout New value of property rememberLayout.
     */
    public void setRememberLayout(boolean rememberLayout) {
        boolean oldRememberLayout = this.rememberLayout;
        this.rememberLayout = rememberLayout;
        propertySupport.firePropertyChange("rememberLayout", new Boolean(oldRememberLayout), new Boolean(rememberLayout));
    }
    
    /**
     * Getter for property mainDimension.
     * @return Value of property mainDimension.
     */
    public Dimension getMainDimension() {
        return this.mainDimension;
    }
    
    /**
     * Setter for property mainDimension.
     * @param mainDimension New value of property mainDimension.
     */
    public void setMainDimension(Dimension mainDimension) {
        Dimension oldMainDimension = this.mainDimension;
        this.mainDimension = mainDimension;
        propertySupport.firePropertyChange("mainDimension", oldMainDimension, mainDimension);
    }
    
    /**
     * Getter for property horizontalSplitPaneLocation.
     * @return Value of property horizontalSplitPaneLocation.
     */
    public Integer getHorizontalSplitPaneLocation() {
        return this.horizontalSplitPaneLocation;
    }
    
    /**
     * Setter for property horizontalSplitPaneLocation.
     * @param horizontalSplitPaneLocation New value of property horizontalSplitPaneLocation.
     */
    public void setHorizontalSplitPaneLocation(Integer horizontalSplitPaneLocation) {
        Integer oldHorizontalSplitPaneLocation = this.horizontalSplitPaneLocation;
        this.horizontalSplitPaneLocation = horizontalSplitPaneLocation;
        propertySupport.firePropertyChange ("horizontalSplitPaneLocation", oldHorizontalSplitPaneLocation, horizontalSplitPaneLocation);
    }

    /**
     * Getter for property verticalSplitPaneLocation.
     * @return Value of property verticalSplitPaneLocation.
     */
    public Integer getVerticalSplitPaneLocation() {
        return this.verticalSplitPaneLocation;
    }

    /**
     * Setter for property verticalSplitPaneLocation.
     * @param verticalSplitPaneLocation New value of property verticalSplitPaneLocation.
     */
    public void setVerticalSplitPaneLocation(Integer verticalSplitPaneLocation) {
        Integer oldVerticalSplitPaneLocation = this.verticalSplitPaneLocation;
        this.verticalSplitPaneLocation = verticalSplitPaneLocation;
        propertySupport.firePropertyChange ("verticalSplitPaneLocation", oldVerticalSplitPaneLocation, verticalSplitPaneLocation);
    }

    /**
     * Getter for property lookAndFeel.
     * @return Value of property lookAndFeel.
     */
    public String getLookAndFeel() {
        return this.lookAndFeel;
    }

    /**
     * Setter for property lookAndFeel.
     * @param lookAndFeel New value of property lookAndFeel.
     */
    public void setLookAndFeel(String lookAndFeel) {
        String oldLookAndFeel = this.lookAndFeel;
        this.lookAndFeel = lookAndFeel;
        propertySupport.firePropertyChange ("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }
    
}
