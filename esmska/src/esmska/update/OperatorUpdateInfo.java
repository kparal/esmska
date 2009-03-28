/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import esmska.data.Config;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;

/** Class for representation of operator update.
 *
 * @author ripper
 */
public class OperatorUpdateInfo {

    private String name;
    private String version;
    private URL downloadUrl;
    private String minProgramVersion;
    private URL iconUrl;

    /** Constructor.
     *
     * @param name operator name; not null nor empty
     * @param version operator version; not null nor empty
     * @param downloadUrl url where to download operator script; not null nor empty
     * @param minProgramVersion minimal required program version to run operator; not null nor empty
     * @param iconUrl url where to download operator icon; empty string is changed to null
     * @throws java.net.MalformedURLException if some of the urls where not valid
     */
    public OperatorUpdateInfo(String name, String version, String downloadUrl,
            String minProgramVersion, String iconUrl) throws MalformedURLException {
        Validate.notEmpty(name);
        Validate.notEmpty(version);
        Validate.notEmpty(downloadUrl);
        Validate.notEmpty(minProgramVersion);

        this.name = name;
        this.version = version;
        this.downloadUrl = new URL(downloadUrl);
        this.minProgramVersion = minProgramVersion;
        this.iconUrl = StringUtils.isNotEmpty(iconUrl) ? new URL(iconUrl) : null;
    }

    /** operator name, not null nor empty */
    public String getName() {
        return name;
    }

    /** operator version, not null nor empty */
    public String getVersion() {
        return version;
    }

    /** url where to download operator script, not null nor empty */
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    /** minimal required program version to run operator, not null nor empty */
    public String getMinProgramVersion() {
        return minProgramVersion;
    }

    /** url where to download operator icon, may be null */
    public URL getIconUrl() {
        return iconUrl;
    }

    /** Returns whether operator required program version is lower or same as
     * current program version (can be used), or not (can't be used). */
    public boolean canBeUsed() {
        return Config.compareProgramVersions(Config.getLatestVersion(), minProgramVersion) >= 0;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).
                append("version", version).
                append("minProgramVersion", minProgramVersion).
                append("downloadUrl", downloadUrl).append("iconUrl", iconUrl).
                toString();
    }


}
