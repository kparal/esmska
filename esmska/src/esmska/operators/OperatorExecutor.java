/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.gui.MainFrame;
import esmska.utils.L10N;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Class containing methods, which can be called from operator scripts.
 *  For each operator script a separate class should be created.
 * @author ripper
 */
public class OperatorExecutor {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    private static final Logger logger = Logger.getLogger(OperatorExecutor.class.getName());
    /** Message that recepient number was wrong. */
    public static final String ERROR_WRONG_NUMBER =
            l10n.getString("OperatorExecutor.ERROR_WRONG_NUMBER");
    /** Message that security code was wrong. */
    public static final String ERROR_WRONG_CODE =
            l10n.getString("OperatorExecutor.ERROR_WRONG_CODE");
    /** Message that message text was wrong. */
    public static final String ERROR_WRONG_TEXT =
            l10n.getString("OperatorExecutor.ERROR_WRONG_TEXT");
    /** Message that sender signature was wrong. */
    public static final String ERROR_WRONG_SIGNATURE =
            l10n.getString("OperatorExecutor.ERROR_WRONG_SIGNATURE");
    /** Message that login or password was wrong. */
    public static final String ERROR_WRONG_AUTH =
            l10n.getString("OperatorExecutor.ERROR_WRONG_AUTH");
    /** Message that user has not waited long enough to send another message
     * or message quota has been reached. */
    public static final String ERROR_LIMIT_REACHED =
            l10n.getString("OperatorExecutor.ERROR_LIMIT_REACHED");
    /** Message that user does not have sufficient credit. */
    public static final String ERROR_NO_CREDIT =
            l10n.getString("OperatorExecutor.ERROR_NO_CREDIT");
    /** Message that sending failed but operator hasn't provided any reason for it. */
    public static final String ERROR_NO_REASON =
            l10n.getString("OperatorExecutor.ERROR_NO_REASON");
    /** Message preceding operator provided error message. */
    public static final String ERROR_OPERATOR_MESSAGE =
            l10n.getString("OperatorExecutor.ERROR_OPERATOR_MESSAGE");
    /** Message that uknown error happened, maybe error in the script. */
    public static final String ERROR_UKNOWN =
            l10n.getString("OperatorExecutor.ERROR_UKNOWN");
    /** Message saying how many free SMS are remaining. */
    public static final String INFO_FREE_SMS_REMAINING = 
            l10n.getString("OperatorExecutor.INFO_FREE_SMS_REMAINING") + " ";
    /** Message saying how much credit is remaining. */
    public static final String INFO_CREDIT_REMAINING = 
            l10n.getString("OperatorExecutor.INFO_CREDIT_REMAINING") + " ";
    /** Message used when operator provides no info whether message was successfuly sent or not. */
    public static final String INFO_STATUS_NOT_PROVIDED = 
            l10n.getString("OperatorExecutor.INFO_STATUS_NOT_PROVIDED");
    
    private OperatorConnector connector = new OperatorConnector();
    private String errorMessage;
    private String operatorMessage;
    private String referer;

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
                return connector.getTextContent();
            } else {
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
                return connector.getTextContent();
            } else {
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
     * @return Recognized image code. Never returns null, may return empty string.
     */
    public String recognizeImage(byte[] imageBytes) throws InterruptedException,
            InvocationTargetException, ExecutionException {
        logger.fine("Showing security code...");
        try {
            if (imageBytes == null) {
                return "";
            }
            final ImageIcon image = new ImageIcon(imageBytes);

            //display dialog
            FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() {
                    JPanel panel = new JPanel();
                    JLabel label = new JLabel(l10n.getString("OperatorExecutor.recognize_number"),
                            image, JLabel.CENTER);
                    label.setHorizontalTextPosition(JLabel.CENTER);
                    label.setVerticalTextPosition(JLabel.TOP);
                    panel.add(label);
                    
                    String imageCode = JOptionPane.showInputDialog(MainFrame.getInstance(),
                            panel, null,
                            JOptionPane.QUESTION_MESSAGE);
                    return imageCode;
                }
            });
            SwingUtilities.invokeAndWait(task);
            //receive result
            String imageCode = task.get();

            return imageCode != null ? imageCode : "";
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, "Could not execute recognizeImage", ex);
            throw ex;
        } catch (ExecutionException ex) {
            logger.log(Level.WARNING, "Could not execute recognizeImage", ex);
            throw ex;
        } catch (InvocationTargetException ex) {
            logger.log(Level.WARNING, "Could not execute recognizeImage", ex);
            throw ex;
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "Could not execute recognizeImage", ex);
            throw ex;
        }
    }

    /** Error message displayed when sending was unsuccessful.
     * You can use simple HTML tags (HTML 3.2).
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** Additional optional message from operator that is shown after message sending. */
    public void setOperatorMessage(String operatorMessage) {
        this.operatorMessage = operatorMessage;
    }
    
    /** Referer used for the following requests.
     * Use null for clearing the field.
     */
    public void setReferer(String referer) {
        this.referer = referer;
    }
    
    /** Pauses the execution for specified amount of time.
     * Nothing happens if the amount is negative. */
    public void sleep(long milliseconds) throws InterruptedException {
        logger.fine("Sleeping for " + milliseconds + " ms...");
        if (milliseconds <= 0) {
            return;
        }
        Thread.sleep(milliseconds);
    }

    /** Error message displayed when sending was unsuccessful. */
    String getErrorMessage() {
        return errorMessage;
    }
    
    /** Additional optional message from operator. */
    String getOperatorMessage() {
        return operatorMessage;
    }
    
    /** Set preferred language to retrieve web content.
     * @param language two-letter language code as defined in ISO 639-1
     */
    void setPreferredLanguage(String language) {
        connector.setLanguage(language);
    }
}
