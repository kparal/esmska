/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.net.URL;
import java.security.PrivilegedActionException;
import java.text.Collator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.script.ScriptException;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.commons.lang.StringUtils;

/** Default implementation of the Gateway interface.
 * This implementation caches all information retrieved from gateway script
 * in order to reduce the performance impact caused by javascript invocations.
 * @author ripper
 */
public class DefaultGateway implements Gateway {

    private static final Logger logger = Logger.getLogger(DefaultGateway.class.getName());
    private URL script;
    private String name, version, maintainer, website, description, minProgramVersion;
    private String[] supportedPrefixes, preferredPrefixes, supportedLanguages;
    private int smsLength,  maxParts,  maxChars,  signatureExtraLength, 
            delayBetweenMessages;
    private Icon icon;
    private boolean loginRequired;
    private static final Pattern namePattern = 
            Pattern.compile("^\\[(\\w\\w|" + CountryPrefix.INTERNATIONAL_CODE + ")\\].+");

    /** Creates new DefaultGateway.
     * 
     * @param script system resource containing gateway script
     * @throws IOException When there are problem accessing the script file
     * @throws ScriptException When gateway script is invalid
     * @throws PrivilegedActionException When gateway script is invalid
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     * @throws IllegalArgumentException When gateway name is not valid
     */
    public DefaultGateway(URL script) throws IOException, ScriptException,
            PrivilegedActionException, IntrospectionException {
        this.script = script;
        
        GatewayInfo info = Gateways.parseInfo(script);
        //check gateway name is valid
        if (info == null || StringUtils.isEmpty(info.getName())) {
            throw new ScriptException("Not a valid gateway script", script.toExternalForm(), 0);
        }
        if (!namePattern.matcher(info.getName()).matches()) {
            throw new IllegalArgumentException("Gateway name not valid: " + info.getName());
        }
        
        //remember all the values from GatewayInfo interface internally in order
        //to increase speed (Java code vs JavaScript execution for every method access).
        name = info.getName();
        version = info.getVersion();
        maintainer = info.getMaintainer();
        minProgramVersion = info.getMinProgramVersion();
        website = info.getWebsite();
        description = info.getDescription();
        supportedPrefixes = info.getSupportedPrefixes();
        preferredPrefixes = info.getPreferredPrefixes();
        smsLength = info.getSMSLength();
        maxParts = info.getMaxParts();
        maxChars = info.getMaxChars();
        signatureExtraLength = info.getSignatureExtraLength();
        delayBetweenMessages = info.getDelayBetweenMessages();
        loginRequired = info.isLoginRequired();
        supportedLanguages = info.getSupportedLanguages();

        //find icon - for "[xx]abc.gateway" look for "[xx]abc.png"
        String iconName = script.toExternalForm().replaceFirst("\\.gateway$", ".png");
        URL iconURL = new URL(iconName);
        icon = new ImageIcon(iconURL);
        if (icon.getIconWidth() <= 0) { //non-existing icon, zero-sized image
            icon = Icons.GATEWAY_DEFAULT;
        }

        logger.log(Level.FINER, "Created new gateway: {0}", toString());
    }

    @Override
    public int compareTo(Gateway o) {
        Collator collator = Collator.getInstance();
        return collator.compare(this.getName(), o.getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Gateway)) {
            return false;
        }
        Gateway o = (Gateway) obj;
        return this.compareTo(o) == 0;
    }

    @Override
    public String toString() {
        return name + " [version=" + version + "]";
    }

    @Override
    public URL getScript() {
        return script;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getMaintainer() {
        return maintainer;
    }

    @Override
    public String getMinProgramVersion() {
        return minProgramVersion;
    }

    @Override
    public String getWebsite() {
        return website;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getSupportedPrefixes() {
        return supportedPrefixes;
    }

    @Override
    public String[] getPreferredPrefixes() {
        return preferredPrefixes;
    }

    @Override
    public int getSMSLength() {
        return smsLength;
    }

    @Override
    public int getMaxParts() {
        return maxParts;
    }

    @Override
    public int getMaxChars() {
        return maxChars;
    }

    @Override
    public int getSignatureExtraLength() {
        return signatureExtraLength;
    }

    @Override
    public int getDelayBetweenMessages() {
        return delayBetweenMessages;
    }

    @Override
    public boolean isLoginRequired() {
        return loginRequired;
    }

    @Override
    public String[] getSupportedLanguages() {
        return supportedLanguages;
    }
}
