/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

import esmska.data.Config;
import esmska.data.Keyring;
import esmska.persistence.PersistenceManager;
import java.util.Collection;
import java.util.TreeSet;

/** Helper class for the Operator interface. 
 * 
 * @author ripper
 */
public class OperatorUtil {
    private static final TreeSet<Operator> operators = PersistenceManager.getOperators();
    private static final Keyring keyring = PersistenceManager.getKeyring();
    private static final Config config = PersistenceManager.getConfig();
    
    /** Find operator by name.
     * @param name Name of the operator. Search is case sensitive.
     * @return Operator implementation, when an operator with such name is found.
     *         If multiple such operators are found, returns the first one found.
     *         Returns null if no operator is found or provided name was null.
     */
    public static Operator getOperator(String name) {
        if (name == null) {
            return null;
        }
        
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
        if (number == null) {
            throw new NullPointerException("number");
        }
        
        for (Operator operator : operators) {
            String prefix = operator.getCountryPrefix();
            if (prefix.length() > 0 && number.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }
    
    /** Guess operator according to phone number or phone number prefix.
     * Searches through operators and finds the best suited one one
     * supporting this phone number.
     * 
     * @param number phone number or it's prefix. The minimum length is two characters,
     *  for shorter input (or null) the method does nothing.
     * @param customOperators collection of operators in which to search.
     *  Use null for searching in all currently available operators.
     * @return the suggested operator or null if none found
     */
    public static Operator suggestOperator(String number, Collection<Operator> customOperators) {
        if (number == null || number.length() < 2) {
            return null;
        }
        
        Collection<Operator> selectedOperators = 
                (customOperators != null ? customOperators : operators);
        Operator operator = null;
        
        //search in operator prefixes
        for (Operator op : selectedOperators) {
            if (matchesWithOperatorPrefix(op, number)) {
                //prefer operators without login requirements
                if (op.isLoginRequired()) {
                    if (operator == null) {
                        operator = op;
                    } else if (keyring.getKey(operator.getName()) == null &&
                            keyring.getKey(op.getName()) != null) {
                        //prefer operators with filled in credentials
                        operator = op;
                    }
                } else {
                    return op;
                }
            }
        }
        
        //if no operator without login found, but some operator with login found
        if (operator != null) {
            return operator;
        }
        
        //search in country prefixes
        for (Operator op : selectedOperators) {
            if (matchesWithCountryPrefix(op, number)) {
                //prefer operators without login requirements
                if (op.isLoginRequired()) {
                    if (operator == null) {
                        operator = op;
                    } else if (keyring.getKey(operator.getName()) == null &&
                            keyring.getKey(op.getName()) != null) {
                        //prefer operators with filled in credentials
                        operator = op;
                    }
                } else {
                    return op;
                }
            }
        }
        
        //if no operator without login found, but some operator with login found
        if (operator != null) {
            return operator;
        }
        
        return null;
    }
    
    /** Returns whether current operator matches the number with some of
     * it's operator prefix.
     * 
     * @param operator operator
     * @param number phone number
     * @return true if current operator matches the number with some of
     * it's operator prefix; false if operator of phone number is null or if
     * phone number is shorter than 2 characters
     */
    public static boolean matchesWithOperatorPrefix(Operator operator, String number) {
        if (operator == null || number == null || number.length() < 2) {
            return false;
        }
        
        //search in operator prefixes
        for (String prefix : operator.getOperatorPrefixes()) {
            if (number.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /** Returns whether current operator matches the number with it's country prefix.
     * 
     * @param operator operator
     * @param number phone number
     * @return true if current operator matches the number with it's country prefix;
     * false if operator of phone number is null or if phone number is shorter 
     * than 2 characters
     */
    public static boolean matchesWithCountryPrefix(Operator operator, String number) {
        if (operator == null || number == null || number.length() < 2) {
            return false;
        }
        
        return number.startsWith(operator.getCountryPrefix());
    }
    
    
    /** String current country prefix from number if possible
     * 
     * @param number number, can be null
     * @return number with stripped country prefix from start if possible; 
     * otherwise non-modified number
     */
    public static String stripCountryPrefix(String number) {
        if (number != null && number.startsWith(config.getCountryPrefix())) {
            number = number.substring(config.getCountryPrefix().length());
        }
        return number;
    }
}
