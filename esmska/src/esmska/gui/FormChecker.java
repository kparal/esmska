/*
 * FormChecker.java
 *
 * Created on 9. srpen 2007, 23:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.gui;

/** Checks for validity of form components
 *
 * @author ripper
 */
public class FormChecker {
    
    /** Check valid name
     * @return true if name is not empty ("" or null), false otherwise
     */
    public static boolean checkContactName(String name) {
        return !isEmpty(name);
    }
    
    /** Check valid number
     * @return true if number is in form +[0-9]{1,15}, false otherwise
     */
    public static boolean checkSMSNumber(String number) {
        if (number == null)
            return false;
        if (!number.startsWith("+"))
            return false;
        number = number.substring(1); //strip the "+"
        if (number.length() < 1 || number.length() > 15)
            return false;
        for (Character c : number.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }
    
    /** Check valid country prefix
     * @return true if prefix is in form +[0-9]{1,3}, false otherwise
     */
    public static boolean checkCountryPrefix(String prefix) {
        if (prefix == null)
            return false;
        if (!prefix.startsWith("+"))
            return false;
        prefix = prefix.substring(1); //strip the "+"
        if (prefix.length() < 1 || prefix.length() > 3)
            return false;
        for (Character c : prefix.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }
    
    /** Check for emptiness
     * @return true if string is empty or null, false otherwise
     */
    public static boolean isEmpty(String s) {
        return (s == null || s.equals(""));
    }
}
