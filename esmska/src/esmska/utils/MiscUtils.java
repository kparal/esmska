/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import org.apache.commons.lang.StringEscapeUtils;

/** Various helper methods.
 *
 * @author ripper
 */
public class MiscUtils {

    /** Escape text using html entities.
     *  Fixes bug in OpenJDK where scaron entity is not replaced by 'š' and
     *  euro by '€'.
     *
     * @see ​StringEscapeUtils#escapeHtml(String)
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return input;
        }
        String output = StringEscapeUtils.escapeHtml(input);
        output = output.replaceAll("\\&scaron;", "\\&#353;");
        output = output.replaceAll("\\&Scaron;", "\\&#352;");
        output = output.replaceAll("\\&euro;", "\\&#8364;");
        return output;
    }

    /** Strip html tags from text.
     *
     * @param input input text
     * @return text with all html tags removed. Entities encoded by html codes
     * are unescaped back to standard characters.
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return input;
        }
        String output = input.replaceAll("\\<.*?>","");
        output = StringEscapeUtils.unescapeHtml(output);
        return output;
    }
    
}
