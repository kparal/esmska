/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import org.apache.commons.lang.StringEscapeUtils;

/** Workaround methods for bugs in JREs
 *
 * @author ripper
 */
public class Workarounds {

    /** Escape text using html entities.
     *  Fixes bug in OpenJDK where scaron entity is not replaced by 'š'.
     *
     * @see org.​apache.​commons.​lang.​StringEscapeUtils#escapeHtml(String)
     */
    public static String escapeHtml(String input) {
        String output = StringEscapeUtils.escapeHtml(input);
        output = output.replaceAll("\\&scaron;", "\\&#353;");
        output = output.replaceAll("\\&Scaron;", "\\&#352;");
        return output;
    }

}
