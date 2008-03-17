/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

import esmska.persistence.PersistenceManager;
import java.util.ArrayList;
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
     *         Return null if no operator is found or provided name was null.
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
}
