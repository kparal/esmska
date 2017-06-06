/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import esmska.Context;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Temp;
import esmska.data.Template;
import static esmska.gui.GatewayMessage.l10n;
import esmska.utils.RuntimeUtils;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import static javax.swing.Action.LARGE_ICON_KEY;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *
 * @author Mizerovi16
 */
public class EditTemplateFrame extends javax.swing.JFrame {
    
    private Template templates = Template.getInstance();   
    private TemplateModel templateModel = new TemplateModel();
    private static final Logger logger = Logger.getLogger(EditTemplateFrame.class.getName());
    private static final Log log = Log.getInstance();
    private Action removeTemplateAction = new RemoveTemplateAction();
    private Action addTemplateAction = new AddTemplateAction();
    
    
    /**
     * Creates new form EditTemplate
     */
    public EditTemplateFrame() {
        initComponents();
        //select first row       
        if (templateModel.getSize() > 0) {
            templateList.getSelectionModel().setSelectionInterval(0, 0);
        }       
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        templateLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        templateList = new javax.swing.JList();
        editTemplateTextField = new javax.swing.JTextField();
        saveTemplateButton = new javax.swing.JButton();
        deleteTemplateButton = new javax.swing.JButton();
        editTemplateButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        new_editLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        org.openide.awt.Mnemonics.setLocalizedText(templateLabel, l10n.getString( "EditTemplateFrame.templateLabel.text")); // NOI18N

        templateList.setModel(templateModel);
        templateList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(templateList);

        editTemplateTextField.setMinimumSize(new java.awt.Dimension(6, 29));
        editTemplateTextField.setPreferredSize(new java.awt.Dimension(6, 29));

        saveTemplateButton.setAction(addTemplateAction);
        saveTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/disk-16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(saveTemplateButton, l10n.getString( "EditTemplateFrame.saveTemplateButton.text")); // NOI18N
        saveTemplateButton.setMaximumSize(new java.awt.Dimension(110, 31));
        saveTemplateButton.setMinimumSize(new java.awt.Dimension(110, 31));
        saveTemplateButton.setPreferredSize(new java.awt.Dimension(110, 31));

        deleteTemplateButton.setAction(removeTemplateAction );
        deleteTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/delete-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(deleteTemplateButton, l10n.getString( "EditTemplateFrame.deleteTemplateButton.text")); // NOI18N
        deleteTemplateButton.setMaximumSize(new java.awt.Dimension(110, 31));
        deleteTemplateButton.setMinimumSize(new java.awt.Dimension(110, 31));
        deleteTemplateButton.setPreferredSize(new java.awt.Dimension(110, 31));

        editTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/edit-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(editTemplateButton, l10n.getString( "EditTemplateFrame.editTemplateButton.text")); // NOI18N
        editTemplateButton.setMaximumSize(new java.awt.Dimension(110, 31));
        editTemplateButton.setMinimumSize(new java.awt.Dimension(110, 31));
        editTemplateButton.setPreferredSize(new java.awt.Dimension(110, 31));
        editTemplateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editTemplateButtonActionPerformed(evt);
            }
        });

        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(closeButton, l10n.getString( "Close_")); // NOI18N
        closeButton.setMaximumSize(new java.awt.Dimension(110, 31));
        closeButton.setMinimumSize(new java.awt.Dimension(110, 31));
        closeButton.setPreferredSize(new java.awt.Dimension(110, 31));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        new_editLabel.setLabelFor(editTemplateTextField);
        org.openide.awt.Mnemonics.setLocalizedText(new_editLabel, l10n.getString( "EditTemplateFrame.new_editLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(new_editLabel)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(editTemplateTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(templateLabel)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(editTemplateButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                            .addComponent(closeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(deleteTemplateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(saveTemplateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(5, 5, 5))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(templateLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(deleteTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addComponent(new_editLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(editTemplateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(saveTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void editTemplateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editTemplateButtonActionPerformed
        Object o = templateList.getSelectedValue();
        if (o == null) {
            return;
        }
        editTemplateTextField.setText(o.toString());
        Temp t = (Temp)o;
        templates.removeTemplate(t);
        editTemplateButton.setEnabled(false);
    }//GEN-LAST:event_editTemplateButtonActionPerformed

    /** Add template to template list */
    private class AddTemplateAction extends AbstractAction {        
        public AddTemplateAction() {
           
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            editTemplateButton.setEnabled(true);
            templateList.requestFocusInWindow(); //always transfer focus
            String t = editTemplateTextField.getText();
            if (t.equals("")) {
                return;
            }
            Temp p = new Temp(t);        
            templates.addTemplate(p);
            templateList.setSelectedValue(p, true);
            log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("EditTemplateFrame.addedTemplate"), t),
                    null, Icons.STATUS_INFO));
            editTemplateTextField.setText("");
        }
        
    }
    
     /** Remove template from template list */
    private class RemoveTemplateAction extends AbstractAction {
        private final String deleteOption = l10n.getString("Delete");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, deleteOption);
        
        public RemoveTemplateAction() {
            super(l10n.getString("Delete_templates"), Icons.get("delete-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Delete_selected_contacts"));
            this.putValue(LARGE_ICON_KEY, Icons.get("delete-22.png"));
            this.setEnabled(true);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            templateList.requestFocusInWindow(); //always transfer focus           
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            
            Temp t = (Temp)templateList.getSelectedValue();
            if  (t==null){
                 return;
            }
            //create warning           
            JLabel label = new JLabel(l10n.getString("EditTemplateFrame.remove_following_template"));
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setRows(1);
            area.append(t.toString());
            area.setCaretPosition(0);
            panel.add(label, BorderLayout.PAGE_START);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);
            
            //confirm
            JOptionPane pane = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE, 
                    JOptionPane.DEFAULT_OPTION, null, options, deleteOption);
            JDialog dialog = pane.createDialog(Context.mainFrame, null);
            dialog.setResizable(true);
            RuntimeUtils.setDocumentModalDialog(dialog);
            dialog.pack();
            dialog.setVisible(true);

            //return if should not delete
            if (!deleteOption.equals(pane.getValue())) {
                return;
            }            
            //delete
            templates.removeTemplate(t);
            String message;           
                message = MessageFormat.format(l10n.getString("EditTemplateFrame.removeTemplate"),
                        t.getTemplate());            
            log.addRecord(new Log.Record(message, null, Icons.STATUS_INFO));
        }
    }
    
    private class TemplateModel extends AbstractListModel {
        private int oldSize = getSize();
        public TemplateModel() {    
            //listen for changes in templates and fire events accordingly
            templates.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (e.getID()) {
                        case Template.ACTION_ADD_TEMPLATE:
                        case Template.ACTION_CHANGE_TEMPLATE:
                            fireContentsChanged(TemplateModel.this, 0, getSize());
                            break;
                        case Template.ACTION_REMOVE_TEMPLATE:
                        case Template.ACTION_CLEAR_TEMPLATES:
                            fireIntervalRemoved(TemplateModel.this, 0, oldSize);
                            break;
                        default:
                            logger.warning("Unknown action event type");
                            assert false : "Unknown action event type";
                    }
                    oldSize = getSize();
                    }
                });
            }
               
        @Override
        public int getSize() {
            return templates.getTemplates().size();
        }
        @Override
        public Object getElementAt(int index) {
            return templates.getTemplate(index);           
        }        
    }   

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton closeButton;
    private javax.swing.JButton deleteTemplateButton;
    private javax.swing.JButton editTemplateButton;
    private javax.swing.JTextField editTemplateTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel new_editLabel;
    private javax.swing.JButton saveTemplateButton;
    private javax.swing.JLabel templateLabel;
    private javax.swing.JList templateList;
    // End of variables declaration//GEN-END:variables
}
