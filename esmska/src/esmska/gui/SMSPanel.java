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
import esmska.utils.Nullator;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
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
    
    private Contact requestedContactSelection;
    
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
        //if not Substance LaF, add clipboard popup menu to text components
        if (!config.getLookAndFeel().equals(ThemeManager.LAF_SUBSTANCE)) {
            ClipboardPopupMenu.register(smsTextPane);
            ClipboardPopupMenu.register(numberTextField);
        }
    }
    
    /** validates sms form and returns status */
    private boolean validateForm(boolean transferFocus) {
        if (FormChecker.isEmpty(envelope.getText())) {
            if (transferFocus)
                smsTextPane.requestFocusInWindow();
            return false;
        }
        if (envelope.getText().length() > envelope.getMaxTextLength()) {
            if (transferFocus)
                smsTextPane.requestFocusInWindow();
            return false;
        }
        if (envelope.getContacts().size() <= 0) {
            if (transferFocus)
                numberTextField.requestFocusInWindow();
            return false;
        }
        for (Contact c : envelope.getContacts()) {
            if (!FormChecker.checkSMSNumber(c.getNumber())) {
                if (transferFocus)
                    numberTextField.requestFocusInWindow();
                return false;
            }
        }
        return true;
    }
    
    /** updates name according to number and operator */
    private boolean lookupContact() {
        String number = numberTextField.getText();
        String operatorName = operatorComboBox.getSelectedOperatorName();
        
        Contact contact = null;
            for (Contact c : contacts) {
                if (c.getNumber() != null && c.getNumber().equals(number) &&
                        Nullator.isEqual(c.getOperator(), operatorName)) {
                    contact = c;
                    break;
                }
            }
        
        if (contact != null) {
            requestedContactSelection = contact;
            actionSupport.fireActionPerformed(ACTION_REQUEST_SELECT_CONTACT, null);
            return true;
        } else {
            actionSupport.fireActionPerformed(ACTION_REQUEST_CLEAR_CONTACT_SELECTION, null);
            return false;
        }
    }
    
    /** get contact which was requested to be selected in contact list */
    public Contact getRequestedContactSelection() {
        return requestedContactSelection;
    }
    
    /** set envelope */
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
        envelope.addPropertyChangeListener(new EnvelopePropertyListener());
    }
    
    /** set selected contacts in contact list or contact to display */
    public void setContacts(Collection<Contact> contacts) {
        if (contacts == null)
            throw new NullPointerException("contacts");

        int count = contacts.size();
        
        if (count == 1) {
            Contact c = contacts.iterator().next();
            String number = c.getNumber();
            if (number.startsWith(config.getCountryPrefix()))
                number = number.substring(config.getCountryPrefix().length());
            numberTextField.setText(number);
            operatorComboBox.setSelectedOperator(c.getOperator());
            nameLabel.setText(c.getName());
        } else if (count < 1) {
            nameLabel.setText(null);
        }
        
        boolean multiSendMode = (count > 1);
        String sendLabel = "Hromadné odesílání";
        if (multiSendMode) {
            String tooltip = "<html>Pro zrušení módu hromadného odesílání<br>"
                    + "označte v seznamu kontaktů jediný kontakt</html>";
            nameLabel.setText(sendLabel);
            nameLabel.setToolTipText(tooltip);
            numberTextField.setText("");
        } else {
            if (sendLabel.equals(nameLabel.getText())) {
                nameLabel.setText("");
            }
            nameLabel.setToolTipText(null);
        }
        numberTextField.setEnabled(! multiSendMode);
        operatorComboBox.setEnabled(! multiSendMode);
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.addAll(contacts);
        if (count < 1) {
            set.add(new Contact(nameLabel.getText(), numberTextField.getText(),
                    operatorComboBox.getSelectedOperatorName()));
        }
        envelope.setContacts(set);
        
        // update components
        sendAction.updateStatus();
        smsTextPaneDocumentFilter.requestUpdate();
    }
    
    /** set sms to display and edit */
    public void setSMS(SMS sms) {
        nameLabel.setText(sms.getName());
        String number = sms.getNumber();
        if (number.startsWith(config.getCountryPrefix()))
            number = number.substring(config.getCountryPrefix().length());
        numberTextField.setText(number);
        smsTextPane.setText(sms.getText());
        operatorComboBox.setSelectedOperator(sms.getOperator());
        smsTextPane.requestFocusInWindow();
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
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel4 = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        numberTextField = new javax.swing.JTextField() {
            @Override
            public String getText() {
                String text = super.getText();
                if (!text.startsWith("+"))
                text = config.getCountryPrefix() + text;
                return text;
            }
        }
        ;
        jScrollPane1 = new javax.swing.JScrollPane();
        smsTextPane = new javax.swing.JTextPane();
        jLabel5 = new javax.swing.JLabel();
        sendButton = new javax.swing.JButton();
        smsCounterLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();
        operatorComboBox = new esmska.gui.OperatorComboBox();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Zpráva"));
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        jLabel4.setDisplayedMnemonic('o');
        jLabel4.setLabelFor(numberTextField);
        jLabel4.setText("Číslo");

        progressBar.setMaximum(1000);
        progressBar.setToolTipText("Graficky zobrazuje zbývající volné místo ve zprávě");

        numberTextField.setColumns(12);
        numberTextField.setToolTipText("<html>\nTelefonní číslo kontaktu včetně předčíslí země.<br>\nPřečíslí země nemusí být vyplněno, pokud je nastaveno<br>\nvýchozí předčíslí v nastavení programu.\n</html>");
        numberTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                numberTextFieldKeyReleased(evt);
            }
        });

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
        String command = "undo";
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK), command);
        smsTextPane.getActionMap().put("undo",undoAction);
        command = "redo";
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK), command);
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), command);
    smsTextPane.getActionMap().put(command, redoAction);

    //ctrl+enter
    command = "send";
    smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.CTRL_DOWN_MASK), command);
    smsTextPane.getActionMap().put(command, sendAction);
    jScrollPane1.setViewportView(smsTextPane);

    jLabel5.setDisplayedMnemonic('t');
    jLabel5.setLabelFor(smsTextPane);
    jLabel5.setText("Text");

    sendButton.setAction(sendAction);
    sendButton.setToolTipText("Odeslat zprávu (Alt+S, Ctrl+Enter)");

    smsCounterLabel.setText("0 znaků (0 sms)");

    jLabel2.setText("Jméno");

    nameLabel.setForeground(new java.awt.Color(0, 51, 255));
    nameLabel.setText("jméno");
    nameLabel.setText(null);

    operatorComboBox.addActionListener(new OperatorComboBoxActionListener());

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5)
                    .addComponent(jLabel4))
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sendButton))
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                    .addComponent(numberTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(operatorComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                    .addComponent(nameLabel)
                    .addGap(115, 115, 115)))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(nameLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel4)
                .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(numberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(jLabel5)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(sendButton)
                .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {numberTextField, operatorComboBox});

    }// </editor-fold>//GEN-END:initComponents
    
    private void numberTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_numberTextFieldKeyReleased
        //update name label
        boolean found = lookupContact();
        
        if (!found) {
            nameLabel.setText("");
            //guess operator
            operatorComboBox.suggestOperator(numberTextField.getText());
        }
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.add(new Contact(nameLabel.getText(), numberTextField.getText(),
                operatorComboBox.getSelectedOperatorName()));
        envelope.setContacts(set);
        
        //update send action
        sendAction.updateStatus();
}//GEN-LAST:event_numberTextFieldKeyReleased

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        smsTextPane.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained
    
    /** Send sms to queue */
    private class SendAction extends AbstractAction {
        public SendAction() {
            super("Poslat");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "send-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "send-22.png")));
            putValue(SHORT_DESCRIPTION,"Odeslat zprávu");
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                    KeyEvent.CTRL_DOWN_MASK));
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
            super("Zpět");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "undo-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "undo-32.png")));
            putValue(SHORT_DESCRIPTION, "Vrátit změnu v textu zprávy");
            putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                    KeyEvent.CTRL_DOWN_MASK));
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
            super("Vpřed");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "redo-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "redo-32.png")));
            putValue(SHORT_DESCRIPTION, "Zopakovat vrácenou změnu v textu zprávy");
            putValue(MNEMONIC_KEY, KeyEvent.VK_V);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                    KeyEvent.CTRL_DOWN_MASK));
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
            super("Zkomprimovat");
            putValue(SMALL_ICON, new ImageIcon(getClass().getResource(RES + "compress-16.png")));
            putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(RES + "compress-32.png")));
            putValue(SHORT_DESCRIPTION,"Vynechat z aktuální zprávy bílé znaky a přepsat ji do tvaru \"CamelCase\"");
            putValue(MNEMONIC_KEY, KeyEvent.VK_K);
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
                    KeyEvent.CTRL_DOWN_MASK));
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
            
            lookupContact();
            
            //update envelope
            Set<Contact> set = new HashSet<Contact>();
            
            set.add(new Contact(nameLabel.getText(), numberTextField.getText(),
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
                progressBar.setMaximum(envelope.getMaxTextLength());
            }
        }
    }
    
    /** Listener for sms text pane */
    private class SMSTextPaneListener extends AbstractDocumentListener {
        /** count number of chars in sms and take action */
        private void countChars(DocumentEvent e) {
            int chars = e.getDocument().getLength();
            int smsCount = envelope.getSMSCount(chars);
            smsCounterLabel.setText(chars + " znaků (" +  smsCount + " sms)");
            if (chars > envelope.getMaxTextLength()) { //chars more than max
                smsCounterLabel.setForeground(Color.RED);
                smsCounterLabel.setText(chars + " znaků (nelze odeslat!)");
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
        private Timer timer = new Timer(100, new ActionListener() { //updating after each event is slow,
            @Override
            public void actionPerformed(ActionEvent e) {            //therefore there is timer
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
            progressBar.setValue(smsTextPane.getText().length());
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
                MainFrame.getInstance().statusPanel.setStatusMessage(
                        "Text je příliš dlouhý!", false, null, false);
                MainFrame.getInstance().statusPanel.hideStatusMessageAfter(5000);
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nameLabel;
    javax.swing.JTextField numberTextField;
    private esmska.gui.OperatorComboBox operatorComboBox;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel smsCounterLabel;
    private javax.swing.JTextPane smsTextPane;
    // End of variables declaration//GEN-END:variables
    
}
