/*
 * QueuePanel.java
 *
 * Created on 3. říjen 2007, 22:05
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.event.ValuedEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import esmska.data.Icons;
import esmska.data.Gateways;
import esmska.data.Queue;
import esmska.data.SMS;
import esmska.data.Gateway;
import esmska.data.event.AbstractListDataListener;
import esmska.data.event.ActionEventSupport;
import esmska.data.event.ValuedEventSupport;
import esmska.utils.L10N;
import esmska.data.event.ValuedListener;
import esmska.utils.MiscUtils;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.openide.awt.Mnemonics;
import org.pushingpixels.substance.api.ColorSchemeAssociationKind;
import org.pushingpixels.substance.api.ComponentState;
import org.pushingpixels.substance.api.SubstanceColorScheme;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.painter.border.SubstanceBorderPainter;
import org.pushingpixels.substance.api.painter.highlight.SubstanceHighlightPainter;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;
import org.pushingpixels.substance.api.skin.SkinChangeListener;

/** SMS queue panel
 *
 * @author  ripper
 */
public class QueuePanel extends javax.swing.JPanel {
    public static enum Events {
        /** A message wants to be edited. Event value: sms to be edited. */
        SMS_EDIT_REQUESTED;
    }

    private static final Logger logger = Logger.getLogger(QueuePanel.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Queue queue = Queue.getInstance();
    private static final Config config = Config.getInstance();
    
    private Action deleteSMSAction = new DeleteSMSAction();
    private Action editSMSAction = new EditSMSAction();
    private Action smsUpAction = new SMSUpAction();
    private Action smsDownAction = new SMSDownAction();
    
    private QueueListModel queueListModel = new QueueListModel();
    //sms which have been requested to be edited
    private QueuePopupMenu popup = new QueuePopupMenu();
    private QueueMouseListener mouseListener;
    //regularly update sms delay indicators
    private Timer timer = new Timer(500, new DelayListener());
    
    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, SMS> valuedSupport = new ValuedEventSupport<Events, SMS>(this);
    public void addValuedListener(ValuedListener<Events, SMS> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, SMS> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>

    /** Creates new form QueuePanel */
    public QueuePanel() {
        initComponents();

        //add mouse listeners to the queue list
        mouseListener = new QueueMouseListener(queueList, popup);
        queueList.addMouseListener(mouseListener);

        //when model is changed update actions
        queueListModel.addListDataListener(new AbstractListDataListener() {
            @Override
            public void onUpdate(ListDataEvent e) {
                queueListValueChanged(null);
            }
        });

        //set visibility of advanced controls
        showAdvancedControls(config.isShowAdvancedControls());
        config.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if ("showAdvancedControls".equals(evt.getPropertyName())) {
                    showAdvancedControls((Boolean)evt.getNewValue());
                }
            }
        });

        queue.addValuedListener(new ValuedListener<Queue.Events, SMS>() {
            @Override
            public void eventOccured(ValuedEvent<Queue.Events, SMS> e) {
                switch (e.getEvent()) {
                    case QUEUE_PAUSED:
                        pausedLabel.setVisible(true);
                        break;
                    case QUEUE_RESUMED:
                        pausedLabel.setVisible(false);
                        break;
                }
                if (MiscUtils.needsResize(QueuePanel.this, MiscUtils.Direction.HEIGHT)) {
                    actionSupport.fireActionPerformed(ActionEventSupport.ACTION_NEED_RESIZE, null);
                }
                QueuePanel.this.revalidate(); //fixes problem with cropped PauseButton
            }
        });
    }

    /** Show or hide advanced controls (buttons, etc) */
    private void showAdvancedControls(boolean show) {
        smsUpButton.setVisible(show);
        smsDownButton.setVisible(show);
        popup.showAdvancedControls(show);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new JPanel();
        jScrollPane2 = new JScrollPane();
        queueList = new JList();
        pausedLabel = new InfoLabel();
        jPanel2 = new JPanel();
        jPanel3 = new JPanel();
        editButton = new JButton();
        deleteButton = new JButton();
        jPanel4 = new JPanel();
        pauseButton = new JToggleButton();
        jPanel5 = new JPanel();
        smsUpButton = new JButton();
        smsDownButton = new JButton();

        setBorder(BorderFactory.createTitledBorder(l10n.getString("QueuePanel.border.title"))); // NOI18N
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        jPanel1.setBorder(null);

        queueList.setModel(queueListModel);
        queueList.setCellRenderer(new SMSQueueListRenderer(queueList));
        queueList.setVisibleRowCount(1);
        queueList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                queueListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(queueList);
        Mnemonics.setLocalizedText(pausedLabel, l10n.getString("QueuePanel.pausedLabel.text"));
        pausedLabel.setVisible(queue.isPaused());

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addComponent(jScrollPane2, Alignment.TRAILING)
            .addComponent(pausedLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane2)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(pausedLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        );

        jPanel2.setBorder(null);
        jPanel2.setLayout(new BorderLayout());

        jPanel3.setBorder(null);

        editButton.setAction(editSMSAction);
        editButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        editButton.setText("");

        deleteButton.setAction(deleteSMSAction);
        deleteButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        deleteButton.setText("");

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(editButton, GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(deleteButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(Alignment.LEADING)
            .addComponent(editButton)
            .addComponent(deleteButton)
        );

        jPanel2.add(jPanel3, BorderLayout.PAGE_START);

        jPanel4.setBorder(null);

        pauseButton.setAction(Actions.getQueuePauseAction(false));
        pauseButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(Alignment.LEADING)
            .addComponent(pauseButton)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(Alignment.LEADING)
            .addComponent(pauseButton)
        );

        jPanel2.add(jPanel4, BorderLayout.PAGE_END);

        smsUpButton.setAction(smsUpAction);
        smsUpButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        smsUpButton.setText("");

        smsDownButton.setAction(smsDownAction);
        smsDownButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        smsDownButton.setText("");

        GroupLayout jPanel5Layout = new GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(Alignment.LEADING)
            .addComponent(smsUpButton)
            .addComponent(smsDownButton)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(smsUpButton)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(smsDownButton))
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(jPanel1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 55, Short.MAX_VALUE)
                    .addComponent(jPanel2, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    private void queueListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_queueListValueChanged
        if (evt != null && evt.getValueIsAdjusting()) {
            return;
        }
        if (queueList.getMaxSelectionIndex() >= queueListModel.getSize()) {
            //selection model has not yet been updated
            return;
        }

        //update form components
        int size = queueList.getSelectedIndices().length;
        SMS sms = (SMS) queueList.getSelectedValue();
        Object[] smses = queueList.getSelectedValues();

        editSMSAction.setEnabled(size == 1 && sms.getStatus() != SMS.Status.SENDING);
        deleteSMSAction.setEnabled(size > 0);
        for (Object s : smses) {
            if (((SMS)s).getStatus() == SMS.Status.SENDING) {
                deleteSMSAction.setEnabled(false);
            }
        }
        
        if (sms != null && size == 1) {
            List<SMS> list = queue.getAll(sms.getGateway());
            int index = list.indexOf(sms);
            smsUpAction.setEnabled(index != 0);
            smsDownAction.setEnabled(index < list.size() - 1);
        } else {
            smsUpAction.setEnabled(false);
            smsDownAction.setEnabled(false);
        }
}//GEN-LAST:event_queueListValueChanged

    private void formFocusGained(FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        pauseButton.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained
    
    /** Erase sms from queue list */
    private class DeleteSMSAction extends AbstractAction {
        public DeleteSMSAction() {
            super(l10n.getString("Delete_messages"), 
                    new ImageIcon(QueuePanel.class.getResource(RES + "delete-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Delete_selected_messages"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(QueuePanel.class.getResource(RES + "delete-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            Object[] smsArray = queueList.getSelectedValues();
            for (Object o : smsArray) {
                SMS sms = (SMS) o;
                queue.remove(sms);
            }

            //transfer focus
            if (queueListModel.getSize() > 0) {
                queueList.requestFocusInWindow();
            } else {
                pauseButton.requestFocusInWindow();
            }
        }
    }
    
    /** Edit sms from queue */
    private class EditSMSAction extends AbstractAction {
        public EditSMSAction() {
            super(l10n.getString("Edit_message"),
                    new ImageIcon(QueuePanel.class.getResource(RES + "edit-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Edit_selected_message"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(QueuePanel.class.getResource(RES + "edit-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            SMS sms = (SMS) queueList.getSelectedValue();
            if (sms == null) {
                return;
            }
            //fire event
            logger.fine("SMS requested for editing: " + sms);
            valuedSupport.fireEventOccured(Events.SMS_EDIT_REQUESTED, sms);
        }
    }
    
    /** move sms up in sms queue */
    private class SMSUpAction extends AbstractAction {
        public SMSUpAction() {
            super(l10n.getString("Move_up"),
                    new ImageIcon(QueuePanel.class.getResource(RES + "up-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("QueuePanel.Move_sms_up_in_the_queue"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(QueuePanel.class.getResource(RES + "up-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            SMS sms = (SMS) queueList.getSelectedValue();
            if (sms == null) {
                return;
            }
            queue.movePosition(sms, -1);
            int index = queueListModel.indexOf(sms);
            queueList.setSelectedIndex(index);
            queueList.ensureIndexIsVisible(index);
        }
    }
    
    /** move sms down in sms queue */
    private class SMSDownAction extends AbstractAction {
        public SMSDownAction() {
            super(l10n.getString("Move_down"),
                    new ImageIcon(QueuePanel.class.getResource(RES + "down-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("QueuePanel.Move_sms_down_in_the_queue"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(QueuePanel.class.getResource(RES + "down-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            SMS sms = (SMS) queueList.getSelectedValue();
            if (sms == null) {
                return;
            }
            queue.movePosition(sms, 1);
            int index = queueListModel.indexOf(sms);
            queueList.setSelectedIndex(index);
            queueList.ensureIndexIsVisible(index);
        }
    }
    
    /** Model for SMSQueueList */
    private class QueueListModel extends AbstractListModel {
        private int oldSize = getSize();

        public QueueListModel() {
            //listen for changes in contacts and fire events accordingly
            queue.addValuedListener(new ValuedListener<Queue.Events, SMS>() {
                @Override
                public void eventOccured(ValuedEvent<Queue.Events, SMS> e) {
                    switch (e.getEvent()) {
                        case SMS_ADDED:
                        case SENDING_SMS:
                        case SMS_SENDING_FAILED:
                        case SMS_POSITION_CHANGED:
                            fireContentsChanged(QueueListModel.this, 0, getSize());
                            break;
                        case SMS_REMOVED:
                        case QUEUE_CLEARED:
                            fireIntervalRemoved(QueueListModel.this, 0, oldSize);
                            break;
                    }
                    oldSize = getSize();
                    timer.start(); //start counting down delays
                }
            });
        }

        @Override
        public SMS getElementAt(int index) {
            return queue.getAll().get(index);
        }
        @Override
        public int getSize() {
            return queue.size();
        }
        public int indexOf(SMS element) {
            return queue.getAll().indexOf(element);
        }
        public void fireContentsChanged(int index0, int index1) {
            super.fireContentsChanged(this, index0, index1);
        }

    }
    
    /** Renderer for items in queue list */
    private class SMSQueueListRenderer extends SubstanceDefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final JLabel delayLabel = new JLabel("", SwingConstants.TRAILING);
        private final ImageIcon sendIcon = new ImageIcon(getClass().getResource(RES + "send-16.png"));
        private final URL messageIconURI = getClass().getResource(RES + "message-32.png");
        private final boolean isSubstance = ThemeManager.isSubstanceCurrentLaF();
        private boolean selected = false; //whether current item is selected
        private final JList jlist; //list to render
        private SubstanceColorScheme scheme; //current Substance color scheme for this list
        private SubstanceColorScheme borderScheme; //current Substance border color scheme for this list
        private SubstanceHighlightPainter highlPainter; //current Substance highlight painter for this list
        private SubstanceBorderPainter borderPainter; //current Substance border painter for this list

        private final JPanel panel = new JPanel(new BorderLayout()) { //panel to wrap multiple labels
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                //Substance is not painting highlights on JPanels, therefore we must
                //handle this painting on our own
                if (isSubstance && selected) {
                    highlPainter.paintHighlight((Graphics2D) g, this, getWidth(),
                            getHeight(), scheme);
                    //do some black magic to get component contour - one pixel is
                    //substracted from right and bottom, bcz it was cut off otherwise
                    Rectangle contour = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
                    borderPainter.paintBorder((Graphics2D) g, this, getWidth(),
                            getHeight(), contour, contour, borderScheme);
                }
            }
        };

        public SMSQueueListRenderer(JList list) {
            this.jlist = list;
            panel.add(delayLabel, BorderLayout.LINE_END);
            
            updateSubstanceSkinValues();
            SubstanceLookAndFeel.registerSkinChangeListener(new SkinChangeListener() {
                @Override
                public void skinChanged() {
                    updateSubstanceSkinValues();
                }
            });
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, false); //looks better without cell focus
            selected = isSelected;
            JLabel label = (JLabel) c;
            SMS sms = (SMS)value;
            
            //set text
            label.setText(StringUtils.defaultIfEmpty(sms.getName(),
                    CountryPrefix.stripCountryPrefix(sms.getNumber())));
            //problematic sms colored
            if ((sms.isProblematic()) && !isSelected) {
                label.setBackground(Color.RED);
            }
            //set colors on other components
            delayLabel.setForeground(label.getForeground());
            delayLabel.setBackground(label.getBackground());
            panel.setBackground(label.getBackground());
            //add gateway logo
            Gateway gateway = Gateways.getGateway(sms.getGateway());
            label.setIcon(gateway != null ? gateway.getIcon() : Icons.GATEWAY_BLANK);
            //set tooltip
            String text = WordUtils.wrap(sms.getText(), 50, null, true);
            text = MiscUtils.escapeHtml(text);
            text = text.replaceAll("\n", "<br>");
            String tooltip = "<html><table><tr><td><img src=\"" + messageIconURI +
                    "\"></td><td valign=top><b>" + label.getText() + "</b><br>" +
                    (StringUtils.isEmpty(sms.getName())?"":CountryPrefix.stripCountryPrefix(sms.getNumber())+", ") +
                    sms.getGateway() + "<br><br>" + text +
                    "</td></tr></table></html>";
            panel.setToolTipText(tooltip);
            //set delay label
            if (queue.getAllWithStatus(SMS.Status.SENDING).contains(sms)) {
                delayLabel.setIcon(sendIcon);
                delayLabel.setText(null);
            } else {
                delayLabel.setIcon(null);
                long delay = queue.getSMSDelay(sms);
                delayLabel.setText(Gateways.convertDelayToHumanString(delay, true));
            }
            //add to panel
            panel.add(label, BorderLayout.CENTER);

            return panel;
        }
        /** on substance skin update reinitialize some properties */
        private void updateSubstanceSkinValues() {
            if (!isSubstance) {
                return;
            }
            scheme = SubstanceLookAndFeel.getCurrentSkin(jlist).getColorScheme(jlist,
                    ColorSchemeAssociationKind.HIGHLIGHT, ComponentState.SELECTED);
            borderScheme = SubstanceLookAndFeel.getCurrentSkin(jlist).getColorScheme(jlist,
                    ColorSchemeAssociationKind.HIGHLIGHT_BORDER, ComponentState.SELECTED);
            highlPainter = SubstanceLookAndFeel.getCurrentSkin(jlist).getHighlightPainter();
            borderPainter = (SubstanceBorderPainter) ObjectUtils.defaultIfNull(
                    SubstanceLookAndFeel.getCurrentSkin(jlist).getHighlightBorderPainter(),
                    SubstanceLookAndFeel.getCurrentSkin(jlist).getBorderPainter());
        }
    }
    
    /** Regularly update the information about current message delays */
    private class DelayListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean timerNeeded = false;

            for (int i = 0; i < queueListModel.getSize(); i++) {
                SMS sms = queueListModel.getElementAt(i);
                long delay = queue.getSMSDelay(sms);
                if (delay > 0) {
                    timerNeeded = true;
                    queueListModel.fireContentsChanged(i, i);
                }
            }

            if (!timerNeeded) {
                //when everything is on 0, no need for timer to run, let's stop it
                timer.stop();
            }
        }
    }

    /** Popup menu in the queue list */
    private class QueuePopupMenu extends JPopupMenu {
        private ArrayList<JMenuItem> advancedItems = new ArrayList<JMenuItem>();

        public QueuePopupMenu() {
            JMenuItem menuItem = null;

            //edit sms action
            menuItem = new JMenuItem(editSMSAction);
            this.add(menuItem);

            //delete sms action
            menuItem = new JMenuItem(deleteSMSAction);
            this.add(menuItem);

            //move sms up action
            menuItem = new JMenuItem(smsUpAction);
            this.add(menuItem);
            advancedItems.add(menuItem);

            //move sms down action
            menuItem = new JMenuItem(smsDownAction);
            this.add(menuItem);
            advancedItems.add(menuItem);

            this.addSeparator();

            //queue pause action
            menuItem = new JMenuItem(Actions.getQueuePauseAction(true));
            this.add(menuItem);
        }

        public void showAdvancedControls(boolean show) {
            for (JMenuItem item : advancedItems) {
                item.setVisible(show);
            }
        }
    }
    
    /** Mouse listener on the queue list */
    private class QueueMouseListener extends ListPopupMouseListener {

        public QueueMouseListener(JList list, JPopupMenu popup) {
            super(list, popup);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            //transfer on left button doubleclick
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                editButton.doClick();
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton deleteButton;
    private JButton editButton;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JPanel jPanel5;
    private JScrollPane jScrollPane2;
    private JToggleButton pauseButton;
    private InfoLabel pausedLabel;
    private JList queueList;
    private JButton smsDownButton;
    private JButton smsUpButton;
    // End of variables declaration//GEN-END:variables
    
}
