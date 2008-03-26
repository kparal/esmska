/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.Icons;
import esmska.operators.Operator;
import esmska.operators.OperatorUtil;
import esmska.persistence.PersistenceManager;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

/** JComboBox showing available operators.
 *
 * @author ripper
 */
public class OperatorComboBox extends JComboBox {
    private static final TreeSet<Operator> operators = PersistenceManager.getOperators();
    private static final Config config = PersistenceManager.getConfig();
    private static final OperatorComboBoxRenderer cellRenderer = new OperatorComboBoxRenderer();
    private DefaultComboBoxModel model = new DefaultComboBoxModel(operators.toArray());
    /** used only for non-existing operators */
    private String operatorName;
    
    public OperatorComboBox() {
        filterOperators();
        setModel(model);
        setRenderer(cellRenderer);
        setToolTipText("Seznam dostupných operátorů");
        if (model.getSize() > 0)
            setSelectedIndex(0);
        
        //add listener to operator filter patterns
        config.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (!"operatorFilter".equals(evt.getPropertyName()))
                    return;
                filterOperators();
                if (model.getSize() > 0)
                    setSelectedIndex(0);
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
        Operator operator = OperatorUtil.getOperator(operatorName);
        if (model.getIndexOf(operator) < 0)
            setSelectedItem(null);
        else
            setSelectedItem(operator);
    }
    
    /** Renderer for items in OperatorComboBox */
    private static class OperatorComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            if (!(value instanceof Operator))
                return c;
            JLabel label = (JLabel) c;
            Operator operator = (Operator)value;
            label.setText(operator != null ? operator.getName() : "Neznámý operátor");
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
