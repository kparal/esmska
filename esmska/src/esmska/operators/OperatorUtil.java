/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

import esmska.persistence.PersistenceManager;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 *
 * @author ripper
 */
public class OperatorUtil {
    private static final TreeSet<Operator> operators = PersistenceManager.getOperators();
    
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
