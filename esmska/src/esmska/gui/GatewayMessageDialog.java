/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ImageCodeDialog.java
 *
 * Created on 15.5.2009, 17:03:16
 */

package esmska.gui;

import esmska.Context;
import esmska.data.SMS;
import esmska.gui.GatewayMessage.TaskPane;
import esmska.utils.L10N;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import org.jdesktop.swingx.JXTaskPaneContainer;

/** Dialog for various alerts from gateways
 *
 * @author ripper
 */
public class GatewayMessageDialog extends JDialog {
    private static GatewayMessageDialog instance;
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String RES = "/esmska/resources/";
    private static final Logger logger = Logger.getLogger(GatewayMessageDialog.class.getName());

    /** index of the last TaskPane removed from the taskContainer */
    private int lastPaneRemovedIndex = 0;

    /** Creates new form GatewayMessageDialog */
    public GatewayMessageDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        instance = this;
        
        initComponents();
        setLocationRelativeTo(parent);

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(new ImageIcon(getClass().getResource(RES + "message-16.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "message-22.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "message-32.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "message-48.png")).getImage());
        setIconImages(images);

        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                formWindowClosing(null);
                GatewayMessageDialog.this.setVisible(false);
            }
        });

        // update container on change
        taskContainer.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                containersChanged();
            }
            @Override
            public void componentRemoved(ContainerEvent e) {
                containersChanged();
            }
            private void containersChanged() {
                expandNextPane();
                updateVisibility();
            }
        });
    }

    /* Get existing instance of GatewayMessageDialog or create a new instance */
    public static GatewayMessageDialog getInstance() {
        if (instance == null) {
            instance = new GatewayMessageDialog(Context.mainFrame, false);
        }
        return instance;
    }

    /** Add a message about SMS sending error
     * @param sms sms that failed
     */
    public void addErrorMsg(SMS sms) {
        logger.log(Level.FINER, "Adding error message: {0}", sms);
        GatewayMessage gm = new GatewayMessage();
        final TaskPane taskPane = gm.showErrorMsg(sms);
        gm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // remove the TaskPane when it wants to be removed
                if (e.getID() == GatewayMessage.CLOSE_ME) {
                    // remember the index of the lastly removed TaskPane
                    lastPaneRemovedIndex = 0;
                    Component[] comps = taskContainer.getComponents();
                    for (int i = 0; i < comps.length; i++) {
                        if (comps[i] == taskPane) {
                            lastPaneRemovedIndex = i;
                        }
                    }
                    taskContainer.remove(taskPane);
                }
            }
        });
        taskContainer.add(taskPane);
    }

    /** Expand the next TaskPane.
     * Call this after some TaskPane has been removed.
     */
    private void expandNextPane() {
        Component[] comps = taskContainer.getComponents();
        if (comps.length <= 0) {
            return;
        }

        // expand the TaskPane following the most recently removed one
        // otherwise the first one
        TaskPane taskPane = (TaskPane) comps[0];
        if (lastPaneRemovedIndex < comps.length) {
            taskPane = (TaskPane) comps[lastPaneRemovedIndex];
        }

        taskPane.setCollapsed(false);
        // focus components in the message
        taskPane.getGatewayMessage().setBestFocus();
    }

    /** Update visibility of this dialog - show when having some TaskPanes,
     hide otherwise.*/
    private void updateVisibility() {
        boolean wasVisible = isVisible();
        setVisible(taskContainer.getComponentCount() > 0);
        if (!wasVisible && isVisible()) {
            logger.finer("Showing GatewayMessageDialog");
            toFront();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new JScrollPane();
        taskContainer = new JXTaskPaneContainer();

        setTitle(l10n.getString("GatewayMessageDialog.title")); // NOI18N
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jScrollPane1.setBorder(null);

        taskContainer.setOpaque(false);
        jScrollPane1.setViewportView(taskContainer);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 468, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // cancel all TaskPanes when dialog closed
        logger.finer("Hiding GatewayMessageDialog, cancelling all requests");
        for (Component comp : taskContainer.getComponents()) {
            TaskPane taskPane = (TaskPane) comp;
            taskPane.getGatewayMessage().cancel();
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JScrollPane jScrollPane1;
    private JXTaskPaneContainer taskContainer;
    // End of variables declaration//GEN-END:variables

}
