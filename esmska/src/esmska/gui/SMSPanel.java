/*
 * SMSPanel.java
 *
 * Created on 8. říjen 2007, 16:19
 */

package esmska.gui;

import esmska.ThemeManager;
import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.Envelope;
import esmska.gui.FormChecker;
import esmska.data.SMS;
import esmska.persistence.PersistenceManager;
import esmska.utils.AbstractDocumentListener;
import esmska.utils.ActionEventSupport;
import esmska.utils.L10N;
import esmska.utils.Nullator;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.undo.UndoManager;

/** Panel for writing and sending sms, and for setting immediate contact
 *
 * @author  ripper
 */
public class SMSPanel extends javax.swing.JPanel {
    public static final int ACTION_REQUEST_CLEAR_CONTACT_SELECTION = 0;
    public static final int ACTION_REQUEST_SELECT_CONTACT = 1;
    public static final int ACTION_SEND_SMS = 2;
    
    private static final Logger logger = Logger.getLogger(SMSPanel.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    /** box for messages */
    private Envelope envelope = new Envelope();
    /** support for undo and redo in sms text pane */
    private UndoManager smsTextUndoManager = new UndoManager();
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    private Config config = PersistenceManager.getConfig();
    
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();
    private CompressAction compressAction = new CompressAction();
    private SendAction sendAction = new SendAction();
    private SMSTextPaneListener smsTextPaneListener = new SMSTextPaneListener();
    private SMSTextPaneDocumentFilter smsTextPaneDocumentFilter;
    private RecipientTextField recipientField;
    
    private Contact requestedContactSelection;
    private boolean disableContactListeners;
    
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Creates new form SMSPanel */
    public SMSPanel() {
        initComponents();
        recipientField = (RecipientTextField) recipientTextField;
        
        //if not Substance LaF, add clipboard popup menu to text components
        if (!config.getLookAndFeel().equals(ThemeManager.LAF.SUBSTANCE)) {
            ClipboardPopupMenu.register(smsTextPane);
            ClipboardPopupMenu.register(recipientTextField);
        }
    }
    
    /** validates sms form and returns status */
    private boolean validateForm(boolean transferFocus) {
        if (Nullator.isEmpty(envelope.getText())) {
            if (transferFocus) {
                smsTextPane.requestFocusInWindow();
            }
            return false;
        }
        if (envelope.getText().length() > envelope.getMaxTextLength()) {
            if (transferFocus) {
                smsTextPane.requestFocusInWindow();
            }
            return false;
        }
        if (envelope.getContacts().size() <= 0) {
            if (transferFocus) {
                recipientTextField.requestFocusInWindow();
            }
            return false;
        }
        for (Contact c : envelope.getContacts()) {
            if (!FormChecker.checkSMSNumber(c.getNumber())) {
                if (transferFocus) {
                    recipientTextField.requestFocusInWindow();
                }
                return false;
            }
        }
        return true;
    }
    
    /** Find contact according to filled name/number and operator
     * @param onlyFullMatch whether to look only for full match (name/number and operator)
     *  or even partial match (name/number only)
     * @return found contact or null if none found
     */
    private Contact lookupContact(boolean onlyFullMatch) {
        String number = recipientField.getNumber();
        String id = recipientTextField.getText(); //name or number
        String operatorName = operatorComboBox.getSelectedOperatorName();
        
        if (Nullator.isEmpty(id)) {
            return null;
        }
        
        Contact contact = null; //match on id
        Contact fullContact = null; //match on id and operator
        
        //search in contact numbers
        if (number != null) {
            for (Contact c : contacts) {
                if (Nullator.isEqual(c.getNumber(), number)) {
                    if (Nullator.isEqual(c.getOperator(), operatorName)) {
                        fullContact = c;
                        break;
                    }
                    if (!onlyFullMatch && contact == null) {
                        contact = c; //remember only first partial match, but search further
                    }
                }
            }
        } else {
            //search in contact names if not number
            for (Contact c : contacts) {
                if (id.equalsIgnoreCase(c.getName())) {
                    if (Nullator.isEqual(c.getOperator(), operatorName)) {
                        fullContact = c;
                        break;
                    }
                    if (!onlyFullMatch && contact == null) {
                        contact = c; //remember only first partial match, but search further
                    }
                }
            }
        }
        
        return (fullContact != null ? fullContact : contact);
    }
    
    /** Request a contact to be selected in contact list. Use null for clearing
     * the selection.
     */
    private void requestSelectContact(Contact contact) {
        if (contact != null) {
            requestedContactSelection = contact;
            actionSupport.fireActionPerformed(ACTION_REQUEST_SELECT_CONTACT, null);
        } else {
            actionSupport.fireActionPerformed(ACTION_REQUEST_CLEAR_CONTACT_SELECTION, null);
        }
    }
    
    /** get contact which was requested to be selected in contact list */
    public Contact getRequestedContactSelection() {
        return requestedContactSelection;
    }
    
    /** get currently used envelope */
    public Envelope getEnvelope() {
        return envelope;
    }
    
    /** set selected contacts in contact list or contact to display */
    public void setContacts(Collection<Contact> contacts) {
        if (contacts == null)
            throw new NullPointerException("contacts");

        disableContactListeners = true;
        int count = contacts.size();
        
        if (count == 1) {
            Contact c = contacts.iterator().next();
            recipientField.setContact(c);
            operatorComboBox.setSelectedOperator(c.getOperator());
        }
        
        boolean multiSendMode = (count > 1);
        if (multiSendMode) {
            recipientTextField.setText(l10n.getString("Multiple_sending"));
        }
        recipientTextField.setEnabled(! multiSendMode);
        operatorComboBox.setEnabled(! multiSendMode);
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.addAll(contacts);
        if (count < 1) {
            Contact contact = recipientField.getContact();
            set.add(new Contact(contact != null ? contact.getName() : null,
                    recipientField.getNumber(),
                    operatorComboBox.getSelectedOperatorName()));
        }
        envelope.setContacts(set);
        
        // update components
        sendAction.updateStatus();
        smsTextPaneDocumentFilter.requestUpdate();
        disableContactListeners = false;
    }
    
    /** set sms to display and edit */
    public void setSMS(final SMS sms) {
        recipientField.setNumber(sms.getNumber());
        smsTextPane.setText(sms.getText());
        
        //recipient textfield will change operator, must wait and change operator back
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                operatorComboBox.setSelectedOperator(sms.getOperator());
                smsTextPane.requestFocusInWindow();
            }
        });
    }
    
    /** get currently written sms text
     * @return currently written sms text or empty string; never null
     */
    public String getText() {
        String text = smsTextPane.getText();
        return text != null ? text : "";
    }
    
    /** get undo action used in sms text pane */
    public Action getUndoAction() {
        return undoAction;
    }
    
    /** get redo action used in sms text pane */
    public Action getRedoAction() {
        return redoAction;
    }
    
    /** get compress action used for compressing sms text */
    public Action getCompressAction() {
        return compressAction;
    }
    
    /** get send action used for sending the sms */
    public Action getSendAction() {
        return sendAction;
    }
    
    /** updates values on progress bars according to currently written message chars*/
    private void updateProgressBars() {
        int smsLength = smsTextPane.getText().length();
        
        //set maximums
        singleProgressBar.setMaximum(envelope.getSMSLength());
        fullProgressBar.setMaximum(envelope.getMaxTextLength());
        
        //if we are at the end of the whole message, the current message length
        //can be lesser than usual
        int remainder = envelope.getMaxTextLength() % envelope.getSMSLength();
        if (envelope.getMaxTextLength() - smsLength < remainder) {
            //we have crossed the remainder border, let's update maximum on progress bar
            singleProgressBar.setMaximum(remainder);
        }
        
        //set values
        fullProgressBar.setValue(smsLength);
        singleProgressBar.setValue(smsLength % envelope.getSMSLength());
        //on the border counts we want progress bar full instead of empty
        if (singleProgressBar.getValue() == 0 && smsLength > 0) {
            singleProgressBar.setValue(singleProgressBar.getMaximum());
        }
        
        //set tooltips
        int current = singleProgressBar.getValue();
        int max = singleProgressBar.getMaximum();
        singleProgressBar.setToolTipText(MessageFormat.format(
                l10n.getString("SMSPanel.singleProgressBar"),
                current, max, (max - current)));
        current = fullProgressBar.getValue();
        max = fullProgressBar.getMaximum();
        fullProgressBar.setToolTipText(MessageFormat.format(
                l10n.getString("SMSPanel.fullProgressBar"),
                current, max, (max - current)));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        operatorComboBox = new esmska.gui.OperatorComboBox();
        fullProgressBar = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        smsTextPane = new javax.swing.JTextPane();
        textLabel = new javax.swing.JLabel();
        sendButton = new javax.swing.JButton();
        smsCounterLabel = new javax.swing.JLabel();
        singleProgressBar = new javax.swing.JProgressBar();
        gatewayLabel = new javax.swing.JLabel();
        recipientTextField = new SMSPanel.RecipientTextField();
        recipientLabel = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder(l10n.getString("SMSPanel.border.title"))); // NOI18N
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        operatorComboBox.addActionListener(new OperatorComboBoxActionListener());

        fullProgressBar.setMaximum(1000);

        smsTextPane.getDocument().addDocumentListener(smsTextPaneListener);
        smsTextPaneDocumentFilter = new SMSTextPaneDocumentFilter();
        ((AbstractDocument)smsTextPane.getStyledDocument()).setDocumentFilter(smsTextPaneDocumentFilter);

        //bind actions and listeners
        smsTextUndoManager.setLimit(-1);
        smsTextPane.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                if (e.getEdit().getPresentationName().contains("style"))
                return;
                smsTextUndoManager.addEdit(e.getEdit());
            }
        });

        //this mapping is here bcz of some weird performance improvements when holding undo key stroke
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        String command = "undo";
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), command);
        smsTextPane.getActionMap().put(command,undoAction);
        command = "redo";
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), command);
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            menuMask|KeyEvent.SHIFT_DOWN_MASK), command);
    smsTextPane.getActionMap().put(command, redoAction);

    //ctrl+enter
    command = "send";
    smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuMask), command);
    smsTextPane.getActionMap().put(command, sendAction);
    jScrollPane1.setViewportView(smsTextPane);

    textLabel.setLabelFor(smsTextPane);
    org.openide.awt.Mnemonics.setLocalizedText(textLabel, l10n.getString("SMSPanel.textLabel.text")); // NOI18N
    textLabel.setToolTipText(l10n.getString("SMSPanel.textLabel.toolTipText")); // NOI18N

    sendButton.setAction(sendAction);
    sendButton.setToolTipText(l10n.getString("SMSPanel.sendButton.toolTipText")); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(smsCounterLabel, l10n.getString("SMSPanel.smsCounterLabel.text")); // NOI18N

    singleProgressBar.setMaximum(1000);

    gatewayLabel.setLabelFor(operatorComboBox);
    org.openide.awt.Mnemonics.setLocalizedText(gatewayLabel, l10n.getString("SMSPanel.gatewayLabel.text")); // NOI18N
    gatewayLabel.setToolTipText(operatorComboBox.getToolTipText());

    recipientLabel.setLabelFor(recipientTextField);
    org.openide.awt.Mnemonics.setLocalizedText(recipientLabel, l10n.getString("SMSPanel.recipientLabel.text")); // NOI18N
    recipientLabel.setToolTipText(recipientTextField.getToolTipText());

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(textLabel)
                        .addComponent(singleProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(fullProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sendButton))
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(recipientLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(recipientTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(gatewayLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(operatorComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)))
            .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {fullProgressBar, singleProgressBar});

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {gatewayLabel, recipientLabel, textLabel});

    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(recipientLabel)
                .addComponent(recipientTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(gatewayLabel)
                .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(sendButton)
                        .addComponent(smsCounterLabel)))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(textLabel)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(singleProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(fullProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {fullProgressBar, singleProgressBar});

    }// </editor-fold>//GEN-END:initComponents
    
    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        smsTextPane.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained
    
    /** Send sms to queue */
    private class SendAction extends AbstractAction {
        public SendAction() {
            L10N.setLocalizedText(this, l10n.getString("Send_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "send-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "send-22.png")));
            putValue(SHORT_DESCRIPTION,l10n.getString("Send_message"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!validateForm(true))
                return;
            
            actionSupport.fireActionPerformed(ACTION_SEND_SMS, null);
            
            smsTextPane.setText(null);
            smsTextUndoManager.discardAllEdits();
            smsTextPane.requestFocusInWindow();
        }
        /** update status according to current conditions */
        public void updateStatus() {
            this.setEnabled(validateForm(false));
        }
    }
    
    /** undo in sms text pane */
    private class UndoAction extends AbstractAction {
        public UndoAction() {
            L10N.setLocalizedText(this, l10n.getString("Undo_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "undo-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "undo-32.png")));
            putValue(SHORT_DESCRIPTION, l10n.getString("SMSPanel.Undo_change_in_message_text"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (smsTextUndoManager.canUndo()) {
                smsTextUndoManager.undo();
                smsTextPaneDocumentFilter.requestUpdate();
            }
        }
        /** update status according to current conditions */
        public void updateStatus() {
            setEnabled(smsTextUndoManager.canUndo());
        }
    }
    
    /** redo in sms text pane */
    private class RedoAction extends AbstractAction {
        public RedoAction() {
            L10N.setLocalizedText(this, l10n.getString("Redo_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "redo-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "redo-32.png")));
            putValue(SHORT_DESCRIPTION, l10n.getString("SMSPanel.Redo_change_in_message_text"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (smsTextUndoManager.canRedo()) {
                smsTextUndoManager.redo();
                smsTextPaneDocumentFilter.requestUpdate();
            }
        }
        /** update status according to current conditions */
        public void updateStatus() {
            setEnabled(smsTextUndoManager.canRedo());
        }
    }
    
    /** compress current sms text by rewriting it to CamelCase */
    private class CompressAction extends AbstractAction {
        public CompressAction() {
            L10N.setLocalizedText(this, l10n.getString("Compress_"));
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "compress-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "compress-32.png")));
            putValue(SHORT_DESCRIPTION,l10n.getString("SMSPanel.compress"));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            String text = smsTextPane.getText();
            if (text == null || text.equals("")) {
                return;
            }

            text = text.replaceAll("\\s", " "); //all whitespace to spaces
            text = Pattern.compile("(\\s)\\s+", Pattern.DOTALL).matcher(text).replaceAll("$1"); //remove duplicate whitespaces
            Pattern pattern = Pattern.compile("\\s+(.)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) { //find next space+character
                text = matcher.replaceFirst(matcher.group(1).toUpperCase()); //replace by upper character
                matcher = pattern.matcher(text);
            }
            text = text.replaceAll(" $", ""); //remove trailing space

            if (!text.equals(smsTextPane.getText())) { //do not replace if already compressed
                smsTextPane.setText(text);
            }
        }
        /** update status according to current conditions */
        public void updateStatus() {
            setEnabled(getText().length() > 0);
        }
    }

    /** Another operator selected */
    private class OperatorComboBoxActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (disableContactListeners) {
                return;
            }
            
            //update text editor listeners
            DocumentEvent event = new DocumentEvent() {
                @Override
                public ElementChange getChange(Element elem) {
                    return null;
                }
                @Override
                public Document getDocument() {
                    return smsTextPane.getDocument();
                }
                @Override
                public int getLength() {
                    return 0;
                }
                @Override
                public int getOffset() {
                    return 0;
                }
                @Override
                public EventType getType() {
                    return EventType.INSERT;
                }
            };
            smsTextPaneListener.onUpdate(event);
            
            //select contact only if full match found
            Contact contact = lookupContact(true);
            if (contact != null) {
                requestSelectContact(contact);
            }
            
            //update envelope
            Set<Contact> set = new HashSet<Contact>();
            
            Contact c = recipientField.getContact();
            set.add(new Contact(c != null ? c.getName() : null,
                    recipientField.getNumber(),
                    operatorComboBox.getSelectedOperatorName()));
            envelope.setContacts(set);
            
            //update components
            smsTextPaneDocumentFilter.requestUpdate();
        }
    }
    
    /** Listener for envelope */
    private class EnvelopePropertyListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("contacts")) {
                updateProgressBars();
            }
        }
    }
    
    /** Listener for sms text pane */
    private class SMSTextPaneListener extends AbstractDocumentListener {
        /** count number of chars in sms and take action */
        private void countChars(DocumentEvent e) {
            int chars = e.getDocument().getLength();
            int smsCount = envelope.getSMSCount(chars);
            smsCounterLabel.setText(MessageFormat.format(l10n.getString("SMSPanel.smsCounterLabel.1"),
                    chars, smsCount));
            if (chars > envelope.getMaxTextLength()) { //chars more than max
                smsCounterLabel.setForeground(Color.RED);
                smsCounterLabel.setText(MessageFormat.format(l10n.getString("SMSPanel.smsCounterLabel.2"),
                        chars));
            } else //chars ok
                smsCounterLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
        /** update form components */
        private void updateUI(DocumentEvent e) {
            try {
                envelope.setText(e.getDocument().getText(0,e.getDocument().getLength()));
            } catch (BadLocationException ex) {
                logger.log(Level.SEVERE, "Error getting sms text", ex);
            }
        }
        @Override
        public void onUpdate(DocumentEvent e) {
            countChars(e);
            updateUI(e);
        }
    }
    
    /** Limit maximum sms length and color it */
    private class SMSTextPaneDocumentFilter extends DocumentFilter {
        private StyledDocument doc;
        private Style regular, highlight;
        //updating after each event is slow, therefore there is timer
        private Timer timer = new Timer(100, new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {           
                colorDocument(0,doc.getLength());
                updateUI();
            }
        });
        public SMSTextPaneDocumentFilter() {
            super();
            timer.setRepeats(false);
            //set styles
            doc = smsTextPane.getStyledDocument();
            Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
            regular = doc.addStyle("regular", def);
            StyleConstants.setForeground(regular, UIManager.getColor("TextArea.foreground"));
            highlight = doc.addStyle("highlight", def);
            StyleConstants.setForeground(highlight, Color.BLUE);
            
            // listen for changes in Look and Feel and change color of regular text
            UIManager.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("lookAndFeel".equals(evt.getPropertyName())) {
                        StyleConstants.setForeground(regular, UIManager.getColor("TextArea.foreground"));
                        SMSTextPaneDocumentFilter.this.requestUpdate();
                    }
                }
            });
        }
        /** update components and actions */
        private void updateUI() {
            compressAction.updateStatus();
            undoAction.updateStatus();
            redoAction.updateStatus();
            sendAction.updateStatus();
            updateProgressBars();
        }
        /** color parts of sms */
        private void colorDocument(int from, int length) {
            while (from < length) {
                int to = ((from / envelope.getSMSLength()) + 1) * envelope.getSMSLength() - 1;
                to = to<length-1?to:length-1;
                doc.setCharacterAttributes(from,to-from+1,getStyle(from),false);
                from = to + 1;
            }
        }
        /** calculate which style is appropriate for given position */
        private Style getStyle(int offset) {
            if ((offset / envelope.getSMSLength()) % 2 == 0) //even sms
                return regular;
            else
                return highlight;
        }
        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            //if reached size limit, crop the text and show a warning
            if ((fb.getDocument().getLength() + (text!=null?text.length():0) - length)
                    > envelope.getMaxTextLength()) {
                MainFrame.getInstance().getStatusPanel().setStatusMessage(
                        l10n.getString("SMSPanel.Text_is_too_long!"), false, null, false);
                MainFrame.getInstance().getStatusPanel().hideStatusMessageAfter(5000);
                int maxlength = envelope.getMaxTextLength() - fb.getDocument().getLength() + length;
                maxlength = Math.max(maxlength, 0);
                text = text.substring(0, maxlength);
            }
            super.replace(fb, offset, length, text, getStyle(offset));
            timer.restart();
        }
        @Override
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            super.insertString(fb, offset, string, attr);
            timer.restart();
        }
        @Override
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            timer.restart();
        }
        /** request recoloring externally */
        public void requestUpdate() {
            timer.restart();
        }
    }
    
    /** Textfield for entering contact name or number */
    public class RecipientTextField extends JTextField {
        /** currently selected contact */
        private Contact contact;
        private RecipientDocumentChange recipientDocumentChange = new RecipientDocumentChange();
        private String tooltip = l10n.getString("SMSPanel.recipientTextField.tooltip");
        private String tooltipTip = l10n.getString("SMSPanel.recipientTextField.tooltip.tip");
        
        public RecipientTextField() {

            //set tooltip
            if (Nullator.isEmpty(config.getCountryPrefix())) {
                setToolTipText(tooltip + tooltipTip + "</html>");
            } else {
                setToolTipText(tooltip + "</html>");
            }
            
            //focus listener
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    selectAll();
                }
                @Override
                public void focusLost(FocusEvent e) {
                    select(0, 0);
                    //try to rewrite phone number to contact name if possible
                    redrawContactName();
                }
            });
            
            //key listener
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent evt) {
                    //on Enter just focus the text area
                    if (evt != null && evt.getKeyCode() == KeyEvent.VK_ENTER) {
                        smsTextPane.requestFocusInWindow();
                        return;
                    }
                }
            });
            
            //document listener
            getDocument().addDocumentListener(new AbstractDocumentListener() {
                @Override
                public void onUpdate(DocumentEvent evt) {
                    if (disableContactListeners) {
                        return;
                    }
                    
                    SwingUtilities.invokeLater(recipientDocumentChange);
                }
            });
            
        }
        
        /** Set contact to display. Will display contact name. Will not change
         displayed text if user is currently editing it. */
        public void setContact(Contact contact) {
            this.contact = contact;
            if (!hasFocus()) {
                super.setText(contact != null ? contact.getName() : null);
            }
        }
        
        /** Get currently chosen contact. May be null. */
        public Contact getContact() {
            return contact;
        }
        
        /** Return visible text. May be contact name or phone number (will include prefix).
         May be null. */
        @Override
        public String getText() {
            if (contact != null) {
                return contact.getNumber();
            }
            
            String text = super.getText();
            if (!Nullator.isEmpty(text) && !text.startsWith("+")) {
                text = config.getCountryPrefix() + text;
            }
            //prepend country prefix if not present and text is a number
            if (FormChecker.checkSMSNumber(text)) {
                return text;
            } else { //text is a name
                return super.getText();
            }
        }
        
        /** Set text to display. Will erase any internally remembered contact. */
        @Override
        public void setText(String text) {
            contact = null;
            super.setText(text);
        }
        
        /** Rewrite phone number to contact name. Used after user finished editing
         the field. */
        public void redrawContactName() {
            if (contact == null) {
                return;
            }
            
            boolean old = disableContactListeners;
            disableContactListeners = true;
           
            super.setText(contact.getName());
           
            disableContactListeners = old;
        }
        
        /** Get phone number of chosen contact or typed phone number. May be null. */
        public String getNumber() {
            if (contact != null) {
                return contact.getNumber();
            }
            
            String text = getText();
            if (FormChecker.checkSMSNumber(text)) {
                return text;
            } else {
                return null;
            }
        }
        
        /** Set phone number to display. Handles country prefix correctly. */
        public void setNumber(String number) {
            if (Nullator.isEmpty(number)) {
                setText("");
            }
            
            if (number.startsWith(config.getCountryPrefix())) {
                number = number.substring(config.getCountryPrefix().length());
            }
            setText(number);
        }
        
        /** Listener for changes in the field document */
        private class RecipientDocumentChange implements Runnable {
            @Override
            public void run() {
                //search for contact
                contact = null;
                contact = lookupContact(false);
                requestSelectContact(contact);

                //if not found and is number, guess operator
                if (contact == null && getNumber() != null) {
                    operatorComboBox.selectSuggestedOperator(getNumber());
                }

                //update envelope
                Set<Contact> set = new HashSet<Contact>();
                set.add(new Contact(contact != null ? contact.getName() : null,
                        getNumber(),
                        operatorComboBox.getSelectedOperatorName()));
                envelope.setContacts(set);

                //update send action
                sendAction.updateStatus();
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar fullProgressBar;
    private javax.swing.JLabel gatewayLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private esmska.gui.OperatorComboBox operatorComboBox;
    private javax.swing.JLabel recipientLabel;
    private javax.swing.JTextField recipientTextField;
    private javax.swing.JButton sendButton;
    private javax.swing.JProgressBar singleProgressBar;
    private javax.swing.JLabel smsCounterLabel;
    private javax.swing.JTextPane smsTextPane;
    private javax.swing.JLabel textLabel;
    // End of variables declaration//GEN-END:variables
    
}
