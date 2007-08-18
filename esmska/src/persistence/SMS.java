/*
 * SMS.java
 *
 * Created on 6. červenec 2007, 17:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package persistence;

import java.net.URL;
import java.text.Normalizer;
import operators.Operator;

/** SMS entity class
 *
 * @author ripper
 */
public class SMS {
    private String number; //recipient number
    private String name; //recipient name
    private String text; //message text
    private String senderNumber;
    private String senderName;
    private URL image; //security image
    private String imageCode; //security image code
    private Operator operator;
    private Status status; //sms status
    private String errMsg; //potential error
    
    /** Status of SMS */
    public static enum Status {
        /** new, waiting for sending */
        WAITING, 
        /** some error occured during sending */
        PROBLEMATIC,
        /** sent ok */
        SENT_OK; 
    }
    
    /** Creates a new instance of SMS */
    public SMS() {
    }
    
    /** remove diacritical marks from text */
    private static String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).
                replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    public String toString() {
        return getName()!=null&&!getName().equals("")?getName():getNumber();
    }
    
    public String getNumber() {
        return number;
    }
    
    public void setNumber(String number) {
        this.number = number;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = removeAccents(text);
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public URL getImage() {
        return image;
    }
    
    public void setImage(URL image) {
        this.image = image;
    }
    
    public String getImageCode() {
        return imageCode;
    }
    
    public void setImageCode(String imageCode) {
        this.imageCode = imageCode;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public void setOperator(Operator operator) {
        this.operator = operator;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getErrMsg() {
        return errMsg;
    }
    
    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
}
