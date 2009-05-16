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

import esmska.data.Icons;
import esmska.data.Operator;
import esmska.data.Operators;
import esmska.data.SMS;
import esmska.transfer.ImageCodeResolver;
import esmska.utils.L10N;
import esmska.utils.RuntimeUtils;
import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jvnet.substance.api.renderers.SubstanceDefaultListCellRenderer;
import org.openide.awt.Mnemonics;

/** Dialog for showing security images to be transcribed to textual form
 *
 * @author ripper
 */
public class ImageCodeDialog extends JDialog implements ImageCodeResolver {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String RES = "/esmska/resources/";
    private static final Logger logger = Logger.getLogger(ImageCodeDialog.class.getName());
    private DefaultListModel queueModel = new DefaultListModel();
    /** list of current sms and their queues in which the current processes are waiting for result */
    private HashMap<SMS, ArrayBlockingQueue<String>> smsQueue = new HashMap<SMS, ArrayBlockingQueue<String>>();
    private ImageIcon defaultIcon = new ImageIcon(ImageCodeDialog.class.getResource(RES + "missing-image-128.png"));

    /** Creates new form ImageCodeDialog */
    public ImageCodeDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        RuntimeUtils.setDocumentModalDialog(this);

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(new ImageIcon(getClass().getResource(RES + "keyring-16.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "keyring-22.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "keyring-32.png")).getImage());
        images.add(new ImageIcon(getClass().getResource(RES + "keyring-48.png")).getImage());
        setIconImages(images);

        //close on Ctrl+W
        String command = "close";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(
                KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageCodeDialog.this.setVisible(false);
            }
        });

        //cancel current image on Escape
        command = "cancel";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButton.doClick(0);
            }
        });

        //set components
        queueScrollPane.setVisible(false);
        queueList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /** This method <b>must not</b> be called from EDT. For description see
     * {@link ImageCodeResolver#resolveImageCode(esmska.data.SMS)}
     */
    @Override
    public boolean resolveImageCode(final SMS sms) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Called from EDT");
        }
        Validate.notNull(sms);
        
        ImageIcon imageIcon = sms.getImage();
        if (imageIcon == null) {
            sms.setImageCode(null);
            return false;
        }

        //will use blocking queue to wait for result in sending thread
        final ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<String>(1);

        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    smsQueue.put(sms, blockingQueue);
                    queueModel.addElement(sms);
                    if (queueModel.getSize() > 1 && !queueScrollPane.isVisible()) {
                        //show list of other waiting sms, if more than one
                        queueScrollPane.setVisible(true);
                        validate();
                    }
                    if (queueList.getSelectedValue() == null) {
                        //if first sms in the queue
                        queueList.setSelectedIndex(0);
                    }
                    ImageCodeDialog thiz = ImageCodeDialog.this;
                    if (!thiz.isVisible()) {
                        thiz.setLocationRelativeTo(thiz.getParent());
                        thiz.setVisible(true);
                    }
                }
            });

            //wait for result blocking current thread
            String result = blockingQueue.take();

            sms.setImageCode(result);
            return StringUtils.isNotEmpty(result);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Resolving image code interrupted", ex);
            return false;
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

        queueScrollPane = new JScrollPane();
        queueList = new JList();
        jPanel1 = new JPanel();
        imageLabel = new JLabel();
        okButton = new JButton();
        cancelButton = new JButton();
        codeTextField = new JTextField();
        jLabel2 = new JLabel();
        introLabel = new JLabel();

        addWindowListener(new WindowAdapter() {
            public void windowDeactivated(WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });

        queueList.setModel(queueModel);
        queueList.setCellRenderer(new QueueRenderer());
        queueList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                queueListValueChanged(evt);
            }
        });
        queueScrollPane.setViewportView(queueList);

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setIcon(defaultIcon);

        Mnemonics.setLocalizedText(okButton, l10n.getString("OK_"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        Mnemonics.setLocalizedText(cancelButton, l10n.getString("Cancel_"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        codeTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                codeTextFieldKeyPressed(evt);
            }
        });

        jLabel2.setLabelFor(codeTextField);

        Mnemonics.setLocalizedText(jLabel2, l10n.getString("ImageCodeDialog.jLabel2.text"));
        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);

        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(imageLabel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(cancelButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(okButton))
                    .addGroup(Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(codeTextField, GroupLayout.DEFAULT_SIZE, 92, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(imageLabel, GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(codeTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        introLabel.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/keyring-48.png"))); // NOI18N
        Mnemonics.setLocalizedText(introLabel, l10n.getString("ImageCodeDialog.introLabel.text"));
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(introLabel, GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(queueScrollPane, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(introLabel)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(queueScrollPane, GroupLayout.DEFAULT_SIZE, 253, Short.MAX_VALUE)
                    .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void queueListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_queueListValueChanged
        if (evt != null && evt.getValueIsAdjusting()) {
            return;
        }

        codeTextField.setText(null);
        codeTextField.requestFocusInWindow();

        SMS sms = (SMS) queueList.getSelectedValue();
        if (sms == null) {
            //deselected, some image probably just recognized
            if (queueModel.getSize() > 0) {
                //select first in the list
                queueList.setSelectedIndex(0);
            } else {
                //no other sms to show, close dialog
                imageLabel.setIcon(defaultIcon);
                setVisible(false);
            }
        } else {
            //display image from selected sms
            imageLabel.setIcon(sms.getImage());
        }
}//GEN-LAST:event_queueListValueChanged

    private void okButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        SMS sms = (SMS) queueList.getSelectedValue();
        if (sms == null) {
            return;
        }

        String code = codeTextField.getText();

        smsQueue.get(sms).add(code);
        smsQueue.remove(sms);
        queueModel.removeElement(sms);
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        SMS sms = (SMS) queueList.getSelectedValue();
        if (sms == null) {
            return;
        }

        smsQueue.get(sms).add(""); //null is not allowed in blocking queue
        smsQueue.remove(sms);
        queueModel.removeElement(sms);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void formWindowDeactivated(WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        if (!isVisible()) {
            //do "cleanup" before hiding
            for (BlockingQueue<String> bq : smsQueue.values()) {
                //cancel all pending sms
                bq.add("");
            }
            //delete all queue
            smsQueue.clear();
            queueModel.clear();

            queueScrollPane.setVisible(false);
        }
    }//GEN-LAST:event_formWindowDeactivated

    private void codeTextFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_codeTextFieldKeyPressed
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_ENTER:
                okButton.doClick(0);
                break;
        }
    }//GEN-LAST:event_codeTextFieldKeyPressed

    /** Renderer for items in sms list */
    private class QueueRenderer extends SubstanceDefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            SMS sms = (SMS) value;
            JLabel label = ((JLabel)c);

            //set text
            label.setText(sms.toString());
            //add operator logo
            Operator operator = Operators.getOperator(sms.getOperator());
            label.setIcon(operator != null ? operator.getIcon() : Icons.OPERATOR_BLANK);

            return label;
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton cancelButton;
    private JTextField codeTextField;
    private JLabel imageLabel;
    private JLabel introLabel;
    private JLabel jLabel2;
    private JPanel jPanel1;
    private JButton okButton;
    private JList queueList;
    private JScrollPane queueScrollPane;
    // End of variables declaration//GEN-END:variables

}
