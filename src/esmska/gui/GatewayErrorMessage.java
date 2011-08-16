package esmska.gui;

import esmska.data.Config;
import esmska.data.Gateway;
import esmska.data.Gateways;
import esmska.data.Icons;
import esmska.data.Keyring;
import esmska.data.Links;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.data.Tuple;
import esmska.gui.GatewayMessageFrame.TaskPane;
import esmska.transfer.GatewayExecutor.Problem;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import javax.swing.Box.Filler;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.openide.awt.Mnemonics;

/** Error message from gateway displayed to a user
 *
 * @author ripper
 */
public class GatewayErrorMessage extends GatewayMessage {
    private static final Config config = Config.getInstance();
    private static final Gateways gateways = Gateways.getInstance();
    
    /** Get short description message of the problem mentioned in provided SMS.*/
    public String getDescription(SMS sms) {
        Validate.notNull(sms.getProblem());
        
        Problem problem = sms.getProblem().get1();
        String param = sms.getProblem().get2();
        Gateway gw = gateways.get(sms.getGateway());
        String website = gw != null ? gw.getWebsite() : null;
        Tuple<String, String> key = Keyring.getInstance().getKey(sms.getGateway());
        String login = key != null ? key.get1() : "";
        String password = key != null ? key.get2() : "";
        
        switch (problem) {
            case CUSTOM_MESSAGE:
                return l10n.getString("GatewayProblem.CUSTOM_MESSAGE");
            case FIX_IN_PROGRESS:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.FIX_IN_PROGRESS"),
                        website);
            case GATEWAY_MESSAGE:
                return l10n.getString("GatewayProblem.GATEWAY_MESSAGE");
            case INTERNAL_MESSAGE:
                return param;
            case LIMIT_REACHED:
                return l10n.getString("GatewayProblem.LIMIT_REACHED");
            case LONG_TEXT:
                return l10n.getString("GatewayProblem.LONG_TEXT");
            case NO_CREDIT:
                return l10n.getString("GatewayProblem.NO_CREDIT");
            case NO_REASON:
                return l10n.getString("GatewayProblem.NO_REASON");
            case SIGNATURE_NEEDED:
                return MessageFormat.format(l10n.getString("GatewayProblem.SIGNATURE_NEEDED"),
                        Links.CONFIG_GATEWAYS);
            case UNKNOWN:
                return l10n.getString("GatewayProblem.UNKNOWN");
            case UNUSABLE:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.UNUSABLE"), website);
            case WRONG_AUTH:
                assert login != null;
                assert password != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_AUTH"),
                        login, password.replaceAll(".", "*"));
            case WRONG_CODE:
                return l10n.getString("GatewayProblem.WRONG_CODE");
            case WRONG_NUMBER:
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_NUMBER"),
                        sms.getNumber());
            case WRONG_SIGNATURE:
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_SIGNATURE"),
                        sms.getSenderNumber(), sms.getSenderName());
            default:
                throw new IllegalStateException("Uknown problem type: " + problem);
        }
    }
    
    /** Get the third-party problem description stored in the provided SMS.
     * Only some problems support third-party descriptions, for other this returns null.
     */
    public String getThirdPartyDescription(SMS sms) {
        Validate.notNull(sms.getProblem());
        
        Problem problem = sms.getProblem().get1();
        String param = sms.getProblem().get2();
        
        switch (problem) {
            case CUSTOM_MESSAGE:
            case GATEWAY_MESSAGE:
                return param;
            default:
                return null;
        }
    }
    
    /** Get help message related to the problem stored in the provided SMS. */
    public String getHelp(SMS sms) {
        Validate.notNull(sms.getProblem());
        
        Problem problem = sms.getProblem().get1();
        String param = sms.getProblem().get2();
        Gateway gw = gateways.get(sms.getGateway());
        String website = gw != null ? gw.getWebsite() : null;
        
        switch (problem) {
            case CUSTOM_MESSAGE:
                return l10n.getString("GatewayProblem.CUSTOM_MESSAGE.help");
            case FIX_IN_PROGRESS:
            case UNUSABLE:
                assert param != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.FIX_IN_PROGRESS.help"),
                        param);
            case GATEWAY_MESSAGE:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.GATEWAY_MESSAGE.help"),
                        website);
            case INTERNAL_MESSAGE:
                return param;
            case LIMIT_REACHED:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.LIMIT_REACHED.help"),
                        website);
            case LONG_TEXT:
                return l10n.getString("GatewayProblem.LONG_TEXT.help");
            case NO_CREDIT:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.NO_CREDIT.help"),
                        website);
            case NO_REASON:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.NO_REASON.help"),
                        website);
            case SIGNATURE_NEEDED:
                return null;
            case UNKNOWN:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.UNKNOWN.help"),
                        website, Links.ISSUES);
            case WRONG_AUTH:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_AUTH.help"),
                        website, Links.CONFIG_GATEWAYS);
            case WRONG_CODE:
                return null;
            case WRONG_NUMBER:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_NUMBER.help"),
                        website);
            case WRONG_SIGNATURE:
                assert website != null;
                return MessageFormat.format(l10n.getString("GatewayProblem.WRONG_SIGNATURE.help"),
                        Links.CONFIG_GATEWAYS, website);
            default:
                throw new IllegalStateException("Uknown problem type: " + problem);
        }
    }
    
    /** Return whether show or hide the retryButton. This button is shown only
     for some problems.
     * @param sms sms with stored problem
     */
    private boolean showRetryButton(SMS sms) {
        Validate.notNull(sms.getProblem());
        
        Problem problem = sms.getProblem().get1();
        switch (problem) {
            case CUSTOM_MESSAGE:
            case GATEWAY_MESSAGE:
            case INTERNAL_MESSAGE:
            case LIMIT_REACHED:
            case NO_REASON:
            case UNKNOWN:
            case WRONG_CODE:
                return true;
            default:
                return false;
        }
    }
    
    /** Creates new form GatewayErrorMessage */
    public GatewayErrorMessage() {
        initComponents();

        //if not Substance LaF, add clipboard popup menu to text components
        if (!config.getLookAndFeel().equals(ThemeManager.LAF.SUBSTANCE)) {
            ClipboardPopupMenu.register(smsTextArea);
            ClipboardPopupMenu.register(thirdPartyTextPane);
        }
    }

    /** Initialize this message to show an SMS error
     * @param sms sms that failed
     */
    public TaskPane showErrorMsg(SMS sms) {
        Validate.notNull(sms);
        
        String description = getDescription(sms);
        String thirdPartyDescription = getThirdPartyDescription(sms);
        String help = getHelp(sms);
        String recipient = extractRecipient(sms);
        String title = MessageFormat.format(l10n.getString("GatewayErrorMessage.smsFailed"), recipient);
        Icon icon = Icons.STATUS_WARNING;

        descriptionLabel.setText("<html>" + description + "</html>");
        if (StringUtils.isEmpty(thirdPartyDescription)) {
            thirdPartyScrollPane.setVisible(false);
        } else {
            thirdPartyScrollPane.setVisible(true);
            thirdPartyTextPane.setText("<html>" + thirdPartyDescription + "</html>");
        }
        helpTextLabel.setText("<html>" + help + "</html>");
        helpLabel.setVisible(StringUtils.isNotEmpty(help));
        smsTextArea.setText(sms.getText());
        retryButton.setVisible(showRetryButton(sms));
        
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

        okButton = new JButton();
        smsTextScrollPane = new JScrollPane();
        smsTextArea = new JTextArea();
        smsTextLabel = new JLabel();
        descriptionLabel = new JHtmlLabel();
        thirdPartyScrollPane = new JScrollPane();
        thirdPartyTextPane = new JTextPane();
        helpTextLabel = new JHtmlLabel();
        helpLabel = new JLabel();
        filler1 = new Filler(new Dimension(0, 1), new Dimension(0, 1), new Dimension(0, 1));
        filler2 = new Filler(new Dimension(0, 1), new Dimension(0, 1), new Dimension(0, 1));
        retryButton = new JButton();
        Mnemonics.setLocalizedText(okButton, l10n.getString("OK_"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        smsTextScrollPane.setVisible(false);

        smsTextArea.setColumns(20);
        smsTextArea.setLineWrap(true);
        smsTextArea.setRows(5);
        smsTextArea.setWrapStyleWord(true);
        smsTextScrollPane.setViewportView(smsTextArea);

        smsTextLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/expand-off-12.png"))); // NOI18N
        Mnemonics.setLocalizedText(smsTextLabel, l10n.getString("GatewayErrorMessage.smsTextLabel.text"));
        smsTextLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        smsTextLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                smsTextLabelMouseClicked(evt);
            }
        });
        Mnemonics.setLocalizedText(descriptionLabel, "<<Failure description>>\t");
        descriptionLabel.setVerticalAlignment(SwingConstants.TOP);

        thirdPartyTextPane.setContentType("text/html; charset=UTF-8"); // NOI18N
        thirdPartyTextPane.setEditable(false);
        thirdPartyTextPane.setFocusable(false);
        thirdPartyScrollPane.setViewportView(thirdPartyTextPane);

        Mnemonics.setLocalizedText(helpTextLabel, "<<Help>>"); // NOI18N
        helpTextLabel.setVisible(false);

        helpLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/expand-off-12.png"))); // NOI18N
        Mnemonics.setLocalizedText(helpLabel, l10n.getString("GatewayErrorMessage.helpLabel.text"));
        helpLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        helpLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                helpLabelMouseClicked(evt);
            }
        });
        Mnemonics.setLocalizedText(retryButton, l10n.getString("GatewayErrorMessage.retryButton.text"));
        retryButton.setToolTipText(l10n.getString("GatewayErrorMessage.retryButton.toolTipText")); // NOI18N
        retryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                retryButtonActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(descriptionLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addComponent(filler1, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addComponent(smsTextScrollPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(retryButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(okButton))
                    .addComponent(smsTextLabel, Alignment.LEADING)
                    .addComponent(helpTextLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addComponent(helpLabel, Alignment.LEADING)
                    .addComponent(filler2, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE)
                    .addComponent(thirdPartyScrollPane, GroupLayout.DEFAULT_SIZE, 238, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(descriptionLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(thirdPartyScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(filler2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(helpLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(helpTextLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(filler1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(smsTextLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(smsTextScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(retryButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        actionSupport.fireActionPerformed(CLOSE_ME, null);
    }//GEN-LAST:event_okButtonActionPerformed

    private void smsTextLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_smsTextLabelMouseClicked
        smsTextScrollPane.setVisible(!smsTextScrollPane.isVisible());
        if (smsTextScrollPane.isVisible()) {
            smsTextLabel.setIcon(Icons.get("expand-on-12.png"));
        } else {
            smsTextLabel.setIcon(Icons.get("expand-off-12.png"));
        }
        this.revalidate();
    }//GEN-LAST:event_smsTextLabelMouseClicked

    private void helpLabelMouseClicked(MouseEvent evt) {//GEN-FIRST:event_helpLabelMouseClicked
        helpTextLabel.setVisible(!helpTextLabel.isVisible());
        if (helpTextLabel.isVisible()) {
            helpLabel.setIcon(Icons.get("expand-on-12.png"));
        } else {
            helpLabel.setIcon(Icons.get("expand-off-12.png"));
        }
        this.revalidate();
    }//GEN-LAST:event_helpLabelMouseClicked

    private void retryButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_retryButtonActionPerformed
        Queue.getInstance().setPaused(false);
        actionSupport.fireActionPerformed(CLOSE_ME, null);
    }//GEN-LAST:event_retryButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JHtmlLabel descriptionLabel;
    private Filler filler1;
    private Filler filler2;
    private JLabel helpLabel;
    private JLabel helpTextLabel;
    private JButton okButton;
    private JButton retryButton;
    private JTextArea smsTextArea;
    private JLabel smsTextLabel;
    private JScrollPane smsTextScrollPane;
    private JScrollPane thirdPartyScrollPane;
    private JTextPane thirdPartyTextPane;
    // End of variables declaration//GEN-END:variables
    
}
