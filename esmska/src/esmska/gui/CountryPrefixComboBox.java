/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.operators.Operator;
import esmska.persistence.PersistenceManager;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.TreeSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

/** JComboBox showing all available telephone country prefixes. Combobox is editable.
 *
 * @author ripper
 */
public class CountryPrefixComboBox extends JComboBox {
    private static final TreeSet<Operator> operators = PersistenceManager.getOperators();
    private DefaultComboBoxModel model;
    
    public CountryPrefixComboBox() {
        setEditable(true);
        setToolTipText("Telefonní předčíslí země");
        
        //set model
        TreeSet<String> prefixes = new TreeSet<String>();
        for (Operator operator : operators) {
            prefixes.add(operator.getCountryPrefix());
        }
        model = new DefaultComboBoxModel(prefixes.toArray());
        setModel(model);
    }
    
    /** Get currently selected (or inserted) prefix.
     * May return null if no item selected.
     */
    public String getSelectedPrefix() {
        return (String) getSelectedItem();
    }
    
    /** Set currently selected prefix.
     * Combobox is editable, custom value may be inserted. Use null to clear selection.
     */
    public void setSelectedPrefix(String prefix) {
        setSelectedItem(prefix);
        //deselect text
        JTextField field = (JTextField) getEditor().getEditorComponent();
        field.select(0, 0);
    }
    
}
