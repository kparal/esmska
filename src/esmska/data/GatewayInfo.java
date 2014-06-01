package esmska.data;

/** Interface pro gateway scripts.
 * All gateway scripts must implement this interface in order to be used in the program.
 * @author ripper
 */
public interface GatewayInfo {

    /** Gateway name.
     * This name will be visible in the list od available gateways.
     * The name must be in the form "[CC]Gateway", where CC is country code as defined
     * in <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a> 
     * and Gateway is the very name of the gateway.<br/>
     * Country code should denote which country is the main interest of the gateway, for example:
     * <ul><li>The gateway is only available in the language of that country.</li>
     * <li>The gateway sends messages only to customers of that country's operator.</li>
     * <li>The gateway requires a SIM card bought from that country's operator.</li></ul>
     * For international gateways, allowing to send SMS to multiple countries, use [INT]Gateway.<br/>
     * This name must be unique amongst other gateway names.
     */
    String getName();
    
    /** Version of the script.
     * This is the the datum of last script modification in the YYYY-MM-DD format.
     */
    String getVersion();
    
    /** Maintainer of the script.
     * This is the name and email of the maintainer (often the author) of the script.
     * The maintainer must be in format "NAME &lt;EMAIL&gt;". Name and email are mandatory.
     */
    String getMaintainer();

    /** Minimal program version required to run this script.
     * This string is in a format "x.y.z", where x,y,z are numbers.
     */
    String getMinProgramVersion();

    /** The URL of the webpage of this gateway. On this URL users can get more information
     * about this gateway, register an account, check if it is working, etc.
     * Can be an empty string, but that is strongly discouraged. The website address should
     * be provided if possible.
     */
    String getWebsite();

    /** Short description of the website (purpose, restrictions, etc). Just one or two
     * sentences. Write it in a language corresponding to the gateway (i.e. english
     * for [INT] websites, local language for local gateways). Can be empty.
     */
    String getDescription();

    /** List of telephone prefixes that are supported by this gateway.
     * All prefixes that are not mentioned here are *not supported* by this gateway.
     * For example for a gateway that can send messages only to numbers originating
     * from Czech Republic (country prefix: +420), and nowhere else, the value is
     * ["+420"]. (The gateway doesn't have to support all numbers starting with +420,
     * but it certainly doesn't support any other prefix.)<br/>
     * The supported prefixes will usually map to country codes. The prefix always
     * starts with "+" sign and is 1-3 digits long. List of country calling codes is on
     * <a href="http://en.wikipedia.org/wiki/Country_calling_codes">Wikipedia</a>.<br/>
     * If the gateway works internationally, allowing to send SMS to multiple countries,
     * this will be an empty array.
     *
     * @return list of supported prefixes; empty array if gateway sends anywhere in the world
     */
    String[] getSupportedPrefixes();

    /** List of telephone prefixes that are preferred by this gateway.
     * Preferred prefixes means that there is really high probability that this
     * gateway will be able to send message to a phone number with that prefix. Sometimes
     * a gateway support just a certain set of customers (of a single cell operator
     * for example) and this is a way how to mark them. It usually concerns just
     * free gateway, paid ones usually send everywhere.<br/>
     * If the gateway sends messages to any phone number within the supported
     * prefixes (see {@link #getSupportedPrefixes}) then this will be an empty
     * array.<br/><br/>
     * Example: When the supported prefix is ["+420"] and this gateway allows sending
     * messages to an operator who owns prefixes "606" and "777", then the resulting
     * array of preferred prefixes is ["+420606", "+420777"].
     *
     * @return list of preferred prefixes; empty array if gateway sends to any phone number
     * in supported prefixes
     */
    String[] getPreferredPrefixes();
    
    /** Length of one SMS.
     * Usually, this number wil be around 160. Many gateways add some characters when sending
     * from their website, therefore this number can be often smaller.
     * It can happen that length of the sms can't be determined (it is different for
     * different numbers). In this case provide a negative number.
     */
    int getSMSLength();
    
    /** Maximum message length the gateway allows to send.
     * This is the maximum number of characters the user is allowed to type in
     * into the textarea on the gateway website.
     */
    int getMaxChars();
    
    /** Number of allowed messages which user can send at once.
     * This is a multiplier of the getMaxChars() number. Some gateways offer only
     * very short crippled messages (eg. max 60 chars, rest with advertisement).
     * You can allow user to write a multiple of this number. The message will be
     * split in the program and send as separate standard messages. Be judicius when
     * specifying this number. Eg. in case of forementioned 60 chars max, multiplier
     * of 5 (therefore writing up to 300 chars) should be absolutely sufficient.
     * For "non-crippled" gateways, you should declare '1' here.
     */
    int getMaxParts();
    
    /** The delay in seconds that must be kept between sending messages. The program
     * will wait for at least this number of seconds before attempting to send
     * another message. If there are no gateway restrictions, use '0'.
     */
    int getDelayBetweenMessages();
    
    /** Indicates for which website languages the script is working.
     * This method is included because gateways may have their website translated
     * into many languages and therefore the response may come somehow localized.<br>
     * If the script works independently of website language (no matching on sentences is done),
     * specify just an empty array.
     * In this case default user language will be used for retrieving the website.<br>
     * If the script works only with one or more specific languages, provide their
     * two-letter codes (as specified by the <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">ISO 639-1 Code</a>) in an array.
     * In this case default user language will be used if it exists in the array,
     * otherwise first language in the array will be used (therefore it is reasonable
     * to specify the most widely used - like english - as first).
     * @return list of two-letter language codes for which this script works,
     *  or empty array if the script works independently of language
     */
    String[] getSupportedLanguages();

    /** The list of all supported features by this gateway.
     * It is a list of strings matching the {@link Gateway.Feature} attributes.
     */
    String[] getFeatures();
}
