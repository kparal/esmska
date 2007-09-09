/*
 * Envelope.java
 *
 * Created on 14. srpen 2007, 20:18
 *
 */

package esmska;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import persistence.Config;
import persistence.Contact;
import persistence.PersistenceManager;
import persistence.SMS;

/** Class for preparing attributes of sms (single or multiple)
 *
 * @author ripper
 */
public class Envelope {
    private Config config;
    private String text;
    private Set<Contact> contacts = new HashSet<Contact>();
    //TODO: add possibility of event listeners
    
    /** Creates a new instance of Envelope */
    public Envelope() {
        this.config = PersistenceManager.getConfig();
    }
    
    /** get text of sms */
    public String getText() {
        return text;
    }
    
    /** set text of sms */
    public void setText(String text) {
        this.text = text;
    }
    
    /** get all recipients */
    public Set<Contact> getContacts() {
        return Collections.unmodifiableSet(contacts);
    }
    
    /** set all recipients */
    public void setContacts(Set<Contact> contacts) {
        this.contacts = contacts;
    }
    
    /** get maximum length of sendable message */
    public int getMaxTextLength() {
        int min = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            int value = c.getOperator().getMaxChars() * c.getOperator().getMaxParts();
            value -= getSignatureLength(c); //subtract signature length
            min = Math.min(min,value);
        }
        return min;
    }
    
    /** get length of one sms */
    public int getSMSLength() {
        int min = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            min = Math.min(min, c.getOperator().getSMSLength());
        }
        return min;
    }
    
    /** get number of sms from these characters */
    public int getSMSCount(int chars) {
        int worstOperator = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            worstOperator = Math.min(worstOperator,
                    c.getOperator().getSMSLength() - getSignatureLength(c));
        }
        
        int count = chars / worstOperator;
        if (chars % worstOperator != 0)
            count++;
        return count;
    }
    
    /** get list of sms's to send */
    public ArrayList<SMS> send() {
        ArrayList<SMS> list = new ArrayList<SMS>();
        for (Contact c : contacts) {
            String text = getText();
            int limit = c.getOperator().getMaxChars();
            for (int i=0;i<text.length();i+=limit) {
                String cutText = text.substring(i,Math.min(i+limit,text.length()));
                SMS sms = new SMS();
                sms.setNumber(c.getNumber());
                sms.setText(cutText);
                sms.setOperator(c.getOperator());
                sms.setName(c.getName());
                if (config.isUseSenderID()) { //append signature if requested
                    sms.setSenderNumber(config.getSenderNumber());
                    sms.setSenderName(config.getSenderName());
                }
                list.add(sms);
            }
        }
        return list;
    }
    
    /** get length of signature needed to be substracted from message length */
    private int getSignatureLength(Contact c) {
        if (config.isUseSenderID() && c.getOperator().isSignatureSupported() &&
                config.getSenderName() != null &&
                config.getSenderName().length() != 0)
            return c.getOperator().getSignatureExtraLength() + config.getSenderName().length();
        else
            return 0;
    }
}
