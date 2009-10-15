/*
 * Config.java
 *
 * Created on 19. červenec 2007, 20:56
 */
package esmska.data;

import esmska.gui.ThemeManager;
import esmska.utils.AlphanumComparator;
import java.awt.Dimension;
import java.beans.*;
import java.io.Serializable;
import java.util.logging.Logger;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/** Config properties of the whole program
 * @author ripper
 */
public class Config extends Object implements Serializable {

    /** How the update checks should be performed */
    public static enum CheckUpdatePolicy {
        /** never check for anything */
        CHECK_NONE,
        /** check only for program updates */
        CHECK_PROGRAM,
        /** check only for gateway updates */
        CHECK_GATEWAYS,
        /** check for program and gateway updates */
        CHECK_ALL
    }

    /** system-wide config */
    private static GlobalConfig globalConfig = GlobalConfig.getInstance();
    /** shared instance */
    private static Config instance = new Config();
    
    private static final String LATEST_VERSION = "0.17.0.99beta";
    private static final Logger logger = Logger.getLogger(Config.class.getName());

    private String version = "";
    private String senderName = "";
    private String senderNumber = "";
    private boolean useSenderID = false;
    private boolean forgetLayout = false;
    private Dimension mainDimension;
    private Integer horizontalSplitPaneLocation;
    private Integer verticalSplitPaneLocation;
    private ThemeManager.LAF lookAndFeel = ThemeManager.LAF.SUBSTANCE;
    private String lafJGoodiesTheme = "Experience Blue";
    private String lafSubstanceSkin = "Business Black Steel";
    private boolean removeAccents = true;
    private CheckUpdatePolicy checkUpdatePolicy = globalConfig.checkUpdatePolicy;
    private boolean checkForUnstableUpdates = false;
    private boolean startCentered = false;
    private boolean toolbarVisible = true;
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
    private boolean demandDeliveryReport = false;
    private boolean showAdvancedSettings = false;
    private boolean debugMode = false;

    /** Get shared instance */
    public static Config getInstance() {
        return instance;
    }

    /** Set shared instance */
    public static void setSharedInstance(Config config) {
        Config.instance = config;
    }
    
    /** Get latest program version */
    public static String getLatestVersion() {
        return LATEST_VERSION;
    }

    /** Whether the current program version is stable or unstable */
    public static boolean isStableVersion() {
        return !LATEST_VERSION.contains("beta");
    }

    /** Whether the program is run as Java WebStart or not */
    public static boolean isRunAsWebStart() {
        //property esmska.isWebstart is set in JNLP file
        return System.getProperty("esmska.isWebstart") != null;
    }

    /** Compares two program versions. Handles if some of them is marked as beta.
     * @param version1 first version. Null means lowest possible version.
     * @param version2 second version. Null means lowest possible version.
     * @return positive number if version1 > version2, zero if version1 == version2,
     *         negative number otherwise
     */
    public static int compareProgramVersions(String version1, String version2) {
        if (version1 == null) {
            return (version2 == null ? 0 : -1);
        }

        String v1 = version1;
        String v2 = version2;

        //handle beta versions
        boolean beta1 = version1.toLowerCase().contains("beta");
        boolean beta2 = version2.toLowerCase().contains("beta");
        if (beta1) {
            v1 = version1.substring(0, version1.toLowerCase().indexOf("beta")).trim();
        }
        if (beta2) {
            v2 = version2.substring(0, version2.toLowerCase().indexOf("beta")).trim();
        }

        AlphanumComparator comparator = new AlphanumComparator();
        if (beta1 && beta2) {
            return comparator.compare(v1, v2) == 0 ? version1.compareTo(version2) :
                comparator.compare(v1, v2);
        } else if (beta1) {
            return (comparator.compare(v1, v2) == 0 ? -1 :
                comparator.compare(v1, v2));
        } else if (beta2) {
            return (comparator.compare(v1, v2) == 0 ? 1 :
                comparator.compare(v1, v2));
        } else {
            return comparator.compare(v1, v2);
        }
    }

    /** Return whether this is the first program run (no config existed before) */
    public boolean isFirstRun() {
        //on first run version is empty
        return StringUtils.isEmpty(version);
    }

    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this) {
        @Override
        public void firePropertyChange(PropertyChangeEvent evt) {
            //do nothing if value not changed - the super method will check it anyway
            //but this way we don't have to log it
            if (ObjectUtils.equals(evt.getOldValue(), evt.getNewValue())) {
                return;
            }
            //ensure privacy on sensitive values
            Object newValue = evt.getNewValue();
            Object oldValue = evt.getOldValue();
            if (ObjectUtils.equals("senderNumber",evt.getPropertyName())) {
                newValue = Contact.anonymizeNumber((String)newValue);
                oldValue = Contact.anonymizeNumber((String)oldValue);
            }
            //log change
            logger.config("Config changed - property: " + evt.getPropertyName() +
                    ", old: " + oldValue + ", new: " + newValue);
            //fire change
            super.firePropertyChange(evt);
        }
    };

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    public String getSenderName() {
        return senderName;
    }

    public String getSenderNumber() {
        return senderNumber;
    }

    public boolean isUseSenderID() {
        return this.useSenderID;
    }

    public boolean isForgetLayout() {
        return this.forgetLayout;
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

    public ThemeManager.LAF getLookAndFeel() {
        return this.lookAndFeel;
    }

    public String getLafJGoodiesTheme() {
        return this.lafJGoodiesTheme;
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

    /**
     * @return never null
     */
    public CheckUpdatePolicy getCheckUpdatePolicy() {
        return checkUpdatePolicy;
    }

    public boolean isCheckForUnstableUpdates() {
        return checkForUnstableUpdates;
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
    
    public boolean isDemandDeliveryReport() {
        return demandDeliveryReport;
    }
    
    public boolean isShowAdvancedSettings() {
        return showAdvancedSettings;
    }

    public boolean isDebugMode() {
        return debugMode;
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

    public void setForgetLayout(boolean forgetLayout) {
        boolean oldForgetLayout = this.forgetLayout;
        this.forgetLayout = forgetLayout;
        changeSupport.firePropertyChange("forgetLayout", oldForgetLayout, forgetLayout);
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

    /** Set current look and feel.
     * 
     * @param lookAndFeel current look and feel. May not be null.
     */
    public void setLookAndFeel(ThemeManager.LAF lookAndFeel) {
        if (lookAndFeel == null) {
            throw new IllegalArgumentException("lookAndFeel may not be null");
        }
        ThemeManager.LAF oldLookAndFeel = this.lookAndFeel;
        this.lookAndFeel = lookAndFeel;
        changeSupport.firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel);
    }

    public void setLafJGoodiesTheme(String lafJGoodiesTheme) {
        String oldLafJGoodiesTheme = this.lafJGoodiesTheme;
        this.lafJGoodiesTheme = lafJGoodiesTheme;
        changeSupport.firePropertyChange("lafJGoodiesTheme", oldLafJGoodiesTheme, lafJGoodiesTheme);
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

    /** Set check update policy
     * @param checkUpdatePolicy null is converted to default value
     */
    public void setCheckUpdatePolicy(CheckUpdatePolicy checkUpdatePolicy) {
        if (checkUpdatePolicy == null) {
            checkUpdatePolicy = CheckUpdatePolicy.CHECK_ALL;
        }
        CheckUpdatePolicy old = this.checkUpdatePolicy;
        this.checkUpdatePolicy = checkUpdatePolicy;
        changeSupport.firePropertyChange("checkUpdatePolicy", old, checkUpdatePolicy);
    }

    /** Set if should check for unstable versions. If currently using unstable
     version this is always set to true, regardless of the input. */
    public void setCheckForUnstableUpdates(boolean checkForUnstableUpdates) {
        if (!isStableVersion()) {
            checkForUnstableUpdates = true;
        }
        boolean old = this.checkForUnstableUpdates;
        this.checkForUnstableUpdates = checkForUnstableUpdates;
        changeSupport.firePropertyChange("checkForUnstableUpdates", old, checkForUnstableUpdates);
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
    
    public void setDemandDeliveryReport(boolean demandDeliveryReport) {
        boolean oldDemandDeliveryReport = this.demandDeliveryReport;
        this.demandDeliveryReport = demandDeliveryReport;
        changeSupport.firePropertyChange("demandDeliveryReport", oldDemandDeliveryReport, demandDeliveryReport);
    }
    
    public void setShowAdvancedSettings(boolean showAdvancedSettings) {
        boolean old = this.showAdvancedSettings;
        this.showAdvancedSettings = showAdvancedSettings;
        changeSupport.firePropertyChange("showAdvancedSettings", old, showAdvancedSettings);
    }

    public void setDebugMode(boolean debugMode) {
        boolean old = this.debugMode;
        this.debugMode = debugMode;
        changeSupport.firePropertyChange("debugMode", old, debugMode);
    }
    // </editor-fold>

    /** Class representing system-wide config. This holds defaults used in
     * Config class. Only changes to GlobalConfig applied before Config class
     * instantiation are reflected.
     */
    public static class GlobalConfig {
        private static final GlobalConfig instance = new GlobalConfig();

        private CheckUpdatePolicy checkUpdatePolicy = CheckUpdatePolicy.CHECK_ALL;

        private GlobalConfig() {
        }

        /** get shared instance */
        public static GlobalConfig getInstance() {
            return instance;
        }

        /** @see Config#setCheckUpdatePolicy */
        public void setCheckUpdatePolicy(CheckUpdatePolicy checkUpdatePolicy) {
            if (checkUpdatePolicy == null) {
                checkUpdatePolicy = CheckUpdatePolicy.CHECK_ALL;
            }
            this.checkUpdatePolicy = checkUpdatePolicy;
        }

    }

}
