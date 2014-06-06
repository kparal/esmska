package esmska.data;

import esmska.utils.MiscUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
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
    private String text;
    private Set<Contact> contacts = new HashSet<Contact>();
    private Gateway gateway; //current reference gateway
    private String senderName; //current reference signature user name

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
            text = MiscUtils.removeAccents(text);
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
        this.gateway = computeWorstGateway();
        this.senderName = extractSenderName();
        changeSupport.firePropertyChange("contacts", oldContacts, contacts);
    }
    
    /* Get current reference sender name used for display and computational purposes */
    public String getSenderName() {
        return senderName;
    }
    
    /* Get the gateway allowing shortest messages from all contacts */
    private Gateway computeWorstGateway() {
        Gateway worstGateway = null;
        int worstLength = Integer.MAX_VALUE;
        
        for (Contact c : contacts) {
            Gateway gw = gateways.get(c.getGateway());
            if (gw == null) {
                continue;
            }
            if (gw.getSMSLength() < worstLength) {
                worstLength = gw.getSMSLength();
                worstGateway = gw;
            }
        }
        
        return worstGateway;
    }
    
    /* Extract sender name from current gateway */
    private String extractSenderName() {
        if (gateway == null) {
            return "";
        } else {
            return gateway.getSenderName();
        }
    }
    
    /** get maximum length of sendable message */
    public int getMaxTextLength() {
        if (gateway != null) {
            return gateway.getMaxChars() * gateway.getMaxParts();
        } else {
            return Gateway.maxMessageLength;
        }
    }
    
    /** get length of one sms */
    public int getSMSLength() {
        if (gateway != null) {
            return gateway.getSMSLength();
        } else {
            return Gateway.maxMessageLength;
        }
    }
    
    /** get number of sms from these characters 
     * @return resulting number of sms
     */
    public int getSMSCount(int chars) {
        int count = chars / getSMSLength();
        if (chars % getSMSLength() != 0) {
            count++;
        }
        return count;
    }
    
    /** generate list of sms's to send */
    public ArrayList<SMS> generate() {
        ArrayList<SMS> list = new ArrayList<SMS>();
        for (Contact c : contacts) {
            Gateway gateway = gateways.get(c.getGateway());
            int limit = (gateway != null ? gateway.getMaxChars() : Gateway.maxMessageLength);
            // fix user signature in multisend mode
            String msgText = StringUtils.removeStart(text, senderName);
            if (gateway != null) {
                msgText = gateway.getSenderName() + msgText;
            }
            String messageId = SMS.generateID();
            // cut out the messages
            for (int i=0;i<msgText.length();i+=limit) {
                String cutText = msgText.substring(i,Math.min(i+limit,msgText.length()));
                SMS sms = new SMS(c.getNumber(), cutText, c.getGateway(), c.getName(), messageId);
                list.add(sms);
            }
        }
        logger.log(Level.FINE, "Envelope specified for {0} contact(s) generated {1} SMS(s)", 
                new Object[]{contacts.size(), list.size()});
        return list;
    }
    
}
