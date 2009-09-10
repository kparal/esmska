/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.persistence.PersistenceManager;
import esmska.utils.RuntimeUtils;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/** Class for updating from older to newer versions of the program.
 * Makes the needed changes when user updates his version.
 * @author ripper
 */
public class LegacyUpdater {

    private static final Logger logger = Logger.getLogger(LegacyUpdater.class.getName());

    /** Checks if some update is needed to be done a executes it if neccessary */
    public static void update() {
        String version = Config.getInstance().getVersion();
        if (version == null) {
            return;
        }
        if (ObjectUtils.equals(version, Config.getLatestVersion())) { //already updated
            return;
        }
        logger.fine("Updating from legacy version " + version + " to current version " +
                Config.getLatestVersion());

        //changes to 0.8.0
        if (Config.compareProgramVersions(version, "0.8.0") < 0) {
            //set country prefix from locale
            if (StringUtils.isEmpty(Config.getInstance().getCountryPrefix())) {
                Config.getInstance().setCountryPrefix(
                        CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            }
        }

        //changes to 0.17.0
        if (Config.compareProgramVersions(version, "0.17.0") < 0) {
            //configuration directory has moved on Mac OS X
            if (RuntimeUtils.isMac()) {
                try {
                    File oldConfigDir = new File(System.getProperty("user.home") + "/Library/Application Support/esmska");
                    File newConfigDir = PersistenceManager.getConfigDir();
                    if (oldConfigDir.exists() && !oldConfigDir.equals(newConfigDir)) {
                        logger.info("Updating config directory - moving from '" +
                                oldConfigDir.getAbsolutePath() + "' to '" +
                                newConfigDir.getAbsolutePath() + "'");
                        //delete newConfigDir, because there are some temporary files created
                        //by the current program run
                        FileUtils.deleteQuietly(newConfigDir);
                        FileUtils.moveDirectory(oldConfigDir, newConfigDir);
                        logger.info("Update of config dir finished, the application will now exit. " +
                                "Please start it again.");
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Update of config dir failed", ex);
                }
            }
        }
    }
}
