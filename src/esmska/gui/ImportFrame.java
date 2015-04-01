package esmska.gui;

import esmska.persistence.ContactParser;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.filechooser.FileFilter;
import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Gateways;
import esmska.data.event.AbstractListDataListener;
import esmska.utils.L10N;
import java.awt.Image;
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
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.openide.awt.Mnemonics;

/** Import contacts from external applications
 *
 * @author  ripper
 */
public class ImportFrame extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(ImportFrame.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Gateways gateways = Gateways.getInstance();
    private static final String infoEsmska = l10n.getString("ImportFrame.infoEsmska");
    private static final String infoKubik = l10n.getString("ImportFrame.infoKubik");
    private static final String infoDreamComSE = l10n.getString("ImportFrame.infoDreamComSE");
    private static final String infoVcard = l10n.getString("ImportFrame.infoVcard");
    private static final String encodingUTF8 = l10n.getString("ImportFrame.encodingUTF8");
    private static final String encodingWin1250 = l10n.getString("ImportFrame.encodingWin1250");
    private static final JFileChooser chooser = new JFileChooser();

    private CardLayout cardLayout;
    private SwingWorker<ArrayList<Contact>,Void> worker; //worker for background thread
    private String actualCard = "applicationPanel";
    
    /** Creates new form ImportFrame */
    public ImportFrame() {
        initComponents();
        
        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(Icons.get("contact-16.png").getImage());
        images.add(Icons.get("contact-22.png").getImage());
        images.add(Icons.get("contact-32.png").getImage());
        images.add(Icons.get("contact-48.png").getImage());
        setIconImages(images);

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
        chooser.resetChoosableFileFilters();
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

        //inform if no contacts found
        contactList.getModel().addListDataListener(new AbstractListDataListener() {
            @Override
            public void onUpdate(ListDataEvent e) {
                int size = contactList.getModel().getSize();
                foundContactsLabel.setText(size > 0 ?
                    MessageFormat.format(l10n.getString("ImportFrame.foundContacts"), size) :
                    l10n.getString("ImportFrame.foundContactsLabel.text"));
                if (actualCard.equals("resultsPanel")) {
                    forwardButton.setEnabled(size > 0);
                }
            }
        });
    }
    
    /** Change frame to parsing state and start parsing provided vCard file
     * @param fileName file to parse; not null
     */
    public void importVCardFile(String fileName) {
        Validate.notNull(fileName);

        vcardRadioButton.setSelected(true);
        updateBrowsePanel();
        cardLayout.show(cardPanel, "browsePanel");
        actualCard = "browsePanel";
        fileTextField.setText(fileName);
        forwardButtonActionPerformed(null);
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
        ArrayList<Contact> skipped = new ArrayList<Contact>();
        for (Object impor : imported) {
            for (Contact exist : Contacts.getInstance().getAll()) {
                Contact imp = (Contact) impor;
                if (ObjectUtils.equals(exist.getName(), imp.getName()) &&
                        ObjectUtils.equals(exist.getNumber(), imp.getNumber())) {
                    skipped.add(imp);
                    break;
                }
            }
        }
        for (Contact skip : skipped) {
            contactListModel.removeElement(skip);
        }
    }
    
    /** remove contacts without known gateway */
    private void removeInvalidGateways() {
        DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
        Object[] imported = contactListModel.toArray();
        ArrayList<Object> skipped = new ArrayList<Object>();
        for (Object impor : imported) {
            Contact c = (Contact) impor;
            if (gateways.get(c.getGateway()) == null) {
                skipped.add(c);
            }
        }
        for (Object skip : skipped) {
            contactListModel.removeElement(skip);
        }
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



        importButtonGroup = new ButtonGroup();
        cardPanel = new JPanel();
        applicationPanel = new JPanel();
        jLabel2 = new JLabel();
        jLabel3 = new JLabel();
        kubikRadioButton = new JRadioButton();
        dreamcomSERadioButton = new JRadioButton();
        esmskaRadioButton = new JRadioButton();
        jLabel4 = new JLabel();
        vcardRadioButton = new JRadioButton();
        browsePanel = new JPanel();
        fileTextField = new JTextField();
        browseButton = new JButton();
        infoLabel = new JLabel();
        fileLabel = new JLabel();
        jLabel22 = new JLabel();
        encodingLabel = new JLabel();
        problemLabel = new JLabel();
        resultsPanel = new JPanel();
        foundContactsLabel = new JLabel();
        jScrollPane1 = new JScrollPane();
        contactList = new JList();
        doImportLabel = new JLabel();
        forwardButton = new JButton();
        progressBar = new JProgressBar();
        backButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("ImportFrame.title")); // NOI18N
        cardPanel.setLayout(new CardLayout());

        jLabel2.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        Mnemonics.setLocalizedText(jLabel2, l10n.getString("ImportFrame.jLabel2.text")); // NOI18N
        Mnemonics.setLocalizedText(jLabel3, l10n.getString("ImportFrame.jLabel3.text"));

        importButtonGroup.add(kubikRadioButton);
        Mnemonics.setLocalizedText(kubikRadioButton, "&Kubík SMS DreamCom"); // NOI18N

        importButtonGroup.add(dreamcomSERadioButton);
        Mnemonics.setLocalizedText(dreamcomSERadioButton, "&DreamCom SE"); // NOI18N

        importButtonGroup.add(esmskaRadioButton);

        Mnemonics.setLocalizedText(esmskaRadioButton, "&Esmska"); // NOI18N
        Mnemonics.setLocalizedText(jLabel4, l10n.getString("ImportFrame.jLabel4.text"));

        importButtonGroup.add(vcardRadioButton);
        vcardRadioButton.setSelected(true);

        Mnemonics.setLocalizedText(vcardRadioButton, "&vCard (*.vcard, *.vcf)");
        GroupLayout applicationPanelLayout = new GroupLayout(applicationPanel);
        applicationPanel.setLayout(applicationPanelLayout);
        applicationPanelLayout.setHorizontalGroup(
            applicationPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(applicationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(applicationPanelLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabel2, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addGroup(applicationPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(vcardRadioButton))
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addGroup(applicationPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(applicationPanelLayout.createParallelGroup(Alignment.LEADING)
                            .addComponent(esmskaRadioButton)
                            .addComponent(kubikRadioButton)
                            .addComponent(dreamcomSERadioButton))
                        .addGap(206, 206, 206)))
                .addContainerGap())
        );
        applicationPanelLayout.setVerticalGroup(
            applicationPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(applicationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(vcardRadioButton)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(esmskaRadioButton)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(kubikRadioButton)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(dreamcomSERadioButton)
                .addContainerGap(145, Short.MAX_VALUE))
        );

        cardPanel.add(applicationPanel, "applicationPanel");







        fileTextField.setToolTipText(l10n.getString("ImportFrame.fileTextField.toolTipText")); // NOI18N
        browseButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/browse-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(browseButton, l10n.getString("ImportFrame.browseButton.text"));
        browseButton.setToolTipText(l10n.getString("ImportFrame.browseButton.toolTipText")); // NOI18N
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        infoLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        Mnemonics.setLocalizedText(infoLabel, "<<info text>>");
        Mnemonics.setLocalizedText(fileLabel, l10n.getString("ImportFrame.fileLabel.text"));
        Mnemonics.setLocalizedText(jLabel22, l10n.getString("ImportFrame.jLabel22.text"));
        encodingLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        Mnemonics.setLocalizedText(encodingLabel, "<<encoding hint>>");
        problemLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        Mnemonics.setLocalizedText(problemLabel, l10n.getString("ImportFrame.problemLabel.text"));
        GroupLayout browsePanelLayout = new GroupLayout(browsePanel);
        browsePanel.setLayout(browsePanelLayout);
        browsePanelLayout.setHorizontalGroup(
            browsePanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(browsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(browsePanelLayout.createParallelGroup(Alignment.LEADING)
                    .addComponent(jLabel22, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addComponent(infoLabel, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addComponent(fileLabel)
                    .addGroup(Alignment.TRAILING, browsePanelLayout.createSequentialGroup()
                        .addComponent(fileTextField, GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(browseButton))
                    .addComponent(encodingLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addComponent(problemLabel, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE))
                .addContainerGap())
        );
        browsePanelLayout.setVerticalGroup(
            browsePanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(browsePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(infoLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(fileLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(browsePanelLayout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(browseButton)
                    .addComponent(fileTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jLabel22, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(encodingLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(problemLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(134, Short.MAX_VALUE))
        );

        cardPanel.add(browsePanel, "browsePanel");

        Mnemonics.setLocalizedText(foundContactsLabel, l10n.getString("ImportFrame.foundContactsLabel.text"));
        contactList.setModel(new DefaultListModel());
        contactList.setCellRenderer(new ContactsListRenderer());
        jScrollPane1.setViewportView(contactList);


        doImportLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        Mnemonics.setLocalizedText(doImportLabel, l10n.getString("ImportFrame.doImportLabel.text"));
        GroupLayout resultsPanelLayout = new GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(resultsPanelLayout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(jScrollPane1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addComponent(foundContactsLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
                    .addComponent(doImportLabel, Alignment.LEADING))
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(foundContactsLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(doImportLabel)
                .addContainerGap())
        );

        cardPanel.add(resultsPanel, "resultsPanel");

        forwardButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/next-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(forwardButton, l10n.getString("ImportFrame.forwardButton.text"));
        forwardButton.setHorizontalTextPosition(SwingConstants.LEADING);
        forwardButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                forwardButtonActionPerformed(evt);
            }
        });

        progressBar.setIndeterminate(true);
        progressBar.setString(l10n.getString("ImportFrame.progressBar.string")); // NOI18N
        progressBar.setStringPainted(true);

        backButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/previous-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(backButton, l10n.getString("ImportFrame.backButton.text"));
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(backButton)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(forwardButton)
                .addContainerGap())
            .addComponent(cardPanel, GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {backButton, forwardButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(cardPanel, GroupLayout.DEFAULT_SIZE, 361, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(forwardButton)
                        .addComponent(backButton))
                    .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {backButton, forwardButton, progressBar});

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void backButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
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
            Mnemonics.setLocalizedText(forwardButton, l10n.getString("ImportFrame.forwardButton.text"));
            forwardButton.setIcon(Icons.get("next-22.png"));
            forwardButton.setHorizontalTextPosition(SwingConstants.LEADING);
            forwardButton.setEnabled(true);
            updateBrowsePanel();
            cardLayout.show(cardPanel, nextCard);
            actualCard = nextCard;
        }
    }//GEN-LAST:event_backButtonActionPerformed
    
    private void forwardButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_forwardButtonActionPerformed
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
                logger.info("File can't be read: " + file.getAbsolutePath());
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
            ArrayList<Contact> importedContacts = new ArrayList<Contact>();
            for (Object o : contactListModel.toArray()) {
                importedContacts.add((Contact) o);
            }
            logger.fine("Imported " + importedContacts.size() + " new contacts: " + importedContacts);

            Contacts.getInstance().addAll(importedContacts);
            Log.getInstance().addRecord(new Log.Record(
                    l10n.getString("MainFrame.import_complete"), null, Icons.STATUS_INFO));

            this.setVisible(false);
            this.dispose();
            return;
        }
}//GEN-LAST:event_forwardButtonActionPerformed
                
private void browseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        String file = doBrowseButton();
        if (file != null) {
            fileTextField.setText(file);
        }
}//GEN-LAST:event_browseButtonActionPerformed
    
    /** handle end of parsing contacts */
    private class ParseContactsFinishedListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (! "state".equals(evt.getPropertyName())) {
                return;
            }
            if (! SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                return;
            }
            try {
                DefaultListModel contactListModel = (DefaultListModel) contactList.getModel();
                contactListModel.clear();
                ArrayList<Contact> contacts = worker.get();
                Collections.sort(contacts);
                for (Contact c : contacts) {
                    contactListModel.addElement(c);
                }
                removeExistingContacts();

                Mnemonics.setLocalizedText(forwardButton, l10n.getString("Import_"));
                forwardButton.setIcon(Icons.get("contact-22.png"));
                forwardButton.setHorizontalTextPosition(SwingConstants.TRAILING);
                forwardButton.setEnabled(contactList.getModel().getSize() > 0);
                cardLayout.show(cardPanel, "resultsPanel");
                actualCard = "resultsPanel";
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while parsing file", ex);
                JOptionPane.showMessageDialog(ImportFrame.this, 
                        l10n.getString("ImportFrame.invalid_file"),
                        null, JOptionPane.ERROR_MESSAGE);
                forwardButton.setEnabled(true);
            } finally {
                progressBar.setVisible(false);
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
            String gateway = (c.getGateway() != null ? c.getGateway() : "");
            label.setText(c.getName() + " (" + number + ", " + gateway + ")");
            return label;
        }
    
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel applicationPanel;
    private JButton backButton;
    private JButton browseButton;
    private JPanel browsePanel;
    private JPanel cardPanel;
    private JList contactList;
    private JLabel doImportLabel;
    private JRadioButton dreamcomSERadioButton;
    private JLabel encodingLabel;
    private JRadioButton esmskaRadioButton;
    private JLabel fileLabel;
    private JTextField fileTextField;
    private JButton forwardButton;
    private JLabel foundContactsLabel;
    private ButtonGroup importButtonGroup;
    private JLabel infoLabel;
    private JLabel jLabel2;
    private JLabel jLabel22;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JScrollPane jScrollPane1;
    private JRadioButton kubikRadioButton;
    private JLabel problemLabel;
    private JProgressBar progressBar;
    private JPanel resultsPanel;
    private JRadioButton vcardRadioButton;
    // End of variables declaration//GEN-END:variables
    
}
