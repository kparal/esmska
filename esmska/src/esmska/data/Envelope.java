/*
 * Envelope.java
 *
 * Created on 14. srpen 2007, 20:18
 *
 */

package esmska.data;

import esmska.operators.Operator;
import esmska.operators.OperatorUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import esmska.persistence.PersistenceManager;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.Normalizer;
import java.util.logging.Logger;

/** Class for preparing attributes of sms (single or multiple)
 *
 * @author ripper
 */
public class Envelope {
    private static final Config config = PersistenceManager.getConfig();
    private static final Logger logger = Logger.getLogger(Envelope.class.getName());
    private String text;
    private Set<Contact> contacts = new HashSet<Contact>();

    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
    
    /** get text of sms */
    public String getText() {
        return text;
    }
    
    /** set text of sms */
    public void setText(String text) {
        String oldText = this.text;
        if (config.isRemoveAccents()) {
            text = removeAccents(text);
        }
        this.text = text;
        changeSupport.firePropertyChange("text", oldText, text);
    }
    
    /** get all recipients */
    public Set<Contact> getContacts() {
        return Collections.unmodifiableSet(contacts);
    }
    
    /** set all recipients */
    public void setContacts(Set<Contact> contacts) {
        Set<Contact> oldContacts = this.contacts;
        this.contacts = contacts;
        changeSupport.firePropertyChange("contacts", oldContacts, contacts);
    }
    
    /** get maximum length of sendable message */
    public int getMaxTextLength() {
        int min = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            Operator operator = OperatorUtil.getOperator(c.getOperator());
            if (operator == null) {
                continue;
            }
            int value = operator.getMaxChars() * operator.getMaxParts();
            value -= getSignatureLength(c); //subtract signature length
            min = Math.min(min,value);
        }
        return min;
    }
    
    /** get length of one sms */
    public int getSMSLength() {
        int min = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            Operator operator = OperatorUtil.getOperator(c.getOperator());
            if (operator == null) {
                continue;
            }
            min = Math.min(min, operator.getSMSLength());
        }
        return min;
    }
    
    /** get number of sms from these characters */
    public int getSMSCount(int chars) {
        int worstOperator = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            Operator operator = OperatorUtil.getOperator(c.getOperator());
            if (operator == null) {
                continue;
            }
            worstOperator = Math.min(worstOperator,
                    operator.getSMSLength());
        }
        
        chars += getSignatureLength();
        int count = chars / worstOperator;
        if (chars % worstOperator != 0) {
            count++;
        }
        return count;
    }
    
    /** Get maximum signature length of the contact operators in the envelope */
    public int getSignatureLength() {
        String senderName = config.getSenderName();
        //user has no signature
        if (!config.isUseSenderID() || senderName == null || senderName.length() <= 0) {
            return 0;
        }
        
        int worstSignature = 0;
        //find maximum signature length
        for (Contact c : contacts) {
            Operator operator = OperatorUtil.getOperator(c.getOperator());
            if (operator == null) {
                continue;
            }
            worstSignature = Math.max(worstSignature, 
                    operator.getSignatureExtraLength());
        }
        
        //no operator supports signature
        if (worstSignature == 0) {
            return 0;
        } else {
            //add the signature length itself
            return worstSignature + senderName.length();
        }
    }
    
    /** generate list of sms's to send */
    public ArrayList<SMS> generate() {
        ArrayList<SMS> list = new ArrayList<SMS>();
        for (Contact c : contacts) {
            Operator operator = OperatorUtil.getOperator(c.getOperator());
            int limit = (operator != null ? operator.getMaxChars() : Integer.MAX_VALUE);
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
        logger.fine("Envelope specified for " + contacts.size() + 
                " contact(s) generated " + list.size() + " SMS(s)");
        return list;
    }
    
    /** get length of signature needed to be substracted from message length */
    private int getSignatureLength(Contact c) {
        Operator operator = OperatorUtil.getOperator(c.getOperator());
        if (operator != null && config.isUseSenderID() &&
                config.getSenderName() != null &&
                config.getSenderName().length() > 0) {
            return operator.getSignatureExtraLength() + config.getSenderName().length();
        } else {
            return 0;
        }
    }
    
    /** remove diacritical marks from text */
    private static String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).
                replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
