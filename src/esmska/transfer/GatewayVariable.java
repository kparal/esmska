package esmska.transfer;

/** Enum of gateway variables applicable in the gateway script.
 * Beware that values of all these variables are encoded in the x-www-form-urlencoded
 * format (which you would want to use anyway, except maybe for some format checking
 * of recepient and sender number).
 * @author ripper
 */
public enum GatewayVariable {
    /** Phone number in fully international format +[0-9]{1,15} */
    NUMBER,
    /** Text of the SMS message */
    MESSAGE,
    /** Name of the sender */
    SENDERNAME,
    /** Phone number of the sender in fully international format +[0-9]{1,15} */
    SENDERNUMBER,
    /** Login name to the gateway website */
    LOGIN,
    /** Password to the gateway website */
    PASSWORD,
    /** This string is non-empty if a delivery report should be sent */
    RECEIPT
}
