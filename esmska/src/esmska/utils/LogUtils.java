/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

/** Helper methods for logging issues.
 *
 * @author ripper
 */
public class LogUtils {

    /** Modify (phone) number into anonymous one
     * @param number (phone) number, may be null
     * @return the same string with all the numbers replaced by 'N'
     */
    public static String anonymizeNumber(String number) {
        if (number == null) {
            return number;
        } else {
            return number.replaceAll("\\d", "N");
        }
    }
}
