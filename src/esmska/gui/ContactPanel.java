package esmska.gui;

import esmska.Context;
import esmska.gui.dnd.ImportContactsTransferHandler;
import esmska.data.Contact;
import esmska.data.Contacts;
import esmska.data.CountryPrefix;
import esmska.data.Icons;
import esmska.data.Log;
import esmska.data.Gateways;
import esmska.data.Gateway;
import esmska.data.event.ActionEventSupport;
import esmska.utils.L10N;
import esmska.utils.MiscUtils;
import esmska.utils.RuntimeUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang.StringUtils;
import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultListCellRenderer;

/** Contact list panel
 *
 * @author  ripper
 */
public class ContactPanel extends javax.swing.JPanel {
    public static final int ACTION_CONTACT_SELECTION_CHANGED = 0;
    public static final int ACTION_CONTACT_CHOSEN = 1;
    
    private static final String RES = "/esmska/resources/";
    private static final Logger logger = Logger.getLogger(ContactPanel.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Contacts contacts = Contacts.getInstance();
    private static final Log log = Log.getInstance();
    private static final Gateways gateways = Gateways.getInstance();

    private Action addContactAction = new AddContactAction(null);
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

        //add DnD support for contact list
        contactList.setDropMode(DropMode.ON);
        contactList.setTransferHandler(new ImportContactsTransferHandler());

        //show new contact hint if there are no contacts
        ((ContactList)contactList).showNewContactHint(contacts.size() <= 0);
        //listen for changes in contacts size and change hint visibility
        contacts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((ContactList)contactList).showNewContactHint(
                        contacts.size() <= 0);
            }
        });
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
        if (name == null || name.length() == 0) {
            return false;
        }
        for (Contact c : contacts.getAll()) {
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
        for (Object o : contactList.getSelectedValues()) {
            selectedContacts.add((Contact) o);
        }
        return selectedContacts;
    }
       
    /** select first contact in contact list, if possible and no other contact selected */
    public void ensureContactSelected() {
        if (contactList.getSelectedIndex() < 0 && contactListModel.getSize() > 0) {
            contactList.setSelectedIndex(0);
        }
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

    /** Shows dialog for adding contact with predefined values
     * @param skeleton skeleton of contact to show as default values; may be null
     */
    public void showAddContactDialog(Contact skeleton) {
        AddContactAction action = new AddContactAction(skeleton);
        action.actionPerformed(null);
    }
    
    /** sets selected index in contact list with making intelligent
     * margins of 3 other contacts visible around the selected one
     */
    private void setSelectedContactIndexWithMargins(int index) {
        contactList.setSelectedIndex(index);
        //let 3 contacts be visible before and after the selected contact
        for (int j = index - 3; j <= index + 3; j++) {
            if (j >= 0 && j < contactListModel.getSize()) {
                contactList.ensureIndexIsVisible(j);
            }
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

        addContactButton = new JButton();
        removeContactButton = new JButton();
        jScrollPane4 = new JScrollPane();
        contactList = new ContactList();
        editContactButton = new JButton();

        setBorder(BorderFactory.createTitledBorder(l10n.getString("ContactPanel.border.title"))); // NOI18N
        addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent evt) {
                formFocusGained(evt);
            }
        });

        addContactButton.setAction(addContactAction);
        addContactButton.setHideActionText(true);
        addContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        addContactButton.setText(l10n.getString("Add"));

        removeContactButton.setAction(removeContactAction);
        removeContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        removeContactButton.setText("");

        contactList.setModel(contactListModel);
        contactList.setToolTipText(l10n.getString("ContactPanel.contactList.toolTipText")); // NOI18N
        contactList.setCellRenderer(new ContactListRenderer());
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
        contactList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                contactListValueChanged(evt);
            }
        });
        contactList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent evt) {
                contactListKeyPressed(evt);
            }
            public void keyTyped(KeyEvent evt) {
                contactListKeyTyped(evt);
            }
        });
        jScrollPane4.setViewportView(contactList);

        editContactButton.setAction(editContactAction);
        editContactButton.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        editContactButton.setText("");

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addContactButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(editContactButton)
                        .addPreferredGap(ComponentPlacement.RELATED)
                        .addComponent(removeContactButton))
                    .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 326, Short.MAX_VALUE)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                    .addComponent(addContactButton)
                    .addComponent(editContactButton)
                    .addComponent(removeContactButton))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    private void contactListValueChanged(ListSelectionEvent evt) {//GEN-FIRST:event_contactListValueChanged
        if (evt.getValueIsAdjusting()) {
            return;
        }
        
        // update components
        int count = contactList.getSelectedIndices().length;
        removeContactAction.setEnabled(count > 0);
        editContactAction.setEnabled(count > 0);
        
        //fire event
        actionSupport.fireActionPerformed(ACTION_CONTACT_SELECTION_CHANGED, null);
    }//GEN-LAST:event_contactListValueChanged

    private void formFocusGained(FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        contactList.requestFocusInWindow();
    }//GEN-LAST:event_formFocusGained

    private void contactListKeyTyped(KeyEvent evt) {//GEN-FIRST:event_contactListKeyTyped
        //do not catch keyboard shortcuts
        if (evt.isActionKey() || evt.isAltDown() || evt.isAltGraphDown() ||
                evt.isControlDown() || evt.isMetaDown()) {
            return;
        }
        
        char chr = evt.getKeyChar();
        
        //skip control characters (enter, etc)
        if (Character.isISOControl(chr)) {
            return;
        }

        //search
        String searchString = searchContactAction.getSearchString();
        searchString += Character.toLowerCase(chr);
        searchContactAction.setSearchString(searchString);
        searchContactAction.actionPerformed(null);
    }//GEN-LAST:event_contactListKeyTyped

    private void contactListKeyPressed(KeyEvent evt) {//GEN-FIRST:event_contactListKeyPressed
        //delete last letter in search string on backspace
        if (evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            String searchString = searchContactAction.getSearchString();
            if (searchString.length() > 0) {
                searchString = searchString.substring(0, searchString.length() - 1);
                searchContactAction.setSearchString(searchString);
                searchContactAction.actionPerformed(null);
            }
            return;
        }
        
        //cancel search string on escape
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

        //delete contact on delete
        if (evt.getKeyCode() == KeyEvent.VK_DELETE) {
            removeContactButton.doClick(0);
            return;
        }
    }//GEN-LAST:event_contactListKeyPressed
    
    /** Add contact to contact list */
    private class AddContactAction extends AbstractAction {
        private final String createOption = l10n.getString("Create");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, createOption);
        private final Contact skeleton;

        /** Constructor
         * @param skeleton skeleton of contact to show as default values; may be null
         */
        public AddContactAction(Contact skeleton) {
            super(l10n.getString("Add_contact"), Icons.get("add-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Add_new_contact"));
            this.putValue(LARGE_ICON_KEY, Icons.get("add-22.png"));
            this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);
            this.skeleton = skeleton;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            contactList.requestFocusInWindow(); //always transfer focus
            ContactDialog contactDialog = new ContactDialog();
            contactDialog.setTitle(l10n.getString("New_contact"));
            contactDialog.setOptions(options, createOption, createOption);
            contactDialog.show(skeleton);
            Contact c = contactDialog.getContact();
            if (c == null) {
                return;
            }
            contacts.add(c);
            contactList.setSelectedValue(c, true);
            log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("ContactPanel.addedContact"), c.getName()),
                    null, Icons.STATUS_INFO));
        }
    }
    
    /** Edit contact from contact list */
    private class EditContactAction extends AbstractAction {
        private final String saveOption = l10n.getString("Save");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, saveOption);
        
        public EditContactAction() {
            super(l10n.getString("Edit_contacts"), Icons.get("edit-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Edit_selected_contacts"));
            this.putValue(LARGE_ICON_KEY, Icons.get("edit-22.png"));
            this.setEnabled(false);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            contactList.requestFocusInWindow(); //always transfer focus

            Object[] selected = contactList.getSelectedValues();

            if (selected.length <= 0) {
                logger.warning("Trying to edit contact when none selected");
                return;
            }

            ContactDialog contactDialog = new ContactDialog();
            contactDialog.setOptions(options, saveOption, saveOption);

            if (selected.length == 1) { //edit single contact
                Contact contact = (Contact) selected[0];
                contactDialog.setTitle(l10n.getString("Edit_contact"));
                Contact edited = new Contact(contact);
                contactDialog.show(edited);
                edited = contactDialog.getContact();
                if (edited == null) {
                    return;
                }
                contact.copyFrom(edited);
                contactList.setSelectedValue(contact, true);
                log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("ContactPanel.editedContact"), contact.getName()),
                    null, Icons.STATUS_INFO));
            } else { //multiple contacts edited
                contactDialog.setTitle(l10n.getString("Edit_contacts_collectively"));
                ArrayList<Contact> list = new ArrayList<Contact>(selected.length);
                for (Object contact : selected) {
                    list.add((Contact) contact);
                }
                contactDialog.show(list);
                Contact c = contactDialog.getContact();
                if (c == null) {
                    return;
                }
                int[] selection = contactList.getSelectedIndices();
                for (Contact contact : list) {
                    //only gateway is common for all contacts
                    contact.setGateway(c.getGateway());
                }
                contactList.setSelectedIndices(selection);
                log.addRecord(new Log.Record(
                    MessageFormat.format(l10n.getString("ContactPanel.editedContacts"), list.size()),
                    null, Icons.STATUS_INFO));
            }
            
        }
    }
    
    /** Remove contact from contact list */
    private class RemoveContactAction extends AbstractAction {
        private final String deleteOption = l10n.getString("Delete");
        private final String cancelOption = l10n.getString("Cancel");
        private final Object[] options = RuntimeUtils.sortDialogOptions(
                cancelOption, deleteOption);
        
        public RemoveContactAction() {
            super(l10n.getString("Delete_contacts"), Icons.get("delete-16.png"));
            this.putValue(SHORT_DESCRIPTION,l10n.getString("Delete_selected_contacts"));
            this.putValue(LARGE_ICON_KEY, Icons.get("delete-22.png"));
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
            contacts.removeAll(condemned);

            String message;
            if (condemned.size() == 1) {
                message = MessageFormat.format(l10n.getString("ContactPanel.removeContact"),
                        condemned.iterator().next().getName());
            } else {
                message = MessageFormat.format(l10n.getString("ContactPanel.removeContacts"),
                        condemned.size());
            }
            log.addRecord(new Log.Record(message, null, Icons.STATUS_INFO));
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
            if (searchString.equals("")) {
                return true;
            }
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
        JLabel newContactLabel = new JLabel(l10n.getString("ContactPanel.new_contact_hint"));

        public ContactList() {
            searchField.setFocusable(false);
            newContactLabel.setVerticalAlignment(JLabel.TOP);
            newContactLabel.setForeground(SystemColor.textInactiveText);

            //listen for changes in contacts and adjust selection accordingly
            Contacts.getInstance().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = getSelectedIndex();
                    switch (e.getID()) {
                        case Contacts.ACTION_ADD_CONTACT:
                        case Contacts.ACTION_REMOVE_CONTACT:
                        case Contacts.ACTION_CLEAR_CONTACTS:
                            clearSelection();
                            break;
                        case Contacts.ACTION_CHANGE_CONTACT:
                            clearSelection();
                            setSelectedIndex(index);
                            break;
                        default:
                            logger.warning("Unknown action event type");
                            assert false : "Unknown action event type";
                    }
                }
            });
        }

        /** show search field in contact list or hide it
         * @param text text to show; empty or null string hides the field
         */
        public void showSearchField(String text) {
            if (StringUtils.isEmpty(text)) {
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

        /** Show hint how to add a new contact */
        public void showNewContactHint(boolean show) {
            if (show && newContactLabel.getParent() == null) {
                add(newContactLabel);
            } else {
                remove(newContactLabel);
            }
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
            if (searchField.getParent() != null) {
                //place searchField to a lower right corner
                Rectangle visibleRect = getVisibleRect();
                int height = (int) searchField.getPreferredSize().getHeight();
                //+1 bcz first char was cut off sometimes
                int width = (int) searchField.getPreferredSize().getWidth() + 1;
                searchField.setBounds(visibleRect.x + visibleRect.width - width,
                    visibleRect.y + visibleRect.height - height, width, height);
            }
            if (newContactLabel.getParent() != null) {
                //place newContactLabel to the center 5px from all borders
                Rectangle visibleRect = getVisibleRect();
                int height = (int) visibleRect.height - 10;
                int width = (int) visibleRect.width - 10;
                newContactLabel.setBounds(visibleRect.x + 5,
                        visibleRect.y + 5, width, height);
            }
        }

        @Override
        public void updateUI() {
            super.updateUI();
            if (searchField != null) {
                searchField.updateUI();
            }
        }
    }
    
    /** Model for contact list */
    private class ContactListModel extends AbstractListModel {
        private int oldSize = getSize();

        public ContactListModel() {
            //listen for changes in contacts and fire events accordingly
            contacts.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (e.getID()) {
                        case Contacts.ACTION_ADD_CONTACT:
                        case Contacts.ACTION_CHANGE_CONTACT:
                            fireContentsChanged(ContactListModel.this, 0, getSize());
                            break;
                        case Contacts.ACTION_REMOVE_CONTACT:
                        case Contacts.ACTION_CLEAR_CONTACTS:
                            fireIntervalRemoved(ContactListModel.this, 0, oldSize);
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
            return contacts.size();
        }
        @Override
        public Contact getElementAt(int index) {
            return contacts.getAll().toArray(new Contact[0])[index];
        }
    }
    
    /** dialog for creating and editing contact */
    private class ContactDialog extends JDialog implements PropertyChangeListener {
        private final ImageIcon contactIcon = Icons.get("contact-48.png");
        private EditContactPanel panel;
        private JOptionPane optionPane;
        private Contact contact;
        private Object[] options;
        private Object initialValue, confirmOption;
        public ContactDialog() {
            super((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, ContactPanel.this),
                    l10n.getString("Contact"), true);
            //integrate modal window better on Mac, must be called before initialization
            RuntimeUtils.setDocumentModalDialog(this);

            init();
            setDefaultCloseOperation(HIDE_ON_CLOSE);

            //handle closing by user
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    formWindowClosing(evt);
                }
            });
        }
        private void init() {
            panel = new EditContactPanel();
            optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, contactIcon, options, initialValue);
            optionPane.addPropertyChangeListener(this);
            setContentPane(optionPane);
            pack();
            panel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getID() == ActionEventSupport.ACTION_NEED_RESIZE) {
                        ContactDialog.this.pack();
                    }
                }
            });
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
            logger.fine("Showing edit contact dialog for contact: " + contact);
            this.contact = contact;
            init();
            setLocationRelativeTo(Context.mainFrame);
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            panel.setContact(contact);
            panel.prepareForShow();
            setVisible(true);
        }
        /** Show dialog for editing multiple contacts. May not be null. */
        public void show(Collection<Contact> contacts) {
            if (contacts.size() <= 1) {
                show(contacts.size() <= 0 ? null : contacts.iterator().next());
                return;
            }

            logger.fine("Showing edit contact dialog for " + contacts.size() + " contacts");
            this.contact = null;
            init();
            setLocationRelativeTo(Context.mainFrame);
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
            panel.setContacts(contacts);
            panel.prepareForShow();
            setVisible(true);
        }
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            
            if (isVisible() && e.getSource() == optionPane && JOptionPane.VALUE_PROPERTY.equals(prop)) {
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
        /** Get currently displayed contact. May be null (cancelled by user). */
        public Contact getContact() {
            return contact;
        }
        /** Respond to user closing */
        private void formWindowClosing(WindowEvent evt) {
            if (evt == null) {
                //window closed programatically
                return;
            }
            //not confirmed
            contact = null;
        }
    }
    
    /** Renderer for items in contact list */
    private class ContactListRenderer extends SubstanceDefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final URL contactIconURI = getClass().getResource(RES + "contact-32.png");
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            Contact contact = (Contact)value;
            JLabel label = ((JLabel)c);
            //add gateway logo
            Gateway gateway = gateways.get(contact.getGateway());
            label.setIcon(gateway != null ? gateway.getIcon() : Icons.GATEWAY_BLANK);
            //set tooltip
            String tooltip = "<html><table><tr><td><img src=\"" + contactIconURI +
                    "\"></td><td valign=top><b>" + MiscUtils.escapeHtml(contact.getName()) +
                    "</b><br>" + CountryPrefix.stripCountryPrefix(contact.getNumber(), true) +
                    "<br>" + MiscUtils.escapeHtml(contact.getGateway()) +
                    "</td></tr></table></html>";
            label.setToolTipText(tooltip);
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
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                //edit contact on left button doubleclick
                editContactAction.actionPerformed(null);
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                //transfer focus on middleclick
                //if user clicked on unselected item, select it
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && !list.isSelectedIndex(index)) {
                    list.setSelectedIndex(index);
                }
                chooseContactAction.actionPerformed(null);
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton addContactButton;
    private JList contactList;
    private JButton editContactButton;
    private JScrollPane jScrollPane4;
    private JButton removeContactButton;
    // End of variables declaration//GEN-END:variables
    
}
