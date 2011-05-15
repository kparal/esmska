package esmska.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class containing links to program websites or other program actions
 *
 * @author ripper
 */
public class Links {
    private static final Logger logger = Logger.getLogger(Links.class.getName());

    /** program homepage */
    public static final String HOMEPAGE = "http://esmska.googlecode.com/";
    /** program download page */
    public static final String DOWNLOAD = "http://code.google.com/p/esmska/wiki/Download?tm=2";
    /** program issue tracker */
    public static final String ISSUES = "http://code.google.com/p/esmska/wiki/Issues";
    /** program FAQ */
    public static final String FAQ = "http://code.google.com/p/esmska/wiki/FAQ";
    /** program support forum */
    public static final String FORUM = "https://answers.launchpad.net/esmska";
    /** program translations */
    public static final String TRANSLATE = "https://translations.launchpad.net/esmska";
    /** program donations and other support */
    public static final String DONATE = "http://code.google.com/p/esmska/wiki/Support";
    /** list of program donators */
    public static final String DONATORS = "http://code.google.com/p/esmska/wiki/Donators";

    /** internal program link telling to open the config dialog on the
     * gateways tab */
    public static final String CONFIG_GATEWAYS = "esmska://config-gateways";
    /** get the update file */
    public static final String CHECK_UPDATE = 
            "http://ripper.profitux.cz/esmska/update/version.php?ref=" + Config.getLatestVersion();
    /** link to send usage statistics to */
    public static final String SEND_STATS = "http://ripper.profitux.cz/esmska/stats/receive.php";
    
    /** Covert string URL to URI. Returns null if conversion fails. */
    public static URI getURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            logger.log(Level.WARNING, "Couldn't convert url to URI: " + url, ex);
            return null;
        }
    }
}
