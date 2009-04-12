/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Operators;
import esmska.data.Operator;
import esmska.data.Operators.Events;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.apache.commons.lang.WordUtils;

/** JComboBox showing available operators.
 *
 * @author ripper
 */
public class OperatorComboBox extends JComboBox {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String RES = "/esmska/resources/";
    private static final Config config = Config.getInstance();
    private static final OperatorComboBoxRenderer cellRenderer = new OperatorComboBoxRenderer();
    private static SortedSet<Operator> operators = Operators.getInstance().getAll();
    private DefaultComboBoxModel model = new DefaultComboBoxModel(operators.toArray());
    /** used only for non-existing operators */
    private String operatorName;
    
    public OperatorComboBox() {
        filterOperators();
        setModel(model);
        setRenderer(cellRenderer);
        setToolTipText(l10n.getString("OperatorComboBox.tooltip"));
        if (model.getSize() > 0) {
            setSelectedIndex(0);
        }
        
        //add listener to operator filter patterns
        config.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!"operatorFilter".equals(evt.getPropertyName())) {
                    return;
                }
                filterOperators();
                if (model.getSize() > 0) {
                    setSelectedIndex(0);
                }
            }
        });

        //add listener to operator updates
        Operators.getInstance().addValuedListener(new ValuedListener<Operators.Events, Operator>() {
            @Override
            public void eventOccured(ValuedEvent<Events, Operator> e) {
                switch (e.getEvent()) {
                    case ADDED_OPERATOR:
                    case ADDED_OPERATORS:
                    case CLEARED_OPERATORS:
                    case REMOVED_OPERATOR:
                    case REMOVED_OPERATORS:
                        updateOperators();
                }
            }
        });
    }
    
    /** Get selected operator in list or null if none selected. */
    public Operator getSelectedOperator() {
        return (Operator) getSelectedItem();
    }
    
    /** Get name of the selected operator. 
     * If unknown operator selected (selection cleared), it returns previously inserted
     * operator name (may be null).
     */
    public String getSelectedOperatorName() {
        return (getSelectedOperator() != null ? getSelectedOperator().getName() : operatorName);
    }
  
// Disabled, because it caused issues when selecting non-existing operator and
// getting the operator back after that. It changed the contact operator from
// unknown string to null.
//
//    /** Set currently selected operator.
//     * Use null for clearing the selection. Non-existing operator will not change the selection.
//     */
//    public void setSelectedOperator(Operator operator) {
//        operatorName = (operator != null ? operator.getName() : null);
//        setSelectedItem(operator);
//    }
    
    /** Set currently selected operator by it's name.
     * Use null for clearing the selection. Non-existing operator will also clear the selection.
     */
    public void setSelectedOperator(String operatorName) {
        this.operatorName = operatorName;
        Operator operator = Operators.getOperator(operatorName);
        if (model.getIndexOf(operator) < 0) {
            setSelectedItem(null);
        } else {
            setSelectedItem(operator);
        }
    }
    
    /** Select operator according to phone number or phone number prefix.
     * Searches through available (displayed) operators and selects the best
     * suited on supporting this phone number. Doesn't change selection if no 
     * such operator is found. Doesn't change selection if no operator prefix 
     * matched and operator with matching country prefix is already selected.
     * @param number phone number or it's prefix. The minimum length is two characters,
     *               for shorter input (or null) the method does nothing.
     */
    public void selectSuggestedOperator(String number) {
        TreeSet<Operator> visibleOperators = new TreeSet<Operator>();
        for (int i = 0; i < model.getSize(); i++) {
            Operator op = (Operator) model.getElementAt(i);
            visibleOperators.add(op);
        }
        
        Operator operator = Operators.suggestOperator(number, visibleOperators);
        
        //none suitable operator found, do nothing
        if (operator == null) {
            return;
        }
        
        //if perfect match found, select it
        if (Operators.matchesWithOperatorPrefix(operator, number)) {
            setSelectedOperator(operator.getName());
            return;
        }
        
        //return if already selected operator with matching country prefix
        Operator selectedOperator = getSelectedOperator();
        if (Operators.matchesWithCountryPrefix(selectedOperator, number)) {
            return;
        }
        
        //select suggested
        setSelectedOperator(operator.getName());
        
    }
          
    /** Renderer for items in OperatorComboBox */
    private static class OperatorComboBoxRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final URL keyringIconURI = getClass().getResource(RES + "keyring-16.png");
        private final String pattern = l10n.getString("OperatorComboBox.operatorTooltip");
        private final String noReg = l10n.getString("OperatorComboBox.noRegistration");
        private final String registration = MessageFormat.format("<img src=\"{0}\"> ", keyringIconURI) +
                l10n.getString("OperatorComboBox.needRegistration");
        private final String international = l10n.getString("OperatorComboBox.international");

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value, 
                    index, isSelected, cellHasFocus);
            if (!(value instanceof Operator)) {
                return c;
            }
            JLabel label = (JLabel) c;
            Operator operator = (Operator)value;
            label.setText(operator.getName());
            label.setIcon(operator.getIcon());

            label.setToolTipText(generateTooltip(operator));
            return label;
        }

        /** Generate tooltip with operator info */
        private String generateTooltip(Operator operator) {
            String country = CountryPrefix.extractCountryCode(operator.getName());
            String local = MessageFormat.format(l10n.getString("OperatorComboBox.onlyCountry"), country);
            String description = WordUtils.wrap(operator.getDescription(), 50, "<br>&nbsp;&nbsp;", false);

            String tooltip = MessageFormat.format(pattern,
                    operator.getName(), operator.getWebsite(), description,
                    operator.isLoginRequired() ? registration : noReg,
                    Operators.convertDelayToHumanString(operator.getDelayBetweenMessages(), false),
                    country.equals(CountryPrefix.INTERNATIONAL_CODE) ? international : local,
                    operator.getVersion());
            return tooltip;
        }
    }
    
    /** Iterates through all operators and leaves in the model only those which
     * matches the user configured patterns
     */
    private void filterOperators() {
        model.removeAllElements();
        String[] patterns = config.getOperatorFilter().split(",");
        ArrayList<Operator> filtered = new ArrayList<Operator>();
        oper: for (Operator operator : operators.toArray(new Operator[0])) {
            for (String pattern : patterns) {
                if (operator.getName().contains(pattern)) {
                    filtered.add(operator);
                    continue oper;
                }
            }
        }
        for (Operator operator : filtered) {
            model.addElement(operator);
        }
    }

    /** Update model when operators are updated */
    private void updateOperators() {
        String opName = getSelectedOperatorName();
        operators = Operators.getInstance().getAll();
        model.removeAllElements();
        for (Operator op : operators) {
            model.addElement(op);
        }
        setSelectedOperator(opName);
    }
}
