
package esmska.gui;

import esmska.data.CountryPrefix;
import esmska.data.SMS;
import esmska.data.event.ActionEventSupport;
import esmska.gui.GatewayMessageFrame.TaskPane;
import esmska.utils.L10N;
import esmska.utils.RuntimeUtils;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.apache.commons.lang.StringUtils;

/** Message from gateway displayed to a user
 *
 * @author ripper
 */
public abstract class GatewayMessage extends JPanel {
    /** The signal to close this message */
    public static final int CLOSE_ME = 0;

    protected static final ResourceBundle l10n = L10N.l10nBundle;
    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    protected ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** Focus the best component in this panel */
    public abstract void setBestFocus();

    /** Cancel this message, do what's most appropriate */
    public abstract void cancel();

    /** Wrap this message as a TaskPane */
    protected TaskPane wrapAsTaskPane(GatewayMessage gm, String title, Icon icon) {
        TaskPane pane = new TaskPane(gm);
        pane.setTitle(title);
        pane.setIcon(icon);
        pane.setScrollOnExpand(true);
        // There's some bug in JXTaskPane causing cropping of JLabels with
        // html on Linux. Disabling animation helps. Let's do it on all systems
        // just to be sure.
        pane.setAnimated(false);
        pane.setCollapsed(true);
        return pane;
    }

    /** Extract recipient (name, number, gateway) from SMS and put it to an
     * unified format, to be used in the task pane.
     */
    protected String extractRecipient(SMS sms) {
        String number = CountryPrefix.stripCountryPrefix(sms.getNumber(), true);
        String recipient = null;
        if (StringUtils.isNotEmpty(sms.getName())) {
            recipient = MessageFormat.format("{0} ({1}, {2})", sms.getName(),
                    number, sms.getGateway());
        } else {
            recipient = MessageFormat.format("{0} ({1})", number, sms.getGateway());
        }
        return recipient;
    }
}
