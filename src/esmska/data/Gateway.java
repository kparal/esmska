package esmska.data;

import esmska.utils.L10N;
import esmska.utils.MiscUtils;
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

/** Class representing a web gateway.
 * This implementation caches all information retrieved from gateway script
 * in order to reduce the performance impact caused by javascript invocations.
 */
public class Gateway implements GatewayInfo, Comparable<Gateway> {
    /** This enum attributes indicate which various features are supported by a given gateway. */
    public static enum Feature {
        /** The gateway supports logging in with username and password, but also
         * works without it. */
        LOGIN,
        /** The gateway requires login with username and password. It means the
         * user must have some credentials assigned from the operator or must
         * register at gateway website prior to using this gateway. */
        LOGIN_ONLY,
        /** The gateway is able to send a message as if it was coming from the
         * specified sender's cell number. */
        SENDER_NUMBER,
        /** The gateway will send a delivery report upon request. This usually
         * means sending an SMS back to the sender as soon as the message reaches
         * the recipient. */
        RECEIPT,
        /** This gateway requires (always or in some circumstances) transcribing
         * a security code (captcha). */
        CAPTCHA,
    }

    public static final String UNKNOWN = L10N.l10nBundle.getString("Gateway.unknown");

    private static final Logger logger = Logger.getLogger(Gateway.class.getName());
    // maximum number of characters the user can send at once
    public static final int maxMessageLength = 1000;
    // maximum multiplier of messages
    private static final int maxMaxParts = 5;
    private URL script;
    private String name, version, maintainer, website, description, minProgramVersion;
    private String[] supportedPrefixes, preferredPrefixes, supportedLanguages, features;
    private int smsLength, maxParts, maxChars, delayBetweenMessages;
    private Icon icon;
    private boolean favorite, hidden;
    private GatewayConfig config = new GatewayConfig();
    private static final Pattern namePattern =
            Pattern.compile("^\\[(\\w\\w|" + CountryPrefix.INTERNATIONAL_CODE + ")\\].+");
    private static final Collator collator = Collator.getInstance();

    /** Creates new Gateway.
     *
     * @param script system resource containing gateway script
     * @throws IOException When there are problem accessing the script file
     * @throws ScriptException When gateway script is invalid
     * @throws PrivilegedActionException When gateway script is invalid
     * @throws IntrospectionException When current JRE does not support JavaScript execution
     * @throws IllegalArgumentException When gateway name is not valid
     */
    public Gateway(URL script) throws IOException, ScriptException,
            PrivilegedActionException, IntrospectionException {
        this.script = script;

        GatewayInfo info = Gateways.parseInfo(script);
        
        //check gateway name is valid
        name = info != null ? info.getName() : null;
        if (StringUtils.isEmpty(name)) {
            throw new ScriptException("Not a valid gateway script", script.toExternalForm(), 0);
        }
        if (!namePattern.matcher(name).matches()) {
            throw new IllegalArgumentException("Gateway name not valid: " + name);
        }

        //remember all the values from GatewayInfo interface internally in order
        //to increase speed (Java code vs JavaScript execution for every method access).
        version = info.getVersion();
        maintainer = info.getMaintainer();
        minProgramVersion = info.getMinProgramVersion();
        website = info.getWebsite();
        description = info.getDescription();
        supportedPrefixes = info.getSupportedPrefixes();
        preferredPrefixes = info.getPreferredPrefixes();
        smsLength = info.getSMSLength();
        maxChars = info.getMaxChars();
        delayBetweenMessages = info.getDelayBetweenMessages();
        supportedLanguages = info.getSupportedLanguages();
        features = info.getFeatures();

        //find icon - for "[xx]abc.gateway" look for "[xx]abc.png"
        String iconName = script.toExternalForm().replaceFirst("\\.gateway$", ".png");
        URL iconURL = new URL(iconName);
        icon = new ImageIcon(iconURL);
        if (icon.getIconWidth() <= 0) { //non-existing icon, zero-sized image
            icon = Icons.GATEWAY_DEFAULT;
        }
        
        // compute maxParts
        maxParts = maxMessageLength / maxChars;
        maxParts = Math.min(maxParts, maxMaxParts);

        logger.log(Level.FINER, "Created new gateway: {0}", toString());
    }


    /** URL of gateway script (file or jar URL). */
    public URL getScript() {
        return script;
    }

    /** Gateway logo icon.
     * Should be a 16x16px PNG with transparent background.
     */
    public Icon getIcon() {
        return icon;
    }

    /** Return whether this gateway has been marked as user favorite */
    public boolean isFavorite() {
        return favorite;
    }

    /** Set this gateway as user favorite */
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    /** Return whether this gateway has been marked as hidden from the user interface */
    public boolean isHidden() {
        return hidden;
    }

    /** Set this gateway as hidden from user interface */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /** Decide whether this particular gateway supports given feature. */
    public boolean hasFeature(Feature feature) {
        for (String featName : features) {
            if (feature.name().equalsIgnoreCase(featName)) {
                return true;
            }
        }
        return false;
    }
    
    /** Number of allowed messages which user can send at once.
     * This is a multiplier of the getMaxChars() number. Some gateways offer only
     * very short crippled messages (eg. max 60 chars, rest with advertisement).
     * To alleviate this problem, the user can write a longer text (a multiple
     * of the allowed messages) and it will be split into separate messages
     * automatically.
     * 
     * This number will get computed dynamically. It will be the highest 
     * multiplier of getMaxChars() which doesn't exceed maxMessageLength chars 
     * message length and simmultaneously a maximum multiplier maxMaxParts.
     */
    public int getMaxParts() {
        return maxParts;
    }

    public GatewayConfig getConfig() {
        return config;
    }

    public void setConfig(GatewayConfig config) {
        this.config = config;
    }
    
    /** Get sender name signature that should be prepended to the message
     * before it is sent.
     * @return empty string if user does not want any signature to be added; 
     *  otherwise user name signature
     */
    public String getSenderName() {
        Signature signature = Signatures.getInstance().get(getConfig().getSignature());
        if (signature == null || StringUtils.isEmpty(signature.getUserName())) {
            // user doesn't want to add any name signature
            return "";
        }
        
        // user wants to add his name signature
        String result = signature.getUserName();
        // remove accents if required
        if (Config.getInstance().isRemoveAccents()) {
            result = MiscUtils.removeAccents(result);
        }
        return result;
    }

    @Override
    public int compareTo(Gateway o) {
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
    public int getMaxChars() {
        return maxChars;
    }

    @Override
    public int getDelayBetweenMessages() {
        return delayBetweenMessages;
    }

    @Override
    public String[] getSupportedLanguages() {
        return supportedLanguages;
    }

    @Override
    public String[] getFeatures() {
        return features;
    }
}
