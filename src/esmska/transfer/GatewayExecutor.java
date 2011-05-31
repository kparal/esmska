package esmska.transfer;

import esmska.data.CountryPrefix;
import esmska.utils.L10N;
import esmska.data.SMS;
import esmska.data.Tuple;
import esmska.utils.LogSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/** Class containing methods, which can be called from gateway scripts.
 *  For each gateway script a separate class should be created.
 * @author ripper
 */
public class GatewayExecutor {
    
    public static enum Problem {
        /** Gateway script author provided his own message.
         * Requires the message as a parameter (you can use HTML 3.2). */
        CUSTOM_MESSAGE,
        /** A fix for this gateway is being worked on.
         * Requires URL for a webpage with more details as a parameter. */
        FIX_IN_PROGRESS,
        /** Gateway provided its own error message. 
         * Requires gateway message as a parameter (you can use HTML 3.2). */
        GATEWAY_MESSAGE,
        /** This is used for internal Esmska purposes. Don't use it from
         inside gateway scripts.
         Requires the message as a parameter (you can use HTML 3.2). */
        INTERNAL_MESSAGE,
        /** The user has not waited long enough to send another message
         * or message quota has been reached. */
        LIMIT_REACHED,
        /** The message text was too long. */
        LONG_TEXT,
        /** The user does not have sufficient credit. */
        NO_CREDIT,
        /** The sending failed but gateway hasn't provided any reason for it. */
        NO_REASON,
        /** The sender signature was missing. */
        SIGNATURE_NEEDED,
        /** Message that unknown error happened, maybe error in the script.
        * Make sure you set this problem before making any other HTTP requests
        * (logging out, etc), because the last web content will get logged
        * automatically right after you set this problem. */
        UNKNOWN,
        /** This gateway is for some reason currently unusable.
         * Requires URL for a webpage with more details as a parameter. */
        UNUSABLE,
        /** The login or password was wrong. */
        WRONG_AUTH,
        /** The security code was wrong. */
        WRONG_CODE,
        /** The recepient number was wrong. */
        WRONG_NUMBER,
        /** The sender signature was wrong. */
        WRONG_SIGNATURE,
    }
    
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Logger logger = Logger.getLogger(GatewayExecutor.class.getName());
    
    /** Message saying how many free SMS are remaining. */
    public static final String INFO_FREE_SMS_REMAINING = 
            l10n.getString("GatewayExecutor.INFO_FREE_SMS_REMAINING") + " ";
    /** Message saying how much credit is remaining. */
    public static final String INFO_CREDIT_REMAINING = 
            l10n.getString("GatewayExecutor.INFO_CREDIT_REMAINING") + " ";
    /** Message used when gateway provides no info whether message was successfully sent or not. */
    public static final String INFO_STATUS_NOT_PROVIDED = 
            l10n.getString("GatewayExecutor.INFO_STATUS_NOT_PROVIDED");
    
    private GatewayConnector connector = new GatewayConnector();
    private String referer;
    private SMS sms;
    private String lastTextContent;

    public GatewayExecutor(SMS sms) {
        this.sms = sms;
    }

    /** For description see {@link GatewayConnector#forgetCookie(
     * java.lang.String, java.lang.String, java.lang.String)}
     */
    public void forgetCookie(String name, String domain, String path) {
        connector.forgetCookie(name, domain, path);
    }

    /** Make a GET request to a provided URL
     * @param url base url where to connect, without any parameters or "?" at the end.
     *            In special cases when you don't use params, you can use url as a full url.
     *            But don't forget that parameters values must be url-encoded, which you can't
     *            do properly in JavaScript.
     * @param params array of url params in form [key1,value1,key2,value2,...]
     * @return content of the response. It may be String (when requesting HTML page) or
     *         just an array of bytes (when requesting eg. an image).
     * @throws IOException when there is some problem in connecting
     */
    public Object getURL(String url, String[] params) throws IOException {
        try {
            connector.setConnection(url, params, false, null);
            connector.setReferer(referer);

            boolean ok = connector.connect();
            if (!ok) {
                throw new IOException("Could not connect to URL");
            }

            if (connector.isTextContent()) {
                lastTextContent = connector.getTextContent();
                return connector.getTextContent();
            } else {
                // we don't log binary content
                lastTextContent = null;
                return connector.getBinaryContent();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not execute getURL", ex);
            throw ex;
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Could not execute getURL", ex);
            throw ex;
        }
    }

    /** Make a POST request with specified data to a provided URL.
     * @param url base url where to connect, without any parameters or "?" at the end.
     *            In special cases when you don't use params, you can use url as a full url.
     *            But don't forget that parameters values must be url-encoded, which you can't
     *            do properly in JavaScript.
     * @param params array of url params in form [key1,value1,key2,value2,...]
     * @param postData array of data to be sent in the request in form [key1,value1,key2,value2,...].
     *                 This data will be properly url-encoded before sending.
     * @return content of the response. It may be String (when requesting HTML page) or
     *         just an array of bytes (when requesting eg. an image).
     * @throws IOException when there is some problem in connecting
     */
    public Object postURL(String url, String[] params, String[] postData) throws IOException {
        try {
            connector.setConnection(url, params, true, postData);
            connector.setReferer(referer);

            boolean ok = connector.connect();
            if (!ok) {
                throw new IOException("Could not connect to URL");
            }

            if (connector.isTextContent()) {
                lastTextContent = connector.getTextContent();
                return connector.getTextContent();
            } else {
                // we don't log binary content
                lastTextContent = null;
                return connector.getBinaryContent();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Could not execute postURL", ex);
            throw ex;
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Could not execute postURL", ex);
            throw ex;
        }
    }

    /** Ask user to recognize provided image code
     * @param imageBytes image bytearray. Java must be able to display this image
     *                   (PNG, GIF, JPEG, maybe something else).
     * @param hint optional hint that can gateway say to user.
     * @return Recognized image code. Never returns null, may return empty string.
     */
    public String recognizeImage(byte[] imageBytes, String hint) throws InterruptedException,
            InvocationTargetException, ExecutionException {
        logger.fine("Resolving security code...");
        if (imageBytes == null && StringUtils.isEmpty(hint)) {
            return "";
        }
        ImageIcon image = imageBytes == null ? null : new ImageIcon(imageBytes);
        sms.setImage(image);
        sms.setImageHint(hint);

        boolean resolved = ImageCodeManager.getResolver().resolveImageCode(sms);
        if (!resolved) {
            logger.info("Could not resolve security code or resolving cancelled");
        }

        return StringUtils.defaultString(sms.getImageCode());
    }

    /** Same as calling setProblem(problem, null). */
    public void setProblem(Object problem) {
        setProblem(problem, null);
    }
    
    /** Problem displayed when sending was unsuccessful.
     * @param problem problem from Problem enum
     * @param param some problems require additional string parameter, see their description
     */
    public void setProblem(Object problem, String param) {
        Problem prob = null;
        if (problem instanceof String) {
            prob = Problem.valueOf((String)problem);
        } else {
            prob = (Problem) problem;
        }
        //process additional params
        Problem[] needParams = new Problem[] {
            Problem.CUSTOM_MESSAGE,
            Problem.FIX_IN_PROGRESS,
            Problem.UNUSABLE,
            Problem.GATEWAY_MESSAGE
        };
        if (ArrayUtils.contains(needParams, prob)) {
            if (StringUtils.isEmpty(param)) {
                throw new IllegalArgumentException("Missing additional parameter " +
                    "for provided problem " + prob);
            }
        }
        // log if UNKNOWN (bad content or crash)
        if (prob == Problem.UNKNOWN) {
            logCrash();
        }
        
        sms.setProblem(new Tuple<Problem, String>(prob, param));
    }

    /** Optional supplemental message from gateway that is shown after message sending. */
    public void setSupplementalMessage(String supplMessage) {
        sms.setSupplMsg(supplMessage);
    }
    
    /** Referer (HTTP 'Referer' header) used for all following requests.
     * Use null for resetting current value back to none.
     */
    public void setReferer(String referer) {
        this.referer = referer;
    }
    
    /** Pauses the execution for specified amount of time.
     * Nothing happens if the amount is negative. */
    public void sleep(long milliseconds) throws InterruptedException {
        logger.log(Level.FINE, "Sleeping for {0} ms...", milliseconds);
        if (milliseconds <= 0) {
            return;
        }
        Thread.sleep(milliseconds);
    }

    /** Extract country prefix from phone number.
     * @param phoneNumber Phone number in fully international format. May be null or
     * incomplete.
     * @return Country prefix if valid one is found in the number.
     * Empty string otherwise.
     */
    public String extractCountryPrefix(String phoneNumber) {
        return StringUtils.defaultString(CountryPrefix.extractCountryPrefix(phoneNumber));
    }

    /** Set preferred language to retrieve web content.
     * @param language two-letter language code as defined in ISO 639-1
     */
    void setPreferredLanguage(String language) {
        connector.setLanguage(language);
    }

    /** Log last webpage content preceding crash.
     * Doesn't get logged twice if webpage debugging already enabled.
     * @param content web page content, may be null
     */
    private void logCrash() {
        if (lastTextContent == null) {
            //nothing to log
            return;
        }
        Level level = LogSupport.getEsmskaLogger().getLevel();
        if (level.equals(Level.ALL)) {
            //this content was already logged
            return;
        }
        LogSupport.getEsmskaLogger().setLevel(Level.ALL);
        logger.log(Level.FINEST, "#### WEB CONTENT START ####\n{0}\n#### WEB CONTENT END ####", lastTextContent);
        LogSupport.getEsmskaLogger().setLevel(level);
    }
}
