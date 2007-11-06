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
import esmska.data.FormChecker;
import esmska.data.SMS;
import esmska.operators.Operator;
import esmska.operators.OperatorEnum;
import esmska.persistence.PersistenceManager;
import esmska.utils.ActionEventSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import org.jvnet.substance.SubstanceLookAndFeel;

/** Panel for writing and sending sms, and for setting immediate contact
 *
 * @author  ripper
 */
public class SMSPanel extends javax.swing.JPanel {
    public static final int ACTION_REQUEST_CLEAR_CONTACT_SELECTION = 0;
    public static final int ACTION_REQUEST_SELECT_CONTACT = 1;
    public static final int ACTION_SEND_SMS = 2;
    
    private static final String RES = "/esmska/resources/";
    private ActionEventSupport actionEventSupport;
    private Config config = PersistenceManager.getConfig();
    /** box for messages */
    private Envelope envelope = new Envelope();
    /** support for undo and redo in sms text pane */
    private UndoManager smsTextUndoManager = new UndoManager();
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    
    private Action smsTextUndoAction;
    private Action smsTextRedoAction;
    private SendAction sendAction = new SendAction();
    private SMSTextPaneListener smsTextPaneListener = new SMSTextPaneListener();
    private SMSTextPaneDocumentFilter smsTextPaneDocumentFilter;
    
    private Contact requestedContactSelection;
    
    /** Creates new form SMSPanel */
    public SMSPanel() {
        initComponents();
        actionEventSupport = new ActionEventSupport(this);
        operatorComboBox.setSelectedItem(operatorComboBox.getSelectedItem());
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
                smsNumberTextField.requestFocusInWindow();
            return false;
        }
        for (Contact c : envelope.getContacts()) {
            if (!FormChecker.checkSMSNumber(c.getNumber())) {
                if (transferFocus)
                    smsNumberTextField.requestFocusInWindow();
                return false;
            }
        }
        return true;
    }
    
    /** updates name according to number and operator */
    private boolean lookupContact() {
        String countryCode = "+420";
        String number = smsNumberTextField.getText();
        Operator operator = (Operator)operatorComboBox.getSelectedItem();
        
        //TODO find out if neccessary
        // skip if already selected right contact
//        Contact selected = null;
//        HashSet<Contact> selecteds = contactPanel.getSelectedContacts();
//        if (selecteds.size() > 0)
//            selected = selecteds.iterator().next();
//        if (selected != null && selected.getCountryCode().equals(countryCode) &&
//                selected.getNumber().equals(number) &&
//                selected.getOperator().equals(operator))
//            return true;
        
        Contact contact = null;
        if (number.length() == 9) {
            for (Contact c : contacts) {
                if (c.getCountryCode() != null && c.getCountryCode().equals(countryCode) &&
                        c.getNumber() != null && c.getNumber().equals(number) &&
                        c.getOperator() != null && c.getOperator().equals(operator)) {
                    contact = c;
                    break;
                }
            }
        }
        
        if (contact != null) {
            requestedContactSelection = contact;
            actionEventSupport.fireActionPerformed(ACTION_REQUEST_SELECT_CONTACT, null);
            return true;
        } else {
            actionEventSupport.fireActionPerformed(ACTION_REQUEST_CLEAR_CONTACT_SELECTION, null);
            return false;
        }
    }
    
    /** get contact which was requested to be selected in contact list */
    public Contact getRequestedContactSelection() {
        return requestedContactSelection;
    }
    
    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
    
    /** set selected contacts in contact list or contact to display */
    public void setContacts(Collection<Contact> contacts) {
        if (contacts == null)
            throw new NullPointerException("contacts");
        int count = contacts.size();
        
        if (count == 1) {
            Contact c = contacts.iterator().next();
            smsNumberTextField.setText(c.getNumber());
            operatorComboBox.setSelectedItem(c.getOperator());
            nameLabel.setText(c.getName());
        }
        
        boolean multiSendMode = (count > 1);
        String sendLabel = "Hromadné odesílání";
        if (multiSendMode) {
            String tooltip = "<html>Pro zrušení módu hromadného odesílání<br>"
                    + "označte v seznamu kontaktů jediný kontakt</html>";
            nameLabel.setText(sendLabel);
            nameLabel.setToolTipText(tooltip);
            smsNumberTextField.setText("");
            smsNumberTextField.setToolTipText(tooltip);
        } else {
            if (nameLabel.getText().equals(sendLabel))
                nameLabel.setText("");
            nameLabel.setToolTipText(null);
            smsNumberTextField.setToolTipText(null);
        }
        smsNumberTextField.setEnabled(! multiSendMode);
        operatorComboBox.setEnabled(! multiSendMode);
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.addAll(contacts);
        if (count < 1)
            set.add(new Contact(nameLabel.getText(), "+420", smsNumberTextField.getText(),
                    (Operator)operatorComboBox.getSelectedItem()));
        envelope.setContacts(set);
        
        // update components
        sendAction.updateStatus();
        smsTextPaneDocumentFilter.requestUpdate();
        if (count > 0)
            smsTextPane.requestFocusInWindow();
    }
    
    /** set sms to display and edit */
    public void setSMS(SMS sms) {
        smsNumberTextField.setText(sms.getNumber().substring(4));
        smsTextPane.setText(sms.getText());
        operatorComboBox.setSelectedItem(sms.getOperator());
        nameLabel.setText(sms.getName());
        smsTextPane.requestFocusInWindow();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        smsNumberTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        smsTextPane = new javax.swing.JTextPane();
        jLabel5 = new javax.swing.JLabel();
        operatorComboBox = new javax.swing.JComboBox();
        sendButton = new javax.swing.JButton();
        smsCounterLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        nameLabel = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Zpr\u00e1va"));
        jLabel4.setDisplayedMnemonic('l');
        jLabel4.setText("\u010c\u00edslo");

        jLabel1.setText("+420");

        smsNumberTextField.setColumns(9);
        smsNumberTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                smsNumberTextFieldKeyReleased(evt);
            }
        });

        //GTK JTextArea bug fix
        if (config.getLookAndFeel().equals(ThemeManager.LAF_SYSTEM))
        smsTextPane.setBackground(SystemColor.text);

        smsTextPane.getDocument().addDocumentListener(smsTextPaneListener);
        smsTextPaneDocumentFilter = new SMSTextPaneDocumentFilter();
        ((AbstractDocument)smsTextPane.getStyledDocument()).setDocumentFilter(smsTextPaneDocumentFilter);

        //set undo and redo actions and bind them
        smsTextUndoManager.setLimit(-1);
        smsTextUndoAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (smsTextUndoManager.canUndo()) {
                    smsTextUndoManager.undo();
                    smsTextPaneDocumentFilter.requestUpdate();
                }
            }
        };
        smsTextRedoAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (smsTextUndoManager.canRedo()) {
                    smsTextUndoManager.redo();
                    smsTextPaneDocumentFilter.requestUpdate();
                }
            }
        };
        smsTextPane.getDocument().addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent e) {
                if (e.getEdit().getPresentationName().contains("style"))
                return;
                smsTextUndoManager.addEdit(e.getEdit());
            }
        });
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,KeyEvent.CTRL_DOWN_MASK),"undo");
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK),"redo");
        smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
            KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK),"redo");
    smsTextPane.getActionMap().put("undo",smsTextUndoAction);
    smsTextPane.getActionMap().put("redo",smsTextRedoAction);

    //ctrl+enter
    smsTextPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,KeyEvent.CTRL_DOWN_MASK),"send");
    smsTextPane.getActionMap().put("send",sendAction);

    jScrollPane1.setViewportView(smsTextPane);

    jLabel5.setDisplayedMnemonic('t');
    jLabel5.setText("Text");

    operatorComboBox.setModel(new DefaultComboBoxModel(OperatorEnum.getAsList().toArray()));
    operatorComboBox.setRenderer(new OperatorComboBoxRenderer());
    operatorComboBox.addActionListener(new OperatorComboBoxActionListener());

    sendButton.setAction(sendAction);
    sendButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);

    smsCounterLabel.setText("0 znak\u016f (0 sms)");

    jLabel2.setText("Jm\u00e9no");

    nameLabel.setForeground(new java.awt.Color(0, 51, 255));
    nameLabel.setText("jm\u00e9no");
    nameLabel.setText(null);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel2)
                .addComponent(jLabel5)
                .addComponent(jLabel4))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sendButton))
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                    .addComponent(jLabel1)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(smsNumberTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(operatorComboBox, 0, 114, Short.MAX_VALUE))
                .addComponent(nameLabel, javax.swing.GroupLayout.Alignment.LEADING))
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
                .addComponent(jLabel1)
                .addComponent(smsNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel5)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(sendButton)
                .addComponent(smsCounterLabel))
            .addContainerGap())
    );
    }// </editor-fold>//GEN-END:initComponents
    
    private void smsNumberTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_smsNumberTextFieldKeyReleased
        //update name label
        boolean found = lookupContact();
        
        if (!found) {
            nameLabel.setText("");
            //guess operator
            Operator op = OperatorEnum.getOperator(smsNumberTextField.getText());
            if (op != null) {
                for (int i=0; i<operatorComboBox.getItemCount(); i++) {
                    if (operatorComboBox.getItemAt(i).getClass().equals(op.getClass())) {
                        operatorComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
        
        //update envelope
        Set<Contact> set = new HashSet<Contact>();
        set.add(new Contact(nameLabel.getText(), "+420", smsNumberTextField.getText(),
                (Operator)operatorComboBox.getSelectedItem()));
        envelope.setContacts(set);
        
        //update send action
        sendAction.updateStatus();
    }//GEN-LAST:event_smsNumberTextFieldKeyReleased
    
    /** Send sms to queue */
    private class SendAction extends AbstractAction {
        public SendAction() {
            super("Poslat", new ImageIcon(MainFrame.class.getResource(RES + "send.png")));
            this.putValue(SHORT_DESCRIPTION,"Odeslat zprávu (Alt+S, Ctrl+Enter)");
            this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            if (!validateForm(true))
                return;
            
            actionEventSupport.fireActionPerformed(ACTION_SEND_SMS, null);
            
            smsTextPane.setText(null);
            smsTextUndoManager.discardAllEdits();
            smsTextPane.requestFocusInWindow();
        }
        /** update status according to conditions  */
        public void updateStatus() {
            this.setEnabled(validateForm(false));
        }
    }
    
    /** Renderer for items in operator combo box */
    private class OperatorComboBoxRenderer implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = (new DefaultListCellRenderer()).getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            Operator operator = (Operator)value;
            ((JLabel)c).setIcon(operator.getIcon());
            return c;
        }
    }
    
    /** Another operator selected */
    private class OperatorComboBoxActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //update text editor listeners
            DocumentEvent event = new DocumentEvent() {
                public ElementChange getChange(Element elem) {
                    return null;
                }
                public Document getDocument() {
                    return smsTextPane.getDocument();
                }
                public int getLength() {
                    return 0;
                }
                public int getOffset() {
                    return 0;
                }
                public EventType getType() {
                    return EventType.INSERT;
                }
            };
            smsTextPaneListener.insertUpdate(event);
            
            lookupContact();
            
            //update envelope
            Set<Contact> set = new HashSet<Contact>();
            set.add(new Contact(nameLabel.getText(), "+420", smsNumberTextField.getText(),
                    (Operator)operatorComboBox.getSelectedItem()));
            envelope.setContacts(set);
            
            //update components
            smsTextPaneDocumentFilter.requestUpdate();
        }
    }
    
    /** Listener for sms text pane */
    private class SMSTextPaneListener implements DocumentListener {
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
                ex.printStackTrace();
            }
            sendAction.updateStatus();
        }
        public void changedUpdate(DocumentEvent e) {
            countChars(e);
            updateUI(e);
        }
        public void insertUpdate(DocumentEvent e) {
            countChars(e);
            updateUI(e);
        }
        public void removeUpdate(DocumentEvent e) {
            countChars(e);
            updateUI(e);
        }
    }
    
    /** Limit maximum sms length and color it */
    private class SMSTextPaneDocumentFilter extends DocumentFilter {
        private StyledDocument doc;
        private Style regular, highlight;
        private Timer timer = new Timer(500, new ActionListener() { //updating after each event is slow,
            public void actionPerformed(ActionEvent e) {            //therefore there is timer
                colorDocument(0,doc.getLength());
            }
        });
        public SMSTextPaneDocumentFilter() {
            super();
            timer.setRepeats(false);
            //set styles
            doc = smsTextPane.getStyledDocument();
            Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
            regular = doc.addStyle("regular", def);
            StyleConstants.setForeground(regular,UIManager.getColor("TextArea.foreground"));
            highlight = doc.addStyle("highlight", def);
            StyleConstants.setForeground(highlight, Color.BLUE);
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
        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if ((fb.getDocument().getLength() + (text!=null?text.length():0) - length) > envelope.getMaxTextLength()) //reached size limit
                return;
            super.replace(fb, offset, length, text, getStyle(offset));
            timer.restart();
        }
        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            super.insertString(fb, offset, string, attr);
            timer.restart();
        }
        public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            timer.restart();
        }
        /** request recoloring externally */
        public void requestUpdate() {
            timer.restart();
        }
    }
    
    public void addActionListener(ActionListener actionListener) {
        actionEventSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionEventSupport.removeActionListener(actionListener);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JComboBox operatorComboBox;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel smsCounterLabel;
    javax.swing.JTextField smsNumberTextField;
    private javax.swing.JTextPane smsTextPane;
    // End of variables declaration//GEN-END:variables
    
}
