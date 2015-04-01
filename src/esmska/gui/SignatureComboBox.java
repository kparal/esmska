package esmska.gui;

import esmska.data.Signature;
import esmska.data.Signatures;
import esmska.data.Signatures.Events;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.awt.Component;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import org.apache.commons.lang.StringUtils;

/** Combobox showing all Signatures.
 */
public final class SignatureComboBox extends JComboBox {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Signatures signatures = Signatures.getInstance();
    private static final SignatureComboBoxRenderer cellRenderer = new SignatureComboBoxRenderer();
    private static final String SEPARATOR = "SEPARATOR";
    private static final String NEW = l10n.getString("Signature.new");
    private static final String tooltip = l10n.getString("SignatureComboBox.tooltip");
    private DefaultComboBoxModel model = new DefaultComboBoxModel();

    public SignatureComboBox() {
        setToolTipText(tooltip);
        updateSignatures();
        setModel(model);
        setRenderer(cellRenderer);
        setSelectedItem(Signature.DEFAULT);

        //add listener to signature updates
        signatures.addValuedListener(new ValuedListener<Signatures.Events, Signature>() {
            @Override
            public void eventOccured(ValuedEvent<Events, Signature> e) {
                switch (e.getEvent()) {
                    case UPDATED:
                        updateSignatures();
                }
            }
        });
    }

    /** Get currently selected signature.
     * @return null if selected item is not Signature
     */
    public Signature getSelectedSignature() {
        if (getSelectedItem() instanceof Signature) {
            return (Signature) getSelectedItem();
        } else {
            return null;
        }
    }

    /** Set selected signature.
     * If no such signature exists, select default one.
     */
    public void setSelectedSignature(String signatureName) {
        Signature signature = signatures.get(signatureName);
        if (model.getIndexOf(signature) < 0) {
            setSelectedItem(Signature.DEFAULT);
        } else {
            setSelectedItem(signature);
        }
    }

    @Override
    public void setSelectedItem(Object anObject) {
        // create new signature profiles on request
        if (NEW.equals(anObject)) {
            String name = JOptionPane.showInputDialog(SignatureComboBox.this,
                    l10n.getString("Signature.new.desc"));
            if (StringUtils.isEmpty(name)) {
                // user cancelled selection
                super.setSelectedItem(Signature.DEFAULT);
                return;
            }
            Signature sig = new Signature(name, null, null);
            boolean added = signatures.add(sig);
            if (!added) {
                // signature of this name already exists
                super.setSelectedItem(Signature.DEFAULT);
                return;
            }
            //select the new signature
            super.setSelectedItem(sig);
            return;
        }
        super.setSelectedItem(anObject);
    }

    /** Detect whether currently selected signature may be edited. */
    public boolean isEditableSelected() {
        Signature signature = getSelectedSignature();
        return signature instanceof Signature && !Signature.NONE.equals(signature);
    }

    /** Detect whether currently selected signature may be removed. */
    public boolean isRemovableSelected() {
        Signature signature = getSelectedSignature();
        return signature instanceof Signature && !signatures.getSpecial().contains(signature);
    }

    /** Reload all signatures. */
    private void updateSignatures() {
        String sigName = getSelectedSignature() != null ? getSelectedSignature().getProfileName() : null;

        model.removeAllElements();
        //add special signatures
        for (Signature sig : signatures.getSpecial()) {
            model.addElement(sig);
        }
        //add custom signatures
        for (Signature sig : signatures.getAll()) {
            model.addElement(sig);
        }
        //add separator
        model.addElement(SEPARATOR);
        //add 'new' item
        model.addElement(NEW);

        setSelectedSignature(sigName);
    }

    /** Render signature items */
    public static class SignatureComboBoxRenderer extends DefaultListCellRenderer {
        private static final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private static final JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value,
                    index, isSelected, cellHasFocus);
            //display separator differently
            if (SEPARATOR.equals(value)) {
                return separator;
            }
            if (!(value instanceof Signature)) {
                return c;
            }
            JLabel label = (JLabel) c;
            Signature signature = (Signature)value;

            String name = signature.getProfileName();
            //translate special signatures
            if (Signature.DEFAULT.equals(signature)) {
                name = l10n.getString("Signature.default");
            } else if (Signature.NONE.equals(signature)) {
                name = l10n.getString("Signature.none");
            }
            label.setText(name);

            return label;
        }
    }
}
