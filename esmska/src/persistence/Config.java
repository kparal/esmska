/*
 * Config.java
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
public class Config extends Object implements Serializable {
    public static String LATEST_VERSION = "0.4.0";
    
    private String version = LATEST_VERSION;
    private boolean rememberQueue = true;
    private String senderName = "";
    private String senderNumber = "";
    private boolean useSenderID = false;
    private boolean rememberLayout = true;
    private Dimension mainDimension;
    private Integer horizontalSplitPaneLocation;
    private Integer verticalSplitPaneLocation;
    private String lookAndFeel = ThemeManager.LAF_SUBSTANCE;
    private boolean lafWindowDecorated = true;
    private String lafJGoodiesTheme = "Experience Blue";
    private String lafSubstanceSkin = "Sahara";
    
    private PropertyChangeSupport propertySupport;
    
    public Config() {
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

    /**
     * Getter for property lafJGoodiesTheme.
     * @return Value of property lafJGoodiesTheme.
     */
    public String getLafJGoodiesTheme() {
        return this.lafJGoodiesTheme;
    }

    /**
     * Setter for property lafJGoodiesTheme.
     * @param lafJGoodiesTheme New value of property lafJGoodiesTheme.
     */
    public void setLafJGoodiesTheme(String lafJGoodiesTheme) {
        String oldLafJGoodiesTheme = this.lafJGoodiesTheme;
        this.lafJGoodiesTheme = lafJGoodiesTheme;
        propertySupport.firePropertyChange ("lafJGoodiesTheme", oldLafJGoodiesTheme, lafJGoodiesTheme);
    }

    /**
     * Getter for property lafWindowDecorated.
     * @return Value of property lafWindowDecorated.
     */
    public boolean isLafWindowDecorated() {
        return this.lafWindowDecorated;
    }

    /**
     * Setter for property lafWindowDecorated.
     * @param lafWindowDecorated New value of property lafWindowDecorated.
     */
    public void setLafWindowDecorated(boolean lafWindowDecorated) {
        boolean oldLafWindowDecorated = this.lafWindowDecorated;
        this.lafWindowDecorated = lafWindowDecorated;
        propertySupport.firePropertyChange ("lafWindowDecorated", new Boolean (oldLafWindowDecorated), new Boolean (lafWindowDecorated));
    }

    /**
     * Getter for property lafSubstanceSkin.
     * @return Value of property lafSubstanceSkin.
     */
    public String getLafSubstanceSkin() {
        return this.lafSubstanceSkin;
    }

    /**
     * Setter for property lafSubstanceSkin.
     * @param lafSubstanceSkin New value of property lafSubstanceSkin.
     */
    public void setLafSubstanceSkin(String lafSubstanceSkin) {
        String oldLafSubstanceSkin = this.lafSubstanceSkin;
        this.lafSubstanceSkin = lafSubstanceSkin;
        propertySupport.firePropertyChange ("lafSubstanceSkin", oldLafSubstanceSkin, lafSubstanceSkin);
    }

    /**
     * Getter for property version.
     * @return Value of property version.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Setter for property version.
     * @param version New value of property version.
     */
    public void setVersion(String version) {
        String oldVersion = this.version;
        this.version = version;
        propertySupport.firePropertyChange ("version", oldVersion, version);
    }

}
