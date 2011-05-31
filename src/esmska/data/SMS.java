package esmska.data;

import esmska.transfer.GatewayExecutor.Problem;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.lang.ObjectUtils;
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
    private ImageIcon image; //security image
    private String imageCode = ""; //security image code
    private String imageHint; //hint from gateway where to find security image (eg. sent to cell)
    private String gateway;
    private Status status = Status.NEW; //sms status
    private String supplMsg = ""; //supplemental message from gateway about remaining credit, etc
    private Tuple<Problem, String> problem; //problem enum, string param
    
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

    /** Shortcut for SMS(number, text, gateway, null). */
    public SMS(String number, String text, String gateway) {
        this(number, text, gateway, null);
    }

    /** Constructs new SMS. For detailed parameters restrictions see individual setter methods.
     * @param number not null nor empty
     * @param text not null
     * @param gateway not null nor empty
     * @param name
     */
    public SMS(String number, String text, String gateway, String name) {
        setNumber(number);
        setText(text);
        setGateway(gateway);
        setName(name);
    }

    /** Get signature matching current gateway or null if no such found. */
    private Signature getSignature() {
        Gateway gw = Gateways.getInstance().get(gateway);
        if (gw == null) {
            return null;
        }
        String sigName = gw.getConfig().getSignature();
        Signature signature = Signatures.getInstance().get(sigName);
        return signature;
    }

    /** Return whether some error occured during sending.
     * SMS is problematic if there is some problem stored.
     */
    public boolean isProblematic() {
        return problem != null;
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
        return getSignature() == null ? "" : getSignature().getUserNumber();
    }

    /** Sender name. Never null. */
    public String getSenderName() {
        return getSignature() == null ? "" : getSignature().getUserName();
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

    /** Name of the recepient. Never null. */
    public String getName() {
        return name;
    }

    /** Supplemental message from gateway. Never null. */
    public String getSupplMsg() {
        return supplMsg;
    }

    /** Get the current problem this SMS has. May be null. */
    public Tuple<Problem, String> getProblem() {
        return problem;
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
            logger.log(Level.FINER, "SMS {0} has a new status: {1}", 
                    new Object[]{this, status});
        }
        this.status = status;
    }

    /** Name of the recipient. Null value is changed to empty string. */
    public void setName(String name) {
        this.name = StringUtils.defaultString(name);
    }

    /** Message from gateway. Null value is changed to empty string. */
    public void setSupplMsg(String supplMsg) {
        supplMsg = StringUtils.defaultString(supplMsg);
        if (!StringUtils.equals(supplMsg, this.supplMsg)) {
            logger.log(Level.FINER, "SMS {0} has a new supplemental message: {1}", 
                    new Object[]{this, supplMsg});
        }
        this.supplMsg = supplMsg;
    }

    /** Set the current problem this SMS has. */
    public void setProblem(Tuple<Problem, String> problem) {
        if (!ObjectUtils.equals(problem, this.problem) && problem != null) {
            logger.log(Level.FINER, "SMS {0} has a new problem: {1}", 
                    new Object[]{this, problem});
        }
        this.problem = problem;
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
                ", gateway=" + gateway + ", status=" + status + ", supplMsg=" +
                supplMsg + ", problem=" + problem + "]";
    }
}
