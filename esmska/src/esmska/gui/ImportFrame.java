/*
 * ImportFrame.java
 *
 * Created on 18. srpen 2007, 23:11
 */

package esmska.gui;

import esmska.persistence.ContactParser;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;
import esmska.data.Contact;
import esmska.operators.OperatorUtil;
import esmska.persistence.PersistenceManager;
import esmska.utils.ActionEventSupport;
import esmska.utils.L10N;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/** Import contacts from external applications
 *
 * @author  ripper
 */
public class ImportFrame extends javax.swing.JFrame {
    public static final int ACTION_IMPORT_CONTACTS = 0;
    private static final Logger logger = Logger.getLogger(ImportFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String infoEsmska = l10n.getString("ImportFrame.infoEsmska");
    private static final String infoKubik = l10n.getString("ImportFrame.infoKubik");
    private static final String infoDreamComSE = l10n.getString("ImportFrame.infoDreamComSE");
    private static final String infoVcard = l10n.getString("ImportFrame.infoVcard");
    private static final String encodingUTF8 = l10n.getString("ImportFrame.encodingUTF8");
    private static final String encodingWin1250 = l10n.getString("ImportFrame.encodingWin1250");

    private CardLayout cardLayout;
    private SwingWorker<ArrayList<Contact>,Void> worker; //worker for background thread
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    private ArrayList<Contact> importedContacts = new ArrayList<Contact>(); //results from import
    private String actualCard = "applicationPanel";
    private JFileChooser chooser = new JFileChooser();
    
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Creates new form ImportFrame */
    public ImportFrame() {
        initComponents();
        
        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImportFrame.this.setVisible(false);
                ImportFrame.this.dispose();
            }
        });
        
        cardLayout = (CardLayout) cardPanel.getLayout();
        progressBar.setVisible(false);
        backButton.setVisible(false);
        chooser.setApproveButtonText(l10n.getString("ImportFrame.Select"));
        chooser.setDialogTitle(l10n.getString("ImportFrame.choose_export_file"));
        chooser.setMultiSelectionEnabled(false);
        chooser.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (vcardRadioButton.isSelected()) {
                    return f.getName().toLowerCase().endsWith(".vcard") ||
                            f.getName().toLowerCase().endsWith(".vcf") ||
                            f.isDirectory();
                } else {
                    return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
                }
            }
            @Override
            public String getDescription() {
                if (vcardRadioButton.isSelected()) {
                    return l10n.getString("ImportFrame.vCard_filter");
                } else {
                    return l10n.getString("ImportFrame.CSV_filter");
                }
            }
        });
    }
    
    /** get list of imported contacts */
    public ArrayList<Contact> getImportedContacts() {
        return importedContacts;
    }
    
    /** browse for file */
    private String doBrowseButton() {
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }
    
    /** remove contacts already present in contact list */
    private void removeExistingContacts() {
        DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
        Object[] imported = contactListModel.toArray();
        ArrayList<Object> skipped = new ArrayList<Object>();
        for (Object impor : imported) {
            for (Contact exist : contacts) {
                if (exist.compareTo((Contact) impor) == 0) {
                    skipped.add(impor);
                    break;
                }
            }
        }
        for (Object skip : skipped)
            contactListModel.removeElement(skip);
    }
    
    /** remove contacts without known operator */
    private void removeInvalidOperators() {
        DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
        Object[] imported = contactListModel.toArray();
        ArrayList<Object> skipped = new ArrayList<Object>();
        for (Object impor : imported) {
            Contact c = (Contact) impor;
            if (OperatorUtil.getOperator(c.getOperator()) == null)
                skipped.add(c);
        }
        for (Object skip : skipped)
            contactListModel.removeElement(skip);
    }
    
    /** update labels on browse panel according to type of import */
    private void updateBrowsePanel() {
        if (esmskaRadioButton.isSelected()) {
            infoLabel.setText(infoEsmska);
            encodingLabel.setText(encodingUTF8);
            problemLabel.setVisible(false);
        } else if (kubikRadioButton.isSelected()) {
            infoLabel.setText(infoDreamComSE);
            encodingLabel.setText(encodingWin1250);
            problemLabel.setVisible(true);
        } else if (dreamcomSERadioButton.isSelected()) {
            infoLabel.setText(infoKubik);
            encodingLabel.setText(encodingWin1250);
            problemLabel.setVisible(true);
        } else if (vcardRadioButton.isSelected()) {
            infoLabel.setText(infoVcard);
            encodingLabel.setText(encodingUTF8);
            problemLabel.setVisible(true);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        importButtonGroup = new javax.swing.ButtonGroup();
        cardPanel = new javax.swing.JPanel();
        applicationPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        kubikRadioButton = new javax.swing.JRadioButton();
        dreamcomSERadioButton = new javax.swing.JRadioButton();
        esmskaRadioButton = new javax.swing.JRadioButton();
        jLabel4 = new javax.swing.JLabel();
        vcardRadioButton = new javax.swing.JRadioButton();
        browsePanel = new javax.swing.JPanel();
        fileTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        infoLabel = new javax.swing.JLabel();
        fileLabel = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        encodingLabel = new javax.swing.JLabel();
        problemLabel = new javax.swing.JLabel();
        resultsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        contactList = new javax.swing.JList();
        jLabel8 = new javax.swing.JLabel();
        validOperatorCheckBox = new javax.swing.JCheckBox();
        forwardButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        backButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("ImportFrame.title")); // NOI18N
        setIconImage(new ImageIcon(getClass().getResource(RES + "contact-48.png")).getImage());

        cardPanel.setLayout(new java.awt.CardLayout());

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, l10n.getString("ImportFrame.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, l10n.getString("ImportFrame.jLabel3.text")); // NOI18N

        importButtonGroup.add(kubikRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(kubikRadioButton, "&Kubík SMS DreamCom"); // NOI18N

        importButtonGroup.add(dreamcomSERadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(dreamcomSERadioButton, "&DreamCom SE"); // NOI18N

        importButtonGroup.add(esmskaRadioButton);
        esmskaRadioButton.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(esmskaRadioButton, "&Esmska"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, l10n.getString("ImportFrame.jLabel4.text")); // NOI18N

        importButtonGroup.add(vcardRadioButton);
        org.openide.awt.Mnemonics.setLocalizedText(vcardRadioButton, "&vCard (*.vcard, *.vcf)"); // NOI18N

        javax.swing.GroupLayout applicationPanelLayout = new javax.swing.GroupLayout(applicationPanel);
        applicationPanel.setLayout(applicationPanelLayout);
        applicationPanelLayout.setHorizontalGroup(
            applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(applicationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(applicationPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(vcardRadioButton))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addGroup(applicationPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(esmskaRadioButton)
                            .addComponent(kubikRadioButton)
                            .addComponent(dreamcomSERadioButton))
                        .addGap(323, 323, 323))
                    .addComponent(jLabel4))
                .addContainerGap())
        );
        applicationPanelLayout.setVerticalGroup(
            applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(applicationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(esmskaRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(kubikRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(dreamcomSERadioButton)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(vcardRadioButton)
                .addContainerGap(139, Short.MAX_VALUE))
        );

        cardPanel.add(applicationPanel, "applicationPanel");

        fileTextField.setToolTipText(l10n.getString("ImportFrame.fileTextField.toolTipText")); // NOI18N

        browseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/browse-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(browseButton, l10n.getString("ImportFrame.browseButton.text")); // NOI18N
        browseButton.setToolTipText(l10n.getString("ImportFrame.browseButton.toolTipText")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        infoLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(infoLabel, "<<info text>>"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, l10n.getString("ImportFrame.fileLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel22, l10n.getString("ImportFrame.jLabel22.text")); // NOI18N

        encodingLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(encodingLabel, "<<encoding hint>>"); // NOI18N

        problemLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(problemLabel, l10n.getString("ImportFrame.problemLabel.text")); // NOI18N

        javax.swing.GroupLayout browsePanelLayout = new javax.swing.GroupLayout(browsePanel);
        browsePanel.setLayout(browsePanelLayout);
        browsePanelLayout.setHorizontalGroup(
            browsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(browsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(browsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel22, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(fileLabel)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, browsePanelLayout.createSequentialGroup()
                        .addComponent(fileTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 379, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(browseButton))
                    .addComponent(encodingLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(problemLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE))
                .addContainerGap())
        );
        browsePanelLayout.setVerticalGroup(
            browsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(browsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(infoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(browsePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(browseButton)
                    .addComponent(fileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel22)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(encodingLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(problemLabel)
                .addContainerGap(130, Short.MAX_VALUE))
        );

        cardPanel.add(browsePanel, "browsePanel");

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, l10n.getString("ImportFrame.jLabel1.text")); // NOI18N

        contactList.setModel(new DefaultListModel());
        contactList.setCellRenderer(new ContactsListRenderer());
        jScrollPane1.setViewportView(contactList);

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, l10n.getString("ImportFrame.jLabel8.text")); // NOI18N

        validOperatorCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(validOperatorCheckBox, l10n.getString("ImportFrame.validOperatorCheckBox.text")); // NOI18N
        validOperatorCheckBox.setToolTipText(l10n.getString("ImportFrame.validOperatorCheckBox.toolTipText")); // NOI18N
        validOperatorCheckBox.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                validOperatorCheckBoxStateChanged(evt);
            }
        });

        javax.swing.GroupLayout resultsPanelLayout = new javax.swing.GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(validOperatorCheckBox)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(jLabel8))
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 225, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(validOperatorCheckBox)
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addContainerGap())
        );

        cardPanel.add(resultsPanel, "resultsPanel");

        forwardButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/next-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(forwardButton, l10n.getString("ImportFrame.forwardButton.text")); // NOI18N
        forwardButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        forwardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                forwardButtonActionPerformed(evt);
            }
        });

        progressBar.setIndeterminate(true);
        progressBar.setString(l10n.getString("ImportFrame.progressBar.string")); // NOI18N
        progressBar.setStringPainted(true);

        backButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/previous-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(backButton, l10n.getString("ImportFrame.backButton.text")); // NOI18N
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 246, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(forwardButton)
                .addContainerGap())
            .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {backButton, forwardButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(forwardButton)
                        .addComponent(backButton))
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {backButton, forwardButton, progressBar});

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        //parsing
        if (actualCard.equals("browsePanel")) {
            backButton.setVisible(false);
            cardLayout.show(cardPanel, "applicationPanel");
            actualCard = "applicationPanel";
            return;
        }
        //result
        if (actualCard.equals("resultsPanel")) {
            String nextCard = "browsePanel";
            forwardButton.setText(l10n.getString("ImportFrame.forwardButton.text"));
            forwardButton.setIcon(new ImageIcon(
                        ImportFrame.class.getResource(RES + "next-22.png")));
            forwardButton.setHorizontalTextPosition(SwingConstants.LEADING);
            updateBrowsePanel();
            cardLayout.show(cardPanel, nextCard);
            actualCard = nextCard;
        }
    }//GEN-LAST:event_backButtonActionPerformed
    
    private void forwardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_forwardButtonActionPerformed
        //introduction panel
        if (actualCard.equals("applicationPanel")) {
            String nextCard = "browsePanel";
            updateBrowsePanel();
            cardLayout.show(cardPanel, nextCard);
            actualCard = nextCard;
            backButton.setVisible(true);
            return;
        }
        //parsing
        if (actualCard.equals("browsePanel")) {
            ContactParser.ContactType type = null;
            String filename = fileTextField.getText();
            if (esmskaRadioButton.isSelected()) {
                type = ContactParser.ContactType.ESMSKA_FILE;
            } else if (kubikRadioButton.isSelected()) {
                type = ContactParser.ContactType.KUBIK_DREAMCOM_FILE;
            } else if (dreamcomSERadioButton.isSelected()) {
                type = ContactParser.ContactType.DREAMCOM_SE_FILE;
            } else if (vcardRadioButton.isSelected()) {
                type = ContactParser.ContactType.VCARD_FILE;
            }
            
            File file = new File(filename);
            if (!(file.isFile() && file.canRead())) {
                JOptionPane.showMessageDialog(this, 
                        MessageFormat.format(l10n.getString("ImportFrame.file_cant_be_read"), file.getAbsolutePath()),
                        null, JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            progressBar.setVisible(true);
            forwardButton.setEnabled(false);
            backButton.setEnabled(false);
            worker = new ContactParser(file, type);
            worker.addPropertyChangeListener(new ParseContactsFinishedListener());
            worker.execute();
            
            return;
        }
        //result
        if (actualCard.equals("resultsPanel")) {
            DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
            importedContacts.clear();
            for (Object o : contactListModel.toArray())
                importedContacts.add((Contact) o);
            
            actionSupport.fireActionPerformed(ACTION_IMPORT_CONTACTS, null);
            this.setVisible(false);
            this.dispose();
            return;
        }
}//GEN-LAST:event_forwardButtonActionPerformed
                
    private void validOperatorCheckBoxStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_validOperatorCheckBoxStateChanged
        if (validOperatorCheckBox.isSelected()) {
            removeInvalidOperators();
        } else {
            DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
            contactListModel.clear();
            try {
                for (Contact c : worker.get())
                    contactListModel.addElement(c);
                removeExistingContacts();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Problem getting new contacts", ex);
            }
        }
    }//GEN-LAST:event_validOperatorCheckBoxStateChanged

private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        String file = doBrowseButton();
        if (file != null) {
            fileTextField.setText(file);
        }
}//GEN-LAST:event_browseButtonActionPerformed
    
    /** handle end of parsing contacts */
    private class ParseContactsFinishedListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (! "state".equals(evt.getPropertyName()))
                return;
            if (! SwingWorker.StateValue.DONE.equals(evt.getNewValue()))
                return;
            try {
                DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
                contactListModel.clear();
                ArrayList<Contact> contacts = worker.get();
                Collections.sort(contacts);
                for (Contact c : contacts)
                    contactListModel.addElement(c);
                removeExistingContacts();
                validOperatorCheckBoxStateChanged(null);
                
                forwardButton.setText(l10n.getString("Import"));
                forwardButton.setIcon(new ImageIcon(
                        ImportFrame.class.getResource(RES + "contact-22.png")));
                forwardButton.setHorizontalTextPosition(SwingConstants.TRAILING);
                cardLayout.show(cardPanel, "resultsPanel");
                actualCard = "resultsPanel";
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while parsing file", ex);
                JOptionPane.showMessageDialog(ImportFrame.this, 
                        l10n.getString("ImportFrame.invalid_file"),
                        null, JOptionPane.ERROR_MESSAGE);
            } finally {
                progressBar.setVisible(false);
                forwardButton.setEnabled(true);
                backButton.setEnabled(true);
            }
        }
    }
    
    private class ContactsListRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component comp = lafRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            JLabel label = (JLabel) comp;
            Contact c = (Contact) value;
            String number = (c.getNumber() != null ? c.getNumber() : "");
            String operator = (c.getOperator() != null ? c.getOperator() : "");
            label.setText(c.getName() + " (" + number + ", " + operator + ")");
            return label;
        }
    
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel applicationPanel;
    private javax.swing.JButton backButton;
    private javax.swing.JButton browseButton;
    private javax.swing.JPanel browsePanel;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JList contactList;
    private javax.swing.JRadioButton dreamcomSERadioButton;
    private javax.swing.JLabel encodingLabel;
    private javax.swing.JRadioButton esmskaRadioButton;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileTextField;
    private javax.swing.JButton forwardButton;
    private javax.swing.ButtonGroup importButtonGroup;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton kubikRadioButton;
    private javax.swing.JLabel problemLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel resultsPanel;
    private javax.swing.JCheckBox validOperatorCheckBox;
    private javax.swing.JRadioButton vcardRadioButton;
    // End of variables declaration//GEN-END:variables
    
}
