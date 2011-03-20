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
    private String imageCode = ""; //security image code
    private String imageHint; //hint from gateway where to find security image (eg. sent to cell)
    private String gateway;
    private Status status = Status.NEW; //sms status
    private String errMsg = ""; //potential error
    private String gatewayMsg = ""; //additional message from gateway
    
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

    /** Shortcut for SMS(number, text, gateway, null, null, null). */
    public SMS(String number, String text, String gateway) {
        this(number, text, gateway, null, null, null);
    }

    /** Constructs new SMS. For detailed parameters restrictions see individual setter methods.
     * @param number not null nor empty
     * @param text not null
     * @param gateway not null nor empty
     * @param name
     * @param senderNumber
     * @param senderName
     */
    public SMS(String number, String text, String gateway, String name,
            String senderNumber, String senderName) {
        setNumber(number);
        setText(text);
        setGateway(gateway);
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

    /** Return name of the recipient, or if that's empty, his number
     */
    public String getRecipient() {
        return StringUtils.defaultIfEmpty(name, number);
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

    /** Hint from gateway where to find security image. May be null. */
    public String getImageHint() {
        return imageHint;
    }

    /** Gateway of the message. Never null nor empty. */
    public String getGateway() {
        return gateway;
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

    /** Message from gateway. Never null. */
    public String getGatewayMsg() {
        return gatewayMsg;
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

    /** Hint from gateway where to find security image. May be null. */
    public void setImageHint(String imageHint) {
        this.imageHint = imageHint;
    }

    /** Gateway of the message. May not be null nor empty. */
    public void setGateway(String gateway) {
        Validate.notEmpty(gateway);
        this.gateway = gateway;
    }

    /** Status of the message. May not be null. */
    void setStatus(Status status) {
        Validate.notNull(status);
        if (this.status != status) {
            logger.finer("SMS " + this + " has a new status: " + status);
        }
        this.status = status;
    }

    /** Error message from sending. Null value is changed to empty string. */
    public void setErrMsg(String errMsg) {
        errMsg = StringUtils.defaultString(errMsg);
        if (!StringUtils.equals(errMsg, this.errMsg)) {
            logger.finer("SMS " + this + " has a new error message: " + errMsg);
        }
        this.errMsg = errMsg;
    }

    /** Name of the recipient. Null value is changed to empty string. */
    public void setName(String name) {
        this.name = StringUtils.defaultString(name);
    }

    /** Message from gateway. Null value is changed to empty string. */
    public void setGatewayMsg(String gatewayMsg) {
        gatewayMsg = StringUtils.defaultString(gatewayMsg);
        if (!StringUtils.equals(gatewayMsg, this.gatewayMsg)) {
            logger.finer("SMS " + this + " has a new gateway message: " + gatewayMsg);
        }
        this.gatewayMsg = gatewayMsg;
    }
    // </editor-fold>
    
    @Override
    public String toString() {
        return "[recipient=" + StringUtils.defaultIfEmpty(name, Contact.anonymizeNumber(number)) +
               ", gateway=" + gateway + "]";
    }

    /** Return very detailed description of the instance, used mainly for
     * debugging purposes.
     */
    public String toDebugString() {
        return "[name=" + name + ", number=" + Contact.anonymizeNumber(number) +
                ", gateway=" + gateway + ", status=" + status + ", gatewayMsg=" +
                gatewayMsg + ", errMsg=" + errMsg + "]";
    }
}
