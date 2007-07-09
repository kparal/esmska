/*
 * OperatorEnum.java
 *
 * Created on 6. červenec 2007, 18:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package operators;

import esmska.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ripper
 */
public enum OperatorEnum {
    Vodafone,
//    T_Mobile,
    O2;
    
    public static List<Operator> getAsList() {
        List<Operator> list = new ArrayList<Operator>();
        for (OperatorEnum oe : OperatorEnum.values())
            list.add(Resolver.getOperator(oe));
        return list;
    }
    
    /** translate OperatorEnum to Operator */
    public static class Resolver {
        public static Operator getOperator(OperatorEnum operatorEnum) {
            switch(operatorEnum) {
                case Vodafone : return new Vodafone();
                case O2 : return new O2();
                default : throw new IllegalArgumentException("operatorEnum");
            }
        }
    }
}
