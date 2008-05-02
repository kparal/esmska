/*
 * SMS.java
 *
 * Created on 6. červenec 2007, 17:15
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska.data;

import javax.swing.ImageIcon;

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
    private ImageIcon image; //security image
    private String imageCode; //security image code
    private String operator;
    private Status status = Status.WAITING; //sms status
    private String errMsg; //potential error
    private String operatorMsg; //additional message from operator
    
    /** Status of SMS */
    public static enum Status {
        /** new, waiting for sending */
        WAITING, 
        /** some error occured during sending */
        PROBLEMATIC,
        /** sent ok */
        SENT_OK; 
    }
    
    // <editor-fold defaultstate="collapsed" desc="Get Methods">
    public String getNumber() {
        return number;
    }
    
    public String getText() {
        return text;
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public ImageIcon getImage() {
        return image;
    }
    
    public String getImageCode() {
        return imageCode;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public String getErrMsg() {
        return errMsg;
    }
    
    public String getName() {
        return name;
    }

    public String getOperatorMsg() {
        return operatorMsg;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Set Methods">
    public void setNumber(String number) {
        this.number = number;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public void setImage(ImageIcon image) {
        this.image = image;
    }
    
    public void setImageCode(String imageCode) {
        this.imageCode = imageCode;
    }
    
    public void setOperator(String operator) {
        this.operator = operator;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setOperatorMsg(String operatorMsg) {
        this.operatorMsg = operatorMsg;
    }
    // </editor-fold>
    
    @Override
    public String toString() {
        return getName()!=null&&!getName().equals("")?getName():getNumber();
    }
}
