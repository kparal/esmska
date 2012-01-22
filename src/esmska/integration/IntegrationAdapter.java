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
import esmska.data.event.ValuedListener;
import esmska.utils.RuntimeUtils;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.apache.commons.lang.StringUtils;

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

        switch (RuntimeUtils.detectOS()) {
            case MAC_OS_X:
                try {
                    instance = (IntegrationAdapter) Class.forName(
                            "esmska.integration.mac.MacIntegration").newInstance();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Cannot set up integration for Mac OS X. " +
                            "Was the program compiled without Mac support?", e);
                    instance = new IntegrationAdapter();
                }
                break;
            case WINDOWS:
                instance = new WindowsIntegration();
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

    /** Returns how the program directory in system directories should be named */
    public String getProgramDirName(String defaultProgramDirName) {
        return defaultProgramDirName;
    }

    /** Get the location of system config directory (not program config directory) */
    public File getConfigDir(File defaultConfigDir) {
        String confDir = System.getenv("XDG_CONFIG_HOME");
        if (StringUtils.isNotEmpty(confDir)) {
            return new File(confDir);
        } else {
            return defaultConfigDir;
        }
    }

    /** Get the location of system data directory (not program data directory) */
    public File getDataDir(File defaultDataDir) {
        String datDir = System.getenv("XDG_DATA_HOME");
        if (StringUtils.isNotEmpty(datDir)) {
            return new File(datDir);
        } else {
            return defaultDataDir;
        }
    }

    /** Get the location of a program log file */
    public File getLogFile(File defaultLogFile) {
        return defaultLogFile;
    }

    /** Inicialize stuff to handle GUI stuff, adjust GUI for the current envirnonment */
    public void activateGUI() {
        return;
    }

    /**
     * <p>Register modal sheet for proper handling. Probably usable only on Mac,
     * other OS doesnt have concept of sheet window.</p>
     *
     * <p>Default implementation does nothing.</p>
     *
     * @param dialog registered dialog
     */
    public void registerModalSheet(JDialog dialog) {
        return;
    }

    /**
     * Is some modal sheet of main window visible?
     *
     * @return in default implementation, it always return <code>false</code>
     */
    public boolean isModalSheetVisible() {
        return false;
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
