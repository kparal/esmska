/*
 * OperatorEnum.java
 *
 * Created on 6. červenec 2007, 18:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.operators;

import esmska.*;
import java.util.ArrayList;
import java.util.List;

/** Enum of operators
 *
 * @author ripper
 */
public enum OperatorEnum {
    Vodafone,
    O2;
    
    private static final String[] VODAFONE_ANTENUMBERS = {"608","773","775","776","777"};
    private static final String[] O2_ANTENUMBERS = {"601","602","606","607",
    "720","721","722","723","724","725","726","727","728","729"};
    
    /** get operators as List */
    public static List<Operator> getAsList() {
        List<Operator> list = new ArrayList<Operator>();
        for (OperatorEnum oe : OperatorEnum.values())
            list.add(getOperator(oe));
        return list;
    }
    
    /** return operator corresponding to beginning of sms number */
    public static Operator getOperator(String antenumber) {
        if (antenumber == null || antenumber.equals(""))
            return null;
        
        antenumber = antenumber.replaceFirst("\\+420", "");
        
        for (String an : VODAFONE_ANTENUMBERS) {
            if (antenumber.startsWith(an))
                return new Vodafone();
        }
        for (String an : O2_ANTENUMBERS) {
            if (antenumber.startsWith(an))
                return new O2();
        }
        return null;
    }
    
    /** translate OperatorEnum to Operator */
    public static Operator getOperator(OperatorEnum operatorEnum) {
        switch(operatorEnum) {
            case Vodafone : return new Vodafone();
            case O2 : return new O2();
            default : throw new IllegalArgumentException("operatorEnum");
        }
    }
    
    /** translate operator string to Operator */
    public static Operator parseOperator(String operator) {
        if (operator == null)
            throw new NullPointerException("operator");
        
        if (operator.equals(new O2().toString()))
            return getOperator(O2);
        else if (operator.equals(new Vodafone().toString()))
            return getOperator(Vodafone);
        else return null;
    }
}
