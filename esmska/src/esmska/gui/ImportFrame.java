/*
 * ImportFrame.java
 *
 * Created on 18. srpen 2007, 23:11
 */

package esmska.gui;

import esmska.persistence.ContactParser;
import java.awt.CardLayout;
import java.awt.Component;
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
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Import contacts from external applications
 *
 * @author  ripper
 */
public class ImportFrame extends javax.swing.JFrame {
    public static final int ACTION_IMPORT_CONTACTS = 0;
    private static final Logger logger = Logger.getLogger(ImportFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final String infoEsmska = "<html>Pro import kontaktů potřebujete mít" +
            " nachystaný CSV soubor vytvořený pomocí funkce \"Exportovat kontakty\". Tento" +
            " soubor zde vyberte.</html>";
    private static final String infoKubik = "<html>Neprve musíte exportovat kontakty " +
            "z programu Kubík SMS DreamCom. Spusťte uvedený program, přejděte do adresáře" +
            " kontaktů a pomocí pravého myšítka exportujte všechny své kontakty do CSV " +
            "souboru. Tento soubor zde následně vyberte.</html>";
    private static final String infoDreamComSE = "<html>Nejprve musíte exportovat kontakty" +
            " z programu DreamCom SE. Spusťte uvedený program, přejděte do adresáře " +
            "kontaktů a pomocí pravého myšítka exportujte všechny své kontakty do CSV " +
            "souboru. Tento soubor zde následně vyberte.</html>";
    private static final String encodingUTF8 = "<html>Program předpokládá, že soubor je " +
            "v kódování UTF-8.</html>";
    private static final String encodingWin1250 = "<html>Program předpokládá, že soubor " +
            "je v kódování windows-1250 (výchozí kódování souborů pro české MS Windows).</html>";

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
        cardLayout = (CardLayout) cardPanel.getLayout();
        progressBar.setVisible(false);
        backButton.setVisible(false);
        chooser.setApproveButtonText("Zvolit");
        chooser.setDialogTitle("Vyberte soubor s exportovanými kontakty");
        chooser.setMultiSelectionEnabled(false);
    }
    
    /** get list of imported contacts */
    public ArrayList<Contact> getImportedContacts() {
        return importedContacts;
    }
    
    /** browse for file */
    private String doBrowseButton() {
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
            }
            @Override
            public String getDescription() {
                return "CSV soubory (*.csv)";
            }
        });
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
        nextButton = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        backButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import kontaktů - Esmska");
        setIconImage(new ImageIcon(getClass().getResource(RES + "contact-48.png")).getImage());

        cardPanel.setLayout(new java.awt.CardLayout());

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        jLabel2.setText("<html>\nImport kontaktů vám dovolí načíst vaše kontakty z jiné aplikace a zkopírovat je do Esmsky. V původní aplikaci zůstanou vaše kontakty nedotčeny.\n</html>");

        jLabel3.setText("Vyberte, ze které aplikace chcete importovat kontakty:");

        importButtonGroup.add(kubikRadioButton);
        kubikRadioButton.setText("Kubík SMS DreamCom");

        importButtonGroup.add(dreamcomSERadioButton);
        dreamcomSERadioButton.setText("DreamCom SE");

        importButtonGroup.add(esmskaRadioButton);
        esmskaRadioButton.setSelected(true);
        esmskaRadioButton.setText("Esmska");

        javax.swing.GroupLayout applicationPanelLayout = new javax.swing.GroupLayout(applicationPanel);
        applicationPanel.setLayout(applicationPanelLayout);
        applicationPanelLayout.setHorizontalGroup(
            applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(applicationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 514, Short.MAX_VALUE)
                    .addComponent(jLabel3)
                    .addGroup(applicationPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(applicationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(esmskaRadioButton)
                            .addComponent(kubikRadioButton)
                            .addComponent(dreamcomSERadioButton))
                        .addGap(323, 323, 323)))
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
                .addContainerGap(204, Short.MAX_VALUE))
        );

        cardPanel.add(applicationPanel, "applicationPanel");

        browseButton.setMnemonic('r');
        browseButton.setText("Procházet...");
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        infoLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        infoLabel.setText("<<info text>>");

        fileLabel.setText("Zvolte vstupní soubor:");

        jLabel22.setText("<html>\nSoubor bude prozkoumán a následně vám bude vypsán seznam kontaktů dostupných pro import. Žádné změny zatím nebudou provedeny.\n</html>");

        encodingLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        encodingLabel.setText("<<encoding hint>>");

        problemLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/info-32.png"))); // NOI18N
        problemLabel.setText("<html>\nPokud budete mít problémy s importem, ověřte, zda nevyšla novější verze Esmsky, a zkuste to v ní.\n</html>");

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
                        .addComponent(fileTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 405, Short.MAX_VALUE)
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
                .addContainerGap(144, Short.MAX_VALUE))
        );

        cardPanel.add(browsePanel, "browsePanel");

        jLabel1.setText("Byly nalezeny následující nové kontakty:");

        contactList.setModel(new DefaultListModel());
        contactList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JLabel label = (JLabel) comp;
                Contact c = (Contact) value;
                label.setText(c.getName() + " (" + c.getNumber() + ", " + c.getOperator() + ")");
                return label;
            }

        });
        jScrollPane1.setViewportView(contactList);

        jLabel8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/contact-48.png"))); // NOI18N
        jLabel8.setText("Pokud chcete tyto kontakty importovat, stiskněte Importovat.");

        validOperatorCheckBox.setSelected(true);
        validOperatorCheckBox.setText("Importovat pouze kontakty se známým operátorem");
        validOperatorCheckBox.setToolTipText("<html>\nPokud je pole zatrhnuto, zobrazí se v seznamu a budou importovány<br>\npouze ty kontakty, s jejichž operátory umí tento program pracovat\n</html>");
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(validOperatorCheckBox)
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addContainerGap())
        );

        cardPanel.add(resultsPanel, "resultsPanel");

        nextButton.setMnemonic('p');
        nextButton.setText("Pokračovat");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        progressBar.setIndeterminate(true);
        progressBar.setString("Prosím čekejte...");
        progressBar.setStringPainted(true);

        backButton.setText("Zpět");
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
                .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(backButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nextButton)
                .addContainerGap())
            .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 538, Short.MAX_VALUE)
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {backButton, nextButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(cardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 368, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(nextButton)
                        .addComponent(backButton))
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {backButton, nextButton, progressBar});

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
            nextButton.setText("Pokračovat");
            nextButton.setIcon(null);
            updateBrowsePanel();
            cardLayout.show(cardPanel, nextCard);
            actualCard = nextCard;
        }
    }//GEN-LAST:event_backButtonActionPerformed
    
    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
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
            }
            
            File file = new File(filename);
            if (!(file.isFile() && file.canRead())) {
                JOptionPane.showMessageDialog(this, "Soubor " + file.getAbsolutePath() + " nelze přečíst!",
                        null, JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            progressBar.setVisible(true);
            nextButton.setEnabled(false);
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
    }//GEN-LAST:event_nextButtonActionPerformed
                
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
                for (Contact c : worker.get())
                    contactListModel.addElement(c);
                removeExistingContacts();
                validOperatorCheckBoxStateChanged(null);
                
                nextButton.setText("Importovat");
                nextButton.setIcon(new ImageIcon(
                        ImportFrame.class.getResource(RES + "contact-16.png")));
                cardLayout.show(cardPanel, "resultsPanel");
                actualCard = "resultsPanel";
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error while parsing file", ex);
                JOptionPane.showMessageDialog(ImportFrame.this, 
                        "<html><h2>Nastala chyba při zpracování souboru!</h2>" +
                        "Soubor zřejmě neobsahuje platné údaje.</html>",
                        null, JOptionPane.ERROR_MESSAGE);
            } finally {
                progressBar.setVisible(false);
                nextButton.setEnabled(true);
                backButton.setEnabled(true);
            }
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
    private javax.swing.ButtonGroup importButtonGroup;
    private javax.swing.JLabel infoLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JRadioButton kubikRadioButton;
    private javax.swing.JButton nextButton;
    private javax.swing.JLabel problemLabel;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JPanel resultsPanel;
    private javax.swing.JCheckBox validOperatorCheckBox;
    // End of variables declaration//GEN-END:variables
    
}
