/*
 * Config.java
 *
 * Created on 19. červenec 2007, 20:56
 */
package esmska.data;

import esmska.ThemeManager;
import java.awt.Dimension;
import java.beans.*;
import java.io.Serializable;

/** Config properties of the whole program
 * @author ripper
 */
public class Config extends Object implements Serializable {

    private static final String LATEST_VERSION = "0.10.0 beta";

    private String version = "";
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
    private boolean removeAccents = true;
    private boolean checkForUpdates = true;
    private boolean startCentered = false;
    private boolean toolbarVisible = false;
    private String countryPrefix = "";
    private String operatorFilter = "";
    private boolean useProxy = false;
    private boolean sameProxy = true;
    private String httpProxy = "";
    private String httpsProxy = "";
    private String socksProxy = "";
    private boolean notificationIconVisible = false;
    private boolean showTips = true;
    private boolean reducedHistory = false;
    private int reducedHistoryCount = 30;
    private boolean startMinimized = false;

    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    public static final String getLatestVersion() {
        return LATEST_VERSION;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderNumber() {
        return senderNumber;
    }

    public boolean isUseSenderID() {
        return this.useSenderID;
    }

    public boolean isRememberLayout() {
        return this.rememberLayout;
    }

    public Dimension getMainDimension() {
        return this.mainDimension;
    }

    public Integer getHorizontalSplitPaneLocation() {
        return this.horizontalSplitPaneLocation;
    }

    public Integer getVerticalSplitPaneLocation() {
        return this.verticalSplitPaneLocation;
    }

    public String getLookAndFeel() {
        return this.lookAndFeel;
    }

    public String getLafJGoodiesTheme() {
        return this.lafJGoodiesTheme;
    }

    public boolean isLafWindowDecorated() {
        return this.lafWindowDecorated;
    }

    public String getLafSubstanceSkin() {
        return this.lafSubstanceSkin;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isRemoveAccents() {
        return this.removeAccents;
    }

    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    public boolean isStartCentered() {
        return startCentered;
    }

    public boolean isToolbarVisible() {
        return toolbarVisible;
    }
    
    public String getCountryPrefix() {
        return countryPrefix;
    }

    public String getOperatorFilter() {
        return operatorFilter;
    }

    public boolean isUseProxy() {
        return useProxy;
    }
    
    public boolean isSameProxy() {
        return sameProxy;
    }
    
    public String getHttpProxy() {
        return httpProxy;
    }

    public String getHttpsProxy() {
        return httpsProxy;
    }

    public String getSocksProxy() {
        return socksProxy;
    }
    
    public boolean isNotificationIconVisible() {
        return notificationIconVisible;
    }

    public boolean isShowTips() {
        return showTips;
    }
    
    public boolean isReducedHistory() {
        return reducedHistory;
    }
    
    public int getReducedHistoryCount() {
        return reducedHistoryCount;
    }
    
    public boolean isStartMinimized() {
        return startMinimized;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    public void setSenderName(String senderName) {
        String old = this.senderName;
        this.senderName = senderName;
        changeSupport.firePropertyChange("senderName", old, senderName);
    }

    public void setSenderNumber(String senderNumber) {
        String old = this.senderNumber;
        this.senderNumber = senderNumber;
        changeSupport.firePropertyChange("senderNumber", old, senderNumber);
    }

    public void setUseSenderID(boolean useSenderID) {
        boolean oldUseSenderID = this.useSenderID;
        this.useSenderID = useSenderID;
        changeSupport.firePropertyChange("useSenderID", oldUseSenderID, useSenderID);
    }

    public void setRememberLayout(boolean rememberLayout) {
        boolean oldRememberLayout = this.rememberLayout;
        this.rememberLayout = rememberLayout;
        changeSupport.firePropertyChange("rememberLayout", oldRememberLayout, rememberLayout);
    }

    public void setMainDimension(Dimension mainDimension) {
        Dimension oldMainDimension = this.mainDimension;
        this.mainDimension = mainDimension;
        changeSupport.firePropertyChange("mainDimension", oldMainDimension, mainDimension);
    }

    public void setHorizontalSplitPaneLocation(Integer horizontalSplitPaneLocation) {
        Integer oldHorizontalSplitPaneLocation = this.horizontalSplitPaneLocation;
        this.horizontalSplitPaneLocation = horizontalSplitPaneLocation;
        changeSupport.firePropertyChange("horizontalSplitPaneLocation", oldHorizontalSplitPaneLocation, horizontalSplitPaneLocation);
    }

    public void setVerticalSplitPaneLocation(Integer verticalSplitPaneLocation) {
        Integer oldVerticalSplitPaneLocation = this.verticalSplitPaneLocation;
        this.verticalSplitPaneLocation = verticalSplitPaneLocation;
        changeSupport.firePropertyChange("verticalSplitPaneLocation", oldVerticalSplitPaneLocation, verticalSplitPaneLocation);
    }

    public void setLookAndFeel(String lookAndFeel) {
        String oldLookAndFeel = this.lookAndFeel;
        this.lookAndFeel = lookAndFeel;
        changeSupport.firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }

    public void setLafJGoodiesTheme(String lafJGoodiesTheme) {
        String oldLafJGoodiesTheme = this.lafJGoodiesTheme;
        this.lafJGoodiesTheme = lafJGoodiesTheme;
        changeSupport.firePropertyChange("lafJGoodiesTheme", oldLafJGoodiesTheme, lafJGoodiesTheme);
    }

    public void setLafWindowDecorated(boolean lafWindowDecorated) {
        boolean oldLafWindowDecorated = this.lafWindowDecorated;
        this.lafWindowDecorated = lafWindowDecorated;
        changeSupport.firePropertyChange("lafWindowDecorated", oldLafWindowDecorated, lafWindowDecorated);
    }

    public void setLafSubstanceSkin(String lafSubstanceSkin) {
        String oldLafSubstanceSkin = this.lafSubstanceSkin;
        this.lafSubstanceSkin = lafSubstanceSkin;
        changeSupport.firePropertyChange("lafSubstanceSkin", oldLafSubstanceSkin, lafSubstanceSkin);
    }

    public void setVersion(String version) {
        String oldVersion = this.version;
        this.version = version;
        changeSupport.firePropertyChange("version", oldVersion, version);
    }

    public void setRemoveAccents(boolean removeAccents) {
        boolean oldRemoveAccents = this.removeAccents;
        this.removeAccents = removeAccents;
        changeSupport.firePropertyChange("removeAccents", oldRemoveAccents, removeAccents);
    }

    public void setCheckForUpdates(boolean checkForUpdates) {
        boolean old = this.checkForUpdates;
        this.checkForUpdates = checkForUpdates;
        changeSupport.firePropertyChange("checkForUpdates", old, checkForUpdates);
    }

    public void setStartCentered(boolean startCentered) {
        boolean old = this.startCentered;
        this.startCentered = startCentered;
        changeSupport.firePropertyChange("startCentered", old, startCentered);
    }

    public void setToolbarVisible(boolean toolbarVisible) {
        boolean old = this.toolbarVisible;
        this.toolbarVisible = toolbarVisible;
        changeSupport.firePropertyChange("toolbarVisible", old, toolbarVisible);
    }
    
    public void setCountryPrefix(String countryPrefix) {
        String old = this.countryPrefix;
        this.countryPrefix = countryPrefix;
        changeSupport.firePropertyChange("countryPrefix", old, countryPrefix);
    }
    
    public void setOperatorFilter(String operatorFilter) {
        String old = this.operatorFilter;
        this.operatorFilter = operatorFilter;
        changeSupport.firePropertyChange("operatorFilter", old, operatorFilter);
    }

    public void setUseProxy(boolean useProxy) {
        boolean old = this.useProxy;
        this.useProxy = useProxy;
        changeSupport.firePropertyChange("useProxy", old, useProxy);
    }
    
    public void setSameProxy(boolean sameProxy) {
        boolean old = this.sameProxy;
        this.sameProxy = sameProxy;
        changeSupport.firePropertyChange("sameProxy", old, sameProxy);
    }
    
    public void setHttpProxy(String httpProxy) {
        String old = this.httpProxy;
        this.httpProxy = httpProxy;
        changeSupport.firePropertyChange("httpProxy", old, httpProxy);
    }

    public void setHttpsProxy(String httpsProxy) {
        String old = this.httpsProxy;
        this.httpsProxy = httpsProxy;
        changeSupport.firePropertyChange("httpsProxy", old, httpsProxy);
    }

    public void setSocksProxy(String socksProxy) {
        String old = this.socksProxy;
        this.socksProxy = socksProxy;
        changeSupport.firePropertyChange("socksProxy", old, socksProxy);
    }
    
    public void setNotificationIconVisible(boolean notificationIconVisible) {
        boolean old = this.notificationIconVisible;
        this.notificationIconVisible = notificationIconVisible;
        changeSupport.firePropertyChange("notificationIconVisible", old, notificationIconVisible);
    }

    public void setShowTips(boolean showTips) {
        boolean old = this.showTips;
        this.showTips = showTips;
        changeSupport.firePropertyChange("showTips", old, showTips);
    }
    
    public void setReducedHistory(boolean reducedHistory) {
        boolean oldReducedHistory = this.reducedHistory;
        this.reducedHistory = reducedHistory;
        changeSupport.firePropertyChange("reducedHistory", oldReducedHistory, reducedHistory);
    }
    
    public void setReducedHistoryCount(int reducedHistoryCount) {
        int oldReducedHistoryCount = this.reducedHistoryCount;
        this.reducedHistoryCount = reducedHistoryCount;
        changeSupport.firePropertyChange("reducedHistoryCount", oldReducedHistoryCount, reducedHistoryCount);
    }
    
    public void setStartMinimized(boolean startMinimized) {
        boolean old = this.startMinimized;
        this.startMinimized = startMinimized;
        changeSupport.firePropertyChange("startMinimized", old, startMinimized);
    }
    // </editor-fold>
}
