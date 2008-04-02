/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.gui.MainFrame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Class containing methods, which can be called from operator scripts.
 *
 * @author ripper
 */
public class OperatorExecutor {
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
    /** Message preceding operator provided error message. */
    public static final String ERROR_OPERATOR_MESSAGE =
            "Zpráva od operátora:<br>";
    /** Message that uknown error happened. */
    public static final String ERROR_UKNOWN =
            "Došlo k neznámé chybě. Operátor možná změnil<br>" +
            "své webové stránky, nebo s nimi má aktuálně potíže.<br>" +
            "Zkuste to později, případně zkontrolujte aktualizaci programu.<br>" +
            "<br>" +
            "Pokud potíže přetrvají a webová brána operátora funguje,<br>" +
            "nahlaste problém na domovských stránkách programu.";
    
    private String errorMessage;
    private String referer;
    private boolean useCookies;

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
        OperatorConnector connector = new OperatorConnector();
        connector.setURL(url);
        connector.setParams(params);
        connector.setReferer(referer);
        connector.setUseCookies(useCookies);

        boolean ok = connector.connect();
        if (!ok) {
            throw new IOException("Could not connect to URL");
        }

        if (connector.isTextContent()) {
            return connector.getTextContent();
        } else {
            return connector.getBinaryContent();
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
        OperatorConnector connector = new OperatorConnector();
        connector.setURL(url);
        connector.setParams(params);
        connector.setPostData(postData);
        connector.setReferer(referer);
        connector.setUseCookies(useCookies);
        connector.setDoPost(true);

        boolean ok = connector.connect();
        if (!ok) {
            throw new IOException("Could not connect to URL");
        }

        if (connector.isTextContent()) {
            return connector.getTextContent();
        } else {
            return connector.getBinaryContent();
        }
    }

    /** Ask user to recognize provided image code
     * @return Recognized image code. Never returns null, may return empty string.
     */
    public String recognizeImage(byte[] imageBytes) throws InterruptedException,
            InvocationTargetException, ExecutionException {
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

    /** Referer used for the following requests. */
    public void setReferer(String referer) {
        this.referer = referer;
    }

    /** Whether to recieve and send cookies in the following requests.
     * Default is false.
     */
    public void setUseCookies(boolean useCookies) {
        this.useCookies = useCookies;
        if (useCookies) {
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager); //TODO: will concurrent SMS sending interfere?
        } else {
            CookieHandler.setDefault(null);
        }
    }
}
