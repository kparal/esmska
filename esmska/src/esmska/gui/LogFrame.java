/*
 * LogFrame.java
 *
 * Created on 25. leden 2008, 21:43
 */

package esmska.gui;

import esmska.data.Icons;
import esmska.data.Log;
import esmska.utils.L10N;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;

/** Display log records
 *
 * @author  ripper
 */
public class LogFrame extends javax.swing.JFrame {
    private static final String RES = "/esmska/resources/";
    private static final Logger logger = Logger.getLogger(LogFrame.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    private Log log = new Log();
    private DefaultListModel logModel = new DefaultListModel();
    

    /** Creates new form LogFrame */
    public LogFrame() {
        initComponents();
        closeButton.requestFocusInWindow();
        this.getRootPane().setDefaultButton(closeButton);
        
        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonActionPerformed(e);
            }
        });
    }
    
    /** set log to display */
    public void setLog(Log log) {
        this.log = log;
        updateList(false);
        log.addActionListener(new LogListener());
    }
    
    /** update log list
     * @param onlyLastRecord if true just append last record, if false reload all
     */
    private void updateList(boolean onlyLastRecord) {
        Log.Record lastRecord = null;
        if (onlyLastRecord) {
            if (log.getRecords().size() > 0) {
                lastRecord = log.getRecords().get(log.getRecords().size()-1);
                logModel.addElement(lastRecord);
            }
        } else {
            logModel.clear();
            for (Log.Record record : log.getRecords()) {
                logModel.addElement(record);
                lastRecord = record;
            }
        }
        logList.setSelectedValue(lastRecord, true);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        logList = new javax.swing.JList();
        closeButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        copyButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("LogFrame.title")); // NOI18N
        setIconImage(new ImageIcon(getClass().getResource(RES + "log-48.png")).getImage());

        logList.setModel(logModel);
        logList.setCellRenderer(new LogRenderer());
        jScrollPane1.setViewportView(logList);

        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(closeButton, l10n.getString("Close_")); // NOI18N
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/clear-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(clearButton, l10n.getString("LogFrame.clearButton.text")); // NOI18N
        clearButton.setToolTipText(l10n.getString("LogFrame.clearButton.toolTipText")); // NOI18N
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        copyButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/copy-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(copyButton, l10n.getString("LogFrame.copyButton.text")); // NOI18N
        copyButton.setToolTipText(l10n.getString("LogFrame.copyButton.toolTipText")); // NOI18N
        copyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(clearButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(copyButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 133, Short.MAX_VALUE)
                        .addComponent(closeButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(closeButton)
                        .addComponent(clearButton))
                    .addComponent(copyButton))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {clearButton, closeButton, copyButton});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        log.clearRecords();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void copyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyButtonActionPerformed
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringBuilder builder = new StringBuilder();
            for (Log.Record record : log.getRecords()) {
                builder.append("[");
                builder.append(timeFormat.format(record.getTime()));
                builder.append("] ");
                builder.append(record.getMessage().replaceAll("<.*?>",""));
                builder.append("\n");
            }
            Transferable text = new StringSelection(builder.toString());
            clipboard.setContents(text, null);
        } catch (IllegalStateException ex) {
            logger.log(Level.WARNING, "System clipboard not available", ex);
        }
    }//GEN-LAST:event_copyButtonActionPerformed
    
    /** update log frame on log change*/
    private class LogListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            switch (e.getID()) {
                case Log.ACTION_ADD_RECORD:
                    updateList(true);
                    break;
                case Log.ACTION_CLEAR_RECORDS:
                    updateList(false);
                    break;
            }
        }
    }
    
    /** Renderer for records in log list */
    private class LogRenderer implements ListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value, 
                    index, isSelected, cellHasFocus);
            Log.Record record = (Log.Record)value;
            //display message and time
            String text = "<html>[" + timeFormat.format(record.getTime()) + "] " +
                    record.getMessage() + "</html>";
            ((JLabel)c).setText(text);
            //add record icon
            ((JLabel)c).setIcon(record.getIcon() != null ? record.getIcon() :
                Icons.STATUS_BLANK);
            return c;
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton clearButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton copyButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList logList;
    // End of variables declaration//GEN-END:variables
    
}
