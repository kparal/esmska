/*
 * @(#) IntegrationAdapter.java	May 11, 2008 - 8:49:54 PM 
 */
package esmska.integration;

import esmska.data.Queue;
import esmska.data.Queue.Events;
import esmska.data.SMS;
import esmska.data.event.ValuedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import esmska.utils.OSType;
import esmska.data.event.ValuedListener;

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
                    logger.log(Level.WARNING, "Cannot set up integration for Mac OS X. " +
                            "Was the program compiled without Mac support?", e);
                    instance = new IntegrationAdapter();
                }
                break;
            default:
                // fall back to default implementation
                instance = new IntegrationAdapter();
                break;
        }

        instance.initialize();
        instance.startUp();
        
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

    /** Set some things on start */
    private void startUp() {
        //set sms count on startup
        setSMSCount(Queue.getInstance().size());
        //listen for changes in sms count
        Queue.getInstance().addValuedListener(new ValuedListener<Queue.Events, SMS>() {
            @Override
            public void eventOccured(ValuedEvent<Events, SMS> e) {
                switch (e.getEvent()) {
                    case QUEUE_CLEARED:
                    case SMS_ADDED:
                    case SMS_REMOVED:
                        setSMSCount(Queue.getInstance().size());
                }
            }
        });
    }
}
