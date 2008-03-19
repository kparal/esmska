/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

/** Interface pro operator scripts.
 * All operator scripts must implement this interface in order to be used in the program.
 * @author ripper
 */
public interface OperatorInfo {

    /** Operator name.
     * This name will be visible in the list od available operators.
     * The name must be in the form "[CC]Operator", where CC is country code as defined
     * in <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166-1 alpha-2</a> and Operator is the name of the operator.
     * For international operators, allowing to send SMS to multiple countries, use [INT]Operator.
     */
    String getName();
    
    /** Version of the script.
     * This is the the datum of last script modification in the YYYY-MM-DD format.
     */
    String getVersion();
    
    /** Author of the script.
     * This is the name and email of the author or latest contributor of the script.
     * The author must be in format "NAME <EMAIL>". Name and email are mandatory.
     */
    String getAuthor();
    
    /** Telephone country prefix.
     * The prefix starts with "+" sign and is 1-3 digits long.
     * List of country calling codes is on <a href="http://en.wikipedia.org/wiki/Country_calling_codes">Wikipedia</a>.
     * Country prefix can be empty string, if the operator works internationally, allowing to send SMS to multiple countries.
     */
    String getCountryPrefix();
    
    /** Array of operator prefixes.
     * You can specify here list of default operator prefixes. This list will be then used
     * to automatically guess correct operator when user is typing in the phone number.
     * All the prefixes must be declared <b>including</b> the country prefix!
     * This is just a user-convenience function, you can easily declare an empty array here.
     */
    String[] getOperatorPrefixes();
    
    /** Length of one SMS.
     * Usually, this number wil be around 160. Many operators add some characters when sending
     * from their website, therefore this number can be often smaller.
     */
    int getSMSLength();
    
    /** Maximum message length the operator allows to send.
     * This is the maximum number of characters the user is allowed to type in
     * into the textarea on the operators website.
     */
    int getMaxChars();
    
    /** Number of allowed messages which user can send at once.
     * This is a multiplier of the getMaxChars() number. Some operators offer only
     * very short crippled messages (eg. max 60 chars, rest with advertisement).
     * You can allow user to write a multiple of this number. The message will be
     * split in the program and send as separate standard messages. Be judicius when
     * specifying this number. Eg. in case of forementioned 60 chars max, multiplier
     * of 5 (therefore writing up to 300 chars) should be absolutely sufficient.
     * For "non-crippled" operators, you should declare '1' here.
     */
    int getMaxParts();
    
    /** Number of extra characters used to display signature.
     * Some operators allow to add signature at the end of the message.
     * This is the number of characters used for "From: " label or something similar.
     * This number will be subtracted from the maximum message length.
     * If your operator doesn't support signatures, declare '0' here.
     */
    int getSignatureExtraLength();
    
}
