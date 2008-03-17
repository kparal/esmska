/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.operators;

/** Enum of operator variables applicable in the operator script.
 *
 * @author ripper
 */
public enum OperatorVariable {
    /** Phone number in fully international format +[0-9]{1,15} */
    NUMBER,
    /** Text of the SMS message */
    MESSAGE,
    /** Name of the sender */
    SENDERNAME,
    /** Phone number of the sender in fully international format +[0-9]{1,15} */
    SENDERNUMBER,
    /** Login name to the operator website */
    LOGIN,
    /** Password to the operator website */
    PASSWORD
}
