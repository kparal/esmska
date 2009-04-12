/*
 * SMS.java
 *
 * Created on 6. červenec 2007, 17:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.data;

import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** SMS entity class
 *
 * @author ripper
 */
public class SMS {
    private static final Logger logger = Logger.getLogger(SMS.class.getName());

    private String number; //recipient number
    private String name; //recipient name
    private String text; //message text
    private String senderNumber;
    private String senderName;
    private ImageIcon image; //security image
    private String imageCode; //security image code
    private String operator;
    private Status status = Status.NEW; //sms status
    private String errMsg; //potential error
    private String operatorMsg; //additional message from operator
    
    /** Status of SMS */
    public static enum Status {
        /** newly created, still not in the queue */
        NEW,
        /** waiting in the queue, but still not ready (waiting for delay, etc) */
        WAITING,
        /** waiting in the queue, ready to be sent */
        READY,
        /** currently being sent */
        SENDING,
        /** successfully sent */
        SENT; 
    }

    /** Shortcut for SMS(number, text, operator, null, null, null). */
    public SMS(String number, String text, String operator) {
        this(number, text, operator, null, null, null);
    }

    /** Constructs new SMS. For detailed parameters restrictions see individual setter methods.
     * @param number not null nor empty
     * @param text not null
     * @param operator not null nor empty
     * @param name
     * @param senderNumber
     * @param senderName
     */
    public SMS(String number, String text, String operator, String name,
            String senderNumber, String senderName) {
        setNumber(number);
        setText(text);
        setOperator(operator);
        setName(name);
        setSenderNumber(senderNumber);
        setSenderName(senderName);
    }

    /** Return whether some error occured during sending.
     * SMS is problematic if there is some error message stored.
     */
    public boolean isProblematic() {
        return StringUtils.isNotEmpty(getErrMsg());
    }

    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    /** Recepient number in international format (starting with "+").
        Never null nor empty. */
    public String getNumber() {
        return number;
    }

    /** Text of the message. Never null. */
    public String getText() {
        return text;
    }

    /** Sender number. Never null. */
    public String getSenderNumber() {
        return senderNumber;
    }

    /** Sender name. Never null. */
    public String getSenderName() {
        return senderName;
    }

    /** Security image. May be null. */
    public ImageIcon getImage() {
        return image;
    }

    /** Security image code. Never null. */
    public String getImageCode() {
        return imageCode;
    }

    /** Operator of the message. Never null nor empty. */
    public String getOperator() {
        return operator;
    }

    /** Status of the message. Never null. */
    public Status getStatus() {
        return status;
    }

    /** Error message from sending. Never null. */
    public String getErrMsg() {
        return errMsg;
    }

    /** Name of the recepient. Never null. */
    public String getName() {
        return name;
    }

    /** Message from operator. Never null. */
    public String getOperatorMsg() {
        return operatorMsg;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    /** Recepient number in international format (starting with "+").
        May not be null nor empty. */
    public void setNumber(String number) {
        Validate.notEmpty(number);
        if (!number.startsWith("+")) {
            throw new IllegalArgumentException("Number does not start with '+': " + number);
        }
        this.number = number;
    }

    /** Text of the message. Not null. */
    public void setText(String text) {
        Validate.notNull(text);
        this.text = text;
    }

    /** Sender number. Null value is changed to empty string. */
    public void setSenderNumber(String senderNumber) {
        this.senderNumber = StringUtils.defaultString(senderNumber);
    }

    /** Sender name. Null value is changed to empty string. */
    public void setSenderName(String senderName) {
        this.senderName = StringUtils.defaultString(senderName);
    }

    /** Security image. May be null. */
    public void setImage(ImageIcon image) {
        this.image = image;
    }

    /** Security image code. Null value is changed to empty string. */
    public void setImageCode(String imageCode) {
        this.imageCode = StringUtils.defaultString(imageCode);
    }

    /** Operator of the message. May not be null nor empty. */
    public void setOperator(String operator) {
        Validate.notEmpty(operator);
        this.operator = operator;
    }

    /** Status of the message. May not be null. */
    void setStatus(Status status) {
        Validate.notNull(status);
        this.status = status;
        logger.finer("SMS status changed: " + toDebugString());
    }

    /** Error message from sending. Null value is changed to empty string. */
    public void setErrMsg(String errMsg) {
        this.errMsg = StringUtils.defaultString(errMsg);
        logger.finer("SMS error message changed: " + toDebugString());
    }

    /** Name of the recepient. Null value is changed to empty string. */
    public void setName(String name) {
        this.name = StringUtils.defaultString(name);
    }

    /** Message from operator. Null value is changed to empty string. */
    public void setOperatorMsg(String operatorMsg) {
        this.operatorMsg = StringUtils.defaultString(operatorMsg);
        logger.finer("SMS operator message changed: " + toDebugString());
    }
    // </editor-fold>
    
    @Override
    public String toString() {
        return StringUtils.defaultIfEmpty(getName(), getNumber());
    }

    public String toDebugString() {
        return "[name=" + name + ", number=" + Contact.anonymizeNumber(number) +
                ", operator=" + operator + ", status=" + status + ", operatorMsg=" +
                operatorMsg + ", errMsg=" + errMsg + "]";
    }
}
