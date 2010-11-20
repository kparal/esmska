/*
 * SMSPanel.java
 *
 * Created on 8. říjen 2007, 16:19
 */

package esmska.gui;

import esmska.Context;
import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.CountryPrefix;
import esmska.data.Envelope;
import esmska.data.Keyring;
import esmska.data.Links;
import esmska.data.Gateway;
import esmska.data.Gateways;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.data.event.AbstractDocumentListener;
import esmska.data.event.ActionEventSupport;
import esmska.utils.L10N;
import esmska.utils.MiscUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
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
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.openide.awt.Mnemonics;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.skin.SkinChangeListener;

/** Panel for writing and sending sms, and for setting immediate contact
 *
 * @author  ripper
 */
public class SMSPanel extends javax.swing.JPanel {
    private static final Logger logger = Logger.getLogger(SMSPanel.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    
    /** box for messages */
    private Envelope envelope = new Envelope();
    /** support for undo and redo in sms text pane */
    private UndoManager smsTextUndoManager = new UndoManager();
    private SortedSet<Contact> contacts = Contacts.getInstance().getAll();
    private Config config = Config.getInstance();
    private Keyring keyring = Keyring.getInstance();
    
    private UndoAction undoAction = new UndoAction();
    private RedoAction redoAction = new RedoAction();
    private CompressAction compressAction = null;
    private SendAction sendAction = new SendAction();
    private SMSTextPaneListener smsTextPaneListener = new SMSTextPaneListener();
    private SMSTextPaneDocumentFilter smsTextPaneDocumentFilter;
    private RecipientTextField recipientField;
    
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
        compressAction = new CompressAction();
        recipientField = (RecipientTextField) recipientTextField;
        
        //if not Substance LaF, add clipboard popup menu to text components
        if (!config.getLookAndFeel().equals(ThemeManager.LAF.SUBSTANCE)) {
            ClipboardPopupMenu.register(smsTextPane);
            ClipboardPopupMenu.register(recipientTextField);
        }

        // on keyring update update credentialsInfoLabel
        keyring.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCredentialsInfoLabel();
            }
        });
    }
    
    /** validates sms form and returns status */
    private boolean validateForm(boolean transferFocus) {
        if (StringUtils.isEmpty(envelope.getText())) {
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
            if (!Contact.isValidNumber(c.getNumber())) {
                if (transferFocus) {
                    recipientTextField.requestFocusInWindow();
                }
                return false;
            }
        }
        return true;
    }
    
    /** Find contact according to filled name/number and gateway
     * @param onlyFullMatch whether to look only for full match (name/number and gateway)
     *  or even partial match (name/number only)
     * @return found contact or null if none found
     */
    private Contact lookupContact(boolean onlyFullMatch) {
        String number = recipientField.getNumber();
        String id = recipientTextField.getText(); //name or number
        String gatewayName = gatewayComboBox.getSelectedGatewayName();
        
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        
        Contact contact = null; //match on id
        Contact fullContact = null; //match on id and gateway
        
        //search in contact numbers
        if (number != null) {
            for (Contact c : contacts) {
                if (ObjectUtils.equals(c.getNumber(), number)) {
                    if (ObjectUtils.equals(c.getGateway(), gatewayName)) {
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
                    if (ObjectUtils.equals(c.getGateway(), gatewayName)) {
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
            Context.mainFrame.getContactPanel().setSelectedContact(contact);
        } else {
            Context.mainFrame.getContactPanel().clearSelection();
        }
    }
    
    /** set selected contacts in contact list or contact to display */
    public void setContacts(Collection<Contact> contacts) {
        Validate.notNull(contacts);

        disableContactListeners = true;
        int count = contacts.size();
        
        if (count == 1) {
            Contact c = contacts.iterator().next();
            recipientField.setContact(c);
            gatewayComboBox.setSelectedGateway(c.getGateway());
        }
        
        boolean multiSendMode = (count > 1);
        if (multiSendMode) {
            recipientTextField.setText(l10n.getString("Multiple_sending"));
        }
        recipientTextField.setEnabled(! multiSendMode);
        gatewayComboBox.setEnabled(! multiSendMode);
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.addAll(contacts);
        if (count < 1) {
            Contact contact = recipientField.getContact();
            set.add(new Contact(contact != null ? contact.getName() : null,
                    recipientField.getNumber(),
                    gatewayComboBox.getSelectedGatewayName()));
        }
        envelope.setContacts(set);
        
        // update components
        sendAction.updateStatus();
        smsTextPaneDocumentFilter.requestUpdate();
        updateNumberInfoLabel();
        disableContactListeners = false;
    }
    
    /** set sms to display and edit */
    public void setSMS(final SMS sms) {
        recipientField.setNumber(sms.getNumber());
        smsTextPane.setText(sms.getText());
        smsTextUndoManager.discardAllEdits();

        //recipient textfield will change gateway, must wait and change gateway back
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                gatewayComboBox.setSelectedGateway(sms.getGateway());
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
        int currentLength = smsTextPane.getText().length();
        int smsLength = envelope.getSMSLength();
        
        //set maximums
        singleProgressBar.setMaximum(Math.max(smsLength, 0));
        fullProgressBar.setMaximum(envelope.getMaxTextLength());
        
        //if we are at the end of the whole message, the current message length
        //can be lesser than usual
        if (smsLength > 0) {
            int remainder = envelope.getMaxTextLength() % smsLength;
            if (envelope.getMaxTextLength() - currentLength < remainder) {
                //we have crossed the remainder border, let's update maximum on progress bar
                singleProgressBar.setMaximum(remainder);
            }
        }
        
        //set values
        fullProgressBar.setValue(currentLength);
        singleProgressBar.setValue(smsLength > 0 ? currentLength % smsLength : 0);
        //on the border counts we want progress bar full instead of empty
        if (singleProgressBar.getValue() == 0 && currentLength > 0) {
            singleProgressBar.setValue(singleProgressBar.getMaximum());
        }
        
        //set tooltips
        int current = singleProgressBar.getValue();
        int max = singleProgressBar.getMaximum();
        boolean infinite = (max == Integer.MAX_VALUE);
        if (infinite) {
            //don't show really big numbers when there is no limit on characters
            singleProgressBar.setToolTipText(MessageFormat.format(
                    l10n.getString("SMSPanel.singleProgressBar"),
                    current, "∞", "∞"));
        } else {
            singleProgressBar.setToolTipText(MessageFormat.format(
                    l10n.getString("SMSPanel.singleProgressBar"),
                    current, max, (max - current)));
        }
        current = fullProgressBar.getValue();
        max = fullProgressBar.getMaximum();
        infinite = (max == Integer.MAX_VALUE);
        if (infinite) {
            //don't show really big numbers when there is no limit on characters
            fullProgressBar.setToolTipText(MessageFormat.format(
                    l10n.getString("SMSPanel.fullProgressBar"),
                    current, "∞", "∞"));
        } else {
            fullProgressBar.setToolTipText(MessageFormat.format(
                    l10n.getString("SMSPanel.fullProgressBar"),
                    current, max, (max - current)));
        }
    }

    /** Show warning if user selected gateway that does not send
     * messages to numbers with specified prefix
     */
    private void updateCountryInfoLabel() {
        countryInfoLabel.setVisible(false);

        //ensure that fields are sufficiently filled in
        Gateway gateway = gatewayComboBox.getSelectedGateway();
        String number = recipientField.getNumber();
        if (gateway == null || !Contact.isValidNumber(number)) {
            return;
        }

        boolean supported = Gateways.isNumberSupported(gateway, number);
        if (!supported) {
            String text = MessageFormat.format(l10n.getString("SMSPanel.countryInfoLabel.text"),
                    StringUtils.join(gateway.getSupportedPrefixes(), ','));
            countryInfoLabel.setText(text);
            countryInfoLabel.setVisible(true);
        }
    }

    /** Show warning if user selected gateway requiring registration
     * and no credentials are filled in
     */
    private void updateCredentialsInfoLabel() {
        Gateway gateway = gatewayComboBox.getSelectedGateway();
        if (gateway != null && gateway.isLoginRequired() &&
                keyring.getKey(gateway.getName()) == null) {
            credentialsInfoLabel.setVisible(true);
        } else {
            credentialsInfoLabel.setVisible(false);
        }
    }

    /** Show warning if the recipient number is not in a valid international
     * format.
     */
    private void updateNumberInfoLabel() {
        numberInfoLabel.setVisible(false);
        if (envelope.getContacts().size() != 1) {
            //multisend mode or no contact selected
            return;
        }
        Contact contact = envelope.getContacts().iterator().next();
        if (StringUtils.isNotEmpty(recipientField.getText())) {
            numberInfoLabel.setVisible(!Contact.isValidNumber(contact.getNumber()));
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gatewayComboBox = new GatewayComboBox();
        fullProgressBar = new JProgressBar();
        jScrollPane1 = new JScrollPane();
        smsTextPane = new JTextPane();
        textLabel = new JLabel();
        sendButton = new JButton();
        smsCounterLabel = new JLabel();
        singleProgressBar = new JProgressBar();
        gatewayLabel = new JLabel();
        recipientTextField = new SMSPanel.RecipientTextField();
        recipientLabel = new JLabel();
        infoPanel = new JPanel();
        credentialsInfoLabel = new InfoLabel();
        numberInfoLabel = new InfoLabel();
        countryInfoLabel = new InfoLabel();

        setBorder(BorderFactory.createTitledBorder(l10n.getString("SMSPanel.border.title"))); // NOI18N
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        gatewayComboBox.addActionListener(new GatewayComboBoxActionListener());
        gatewayComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                gatewayComboBoxItemStateChanged(evt);
            }
        });

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
    Mnemonics.setLocalizedText(textLabel,l10n.getString("SMSPanel.textLabel.text")); // NOI18N
    textLabel.setToolTipText(l10n.getString("SMSPanel.textLabel.toolTipText")); // NOI18N

    sendButton.setAction(sendAction);

    sendButton.setToolTipText(l10n.getString("SMSPanel.sendButton.toolTipText")); // NOI18N
        Mnemonics.setLocalizedText(smsCounterLabel, l10n.getString("SMSPanel.smsCounterLabel.text")); // NOI18N

    singleProgressBar.setMaximum(1000);

    gatewayLabel.setLabelFor(gatewayComboBox);
        Mnemonics.setLocalizedText(gatewayLabel, l10n.getString("SMSPanel.gatewayLabel.text")); // NOI18N
    gatewayLabel.setToolTipText(gatewayComboBox.getToolTipText());

    recipientLabel.setLabelFor(recipientTextField);
        Mnemonics.setLocalizedText(recipientLabel, l10n.getString("SMSPanel.recipientLabel.text")); // NOI18N
    recipientLabel.setToolTipText(recipientTextField.getToolTipText());

    infoPanel.addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent evt) {
            infoPanelComponentResized(evt);
        }
    });
    Mnemonics.setLocalizedText(credentialsInfoLabel,l10n.getString(
        "SMSPanel.credentialsInfoLabel.text"));
    credentialsInfoLabel.setText(MessageFormat.format(
        l10n.getString("SMSPanel.credentialsInfoLabel.text"), Links.CONFIG_CREDENTIALS));
credentialsInfoLabel.setVisible(false);

        Mnemonics.setLocalizedText(numberInfoLabel, l10n.getString("SMSPanel.numberInfoLabel.text")); // NOI18N
numberInfoLabel.setVisible(false);

        Mnemonics.setLocalizedText(countryInfoLabel, l10n.getString("SMSPanel.countryInfoLabel.text")); // NOI18N
countryInfoLabel.setVisible(false);

        GroupLayout infoPanelLayout = new GroupLayout(infoPanel);
infoPanel.setLayout(infoPanelLayout);
infoPanelLayout.setHorizontalGroup(
    infoPanelLayout.createParallelGroup(Alignment.LEADING)
    .addComponent(credentialsInfoLabel, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
    .addComponent(numberInfoLabel, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
    .addComponent(countryInfoLabel, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
    );
    infoPanelLayout.setVerticalGroup(
        infoPanelLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(Alignment.TRAILING, infoPanelLayout.createSequentialGroup()
            .addComponent(numberInfoLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED)
            .addComponent(countryInfoLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(credentialsInfoLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
    );

        GroupLayout layout = new GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(recipientLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(recipientTextField, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE))
                        .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                            .addComponent(gatewayLabel)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(gatewayComboBox, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(textLabel)
                                .addComponent(singleProgressBar, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
                                .addComponent(fullProgressBar, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(smsCounterLabel, GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                                    .addPreferredGap(ComponentPlacement.RELATED)
                                    .addComponent(sendButton))
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)))))
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(74, 74, 74)
                    .addComponent(infoPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addContainerGap())
    );

    layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {fullProgressBar, singleProgressBar});

    layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {gatewayLabel, recipientLabel, textLabel});

    layout.setVerticalGroup(
        layout.createParallelGroup(Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(recipientLabel)
                .addComponent(recipientTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                .addComponent(gatewayLabel)
                .addComponent(gatewayComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(Alignment.LEADING)
                .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(infoPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addGroup(layout.createParallelGroup(Alignment.LEADING, false)
                        .addComponent(sendButton)
                        .addComponent(smsCounterLabel)))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(textLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(singleProgressBar, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(fullProgressBar, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
    );

    layout.linkSize(SwingConstants.VERTICAL, new Component[] {fullProgressBar, singleProgressBar});

    }// </editor-fold>//GEN-END:initComponents
    
    private void formFocusGained(FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        smsTextPane.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained

    private void gatewayComboBoxItemStateChanged(ItemEvent evt) {//GEN-FIRST:event_gatewayComboBoxItemStateChanged
        updateCredentialsInfoLabel();
        updateCountryInfoLabel();
    }//GEN-LAST:event_gatewayComboBoxItemStateChanged

    private void infoPanelComponentResized(ComponentEvent evt) {//GEN-FIRST:event_infoPanelComponentResized
        if (MiscUtils.needsResize(this, MiscUtils.Direction.HEIGHT)) {
            actionSupport.fireActionPerformed(ActionEventSupport.ACTION_NEED_RESIZE, null);
        }
    }//GEN-LAST:event_infoPanelComponentResized
    
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
            if (!validateForm(true)) {
                return;
            }
            logger.fine("Sending new message to queue");
            Queue.getInstance().addAll(envelope.generate());
            
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
        /** is message selected just partially or as a whole? */
        private boolean partialSelection = false;
        public CompressAction() {
            updateLabels();
            putValue(SHORT_DESCRIPTION,l10n.getString("SMSPanel.compress"));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "compress-32.png")));
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            // add listener for selection changes and update labels accordingly
            smsTextPane.addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(CaretEvent e) {
                    boolean ps = (smsTextPane.getSelectedText() != null);
                    if (ps != partialSelection) {
                        partialSelection = ps;
                        updateLabels();
                    }
                }
            });
        }
        /** updates labels according to current status */
        private void updateLabels() {
            if (partialSelection) {
                L10N.setLocalizedText(this, l10n.getString("CompressText_"));
            } else {
                L10N.setLocalizedText(this, l10n.getString("Compress_"));
            }
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.fine("Compressing message");
            String text = partialSelection ? smsTextPane.getSelectedText() : smsTextPane.getText();
            if (StringUtils.isEmpty(text)) {
                return;
            }

            String newText = text.replaceAll("\\s", " "); //all whitespace to spaces
            newText = Pattern.compile("(\\s)\\s+", Pattern.DOTALL).matcher(newText).replaceAll("$1"); //remove duplicate whitespaces
            Pattern pattern = Pattern.compile("\\s+(.)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(newText);
            while (matcher.find()) { //find next space+character
                newText = matcher.replaceFirst(matcher.group(1).toUpperCase()); //replace by upper character
                matcher = pattern.matcher(newText);
            }
            newText = newText.replaceAll(" $", ""); //remove trailing space

            if (newText.equals(text)) { //do not replace if already compressed
                return;
            }

            // replace the old text with the compressed one
            if (partialSelection) {
                // replace text and leave it selected
                int selectIndex = smsTextPane.getSelectionStart();
                smsTextPane.replaceSelection(newText);
                smsTextPane.setSelectionStart(selectIndex);
                smsTextPane.setSelectionEnd(selectIndex + newText.length());
            } else {
                smsTextPane.setText(newText);
            }
        }
        /** update status according to current conditions */
        public void updateStatus() {
            setEnabled(getText().length() > 0);
        }
    }

    /** Another gateway selected */
    private class GatewayComboBoxActionListener implements ActionListener {
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
                    gatewayComboBox.getSelectedGatewayName()));
            envelope.setContacts(set);
            
            //update components
            smsTextPaneDocumentFilter.requestUpdate();
        }
    }
    
    /** Listener for sms text pane */
    private class SMSTextPaneListener extends AbstractDocumentListener {
        /** count number of chars in sms and take action */
        private void countChars(DocumentEvent e) {
            int chars = e.getDocument().getLength();
            int smsCount = envelope.getSMSCount(chars);
            if (smsCount < 0) {
                //don't count messages
                smsCounterLabel.setText(MessageFormat.format(l10n.getString("SMSPanel.smsCounterLabel.3"),
                    chars, smsCount));
            } else {
                //count messags
                smsCounterLabel.setText(MessageFormat.format(l10n.getString("SMSPanel.smsCounterLabel.1"),
                    chars, smsCount));
            }
            if (chars > envelope.getMaxTextLength()) {
                //chars more than max
                smsCounterLabel.setForeground(Color.RED);
                smsCounterLabel.setText(MessageFormat.format(l10n.getString("SMSPanel.smsCounterLabel.2"),
                        chars));
            } else {
                //chars ok
                smsCounterLabel.setForeground(UIManager.getColor("Label.foreground"));
            }
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
        private Color alternateTextColor = Color.BLUE;
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
            highlight = doc.addStyle("highlight", def);
            lafChangedImpl();
            
            // listen for changes in Look and Feel and change color of regular text
            UIManager.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("lookAndFeel".equals(evt.getPropertyName())) {
                        lafChanged();
                    }
                }
            });
            // the same for substance skins (they do not notify UIManager from some reason)
            SubstanceLookAndFeel.registerSkinChangeListener(new SkinChangeListener() {
                @Override
                public void skinChanged() {
                    lafChanged();
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
            int smsLength = envelope.getSMSLength();
            while (from < length) {
                int to = 0;
                if (smsLength <= 0) {
                    //unspecified sms length, color it all with same color
                    to = length - 1;
                } else {
                    to = ((from / smsLength) + 1) * smsLength - 1;
                }
                to = to < length-1 ? to : length-1;
                doc.setCharacterAttributes(from,to-from+1,getStyle(from),false);
                from = to + 1;
            }
        }
        /** calculate which style is appropriate for given position */
        private Style getStyle(int offset) {
            if (envelope.getSMSLength() <= 0) {
                //unspecified sms length
                return regular;
            }
            if ((offset / envelope.getSMSLength()) % 2 == 0) {
                //even sms
                return regular;
            } else {
                return highlight;
            }
        }
        @Override
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            //if reached size limit, crop the text and show a warning
            if ((fb.getDocument().getLength() + (text!=null?text.length():0) - length)
                    > envelope.getMaxTextLength()) {
                Context.mainFrame.getStatusPanel().setStatusMessage(
                        l10n.getString("SMSPanel.Text_is_too_long!"), null, null, false);
                Context.mainFrame.getStatusPanel().hideStatusMessageAfter(5000);
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
        /** update text color on LaF change */
        private void lafChanged() {
            lafChangedImpl();
            SMSTextPaneDocumentFilter.this.requestUpdate();
        }
        /** main executive code for lafChanged() without any notifications or callbacks */
        private void lafChangedImpl() {
            StyleConstants.setForeground(regular, UIManager.getColor("TextArea.foreground"));
            alternateTextColor = ThemeManager.isCurrentSkinDark() ? Color.CYAN : Color.BLUE;
            StyleConstants.setForeground(highlight, alternateTextColor);
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
            if (StringUtils.isEmpty(config.getCountryPrefix())) {
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
            if (StringUtils.isNotEmpty(text) && !text.startsWith("+")) {
                text = config.getCountryPrefix() + text;
            }
            //prepend country prefix if not present and text is a number
            if (Contact.isValidNumber(text)) {
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
            if (Contact.isValidNumber(text)) {
                return text;
            } else {
                return null;
            }
        }
        
        /** Set phone number to display. Handles country prefix correctly. */
        public void setNumber(String number) {
            if (StringUtils.isEmpty(number)) {
                setText("");
            }
            
            setText(CountryPrefix.stripCountryPrefix(number));
        }
        
        /** Listener for changes in the recipient field */
        private class RecipientDocumentChange implements Runnable {
            @Override
            public void run() {
                //search for contact
                contact = null;
                contact = lookupContact(false);
                requestSelectContact(null);    //ensure contact selection will fire
                requestSelectContact(contact); //event even if the same contact

                //if not found and is number, guess gateway
                if (contact == null && getNumber() != null) {
                    gatewayComboBox.selectSuggestedGateway(getNumber());
                }
                
                //update envelope
                Set<Contact> set = new HashSet<Contact>();
                set.add(new Contact(contact != null ? contact.getName() : null,
                        getNumber(), gatewayComboBox.getSelectedGatewayName()));
                envelope.setContacts(set);

                //update components
                sendAction.updateStatus();
                updateCountryInfoLabel();
                updateNumberInfoLabel();
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private InfoLabel countryInfoLabel;
    private InfoLabel credentialsInfoLabel;
    private JProgressBar fullProgressBar;
    private GatewayComboBox gatewayComboBox;
    private JLabel gatewayLabel;
    private JPanel infoPanel;
    private JScrollPane jScrollPane1;
    private InfoLabel numberInfoLabel;
    private JLabel recipientLabel;
    private JTextField recipientTextField;
    private JButton sendButton;
    private JProgressBar singleProgressBar;
    private JLabel smsCounterLabel;
    private JTextPane smsTextPane;
    private JLabel textLabel;
    // End of variables declaration//GEN-END:variables
    
}
