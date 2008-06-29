/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.persistence.PersistenceManager;
import esmska.utils.Nullator;
import java.util.Locale;

/** Class for updating from older to newer versions of the program.
 * Makes the needed changes when user updates his version.
 * @author ripper
 */
public class LegacyUpdater {

    /** Checks if some update is needed to be done a executes it if neccessary */
    public static void update() {
        String version = PersistenceManager.getConfig().getVersion();
        if (version == null) {
            return;
        }
        if (Nullator.isEqual(version, Config.getLatestVersion())) { //already updated
            return;
        }

        //changes to 0.8.0
        if (UpdateChecker.compareVersions(version, "0.8.0") < 0) {
            //set country prefix from locale
            if (Nullator.isEmpty(PersistenceManager.getConfig().getCountryPrefix())) {
                PersistenceManager.getConfig().setCountryPrefix(
                        CountryPrefix.getCountryPrefix(Locale.getDefault().getCountry()));
            }
        }

    }
}
