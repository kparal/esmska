package esmska.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import esmska.data.Config;
import esmska.data.History;
import esmska.data.Icons;
import esmska.data.event.AbstractDocumentListener;
import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import esmska.utils.RuntimeUtils;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.openide.awt.Mnemonics;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;

/** Display all sent messages in a frame
 *
 * @author  ripper
 */
public class HistoryFrame extends javax.swing.JFrame {
    public static enum Events {
        /** A message is requested to be resent. Event value: History record to resend. */
        RESEND_SMS;
    }
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Logger logger = Logger.getLogger(HistoryFrame.class.getName());
    private static final Config config = Config.getInstance();
    private DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private History history = History.getInstance();
    private HistoryTableModel historyTableModel = new HistoryTableModel();
    private TableRowSorter<HistoryTableModel> historyTableSorter = new TableRowSorter<HistoryTableModel>(historyTableModel);
    private HistoryRowFilter historyTableFilter = new HistoryRowFilter();
    private Action deleteAction = new DeleteAction();
    private Action resendAction = new ResendAction();
    private History.Record selectedHistory;

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, History.Record> valuedSupport = new ValuedEventSupport<Events, History.Record>(this);
    public void addValuedListener(ValuedListener<Events, History.Record> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, History.Record> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    /** Creates new form HistoryFrame */
    public HistoryFrame() {
        initComponents();

        //set window images
        ArrayList<Image> images = new ArrayList<Image>();
        images.add(Icons.get("history-16.png").getImage());
        images.add(Icons.get("history-32.png").getImage());
        images.add(Icons.get("history-48.png").getImage());
        setIconImages(images);

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
        
        //if not Substance LaF, add clipboard popup menu to text components
        if (!config.getLookAndFeel().equals(ThemeManager.LAF.SUBSTANCE)) {
            ClipboardPopupMenu.register(searchField);
            ClipboardPopupMenu.register(textArea);
        }
        
        //select first row
        if (historyTableModel.getRowCount() > 0) {
            historyTable.getSelectionModel().setSelectionInterval(0, 0);
        }
        history.addActionListener(new HistoryActionListener());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        deleteButton = new JButton();
        resendButton = new JButton();
        closeButton = new JButton();
        searchField = new JTextField();
        searchLabel = new JLabel();
        clearButton = new JButton();
        jSplitPane1 = new JSplitPane();
        jScrollPane1 = new JScrollPane();
        historyTable = new JTable();
        jPanel1 = new JPanel();
        jLabel2 = new JLabel();
        jLabel1 = new JLabel();
        jLabel3 = new JLabel();
        jLabel4 = new JLabel();
        jLabel5 = new JLabel();
        jLabel6 = new JLabel();
        dateLabel = new JLabel();
        nameLabel = new JLabel();
        numberLabel = new JLabel();
        gatewayLabel = new JLabel();
        senderNumberLabel = new JLabel();
        senderNameLabel = new JLabel();
        jScrollPane2 = new JScrollPane();
        textArea = new JTextArea();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("HistoryFrame.title")); // NOI18N
        addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(WindowEvent evt) {
            }
        });

        deleteButton.setAction(deleteAction);

        resendButton.setAction(resendAction);

        closeButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        Mnemonics.setLocalizedText(closeButton, l10n.getString("Close_"));
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        searchField.setColumns(15);
        searchField.setToolTipText(l10n.getString("HistoryFrame.searchField.toolTipText")); // NOI18N
        searchField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                historyTableFilter.requestUpdate();
            }
        });

        //on Mac OS X this will create a native search field with inline icons
        searchField.putClientProperty("JTextField.variant", "search");
        String command = "clear";
        searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), command);
        searchField.getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText(null);
            }
        });

        //focus on Ctrl+F
        command = "focus search field";
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
            command);
        getRootPane().getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchFieldFocusGained(null);
            }
        });
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                searchFieldFocusGained(evt);
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                searchFieldKeyPressed(evt);
            }
        });

        searchLabel.setLabelFor(searchField);
        Mnemonics.setLocalizedText(searchLabel, l10n.getString("HistoryFrame.searchLabel.text")); // NOI18N
        searchLabel.setToolTipText(searchField.getToolTipText());

        clearButton.setIcon(new ImageIcon(getClass().getResource("/esmska/resources/clear-22.png"))); // NOI18N
        clearButton.setMnemonic('r');
        clearButton.setToolTipText(l10n.getString("HistoryFrame.clearButton.toolTipText")); // NOI18N
        clearButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        if (RuntimeUtils.isMac() && config.getLookAndFeel().equals(ThemeManager.LAF.SYSTEM)) {
            clearButton.setVisible(false);
        }
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        jSplitPane1.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setContinuousLayout(true);

        historyTable.setModel(historyTableModel);
        historyTable.setDefaultRenderer(Date.class, new TableDateRenderer());
        historyTable.getSelectionModel().addListSelectionListener(new HistoryTableListener());
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        historyTableSorter.setSortKeys(sortKeys);
        historyTable.setRowSorter(historyTableSorter);
        historyTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                historyTableMouseClicked(evt);
            }
        });
        historyTable.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                historyTableKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(historyTable);

        jSplitPane1.setLeftComponent(jScrollPane1);











        Mnemonics.setLocalizedText(jLabel2, l10n.getString("HistoryFrame.jLabel2.text")); // NOI18N
        Mnemonics.setLocalizedText(jLabel1, l10n.getString("HistoryFrame.jLabel1.text"));
        Mnemonics.setLocalizedText(jLabel3, l10n.getString("HistoryFrame.jLabel3.text"));
        Mnemonics.setLocalizedText(jLabel4, l10n.getString("HistoryFrame.jLabel4.text"));
        Mnemonics.setLocalizedText(jLabel5, l10n.getString("HistoryFrame.jLabel5.text"));
        Mnemonics.setLocalizedText(jLabel6, l10n.getString("HistoryFrame.jLabel6.text"));
        Mnemonics.setLocalizedText(dateLabel, "    ");
        nameLabel.setFont(nameLabel.getFont().deriveFont(nameLabel.getFont().getStyle() | Font.BOLD));
        Mnemonics.setLocalizedText(nameLabel, "    ");
        Mnemonics.setLocalizedText(numberLabel, "    ");
        Mnemonics.setLocalizedText(gatewayLabel, "    ");
        Mnemonics.setLocalizedText(senderNumberLabel, "    ");
        Mnemonics.setLocalizedText(senderNameLabel, "    ");

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(textArea);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(gatewayLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(dateLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(nameLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(numberLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(senderNumberLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(senderNameLabel)))
                .addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jLabel1, jLabel2, jLabel3, jLabel4, jLabel5, jLabel6});

        jPanel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {dateLabel, gatewayLabel, nameLabel, numberLabel, senderNameLabel, senderNumberLabel});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(dateLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(nameLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(numberLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(gatewayLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(senderNumberLabel))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(senderNameLabel)))
            .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(jPanel1);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 533, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                            .addComponent(closeButton)
                            .addComponent(resendButton)
                            .addComponent(deleteButton)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(searchLabel)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(searchField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(clearButton)))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {closeButton, deleteButton, resendButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(searchField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(searchLabel))
                    .addComponent(clearButton))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(resendButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(deleteButton)
                        .addPreferredGap(ComponentPlacement.RELATED, 336, Short.MAX_VALUE)
                        .addComponent(closeButton))
                    .addComponent(jSplitPane1, GroupLayout.DEFAULT_SIZE, 444, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(SwingConstants.VERTICAL, new Component[] {closeButton, deleteButton, resendButton});

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void closeButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
}//GEN-LAST:event_closeButtonActionPerformed

    private void historyTableMouseClicked(MouseEvent evt) {//GEN-FIRST:event_historyTableMouseClicked
        //only on left double click
        if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() >= 2) {
            resendButton.doClick(0);
        }
    }//GEN-LAST:event_historyTableMouseClicked

    private void historyTableKeyPressed(KeyEvent evt) {//GEN-FIRST:event_historyTableKeyPressed
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_ENTER: 
                evt.consume();
                resendButton.doClick(0);
                break;
            case KeyEvent.VK_DELETE:
                deleteButton.doClick(0);
                break;
        }
    }//GEN-LAST:event_historyTableKeyPressed

    private void clearButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        searchField.setText(null);
        searchField.requestFocusInWindow();
    }//GEN-LAST:event_clearButtonActionPerformed

    private void searchFieldFocusGained(FocusEvent evt) {//GEN-FIRST:event_searchFieldFocusGained
        searchField.selectAll();
    }//GEN-LAST:event_searchFieldFocusGained

    private void searchFieldKeyPressed(KeyEvent evt) {//GEN-FIRST:event_searchFieldKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DOWN ||
                evt.getKeyCode() == KeyEvent.VK_UP) {
            //redirect the event to the history table
            historyTable.requestFocusInWindow();
            historyTable.dispatchEvent(evt);
        }
    }//GEN-LAST:event_searchFieldKeyPressed

    private void formWindowGainedFocus(WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        historyTable.requestFocusInWindow();
    }//GEN-LAST:event_formWindowGainedFocus

    /** Delete sms from history */
    private class DeleteAction extends AbstractAction {
        private final String deleteOption = l10n.getString("Delete");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, deleteOption);
        private final String message = l10n.getString("HistoryFrame.remove_selected");
        
        public DeleteAction() {
            L10N.setLocalizedText(this, l10n.getString("Delete_"));
            putValue(LARGE_ICON_KEY, Icons.get("delete-22.png"));
            putValue(SHORT_DESCRIPTION, l10n.getString("Delete_selected_messages_from_history"));
            this.setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            historyTable.requestFocusInWindow(); //always transfer focus
            int[] rows = historyTable.getSelectedRows();
            if (rows.length <= 0) {
                return;
            }
            ArrayList<History.Record> histToDelete = new ArrayList<History.Record>();
            for (int i : rows) {
                i = historyTable.getRowSorter().convertRowIndexToModel(i);
                histToDelete.add(history.getRecord(i));
            }
            
            //show dialog
            JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, 
                    JOptionPane.DEFAULT_OPTION, null, options, deleteOption);
            JDialog dialog = pane.createDialog(HistoryFrame.this, null);
            dialog.setResizable(true);
            RuntimeUtils.setDocumentModalDialog(dialog);
            dialog.pack();
            dialog.setVisible(true);

            //return if should not delete
            if (!deleteOption.equals(pane.getValue())) {
                return;
            }

            //confirmed, let's delete it
            history.removeRecords(histToDelete);
            
            //refresh table
            historyTableModel.fireTableDataChanged();
            historyTable.getSelectionModel().clearSelection();
        }
    }

    /** Resend chosen sms from history */
    private class ResendAction extends AbstractAction {

        public ResendAction() {
            L10N.setLocalizedText(this, l10n.getString("Forward_"));
            putValue(LARGE_ICON_KEY, Icons.get("send-22.png"));
            putValue(SHORT_DESCRIPTION, l10n.getString("HistoryFrame.resend_message"));
            this.setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (selectedHistory == null) {
                return;
            }
            logger.fine("Forwarding message from history: " + selectedHistory);
            //fire event and close
            valuedSupport.fireEventOccured(Events.RESEND_SMS, selectedHistory);
            closeButton.doClick(0);
        }
    }

    /** Listener for history changes */
    private class HistoryActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            historyTableModel.fireTableDataChanged();
            historyTable.getSelectionModel().clearSelection();
        }
    }
    
    /** Table model for showing sms history */
    private class HistoryTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return history.getRecords().size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            History.Record record = history.getRecord(rowIndex);
            switch (columnIndex) {
                case 0:
                    return record.getDate();
                case 1:
                    String name = record.getName();
                    return name != null && !name.equals("") ? name : record.getNumber();
                case 2:
                    return record.getText().replaceAll("\n+", " "); //show spaces instead of linebreaks
                default:
                    logger.warning("Index out of bounds!");
                    return null;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return l10n.getString("Date");
                case 1:
                    return l10n.getString("Recipient");
                case 2:
                    return l10n.getString("Text");
                default:
                    logger.warning("Index out of bounds!");
                    return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Date.class;
                case 1:
                case 2:
                    return String.class;
                default:
                    logger.warning("Index out of bounds!");
                    return Object.class;
            }
        }
    }

    /** Listener for changes in history table */
    private class HistoryTableListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int index = historyTable.getSelectedRow();
            boolean selected = (index >= 0);
            deleteAction.setEnabled(selected);
            resendAction.setEnabled(selected);

            History.Record record = null;
            if (selected) {
                index = historyTable.getRowSorter().convertRowIndexToModel(index);
                record = history.getRecord(index);
            }

            dateLabel.setText(selected ? df.format(record.getDate()) : null);
            nameLabel.setText(selected ? record.getName() : null);
            numberLabel.setText(selected ? record.getNumber() : null);
            gatewayLabel.setText(selected ? record.getGateway() : null);
            senderNameLabel.setText(selected ? record.getSenderName() : null);
            senderNumberLabel.setText(selected ? record.getSenderNumber() : null);
            textArea.setText(selected ? record.getText() : null);
            textArea.setCaretPosition(0);

            selectedHistory = record;
        }
    }

    /** Renderer for date columns in history table */
    private class TableDateRenderer extends SubstanceDefaultTableCellRenderer {
        private final ImageIcon icon = Icons.get("message-16.png");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel label = (JLabel) comp;
            label.setText(df.format(value));
            label.setIcon(icon);
            return label;
        }
    }
    
    /** Filter for searching in history table */
    private class HistoryRowFilter extends RowFilter<HistoryTableModel, Integer> {
        private Timer timer = new Timer(500, new ActionListener() { //updating after each event is slow,
            @Override
            public void actionPerformed(ActionEvent e) {            //therefore there is timer
                historyTableSorter.setRowFilter(HistoryRowFilter.this);
                if (historyTable.getRowCount() > 0) {
                    historyTable.changeSelection(0, 0, false, false);
                    historyTable.scrollRectToVisible(historyTable.getCellRect(0, 0, true));
                }
            }
        });
        public HistoryRowFilter() {
            timer.setRepeats(false);
        }
        @Override
        public boolean include(Entry<? extends HistoryTableModel, ? extends Integer> entry) {
            History.Record record = history.getRecord(entry.getIdentifier());
            String pattern = searchField.getText().toLowerCase();
            //search through text, name, number and date
            if (record.getText() != null && record.getText().toLowerCase().contains(pattern)) {
                return true;
            }
            if (record.getNumber() != null && record.getNumber().toLowerCase().contains(pattern)) {
                return true;
            }
            if (record.getName() != null && record.getName().toLowerCase().contains(pattern)) {
                return true;
            }
            if (record.getDate() != null && df.format(record.getDate()).toLowerCase().contains(pattern)) {
                return true;
            }
            return false;
        }
        /** request history search filter to be updated */
        public void requestUpdate() {
            timer.restart();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton clearButton;
    private JButton closeButton;
    private JLabel dateLabel;
    private JButton deleteButton;
    private JLabel gatewayLabel;
    private JTable historyTable;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JPanel jPanel1;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JSplitPane jSplitPane1;
    private JLabel nameLabel;
    private JLabel numberLabel;
    private JButton resendButton;
    private JTextField searchField;
    private JLabel searchLabel;
    private JLabel senderNameLabel;
    private JLabel senderNumberLabel;
    private JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
