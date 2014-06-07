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
import org.apache.commons.lang.Validate;

/** Class for preparing attributes of sms (single or multiple)
 *
 * @author ripper
 */
public class Envelope {
    private static final Config config = Config.getInstance();
    private static final Logger logger = Logger.getLogger(Envelope.class.getName());
    private static final Gateways gateways = Gateways.getInstance();
    private String text = "";
    private Set<Contact> contacts = new HashSet<Contact>();
    private Gateway gateway; //current reference gateway
    private String senderName; //current reference signature user name
    // how much (in percents) you can cut down the message in order to keep word boundaries
    private static final double wordCutSpread = 0.1; 
    
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
        text = StringUtils.defaultString(text, "");
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

    /** get maximum length of sendable message
     * @param customText a message text to measure. Because of cutting message
     * by word boundaries, the maximum length varies depending on how the input
     * text is structured.
     */
    public int getMaxTextLength(String customText) {
        if (gateway == null) {
            return Gateway.maxMessageLength;
        }
        
        int maxLength = gateway.getMaxChars() * gateway.getMaxParts();
        
        //some characters were wasted by splitting text by word boundaries
        int lostChars = computeLostCharsByWordCutting(customText, gateway.getMaxChars());
        maxLength -= lostChars;

        return maxLength;
    }
    
    /** get maximum length of sendable message. Uses envelope text to measure it.
     */
    public int getMaxTextLength() {
        return getMaxTextLength(text);
    }
    
    /** get length of one sms */
    public int getSMSLength() {
        if (gateway != null) {
            return gateway.getSMSLength();
        } else {
            return Gateway.maxMessageLength;
        }
    }

    /**
     * Get number of sms pieces cutting from msgText depending on max SMS length
     * limit. The pieces will be split by word boundaries, unless it would take
     * away more than 10% of the text - in that case it will be split 
     * by characters (splitting the word).
     *
     * @param msgText full message text
     * @return resulting number of sms cutting from msgText
     */
    public int getSMSCount(String msgText, int limit) {
        return getIndicesOfCuts(msgText,limit).size();
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
            ArrayList<String> messages = cutOutMessages(msgText, limit);
            for (String cutText : messages) {
                SMS sms = new SMS(c.getNumber(), cutText, c.getGateway(), c.getName(), messageId);
                list.add(sms);
            }
        }
        logger.log(Level.FINE, "Envelope specified for {0} contact(s) generated {1} SMS(s)",
                new Object[]{contacts.size(), list.size()});
        return list;
    }

    /** Take a full message text and cut it out into pieces depending on max SMS
     * length limit, while trying to keep word boundaries.
     *
     * @param msgText full message text
     * @param limit max message/piece length
     * @return a list of cut out pieces of msgText
     */
    private ArrayList<String> cutOutMessages(String msgText, int limit) {
        ArrayList<String> messages = new ArrayList<String>();
        
        int from = 0; //start index of cut, inclusive
        //to - ending index, exclusive
        for (Integer to : getIndicesOfCuts(msgText, limit)) {
            String cutText = msgText.substring(from, to);
            messages.add(cutText);
            from = to;
        }

        return messages;
    }

    /** Cut text into pieces while trying to keep word boundaries.
     * 
     * @param msgText full message text
     * @param limit max single piece length
     * @return list containing indices of cuts (in ascending order)
     */
    public ArrayList<Integer> getIndicesOfCuts(String msgText, int limit) {
        Validate.isTrue(limit > 0, "Can't have zero or negative length limit");
        
        ArrayList<Integer> listOfCuts = new ArrayList<Integer>();
        
        int cutLength;
        for (int from = 0; from < msgText.length(); from += cutLength) {
            int indexOfCut = findIndexOfCut(msgText, from, limit);
            cutLength = indexOfCut - from; //length of currently cut out text
            
            if (cutLength <= 0) {
                // we would be stuck in a loop
                throw new IllegalStateException("Error while message cutting");
            }
            
            listOfCuts.add(indexOfCut); //add index into list
        }

        return listOfCuts;
    }
    
    /** Find the first index of cut in a text, which would try to keep word
     * boundaries.
     * If the word boundaries can't be kept (controlled by wordCutSpread),
     * standard per-character cutting will be done instead.
     * 
     * @param msgText full message text
     * @param start the beginning index, inclusive
     * @param limit max message/piece length
     * @return the ending index of the cut, exclusive
     */
    private int findIndexOfCut(String msgText, int start, int limit) {
        //initial index of cut, not aware of word boundaries
        int indexOfCut = start + limit;

        if (indexOfCut >= msgText.length()) {
            //we've already exceeding text length, no word searching needed
            return msgText.length();
        }
        
        if (Character.isWhitespace(msgText.charAt(indexOfCut)) ||
                Character.isWhitespace(msgText.charAt(indexOfCut - 1))) {
            //we've hit the ideal spot between words, no more work needed
            return indexOfCut;
        }

        //compute the index we can't cross when searching for word boundaries
        int minIndexOfCut = start + ((int)(limit * (1 - wordCutSpread)));
        
        while (indexOfCut > 0 &&
                !Character.isWhitespace(msgText.charAt(indexOfCut - 1))) {
            //traverse left until we find a whitespace
            indexOfCut--;
            
            if (indexOfCut < minIndexOfCut) {
                //whitespace not found and we've already crossed the allowed line
                //return the original cut
                return (start + limit);
            }
        }

        return indexOfCut;
    }

    /**
     * If exists penultimate index of cut msgText to SMS pieces, find it,
     * other return 0;
     * 
     * @param msgText full message text
     * @param limit max length of SMS
     * @return penultimate index of cut or 0
     */
    public int getPenultimateIndexOfCut(String msgText, int limit) {
        ArrayList<Integer> indicesOfCuts = getIndicesOfCuts(msgText, limit);
        if (indicesOfCuts.size() <= 1) {
            return 0;
        }
        
        return  (indicesOfCuts.get(indicesOfCuts.size() - 2));
    }

    /**
     * This function calculates the number of white spaces resulting by splitting 
     * of msgText to the pieces by word boundaries.
     * 
     * @param msgText full message text
     * @param limit max length of SMS
     * @return the number of white spaces
     */
    private int computeLostCharsByWordCutting(String msgText, int limit) {
        int lostChars = ((getSMSCount(msgText, limit) - 1) * limit)
                - getPenultimateIndexOfCut(msgText, limit);
        if (lostChars <= 0) {
            return 0;
        }
        return lostChars;
    }

    
}
