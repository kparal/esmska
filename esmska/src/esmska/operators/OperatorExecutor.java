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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author ripper
 */
public class OperatorExecutor {

    private Boolean success;
    private String errorMessage;

    public Object getURL(String url) throws IOException {
        OperatorConnector connector = new OperatorConnector();
        connector.setURL(url);

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

    public String recognizeImage(byte[] imageBytes) throws InterruptedException,
            InvocationTargetException, ExecutionException {
        if (imageBytes == null) {
            return "";
        }

        ImageIcon image = new ImageIcon(imageBytes);

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
        String imageCode = task.get();

        return imageCode != null ? imageCode : "";
    }

    public Object postURL(String url, String postData) throws IOException {
        OperatorConnector connector = new OperatorConnector();
        connector.setURL(url);
        connector.setDoPost(true);
        connector.setPostData(postData);

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

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean getSuccess() {
        return success != null ? success : false;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
