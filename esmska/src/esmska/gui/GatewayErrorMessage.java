
/*
 * GatewayErrorMessage.java
 *
 * Created on 8.1.2011, 16:01:32
 */

package esmska.gui;

import esmska.data.Icons;
import esmska.data.SMS;
import esmska.gui.GatewayMessageDialog.TaskPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import org.openide.awt.Mnemonics;

/** Error message from gateway displayed to a user
 *
 * @author ripper
 */
public class GatewayErrorMessage extends GatewayMessage {

    /** Creates new form GatewayErrorMessage */
    public GatewayErrorMessage() {
        initComponents();
    }

    /** Initialize this message to show an SMS error
     * @param sms sms that failed
     */
    public TaskPane showErrorMsg(SMS sms) {
        String cause = (sms.getErrMsg() != null ? sms.getErrMsg().trim() : "");
        String recipient = extractRecipient(sms);
        String title = MessageFormat.format(l10n.getString("GatewayErrorMessage.smsFailed"), recipient);
        Icon icon = Icons.STATUS_WARNING;

        msgLabel.setText("<html>" + cause + "</html>");

        return wrapAsTaskPane(this, title, icon);
    }

    /** Focus the best component in this panel */
    @Override
    public void setBestFocus() {
        okButton.requestFocusInWindow();
    }

    /** Cancel this message, do what's most appropriate */
    @Override
    public void cancel() {
        okButton.doClick(0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        msgLabel = new JHtmlLabel();
        okButton = new JButton();
        Mnemonics.setLocalizedText(msgLabel, "<<Some text>>\t");
        msgLabel.setVerticalAlignment(SwingConstants.TOP);
        Mnemonics.setLocalizedText(okButton, l10n.getString("OK_"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(msgLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(okButton))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(msgLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(okButton)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        actionSupport.fireActionPerformed(CLOSE_ME, null);
    }//GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JHtmlLabel msgLabel;
    private JButton okButton;
    // End of variables declaration//GEN-END:variables
    
}
