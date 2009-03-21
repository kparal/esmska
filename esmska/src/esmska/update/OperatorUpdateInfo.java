/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.update;

import java.net.MalformedURLException;
import java.net.URL;
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
     * @param name operator name
     * @param version operator version
     * @param downloadUrl url where to download operator script
     * @param minProgramVersion minimal required program version to run operator
     * @param iconUrl url where to download operator icon
     * @throws java.net.MalformedURLException if some of the urls where not valid
     * @throws IllegalArgumentException if some parameters are null or empty
     */
    public OperatorUpdateInfo(String name, String version, String downloadUrl,
            String minProgramVersion, String iconUrl) throws MalformedURLException {
        Validate.notEmpty(name);
        Validate.notEmpty(version);
        Validate.notEmpty(downloadUrl);
        Validate.notEmpty(minProgramVersion);
        Validate.notEmpty(iconUrl);

        this.name = name;
        this.version = version;
        this.downloadUrl = new URL(downloadUrl);
        this.minProgramVersion = minProgramVersion;
        this.iconUrl = new URL(iconUrl);
    }

    /** operator name */
    public String getName() {
        return name;
    }

    /** operator version */
    public String getVersion() {
        return version;
    }

    /** url where to download operator script */
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    /** minimal required program version to run operator */
    public String getMinProgramVersion() {
        return minProgramVersion;
    }

    /** url where to download operator icon */
    public URL getIconUrl() {
        return iconUrl;
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
