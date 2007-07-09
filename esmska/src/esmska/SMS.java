/*
 * SMS.java
 *
 * Created on 6. červenec 2007, 17:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import java.net.URL;
import operators.Operator;

/**
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
    
    public enum Status {
        WAITING, //new, waiting for sending
        PROBLEMATIC, //some error occured during sending
        SENT_OK; //sent ok
    }
    
    /** Creates a new instance of SMS */
    public SMS() {
    }

    public String toString() {
        return getName()!=null?getName():getNumber();
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
        this.text = text;
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
