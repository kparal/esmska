/*
 * FormChecker.java
 *
 * Created on 9. srpen 2007, 23:03
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.data;

/** Checks for validity of form components
 *
 * @author ripper
 */
public class FormChecker {
    
    /** Creates a new instance of FormChecker */
    private FormChecker() {
    }
    
    /** Check valid name */
    public static boolean checkContactName(String name) {
        return !isEmpty(name);
    }
    
    /** Check valid number */
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
    
    /** Check for emptiness */
    public static boolean isEmpty(String s) {
        return (s == null || s.equals(""));
    }
}
