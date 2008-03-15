/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

/**
 *
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
    
    /** Telephone country prefix.
     * The prefix starts with "+" sign and is 1-3 digits long.
     * List of country calling codes is on <a href="http://en.wikipedia.org/wiki/Country_calling_codes">Wikipedia</a>.
     * Country prefix can be empty string, if the operator works internationally, allowing to send SMS to multiple countries.
     */
    String getCountryPrefix();
    
    /** Length of one SMS.
     * Usually, this wil be around 160. Many operators add some characters when sending
     * from their website, therefore this number can be smaller.
     */
    int getSMSLength();
    
    /** Number of allowed SMS's which user can send at once.
     * The operator can allow to write multiple messages and split it automatically.
     */
    int getMaxParts();
    
    /** maximum sendable chars in one pass */
    int getMaxChars();
    
    /** number of characters needed to add to signature,
     * therefore strip from message length */
    int getSignatureExtraLength();
    
}
