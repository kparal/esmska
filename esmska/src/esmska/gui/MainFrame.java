/*
 * MainFrame.java
 *
 * Created on 6. červenec 2007, 15:37
 */

package esmska.gui;

import esmska.*;
import esmska.data.Envelope;
import esmska.persistence.ExportManager;
import esmska.data.FormChecker;
import esmska.transfer.SMSSender;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentEvent.EventType;
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
import esmska.operators.Operator;
import esmska.operators.OperatorEnum;
import org.jvnet.substance.SubstanceLookAndFeel;
import esmska.data.Config;
import esmska.data.Contact;
import esmska.persistence.PersistenceManager;
import esmska.data.SMS;

/**
 * MainFrame form
 *
 * @author ripper
 */
public class MainFrame extends javax.swing.JFrame {
    private static MainFrame instance;
    private static final String RES = "/esmska/resources/";
    
    private Action quitAction = new QuitAction();
    private SendAction sendAction = new SendAction();
    private Action smsQueuePauseAction = new SMSQueuePauseAction();
    private Action deleteSMSAction = new DeleteSMSAction();
    private Action editSMSAction = new EditSMSAction();
    private Action aboutAction = new AboutAction();
    private Action configAction = new ConfigAction();
    private Action smsUpAction = new SMSUpAction();
    private Action smsDownAction = new SMSDownAction();
    private Action smsTextUndoAction;
    private Action smsTextRedoAction;
    private ImportAction importAction = new ImportAction();
    private Action exportAction = new ExportAction();
    private SMSTextPaneListener smsTextPaneListener = new SMSTextPaneListener();
    private SMSTextPaneDocumentFilter smsTextPaneDocumentFilter;
    private SMSQueueListModel smsQueueListModel = new SMSQueueListModel();
    
    /** actual queue of sms's */
    private List<SMS> smsQueue = PersistenceManager.getQueue();
    /** sender of sms */
    private SMSSender smsSender;
    /** box for messages */
    private Envelope envelope;
    /** timer to send another sms after defined delay */
    private Timer smsDelayTimer = new Timer(1000,new SMSDelayActionListener());
    /** support for undo and redo in sms text pane */
    private UndoManager smsTextUndoManager = new UndoManager();
    /** manager of persistence data */
    private PersistenceManager persistenceManager;
    /** program configuration */
    private Config config = PersistenceManager.getConfig();
    /** sms contacts */
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    
    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        instance = this;
        initComponents();
        
        //set tooltip delay
        ToolTipManager.sharedInstance().setInitialDelay(750);
        ToolTipManager.sharedInstance().setDismissDelay(60000);
        
        //init custom components
        smsSender = new SMSSender(smsQueue);
        envelope = new Envelope();
        
        //load config
        try {
            persistenceManager = PersistenceManager.getInstance();
        } catch (IOException ex) {
            ex.printStackTrace();
            printStatusMessage("Nepovedlo se vytvořit adresář s nastavením programu!");
        }
        loadConfig();
        if (smsQueue.size() > 0)
            pauseSMSQueue();
        
        //setup components
        smsDelayTimer.setInitialDelay(0);
    }
    
    public static MainFrame getInstance() {
        if (instance == null)
            instance = new MainFrame();
        return instance;
    }
    
    /** Executed when selection is changed in contact list */
    private void contactListSelectionChanged() {
            HashSet<Contact> selectedContacts = contactPanel.getSelectedContacts();
            int count = selectedContacts.size();
            
            // fill sms components with current contact
            if (count == 1) { //only one contact selected
                Contact c = selectedContacts.iterator().next();
                smsNumberTextField.setText(c.getNumber());
                operatorComboBox.setSelectedItem(c.getOperator());
                nameLabel.setText(c.getName());
            }
            
            //set multisend mode
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
            set.addAll(selectedContacts);
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
            
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        statusPanel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        smsDelayProgressBar = new javax.swing.JProgressBar();
        horizontalSplitPane = new javax.swing.JSplitPane();
        verticalSplitPane = new javax.swing.JSplitPane();
        smsPanel = new javax.swing.JPanel();
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
        queuePanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        smsQueueList = new javax.swing.JList();
        pauseButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        smsUpButton = new javax.swing.JButton();
        smsDownButton = new javax.swing.JButton();
        contactPanel = new esmska.gui.ContactPanel();
        menuBar = new javax.swing.JMenuBar();
        programMenu = new javax.swing.JMenu();
        configMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        toolsMenu = new javax.swing.JMenu();
        importMenuItem = new javax.swing.JMenuItem();
        exportMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Esmska");
        setIconImage(new ImageIcon(getClass().getResource(RES + "esmska.png")).getImage());
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        statusPanel.setFocusable(false);

        statusMessageLabel.setText("V\u00edtejte");
        statusMessageLabel.setFocusable(false);

        statusAnimationLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/task-idle.png")));
        statusAnimationLabel.setFocusable(false);

        smsDelayProgressBar.setMaximum(15);
        smsDelayProgressBar.setFocusable(false);
        smsDelayProgressBar.setString("Dal\u0161\u00ed sms za: ");
        smsDelayProgressBar.setStringPainted(true);
        smsDelayProgressBar.setVisible(false);

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 608, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 380, Short.MAX_VALUE)
                .addComponent(smsDelayProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(statusAnimationLabel)
                    .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(smsDelayProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(statusMessageLabel))))
        );

        horizontalSplitPane.setBorder(null);
        horizontalSplitPane.setResizeWeight(0.5);
        horizontalSplitPane.setContinuousLayout(true);
        verticalSplitPane.setBorder(null);
        verticalSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setResizeWeight(1.0);
        verticalSplitPane.setContinuousLayout(true);
        smsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Zpr\u00e1va"));
        jLabel4.setDisplayedMnemonic('l');
        jLabel4.setLabelFor(smsNumberTextField);
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
    jLabel5.setLabelFor(smsTextPane);
    jLabel5.setText("Text");

    operatorComboBox.setModel(new DefaultComboBoxModel(OperatorEnum.getAsList().toArray()));
    operatorComboBox.setRenderer(new OperatorComboBoxRenderer());
    operatorComboBox.addActionListener(new OperatorComboBoxActionListener());
    operatorComboBox.setSelectedItem(operatorComboBox.getSelectedItem());

    sendButton.setAction(sendAction);

    smsCounterLabel.setText("0 znak\u016f (0 sms)");

    jLabel2.setText("Jm\u00e9no");

    nameLabel.setForeground(new java.awt.Color(0, 51, 255));

    javax.swing.GroupLayout smsPanelLayout = new javax.swing.GroupLayout(smsPanel);
    smsPanel.setLayout(smsPanelLayout);
    smsPanelLayout.setHorizontalGroup(
        smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(smsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel2)
                .addComponent(jLabel4)
                .addComponent(jLabel5))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(smsPanelLayout.createSequentialGroup()
                    .addComponent(jLabel1)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(smsNumberTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(operatorComboBox, 0, 97, Short.MAX_VALUE))
                .addComponent(nameLabel)
                .addGroup(smsPanelLayout.createSequentialGroup()
                    .addComponent(smsCounterLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(sendButton))
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE))
            .addContainerGap())
    );
    smsPanelLayout.setVerticalGroup(
        smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(smsPanelLayout.createSequentialGroup()
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(nameLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel4)
                .addComponent(jLabel1)
                .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(smsNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel5)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(smsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(sendButton)
                .addComponent(smsCounterLabel))
            .addContainerGap())
    );
    verticalSplitPane.setLeftComponent(smsPanel);

    queuePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fronta"));
    smsQueueList.setModel(smsQueueListModel);
    smsQueueList.setCellRenderer(new SMSQueueListRenderer());
    smsQueueList.setVisibleRowCount(4);
    smsQueueList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
        public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
            smsQueueListValueChanged(evt);
        }
    });

    jScrollPane2.setViewportView(smsQueueList);

    pauseButton.setAction(smsQueuePauseAction);
    pauseButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    pauseButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

    editButton.setAction(editSMSAction);
    editButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    editButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

    deleteButton.setAction(deleteSMSAction);
    deleteButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    deleteButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

    smsUpButton.setAction(smsUpAction);
    smsUpButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    smsUpButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

    smsDownButton.setAction(smsDownAction);
    smsDownButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
    smsDownButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

    javax.swing.GroupLayout queuePanelLayout = new javax.swing.GroupLayout(queuePanel);
    queuePanel.setLayout(queuePanelLayout);
    queuePanelLayout.setHorizontalGroup(
        queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(queuePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(smsUpButton)
                .addComponent(smsDownButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, queuePanelLayout.createSequentialGroup()
                    .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(pauseButton))
                .addComponent(editButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
    );

    queuePanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});

    queuePanelLayout.setVerticalGroup(
        queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(queuePanelLayout.createSequentialGroup()
            .addGroup(queuePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, queuePanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(pauseButton))
                .addGroup(queuePanelLayout.createSequentialGroup()
                    .addComponent(smsUpButton)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(smsDownButton))
                .addGroup(queuePanelLayout.createSequentialGroup()
                    .addComponent(editButton)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(deleteButton))
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 26, Short.MAX_VALUE))
            .addContainerGap())
    );

    queuePanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteButton, editButton, pauseButton, smsDownButton, smsUpButton});

    verticalSplitPane.setRightComponent(queuePanel);

    horizontalSplitPane.setLeftComponent(verticalSplitPane);

    contactPanel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            contactListSelectionChanged();
        }
    });
    horizontalSplitPane.setRightComponent(contactPanel);

    programMenu.setMnemonic('r');
    programMenu.setText("Program");
    configMenuItem.setAction(configAction);
    programMenu.add(configMenuItem);

    aboutMenuItem.setAction(aboutAction);
    programMenu.add(aboutMenuItem);

    exitMenuItem.setAction(quitAction);
    programMenu.add(exitMenuItem);

    menuBar.add(programMenu);

    toolsMenu.setMnemonic('n');
    toolsMenu.setText("N\u00e1stroje");
    importMenuItem.setAction(importAction);
    toolsMenu.add(importMenuItem);

    exportMenuItem.setAction(exportAction);
    toolsMenu.add(exportMenuItem);

    menuBar.add(toolsMenu);

    setJMenuBar(menuBar);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(statusPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(horizontalSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 584, Short.MAX_VALUE)
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(horizontalSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );
    pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        saveConfig();
        saveContacts();
        saveQueue();
    }//GEN-LAST:event_formWindowClosing
    
    private void smsQueueListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_smsQueueListValueChanged
        //update form components
        if (!evt.getValueIsAdjusting()) {
            int queueSize = smsQueueListModel.getSize();
            int selectedItems = smsQueueList.getSelectedIndices().length;
            deleteSMSAction.setEnabled(queueSize != 0 && selectedItems != 0);
            editSMSAction.setEnabled(queueSize != 0 && selectedItems == 1);
            smsUpAction.setEnabled(queueSize != 0 && selectedItems == 1);
            smsDownAction.setEnabled(queueSize != 0 && selectedItems == 1);
        }
    }//GEN-LAST:event_smsQueueListValueChanged
    
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
    
    /** Prints message to status bar */
    public void printStatusMessage(String message) {
        statusMessageLabel.setText(message);
    }
    
    /** Tells main form whether it should display task busy icon */
    public void setTaskRunning(boolean b) {
        if (b == false)
            statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource(RES + "task-idle.png")));
        else
            statusAnimationLabel.setIcon(new ImageIcon(getClass().getResource(RES + "task-busy.gif")));
    }
    
    /** Notifies about change in sms queue */
    public void smsProcessed(SMS sms) {
        int index = smsQueueListModel.indexOf(sms);
        if (sms.getStatus() == SMS.Status.SENT_OK) {
            smsQueueListModel.remove(sms);
        }
        if (sms.getStatus() == SMS.Status.PROBLEMATIC) {
            smsQueueListModel.fireContentsChanged(
                    smsQueueListModel, index, index);
        }
    }
    
    /** Pauses sms queue */
    public void pauseSMSQueue() {
        smsQueuePauseAction.actionPerformed(null);
    }
    
    /** Forces delay before sending another sms */
    public void setSMSDelay() {
        smsSender.setDelayed(true);
        smsDelayTimer.start();
    }
    
    /** Import additional contacts */
    public void importContacts(Collection<Contact> contacts) {
        contactPanel.clearSelection();
        contactPanel.addContacts(contacts);
    }
    
    /** updates name according to number and operator */
    private boolean lookupContact() {
        if (contacts == null)
            return false;
        
        String countryCode = "+420";
        String number = smsNumberTextField.getText();
        Operator operator = (Operator)operatorComboBox.getSelectedItem();
        
        // skip if already selected right contact
        Contact selected = null;
        HashSet<Contact> selecteds = contactPanel.getSelectedContacts();
        if (selecteds.size() > 0)
            selected = selecteds.iterator().next();
        if (selected != null && selected.getCountryCode().equals(countryCode) &&
                selected.getNumber().equals(number) &&
                selected.getOperator().equals(operator))
            return true;
        
        Contact contact = null;
        for (Contact c : contacts) {
            if (c.getCountryCode() != null && c.getCountryCode().equals(countryCode) &&
                    c.getNumber() != null && c.getNumber().equals(number) &&
                    c.getOperator() != null && c.getOperator().equals(operator)) {
                contact = c;
                break;
            }
        }
        
        if (contact != null) {
            contactPanel.setSelectedContact(contact);
            return true;
        } else {
            contactPanel.clearSelection();
            return false;
        }
    }
    
    /** validates sms form and returns status */
    private boolean validateForm(boolean transferFocus) {
        if (envelope == null)
            return false;
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
    
    /** save program configuration */
    private void saveConfig() {
        //save frame layout
        config.setMainDimension(this.getSize());
        config.setHorizontalSplitPaneLocation(horizontalSplitPane.getDividerLocation());
        config.setVerticalSplitPaneLocation(verticalSplitPane.getDividerLocation());
        
        try {
            persistenceManager.saveConfig();
        } catch (Exception ex) {
            ex.printStackTrace();
            printStatusMessage("Nepodařilo se uložit nastavení!");
        }
    }
    
    /** load program configuration */
    private void loadConfig() {
        if (config.isRememberLayout()) { //set frame layout
            Dimension mainDimension = config.getMainDimension();
            Integer horizontalSplitPaneLocation = config.getHorizontalSplitPaneLocation();
            Integer verticalSplitPaneLocation = config.getVerticalSplitPaneLocation();
            if (mainDimension != null)
                this.setSize(mainDimension);
            if (horizontalSplitPaneLocation != null)
                horizontalSplitPane.setDividerLocation(horizontalSplitPaneLocation);
            if (verticalSplitPaneLocation != null)
                verticalSplitPane.setDividerLocation(verticalSplitPaneLocation);
        }
    }
    
    /** save contacts */
    private void saveContacts() {
        try {
            persistenceManager.saveContacts();
        } catch (Exception ex) {
            ex.printStackTrace();
            printStatusMessage("Nepodařilo se uložit kontakty!");
        }
    }
    
    /** save sms queue */
    private void saveQueue() {
        try {
            persistenceManager.saveQueue();
        } catch (IOException ex) {
            ex.printStackTrace();
            printStatusMessage("Nepodařilo se uložit frontu sms!");
        }
    }
    
    /** Show about frame */
    private class AboutAction extends AbstractAction {
        AboutFrame aboutFrame;
        public AboutAction() {
            super("O programu", new ImageIcon(MainFrame.class.getResource(RES + "about-small.png")));
            this.putValue(MNEMONIC_KEY,KeyEvent.VK_O);
        }
        public void actionPerformed(ActionEvent e) {
            if (aboutFrame != null && aboutFrame.isVisible()) {
                aboutFrame.requestFocus();
            } else {
                aboutFrame = new AboutFrame();
                aboutFrame.setLocationRelativeTo(MainFrame.this);
                aboutFrame.setVisible(true);
            }
        }
    }
    
    /** Quit the program */
    private class QuitAction extends AbstractAction {
        public QuitAction() {
            super("Ukončit", new ImageIcon(MainFrame.class.getResource(RES + "exit-small.png")));
            this.putValue(MNEMONIC_KEY,KeyEvent.VK_U);
        }
        public void actionPerformed(ActionEvent e) {
            MainFrame.this.formWindowClosing(null);
            System.exit(0);
        }
    }
    
    /** Show config frame */
    private class ConfigAction extends AbstractAction {
        private ConfigFrame configFrame;
        public ConfigAction() {
            super("Nastavení", new ImageIcon(MainFrame.class.getResource(RES + "config-small.png")));
            this.putValue(MNEMONIC_KEY,KeyEvent.VK_N);
        }
        public void actionPerformed(ActionEvent e) {
            if (configFrame != null && configFrame.isVisible()) {
                configFrame.requestFocus();
            } else {
                configFrame = new ConfigFrame();
                configFrame.setLocationRelativeTo(MainFrame.this);
                configFrame.setVisible(true);
            }
        }
    }
    
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
            
            for (SMS sms : envelope.send()) {
                smsQueueListModel.add(sms);
                smsSender.announceNewSMS();
            }
            
            smsTextPane.setText(null);
            smsTextUndoManager.discardAllEdits();
            smsTextPane.requestFocusInWindow();
        }
        /** update status according to conditions  */
        public void updateStatus() {
            this.setEnabled(validateForm(false));
        }
    }
    
    /** Erase sms from queue list */
    private class DeleteSMSAction extends AbstractAction {
        public DeleteSMSAction() {
            super(null, new ImageIcon(MainFrame.class.getResource(RES + "delete.png")));
            this.putValue(SHORT_DESCRIPTION,"Odstranit označené zprávy");
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            Object[] smsArray = smsQueueList.getSelectedValues();
            for (Object o : smsArray) {
                SMS sms = (SMS) o;
                smsQueueListModel.remove(sms);
            }
        }
    }
    
    /** Edit sms from queue */
    private class EditSMSAction extends AbstractAction {
        public EditSMSAction() {
            super(null, new ImageIcon(MainFrame.class.getResource(RES + "edit.png")));
            this.putValue(SHORT_DESCRIPTION,"Upravit označenou zprávu");
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            SMS sms = (SMS) smsQueueList.getSelectedValue();
            if (sms == null)
                return;
            contactPanel.clearSelection();
            smsNumberTextField.setText(sms.getNumber().substring(4));
            smsTextPane.setText(sms.getText());
            operatorComboBox.setSelectedItem(sms.getOperator());
            nameLabel.setText(sms.getName());
            smsQueueListModel.remove(sms);
            smsTextPane.requestFocusInWindow();
        }
    }
    
    /** move sms up in sms queue */
    private class SMSUpAction extends AbstractAction {
        public SMSUpAction() {
            super(null,new ImageIcon(MainFrame.class.getResource(RES + "up.png")));
            this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě výše");
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            int index = smsQueueList.getSelectedIndex();
            if (index <= 0) //cannot move up first item
                return;
            synchronized(smsQueue) {
                Collections.swap(smsQueue,index,index-1);
            }
            smsQueueListModel.fireContentsChanged(
                    smsQueueListModel, index-1, index);
            smsQueueList.setSelectedIndex(index-1);
            smsQueueList.ensureIndexIsVisible(index-1);
        }
    }
    
    /** move sms down in sms queue */
    private class SMSDownAction extends AbstractAction {
        public SMSDownAction() {
            super(null,new ImageIcon(MainFrame.class.getResource(RES + "down.png")));
            this.putValue(SHORT_DESCRIPTION,"Posunout sms ve frontě níže");
            this.setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            int index = smsQueueList.getSelectedIndex();
            if (index < 0 || index >= smsQueueListModel.getSize() - 1) //cannot move down last item
                return;
            synchronized(smsQueue) {
                Collections.swap(smsQueue,index,index+1);
            }
            smsQueueListModel.fireContentsChanged(
                    smsQueueListModel, index, index+1);
            smsQueueList.setSelectedIndex(index+1);
            smsQueueList.ensureIndexIsVisible(index+1);
        }
    }
    
    /** Pause/unpause the sms queue */
    private class SMSQueuePauseAction extends AbstractAction {
        private boolean makePause = true;
        private final String descRunning = "Pozastavit odesílání sms ve frontě (Alt+P)";
        private final String descStopped = "Pokračovat v odesílání sms ve frontě (Alt+P)";
        public SMSQueuePauseAction() {
            super(null, new ImageIcon(MainFrame.class.getResource(RES + "pause.png")));
            this.putValue(SHORT_DESCRIPTION,descRunning);
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }
        public void actionPerformed(ActionEvent e) {
            if (makePause) {
                smsSender.setPaused(true);
                this.putValue(LARGE_ICON_KEY,
                        new ImageIcon(MainFrame.class.getResource(RES + "start.png")));
                this.putValue(SHORT_DESCRIPTION,descStopped);
            } else {
                smsSender.setPaused(false);
                this.putValue(LARGE_ICON_KEY,
                        new ImageIcon(MainFrame.class.getResource(RES + "pause.png")));
                this.putValue(SHORT_DESCRIPTION,descRunning);
            }
            makePause = !makePause;
        }
    }
    
    /** import data from other programs */
    private class ImportAction extends AbstractAction {
        private ImportFrame importFrame;
        public ImportAction() {
            super("Import kontaktů", new ImageIcon(MainFrame.class.getResource(RES + "contact-small.png")));
            this.putValue(SHORT_DESCRIPTION,"Importovat kontakty z jiných aplikací");
            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        }
        public void actionPerformed(ActionEvent e) {
            if (importFrame != null && importFrame.isVisible()) {
                importFrame.requestFocus();
            } else {
                importFrame = new ImportFrame();
                importFrame.setLocationRelativeTo(MainFrame.this);
                importFrame.setVisible(true);
            }
        }
    }
    
    /** export data for other programs */
    private class ExportAction extends AbstractAction {
        public ExportAction() {
            super("Export kontaktů", new ImageIcon(MainFrame.class.getResource(RES + "contact-small.png")));
            this.putValue(SHORT_DESCRIPTION,"Exportovat kontakty do souboru");
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }
        public void actionPerformed(ActionEvent e) {
            ExportManager.exportContacts(MainFrame.this,contacts);
        }
    }
    
    /** Model for SMSQueueList */
    private class SMSQueueListModel extends AbstractListModel {
        public SMS getElementAt(int index) {
            return smsQueue.get(index);
        }
        public int getSize() {
            return smsQueue.size();
        }
        public int indexOf(SMS element) {
            return smsQueue.indexOf(element);
        }
        public void add(SMS element) {
            if (smsQueue.add(element)) {
                int index = smsQueue.indexOf(element);
                fireIntervalAdded(this, index, index);
            }
        }
        public boolean contains(SMS element) {
            return smsQueue.contains(element);
        }
        public boolean remove(SMS element) {
            int index = smsQueue.indexOf(element);
            boolean removed = smsQueue.remove(element);
            if (removed) {
                fireIntervalRemoved(this, index, index);
            }
            return removed;
        }
        protected void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1);
        }
        protected void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1);
        }
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }
    }
    
    /** Renderer for items in queue list */
    private class SMSQueueListRenderer implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = (new DefaultListCellRenderer()).getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            SMS sms = (SMS)value;
            //problematic sms colored
            if ((sms.getStatus() == SMS.Status.PROBLEMATIC) && !isSelected) {
                c.setBackground(Color.RED);
            }
            //add operator logo
            ((JLabel)c).setIcon(sms.getOperator().getIcon());
            //set tooltip
            ((JLabel)c).setToolTipText(wrapToHTML(sms.getText()));
            
            return c;
        }
        /** transform string to html with linebreaks */
        private String wrapToHTML(String text) {
            StringBuilder output = new StringBuilder();
            output.append("<html>");
            int from = 0;
            while (from < text.length()) {
                int to = from + 50;
                to = text.indexOf(' ',to);
                if (to < 0)
                    to = text.length();
                output.append(text.substring(from, to));
                output.append("<br>");
                from = to + 1;
            }
            output.append("</html>");
            return output.toString();
        }
    }
    
    /** Progress bar action listener after sending sms */
    private class SMSDelayActionListener implements ActionListener {
        private final int DELAY = 15;
        private int seconds = 0;
        public void actionPerformed(ActionEvent e) {
            if (seconds <= DELAY) { //still wainting
                smsDelayProgressBar.setValue(seconds);
                smsDelayProgressBar.setString("Další sms za: " + (DELAY-seconds) + "s");
                if (seconds == 0)
                    smsDelayProgressBar.setVisible(true);
                seconds++;
            } else { //delay finished
                smsDelayTimer.stop();
                smsDelayProgressBar.setVisible(false);
                seconds = 0;
                smsSender.setDelayed(false);
                smsSender.announceNewSMS();
            }
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
            if (envelope != null) {
                Set<Contact> set = new HashSet<Contact>();
                set.add(new Contact(nameLabel.getText(), "+420", smsNumberTextField.getText(),
                        (Operator)operatorComboBox.getSelectedItem()));
                envelope.setContacts(set);
            }
            
            //update components
            smsTextPaneDocumentFilter.requestUpdate();
        }
    }
    
    /** Listener for sms text pane */
    private class SMSTextPaneListener implements DocumentListener {
        /** count number of chars in sms and take action */
        private void countChars(DocumentEvent e) {
            int chars = e.getDocument().getLength();
            int smsCount = envelope != null? envelope.getSMSCount(chars) : 0;
            smsCounterLabel.setText(chars + " znaků (" +  smsCount + " sms)");
            if (envelope != null && chars > envelope.getMaxTextLength()) { //chars more than max
                smsCounterLabel.setForeground(Color.RED);
                smsCounterLabel.setText(chars + " znaků (nelze odeslat!)");
            } else //chars ok
                smsCounterLabel.setForeground(UIManager.getColor("Label.foreground"));
        }
        /** update form components */
        private void updateUI(DocumentEvent e) {
            if (envelope != null) {
                try {
                    envelope.setText(e.getDocument().getText(0,e.getDocument().getLength()));
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
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
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem configMenuItem;
    private esmska.gui.ContactPanel contactPanel;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editButton;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JSplitPane horizontalSplitPane;
    private javax.swing.JMenuItem importMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JComboBox operatorComboBox;
    private javax.swing.JButton pauseButton;
    private javax.swing.JMenu programMenu;
    private javax.swing.JPanel queuePanel;
    private javax.swing.JButton sendButton;
    private javax.swing.JLabel smsCounterLabel;
    private javax.swing.JProgressBar smsDelayProgressBar;
    private javax.swing.JButton smsDownButton;
    javax.swing.JTextField smsNumberTextField;
    private javax.swing.JPanel smsPanel;
    private javax.swing.JList smsQueueList;
    private javax.swing.JTextPane smsTextPane;
    private javax.swing.JButton smsUpButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JSplitPane verticalSplitPane;
    // End of variables declaration//GEN-END:variables
}
