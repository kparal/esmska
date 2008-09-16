/*
 * ContactPanel.java
 *
 * Created on 3. říjen 2007, 18:48
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.Contact;
import esmska.data.Icons;
import esmska.integration.MacUtils;
import esmska.operators.Operator;
import esmska.operators.OperatorUtil;
import esmska.persistence.PersistenceManager;
import esmska.utils.ActionEventSupport;
import esmska.utils.L10N;
import esmska.utils.DialogButtonSorter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jvnet.substance.SubstanceLookAndFeel;

/** Contact list panel
 *
 * @author  ripper
 */
public class ContactPanel extends javax.swing.JPanel {
    public static final int ACTION_CONTACT_SELECTION_CHANGED = 0;
    public static final int ACTION_CONTACT_CHOSEN = 1;
    
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private TreeSet<Contact> contacts = PersistenceManager.getContacs();
    private Config config = PersistenceManager.getConfig();
    
    private Action addContactAction = new AddContactAction();
    private Action editContactAction = new EditContactAction();
    private Action removeContactAction = new RemoveContactAction();
    private Action chooseContactAction = new ChooseContactAction();
    private SearchContactAction searchContactAction = new SearchContactAction();
    private ContactListModel contactListModel = new ContactListModel();
    private ContactPopupMenu popup = new ContactPopupMenu();
    private ContactMouseListener mouseListener;

    // <editor-fold defaultstate="collapsed" desc="ActionEvent support">
    private ActionEventSupport actionSupport = new ActionEventSupport(this);
    public void addActionListener(ActionListener actionListener) {
        actionSupport.addActionListener(actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        actionSupport.removeActionListener(actionListener);
    }
    // </editor-fold>
    
    /** Creates new form ContactPanel */
    public ContactPanel() {
        initComponents();
        
        //add mouse listeners to the contact list
        mouseListener = new ContactMouseListener(contactList, popup);
        contactList.addMouseListener(mouseListener);
    }
    
    /** clear selection of contact list */
    public void clearSelection() {
        contactList.clearSelection();
    }
    
    /** set selected contact in contact list */
    public void setSelectedContact(Contact contact) {
        contactList.setSelectedValue(contact, true);
    }
    
    /** set selected contact in contact list based on contact name
     * @return true, if contact with same name was found, false otherwise
     */
    public boolean setSelectedContact(String name) {
        if (name == null || name.length() == 0)
            return false;
        for (Contact c : contacts) {
            if (c.getName().equals(name)) {
                contactList.setSelectedValue(c, true);
                return true;
            }
        }
        return false;
    }
    
    /** Return selected contacts
     * @return Collection of selected contacts. Zero length collection if noone selected.
     */
    public HashSet<Contact> getSelectedContacts() {
        HashSet<Contact> selectedContacts = new HashSet<Contact>();
        for (Object o : contactList.getSelectedValues())
            selectedContacts.add((Contact) o);
        return selectedContacts;
    }
       
    /** add new contacts to the contact list */
    public void addContacts(Collection<Contact> contacts) {
        contactListModel.addAll(contacts);
    }
    
    /** select first contact in contact list, if possible and no other contact selected */
    public void ensureContactSelected() {
        if (contactList.getSelectedIndex() < 0 && contactListModel.getSize() > 0)
            contactList.setSelectedIndex(0);
    }
    
    /** Add margins to selected contact to make selection nicer. Has effect only
     * if single contact selected.
     */
    public void makeNiceSelection() {
        int[] indices = contactList.getSelectedIndices();
        if (indices.length != 1) {
            return;
        }
        setSelectedContactIndexWithMargins(indices[0]);
    }
    
    /** sets selected index in contact list with making intelligent
     * margins of 3 other contacts visible around the selected one
     */
    private void setSelectedContactIndexWithMargins(int index) {
        contactList.setSelectedIndex(index);
        //let 3 contacts be visible before and after the selected contact
        for (int j = index - 3; j <= index + 3; j++) {
            if (j >= 0 && j < contactListModel.getSize())
                contactList.ensureIndexIsVisible(j);
        }
        contactList.ensureIndexIsVisible(index);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addContactButton = new javax.swing.JButton();
        removeContactButton = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        contactList = new ContactList();
        editContactButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(l10n.getString("ContactPanel.border.title"))); // NOI18N
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        addContactButton.setAction(addContactAction);
        addContactButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        addContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        addContactButton.setText("");

        removeContactButton.setAction(removeContactAction);
        removeContactButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        removeContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        removeContactButton.setText("");

        contactList.setModel(contactListModel);
        contactList.setToolTipText(l10n.getString("ContactPanel.contactList.toolTipText")); // NOI18N
        contactList.setCellRenderer(new ContactListRenderer());
        //key shortcuts
        String command = "choose contact";
        contactList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), command);
        contactList.getActionMap().put(command, chooseContactAction);

        command = "focus contacts";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_K,KeyEvent.ALT_DOWN_MASK), command);
        getActionMap().put(command, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                contactList.requestFocusInWindow();
            }
        });
        contactList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                contactListValueChanged(evt);
            }
        });
        contactList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                contactListKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                contactListKeyTyped(evt);
            }
        });
        jScrollPane4.setViewportView(contactList);

        editContactButton.setAction(editContactAction);
        editContactButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        editContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        editContactButton.setText("");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addContactButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editContactButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeContactButton))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(addContactButton)
                    .addComponent(editContactButton)
                    .addComponent(removeContactButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    private void contactListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_contactListValueChanged
        if (evt.getValueIsAdjusting())
            return;
        
        // update components
        int count = contactList.getSelectedIndices().length;
        removeContactAction.setEnabled(count != 0);
        editContactAction.setEnabled(count == 1);
        
        //fire event
        actionSupport.fireActionPerformed(ACTION_CONTACT_SELECTION_CHANGED, null);
    }//GEN-LAST:event_contactListValueChanged

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        contactList.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained

    private void contactListKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contactListKeyTyped
        //do not catch keyboard shortcuts
        if (evt.isActionKey() || evt.isAltDown() || evt.isAltGraphDown() ||
                evt.isControlDown() || evt.isMetaDown()) {
            return;
        }
        
        char chr = evt.getKeyChar();

        //skip when not letter nor digit nor whitespace, or is enter
        if ((!Character.isLetterOrDigit(chr) && !Character.isWhitespace(chr)) ||
                Character.getNumericValue(chr) == Character.getNumericValue(KeyEvent.VK_ENTER)) {
            return;
        }

        //search
        String searchString = searchContactAction.getSearchString();
        searchString += Character.toLowerCase(chr);
        searchContactAction.setSearchString(searchString);
        searchContactAction.actionPerformed(null);
    }//GEN-LAST:event_contactListKeyTyped

    private void contactListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_contactListKeyPressed
        //process backspace
        if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            String searchString = searchContactAction.getSearchString();
            if (searchString.length() > 0) {
                searchString = searchString.substring(0, searchString.length() - 1);
                searchContactAction.setSearchString(searchString);
                searchContactAction.actionPerformed(null);
            }
            return;
        }
        
        //process escape
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            searchContactAction.setSearchString("");
            searchContactAction.actionPerformed(null);
            return;
        }

        //move to another matching contact when searching and using arrows (and prolong the delay)
        if ((evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN) &&
                !searchContactAction.getSearchString().equals("")) {
            int index = Math.max(contactList.getSelectedIndex(), 0);
            if (evt.getKeyCode() == KeyEvent.VK_DOWN) { //go to next matching contact
                index++;
                for (; index < contactListModel.getSize(); index++) {
                    Contact contact = contactListModel.getElementAt(index);
                    if (searchContactAction.isContactMatched(contact)) {
                        setSelectedContactIndexWithMargins(index);
                        break;
                    }
                }
            } else { //go to previous matching contact
                index--;
                for (; index >= 0; index--) {
                    Contact contact = contactListModel.getElementAt(index);
                    if (searchContactAction.isContactMatched(contact)) {
                        setSelectedContactIndexWithMargins(index);
                        break;
                    }
                }
            }
            evt.consume();
            searchContactAction.restartTimer();
            ((ContactList)contactList).repaintSearchField();
        }
    }//GEN-LAST:event_contactListKeyPressed
    
    /** Add contact to contact list */
    private class AddContactAction extends AbstractAction {
        private final String createOption = l10n.getString("Create");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = DialogButtonSorter.sortOptions(
                cancelOption, createOption);
        
        public AddContactAction() {
            super(l10n.getString("Add_contact"),
                    new ImageIcon(ContactPanel.class.getResource(RES + "add-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Add_new_contact"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "add-22.png")));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            contactList.requestFocusInWindow(); //always transfer focus
            ContactDialog contactDialog = new ContactDialog();
            contactDialog.setTitle(l10n.getString("New_contact"));
            contactDialog.setOptions(options, createOption, createOption);
            contactDialog.show(null);
            Contact c = contactDialog.getContact();
            if (c == null) {
                return;
            }
            contactListModel.add(c);
            
            contactList.clearSelection();
            contactList.setSelectedValue(c, true);
        }
    }
    
    /** Edit contact from contact list */
    private class EditContactAction extends AbstractAction {
        private final String saveOption = l10n.getString("Save");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = DialogButtonSorter.sortOptions(
                cancelOption, saveOption);
        
        public EditContactAction() {
            super(l10n.getString("Edit_contact"),
                    new ImageIcon(ContactPanel.class.getResource(RES + "edit-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Edit_selected_contact"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "edit-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            contactList.requestFocusInWindow(); //always transfer focus
            Contact contact = (Contact)contactList.getSelectedValue();
            ContactDialog contactDialog = new ContactDialog();
            contactDialog.setTitle(l10n.getString("Edit_contact"));
            contactDialog.setOptions(options, saveOption, saveOption);
            contactDialog.show(contact);
            Contact c = contactDialog.getContact();
            if (c == null) {
                return;
            }
            contactListModel.remove(contact);
            contactListModel.add(c);
            
            contactList.clearSelection();
            contactList.setSelectedValue(c, true);
        }
    }
    
    /** Remove contact from contact list */
    private class RemoveContactAction extends AbstractAction {
        private final String deleteOption = l10n.getString("Delete");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = DialogButtonSorter.sortOptions(
                cancelOption, deleteOption);
        
        public RemoveContactAction() {
            super(l10n.getString("Delete_contacts"),
                    new ImageIcon(ContactPanel.class.getResource(RES + "remove-16.png")));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Delete_selected_contacts"));
            this.putValue(LARGE_ICON_KEY,
                    new ImageIcon(ContactPanel.class.getResource(RES + "remove-22.png")));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            contactList.requestFocusInWindow(); //always transfer focus
            
            HashSet<Contact> condemned = getSelectedContacts();
            
            //create warning
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            JLabel label = new JLabel(l10n.getString("ContactPanel.remove_following_contacts"));
            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setRows(5);
            for (Contact c : condemned) {
                area.append(c.getName() + "\n");
            }
            area.setCaretPosition(0);
            panel.add(label, BorderLayout.PAGE_START);
            panel.add(new JScrollPane(area), BorderLayout.CENTER);
            
            //confirm
            JOptionPane pane = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE, 
                    JOptionPane.DEFAULT_OPTION, null, options, cancelOption);
            JDialog dialog = pane.createDialog(MainFrame.getInstance(), null);
            dialog.setResizable(true);
            MacUtils.setDocumentModalDialog(dialog);
            dialog.pack();
            dialog.setVisible(true);

            //return if should not delete
            if (!deleteOption.equals(pane.getValue())) {
                return;
            }
            
            //delete
            contactListModel.removeAll(condemned);
        }
    }
    
    /** Choose contact in contact list by keyboard or mouse */
    private class ChooseContactAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (contactList.getSelectedIndex() >= 0) {
                actionSupport.fireActionPerformed(ACTION_CONTACT_CHOSEN, null);
            }
        }
    }
    
    /** Search for contact in contact list */
    private class SearchContactAction extends AbstractAction {
        private String searchString = "";
        /** "forgetting" timer, time to forget the searched string */
        private Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchString = "";
                SearchContactAction.this.actionPerformed(null);
            }
        });
        
        public SearchContactAction() {
            timer.setRepeats(false);
        }
        
        /** update the graphical highlighting */
        private void updateRendering() {
            ((ContactList)contactList).showSearchField(searchString);
            contactList.repaint();
        }
        
        /** do the search */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (searchString.equals("")) {
                updateRendering();
                return;
            }
            for (int i = 0; i < contactListModel.getSize(); i++) {
                Contact contact = contactListModel.getElementAt(i);
                if (isContactMatched(contact)) {
                    setSelectedContactIndexWithMargins(i);
                    break;
                }
            }
            updateRendering();
            restartTimer();
        }
        
        /** @return true if contact is matched by search string, false otherwise */
        public boolean isContactMatched(Contact contact) {
            if (searchString.equals(""))
                return true;
            return (contact.getName().toLowerCase().contains(searchString) ||
                        contact.getNumber().contains(searchString));
        }
        
        /** set string to be searched in contact list */
        public void setSearchString(String searchString) {
            this.searchString = searchString;
        }
        
        /** get string searched in contact list */
        public String getSearchString() {
            return searchString;
        }
        
        /** force the search timer to restart (therefore prolong the delay) */
        public void restartTimer() {
            timer.restart();
        }
    }
    
    /** JList with contacts */
    private class ContactList extends JList {
        JTextField searchField = new JTextField();

        public ContactList() {
            searchField.setFocusable(false);
        }
        
        /** show search field in contact list or hide it
         * @param text text to show; empty or null string hides the field
         */
        public void showSearchField(String text) {
            if (text == null || "".equals(text)) {
                remove(searchField);
            } else {
                searchField.setText(text);
                if (searchField.getParent() == null) {
                    add(searchField);
                }
            }
            searchField.invalidate();
            validate();
        }
        
        /** repaints only the search field, not the whole container */
        public void repaintSearchField() {
            Rectangle oldBounds = searchField.getBounds();
            searchField.invalidate();
            contactList.validate();
            contactList.repaint(oldBounds); //repaint old bounds
            contactList.repaint(searchField.getBounds()); //repaint new bounds
        }
        
        @Override
        public void doLayout() {
            super.doLayout();
            
            Rectangle visibleRect = getVisibleRect();
            int height = (int) searchField.getPreferredSize().getHeight();
            //+1 bcz first char was cut off sometimes
            int width = (int) searchField.getPreferredSize().getWidth() + 1;
            searchField.setBounds(visibleRect.x + visibleRect.width - width,
                visibleRect.y + visibleRect.height - height, width, height);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (searchField != null)
                searchField.updateUI();
        }
    }
    
    /** Model for contact list */
    private class ContactListModel extends AbstractListModel {
        @Override
        public int getSize() {
            return contacts.size();
        }
        @Override
        public Contact getElementAt(int index) {
            return contacts.toArray(new Contact[0])[index];
        }
        public int indexOf(Contact element) {
            return new ArrayList<Contact>(contacts).indexOf(element);
        }
        public void add(Contact element) {
            if (contacts.add(element)) {
                int index = indexOf(element);
                fireIntervalAdded(this, index, index);
            }
        }
        public boolean contains(Contact element) {
            return contacts.contains(element);
        }
        public boolean remove(Contact element) {
            int index = indexOf(element);
            boolean removed = contacts.remove(element);
            if (removed) {
                fireIntervalRemoved(this, index, index);
            }
            return removed;
        }
        public void removeAll(Collection<Contact> elements) {
//            for (Object o : elements)
//                remove((Contact)o); //TODO fix 'out of memory' when using remove()
            int size = getSize();
            contacts.removeAll(elements);
            fireIntervalRemoved(this, 0, size);
        }
        public void addAll(Collection<Contact> elements) {
            contacts.addAll(elements);
            fireContentsChanged(this, 0, getSize());
        }
        @Override
        protected void fireIntervalRemoved(Object source, int index0, int index1) {
            super.fireIntervalRemoved(source, index0, index1);
        }
        @Override
        protected void fireIntervalAdded(Object source, int index0, int index1) {
            super.fireIntervalAdded(source, index0, index1);
        }
        @Override
        protected void fireContentsChanged(Object source, int index0, int index1) {
            super.fireContentsChanged(source, index0, index1);
        }
    }
    
    /** dialog for creating and editing contact */
    private class ContactDialog extends JDialog implements PropertyChangeListener {
        private final ImageIcon contactIcon = new ImageIcon(ContactPanel.class.getResource(RES + "contact-48.png"));
        private EditContactPanel panel;
        private JOptionPane optionPane;
        private Contact contact;
        private Object[] options;
        private Object initialValue, confirmOption;
        public ContactDialog() {
            super((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, ContactPanel.this),
                    l10n.getString("Contact"), true);
            init();
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            //integrate modal window better on Mac
            MacUtils.setDocumentModalDialog(this);
        }
        private void init() {
            panel = new EditContactPanel();
            optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, contactIcon, options, initialValue);
            optionPane.addPropertyChangeListener(this);
            setContentPane(optionPane);
            pack();
        }
        /** Set options to display as buttons
         * @param options possible options
         * @param initialValue default option
         * @param confirmOption option which confirms the dialog; other options cancels it.
         *  Can't be null.
         */
        public void setOptions(Object[] options, Object initialValue, Object confirmOption) {
            this.options = options;
            this.initialValue = initialValue;
            this.confirmOption = confirmOption;
        }
        /** Show dialog with existing or new (null) contact */
        public void show(Contact contact) {
            this.contact = contact;
            init();
            setLocationRelativeTo(MainFrame.getInstance());
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            panel.setContact(contact);
            panel.prepareForShow();
            setVisible(true);
        }
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            
            if (isVisible()
            && (e.getSource() == optionPane)
            && (JOptionPane.VALUE_PROPERTY.equals(prop))) {
                Object value = optionPane.getValue();
                
                if (value == JOptionPane.UNINITIALIZED_VALUE) {
                    //ignore reset
                    return;
                }
                if (!value.equals(confirmOption)) { //not confirmed
                    contact = null;
                    setVisible(false);
                    return;
                }
                
                //verify inputs
                if (!panel.validateForm()) {
                    optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
                    return;
                }
                //inputs verified, all ok
                contact = panel.getContact();
                setVisible(false);
            }
        }
        /** Get currently displayed contact */
        public Contact getContact() {
            return contact;
        }
    }
    
    /** Renderer for items in contact list */
    private class ContactListRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            Contact contact = (Contact)value;
            JLabel label = ((JLabel)c);
            //add operator logo
            Operator operator = OperatorUtil.getOperator(contact.getOperator());
            label.setIcon(operator != null ? operator.getIcon() : Icons.OPERATOR_BLANK);
            //set tooltip
            String number = contact.getNumber();
            if (number.startsWith(config.getCountryPrefix()))
                number = number.substring(config.getCountryPrefix().length());
            label.setToolTipText(number);
            //set background on non-matching contacts when searching
            if (!searchContactAction.getSearchString().equals("") &&
                    !searchContactAction.isContactMatched(contact)) {
                label.setBackground(label.getBackground().darker());
                label.setForeground(label.getForeground().darker());
            }
            return label;
        }
    }
    
    /** Popup menu in the contact list */
    private class ContactPopupMenu extends JPopupMenu {

        public ContactPopupMenu() {
            JMenuItem menuItem = null;

            //add contact action
            menuItem = new JMenuItem(addContactAction);
            this.add(menuItem);

            //edit contact action
            menuItem = new JMenuItem(editContactAction);
            this.add(menuItem);

            //remove contact action
            menuItem = new JMenuItem(removeContactAction);
            this.add(menuItem);
        }
    }
    
    /** Mouse listener on the contact list */
    private class ContactMouseListener extends ListPopupMouseListener {

        public ContactMouseListener(JList list, JPopupMenu popup) {
            super(list, popup);
        }
        
        @Override
        public void mouseClicked(MouseEvent e) {
            //transfer on left button doubleclick
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                chooseContactAction.actionPerformed(null);
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addContactButton;
    private javax.swing.JList contactList;
    private javax.swing.JButton editContactButton;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JButton removeContactButton;
    // End of variables declaration//GEN-END:variables
    
}
