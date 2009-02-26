/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.Icons;
import esmska.data.Operators;
import esmska.operators.Operator;
import esmska.utils.L10N;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/** JComboBox showing available operators.
 *
 * @author ripper
 */
public class OperatorComboBox extends JComboBox {
    private static final Logger logger = Logger.getLogger(OperatorComboBox.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final SortedSet<Operator> operators = Operators.getInstance().getAll();
    private static final Config config = Config.getInstance();
    private static final OperatorComboBoxRenderer cellRenderer = new OperatorComboBoxRenderer();
    private DefaultComboBoxModel model = new DefaultComboBoxModel(operators.toArray());
    /** used only for non-existing operators */
    private String operatorName;
    
    public OperatorComboBox() {
        filterOperators();
        setModel(model);
        setRenderer(cellRenderer);
        setToolTipText(l10n.getString("OperatorComboBox.List_of_available_operators_web_gateways"));
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
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value, 
                    index, isSelected, cellHasFocus);
            if (!(value instanceof Operator)) {
                return c;
            }
            JLabel label = (JLabel) c;
            Operator operator = (Operator)value;
            label.setText(operator != null ? operator.getName() : l10n.getString("Unknown_operator"));
            label.setIcon(operator != null ? operator.getIcon() : Icons.OPERATOR_BLANK);
            return label;
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
    
}
