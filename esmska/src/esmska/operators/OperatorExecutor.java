/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.gui.MainFrame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

    private static final Logger logger = Logger.getLogger(OperatorExecutor.class.getName());
    /** Message that recepient number was wrong. */
    public static final String ERROR_WRONG_NUMBER =
            "Zadali jste nesprávné číslo příjemce.";
    /** Message that security code was wrong. */
    public static final String ERROR_WRONG_CODE =
            "Špatně jste opsali bezpečnostní kód.";
    /** Message that message text was wrong. */
    public static final String ERROR_WRONG_TEXT =
            "Zadali jste nesprávný text zprávy. Prázdný, nebo příliš dlouhý?";
    /** Message that sender signature was wrong. */
    public static final String ERROR_WRONG_SIGNATURE =
            "Zadali jste nesprávný podpis odesilatele (číslo či jméno).";
    /** Message that login or password was wrong. */
    public static final String ERROR_WRONG_AUTH =
            "Zadali jste nesprávné přihlašovací údaje.<br>" +
            "Možná jste je jen zapomněli vyplnit v nastavení programu.";
    /** Message that user has not waited long enough to send another message
     * or message quota has been reached. */
    public static final String ERROR_LIMIT_REACHED =
            "Odesíláte zprávu příliš brzy. Operátor buď vyžaduje určitý interval,<br>" +
            "který je nutný vyčkat před odesláním další zprávy, nebo omezuje<br>" +
            "maximální možný počet zpráv za jistý časový úsek. Zkuste to později.";
    /** Message preceding operator provided error message. */
    public static final String ERROR_OPERATOR_MESSAGE =
            "Zpráva od operátora:<br>";
    /** Message that uknown error happened. */
    public static final String ERROR_UKNOWN =
            "Došlo k neznámé chybě. Operátor možná změnil<br>" +
            "své webové stránky, nebo s nimi má aktuálně potíže.<br>" +
            "Zkuste to později.<br>" +
            "<br>" +
            "Taktéž se ujistěte, máte funkční připojení k Internetu<br>" +
            "a že používáte nejnovější verzi programu.<br>" +
            "<br>" +
            "Pokud potíže přetrvají a webová brána operátora funguje,<br>" +
            "nahlaste problém na domovských stránkách programu.";
    /** Message saying how many free SMS are remaining. */
    public static final String INFO_FREE_SMS_REMAINING = "Zbývá volných SMS: ";
    
    private OperatorConnector connector = new OperatorConnector();
    private String errorMessage;
    private String operatorMessage;
    private String referer;

    /** Make a GET request to a provided URL
     * @param url base url where to connect, without any parameters or "?" at the end
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
        try {
            if (imageBytes == null) {
                return "";
            }

            ImageIcon image = new ImageIcon(imageBytes);

            //display dialog
            final JPanel panel = new JPanel();
            JLabel label = new JLabel("Opište kód z obrázku:",
                    image, JLabel.CENTER);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.setVerticalTextPosition(JLabel.TOP);
            panel.add(label);
            FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() {
                    String imageCode = JOptionPane.showInputDialog(MainFrame.getInstance(),
                            panel, "Kontrolní kód",
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

    /** Error message displayed when sending was unsuccessful. */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /** Additional optional message from operator that is shown after message sending. */
    public void setOperatorMessage(String operatorMessage) {
        this.operatorMessage = operatorMessage;
    }
    
    /** Additional optional message from operator. */
    public String getOperatorMessage() {
        return operatorMessage;
    }

    /** Referer used for the following requests.
     * Use null for clearing the field.
     */
    public void setReferer(String referer) {
        this.referer = referer;
    }
    
}
