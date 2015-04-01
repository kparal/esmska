package esmska.update;

import esmska.data.Config;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;

/** Class for representation of gateway update.
 *
 * @author ripper
 */
public class GatewayUpdateInfo {

    private String name;
    private String fileName;
    private String version;
    private String minProgramVersion;
    private URL downloadUrl;
    private URL iconUrl;

    /** Constructor.
     *
     * @param name gateway name; not null nor empty
     * @param version gateway version; not null nor empty
     * @param fileName name of gateway script without suffix; not null nor empty
     * @param minProgramVersion minimal required program version to run gateway; not null nor empty
     * @param downloadUrl url where to download gateway script; not null nor empty
     * @param iconUrl url where to download gateway icon; empty string is changed to null
     * @throws java.net.MalformedURLException if some of the urls where not valid
     */
    public GatewayUpdateInfo(String name, String fileName, String version,
            String minProgramVersion, String downloadUrl, String iconUrl)
            throws MalformedURLException {
        Validate.notEmpty(name);
        Validate.notEmpty(fileName);
        Validate.notEmpty(version);
        Validate.notEmpty(minProgramVersion);
        Validate.notEmpty(downloadUrl);

        this.name = name;
        this.fileName = fileName;
        this.version = version;
        this.minProgramVersion = minProgramVersion;
        this.downloadUrl = new URL(downloadUrl);
        this.iconUrl = StringUtils.isNotEmpty(iconUrl) ? new URL(iconUrl) : null;
    }

    /** gateway name, not null nor empty */
    public String getName() {
        return name;
    }

    /** name of gateway file (without 'gateway' suffix) */
    public String getFileName() {
        return fileName;
    }

    /** gateway version, not null nor empty */
    public String getVersion() {
        return version;
    }

    /** url where to download gateway script, not null nor empty */
    public URL getDownloadUrl() {
        return downloadUrl;
    }

    /** minimal required program version to run gateway, not null nor empty */
    public String getMinProgramVersion() {
        return minProgramVersion;
    }

    /** url where to download gateway icon, may be null */
    public URL getIconUrl() {
        return iconUrl;
    }

    /** Returns whether gateway required program version is lower or same as
     * current program version (can be used), or not (can't be used). */
    public boolean canBeUsed() {
        return Config.compareProgramVersions(Config.getLatestVersion(), minProgramVersion) >= 0;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).
                append("fileName", fileName).
                append("version", version).
                append("minProgramVersion", minProgramVersion).
                append("downloadUrl", downloadUrl).append("iconUrl", iconUrl).
                toString();
    }


}
