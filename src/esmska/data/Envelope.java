package esmska.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.Normalizer;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

/** Class for preparing attributes of sms (single or multiple)
 *
 * @author ripper
 */
public class Envelope {
    private static final Config config = Config.getInstance();
    private static final Logger logger = Logger.getLogger(Envelope.class.getName());
    private static final Gateways gateways = Gateways.getInstance();
    private static final Signatures signatures = Signatures.getInstance();
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
            Gateway gateway = gateways.get(c.getGateway());
            if (gateway == null) {
                continue;
            }
            int value = gateway.getMaxChars() * gateway.getMaxParts();
            value -= getSignatureLength(c); //subtract signature length
            min = Math.min(min,value);
        }
        return min;
    }
    
    /** get length of one sms
     * @return length of one sms or -1 when sms length is unspecified
     */
    public int getSMSLength() {
        int min = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            Gateway gateway = gateways.get(c.getGateway());
            if (gateway == null) {
                continue;
            }
            min = Math.min(min, gateway.getSMSLength());
        }
        if (min <= 0) {
            //sms length is unspecified
            min = -1;
        }
        return min;
    }
    
    /** get number of sms from these characters 
     * @return resulting number of sms or -1 when length of sms is unspecified
     */
    public int getSMSCount(int chars) {
        int worstGateway = Integer.MAX_VALUE;
        for (Contact c : contacts) {
            Gateway gateway = gateways.get(c.getGateway());
            if (gateway == null) {
                continue;
            }
            worstGateway = Math.min(worstGateway,
                    gateway.getSMSLength());
        }

        if (worstGateway <= 0) {
            //sms length is unspecified
            return -1;
        }

        chars += getSignatureLength();
        int count = chars / worstGateway;
        if (chars % worstGateway != 0) {
            count++;
        }
        return count;
    }
    
    /** Get maximum signature length of the contact gateways in the envelope */
    public int getSignatureLength() {
        int worstSignature = 0;
        //find maximum signature length
        for (Contact c : contacts) {
            Gateway gateway = gateways.get(c.getGateway());
            if (gateway == null) {
                continue;
            }
            Signature signature = signatures.get(gateway.getConfig().getSignature());
            if (signature == null) {
                continue;
            }
            worstSignature = Math.max(worstSignature, 
                    gateway.getSignatureExtraLength() + StringUtils.length(signature.getUserName()));
        }
        
        return worstSignature;
    }
    
    /** generate list of sms's to send */
    public ArrayList<SMS> generate() {
        ArrayList<SMS> list = new ArrayList<SMS>();
        for (Contact c : contacts) {
            Gateway gateway = gateways.get(c.getGateway());
            int limit = (gateway != null ? gateway.getMaxChars() : Integer.MAX_VALUE);
            for (int i=0;i<text.length();i+=limit) {
                String cutText = text.substring(i,Math.min(i+limit,text.length()));
                SMS sms = new SMS(c.getNumber(), cutText, c.getGateway());
                sms.setName(c.getName());
                list.add(sms);
            }
        }
        logger.fine("Envelope specified for " + contacts.size() + 
                " contact(s) generated " + list.size() + " SMS(s)");
        return list;
    }
    
    /** get length of signature needed to be subtracted from message length */
    private int getSignatureLength(Contact c) {
        Gateway gateway = gateways.get(c.getGateway());
        if (gateway != null) {
             Signature signature = signatures.get(gateway.getConfig().getSignature());
             if (signature != null && StringUtils.length(signature.getUserName()) > 0) {
                 return gateway.getSignatureExtraLength() + signature.getUserName().length();
             }
        }
        return 0;
    }
    
    /** remove diacritical marks from text */
    private static String removeAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).
                replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
