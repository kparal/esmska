/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.utils;

/** Helper class for commonly used methods with modified behaviour
 * for null objects
 *
 * @author ripper
 */
public class Nullator {

    /** Tests two objects for equality. This is the same as o1.equals(o2),
     * but handles cases where objects are null.
     * @param o1 first object
     * @param o2 second object
     * @return true if o1.equals(o2) or both o1 and o2 are null; false otherwise
     */
    public static boolean isEqual(Object o1, Object o2) {
        if (o1 == null) {
            return (o2 == null);
        }
        return o1.equals(o2);
    }
    
    /** Check if string is empty, which means null or empty string.
     * 
     * @param string input string
     * @return true, if string is null or "" (zero-length); false otherwise
     */
    public static boolean isEmpty(String string) {
        return (string == null) || (string.length() == 0);
    }
}
