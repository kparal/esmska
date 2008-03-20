/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

import esmska.persistence.PersistenceManager;
import java.util.TreeSet;

/** Helper class for the Operator interface. 
 * 
 * @author ripper
 */
public class OperatorUtil {
    private static final TreeSet<Operator> operators = PersistenceManager.getOperators();
    
    /** Find operator by name.
     * @param name Name of the operator. Search is case sensitive.
     * @return Operator implementation, when an operator with such name is found.
     *         If multiple such operators are found, returns the first one found.
     *         Returns null if no operator is found or provided name was null.
     */
    public static Operator getOperator(String name) {
        if (name == null)
            return null;
        
        for (Operator operator : operators) {
            if (operator.getName().equals(name)) {
                return operator;
            }
        }
        return null;
    }
    
    /** Extract country prefix from phone number.
     * This method searches through available operators and checks if supplied
     * number starts with any of supported prefixes.
     * @param number Phone number in fully international format.
     * @return Country prefix if such is found amongst list of supported operator
     *         prefixes. Null otherwise.
     * @throws NullPointerException If number is null.
     */
    public static String getCountryPrefix(String number) {
        if (number == null)
            throw new NullPointerException("number");
        
        for (Operator operator : operators) {
            String prefix = operator.getCountryPrefix();
            if (prefix.length() > 0 && number.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }
}
