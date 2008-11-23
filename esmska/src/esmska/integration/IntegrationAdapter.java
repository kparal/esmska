/*
 * @(#) IntegrationAdapter.java	May 11, 2008 - 8:49:54 PM 
 */
package esmska.integration;

import java.util.logging.Level;
import java.util.logging.Logger;
import esmska.utils.OSType;

/**
 * Integration adapter. Used to integrate program more closely to specific operating system.
 * 
 * @author  Marian Bouček
 * @version 1.0
 */
public class IntegrationAdapter {

    private static final Logger logger = Logger.getLogger(IntegrationAdapter.class.getName());
    private static IntegrationAdapter instance = null;
    protected ActionBean bean;

    // singleton API ------------------------------------------------------------
    /**
     * Constructor.
     */
    protected IntegrationAdapter() {
        super();
    }

    /**
     * Return instance of singleton.
     * 
     * @return instance
     */
    public static IntegrationAdapter getInstance() {
        if (instance != null) {
            return instance;
        }

        switch (OSType.detect()) {
            case MAC_OS_X:
                try {
                    instance = (IntegrationAdapter) Class.forName(
                            "esmska.integration.MacIntegration").newInstance();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Cannot set up integration for Mac OS X.", e);
                    instance = new IntegrationAdapter();
                }
                break;
            default:
                // fall back to default implementation
                instance = new IntegrationAdapter();
                break;
        }

        instance.initialize();

        return instance;
    }

    /**
     * Initializes adapter.
     */
    protected void initialize() {
    }

    // public interface ---------------------------------------------------------
    /**
     * Set action bean.
     * 
     * @param bean action bean
     */
    public void setActionBean(ActionBean bean) {
        if (bean == null) {
            throw new IllegalArgumentException("Action bean must not be null.");
        }

        this.bean = bean;
    }

    /**
     * Set SMS count. Location where to display is platform specific.
     * 
     * @param count new sms count. Use null to clear text.
     */
    public void setSMSCount(Integer count) {
    }
}
