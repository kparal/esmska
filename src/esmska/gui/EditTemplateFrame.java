/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import esmska.Context;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.SMSTemplates;
import esmska.data.SMSTemplate;
import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author Mizerovi16
 */
public class EditTemplateFrame extends javax.swing.JFrame {
     public static enum Events {
        INSERT_TEMPLATE;
    }
    
    private SMSTemplates templates = SMSTemplates.getInstance();   
    private TemplateModel templateModel = new TemplateModel();
    private static final Logger logger = Logger.getLogger(EditTemplateFrame.class.getName());
    private static final Log log = Log.getInstance();
    private Action removeTemplateAction = new RemoveTemplateAction();
//    private Action addTemplateAction = new AddTemplateAction();
    private Action insertTemplateAction = new InsertTemplateAction();
    private Action newTemplateAction = new NewTemplateAction();
    private Action editTemplateAction = new EditTemplateAction();
    private SMSTemplate selectedTemplate;
    
    
    private ValuedEventSupport<EditTemplateFrame.Events, SMSTemplate> valuedSupport = new ValuedEventSupport<EditTemplateFrame.Events, SMSTemplate>(this);
    public void addValuedListener(ValuedListener<EditTemplateFrame.Events, SMSTemplate> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<EditTemplateFrame.Events, SMSTemplate> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    
    /**
     * Creates new form EditTemplate
     */
    public EditTemplateFrame() {
        initComponents();
        //select first row       
        if (templateModel.getSize() > 0) {
            templateList.getSelectionModel().setSelectionInterval(0, 0);
        } 
        templates.addActionListener(new TemplateActionListener());
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
        saveTemplateButton = new javax.swing.JButton();
        deleteTemplateButton = new javax.swing.JButton();
        editTemplateButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        addTemplateButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString( "EditTemplateFrame.title")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(templateLabel, l10n.getString( "EditTemplateFrame.templateLabel.text")); // NOI18N

        templateList.setModel(templateModel);
        templateList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        templateList.getSelectionModel().addListSelectionListener(new TemplateListListener());
        jScrollPane1.setViewportView(templateList);

        saveTemplateButton.setAction(newTemplateAction);
        saveTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/add-22.png"))); // NOI18N
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

        editTemplateButton.setAction(editTemplateAction);
        editTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/edit-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(editTemplateButton, l10n.getString( "EditTemplateFrame.editTemplateButton.text")); // NOI18N
        editTemplateButton.setMaximumSize(new java.awt.Dimension(110, 31));
        editTemplateButton.setMinimumSize(new java.awt.Dimension(110, 31));
        editTemplateButton.setPreferredSize(new java.awt.Dimension(110, 31));

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

        addTemplateButton.setAction(insertTemplateAction);
        addTemplateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/template-to-SMS-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(addTemplateButton, l10n.getString( "EditTemplateFrame.addTemplateButton")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(templateLabel)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(editTemplateButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE)
                            .addComponent(deleteTemplateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(saveTemplateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(250, 250, 250)
                        .addComponent(closeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(5, 5, 5))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(templateLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(saveTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(59, 59, 59)
                        .addComponent(editTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(deleteTemplateButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addTemplateButton)
                    .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
//        SaveTemplaveExit();
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private class NewTemplateAction extends AbstractAction {
        private final String createOption = l10n.getString("Create");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, createOption);
        
        public NewTemplateAction() {
            super(l10n.getString("Create_templates"), Icons.get("about-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Create_new_template"));
            this.putValue(LARGE_ICON_KEY, Icons.get("about-22.png"));
            this.setEnabled(true);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            templateList.requestFocusInWindow(); //always transfer focus           
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());         
           
            //create Dialog           
            JLabel label = new JLabel(l10n.getString("EditTemplateFrame.create_new_template"));
            JTextArea area = new JTextArea();
            area.setEditable(true);
            area.setRows(2);
            area.setCaretPosition(0);
            panel.add(label, BorderLayout.PAGE_START);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);
            
            //confirm
            JOptionPane pane = new JOptionPane(panel, JOptionPane.DEFAULT_OPTION, 
                    JOptionPane.DEFAULT_OPTION, null, options, createOption);
            JDialog dialog = pane.createDialog(Context.mainFrame, null);
            dialog.setResizable(true);
            RuntimeUtils.setDocumentModalDialog(dialog);
            dialog.pack();
            dialog.setVisible(true);

            //return if should not create
            if (!createOption.equals(pane.getValue())) {
                return;
            }            
            //create
            String templateText = area.getText();
            if (templateText.equals("")) {
                return;
            }
            SMSTemplate p = new SMSTemplate(templateText);        
            templates.addTemplate(p);
            templateList.setSelectedValue(p, true);
            log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("EditTemplateFrame.addedTemplate"), templateText),
                    null, Icons.STATUS_INFO));
            
        }
    }
    
    private class EditTemplateAction extends AbstractAction {
        private final String editOption = l10n.getString("Edit");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, editOption);
        
        public EditTemplateAction() {
            super(l10n.getString("Edit_template"), Icons.get("about-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Edit_template"));
            this.putValue(LARGE_ICON_KEY, Icons.get("about-22.png"));
            this.setEnabled(true);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            templateList.requestFocusInWindow(); //always transfer focus 
            SMSTemplate template = (SMSTemplate)templateList.getSelectedValue();
            if  (template==null){
                return;
            }
            SMSTemplate editedTemplate = new SMSTemplate(template);
            //create Dialog 
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());                    
            JLabel label = new JLabel(l10n.getString("EditTemplateFrame.edit_template"));
            JTextArea area = new JTextArea();
            area.setEditable(true);
            area.setCaretPosition(0);
            area.setText(editedTemplate.toString());
            panel.add(label, BorderLayout.PAGE_START);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);          
            //confirm
            JOptionPane pane = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE, 
                    JOptionPane.DEFAULT_OPTION, null, options, editOption);
            JDialog dialog = pane.createDialog(Context.mainFrame, null);
            dialog.setResizable(true);
            RuntimeUtils.setDocumentModalDialog(dialog);
            dialog.pack();
            dialog.setVisible(true);

            //return if should not edit
            if (!editOption.equals(pane.getValue())) {
                return;
            }            
            //edit
            String tempalteEdit = area.getText();
            editedTemplate.setTemplate(tempalteEdit);
            if (editedTemplate.toString().equals("")) {
                return;
            }
            template.copyFrom(editedTemplate);                 
            templateList.setSelectedValue(template, true);
            log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("EditTemplateFrame.editedTemplate"), template),
                    null, Icons.STATUS_INFO));          
        }
    }
    
     private class InsertTemplateAction extends AbstractAction {
        public InsertTemplateAction() {
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedTemplate == null) {
                return;
            }
            logger.fine("Forwarding text from template: " + selectedTemplate);
            //fire event
            valuedSupport.fireEventOccured(EditTemplateFrame.Events.INSERT_TEMPLATE, selectedTemplate);
            
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
            
            SMSTemplate t = (SMSTemplate)templateList.getSelectedValue();
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
    
     /** Listener for template changes */
    private class TemplateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            templateList.getSelectionModel().clearSelection();
        }
    }
    
    /** Listener for changes in template list */
    private class TemplateListListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if  (e.getValueIsAdjusting()) {
                return;
            }
            int index = templateList.getSelectedIndex();
            boolean selected = (index >= 0);
            insertTemplateAction.setEnabled(selected);
            editTemplateAction.setEnabled(selected);
            removeTemplateAction.setEnabled(selected);

            SMSTemplate temp = null;
            if (selected) {
                index = templateList.getSelectedIndex();
                temp = templates.getTemplate(index);
            }            
            selectedTemplate = temp;
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
                        case SMSTemplates.ACTION_ADD_TEMPLATE:
                        case SMSTemplates.ACTION_CHANGE_TEMPLATE:
                            fireContentsChanged(TemplateModel.this, 0, getSize());
                            break;
                        case SMSTemplates.ACTION_REMOVE_TEMPLATE:
                        case SMSTemplates.ACTION_CLEAR_TEMPLATES:
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
    private javax.swing.JButton addTemplateButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton deleteTemplateButton;
    private javax.swing.JButton editTemplateButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton saveTemplateButton;
    private javax.swing.JLabel templateLabel;
    private javax.swing.JList templateList;
    // End of variables declaration//GEN-END:variables
}
