/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Gateways;
import esmska.data.Gateway;
import esmska.data.Gateways.Events;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

/** JComboBox showing available gateways.
 *
 * @author ripper
 */
public class GatewayComboBox extends JComboBox {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String RES = "/esmska/resources/";
    private static final Config config = Config.getInstance();
    private static final GatewayComboBoxRenderer cellRenderer = new GatewayComboBoxRenderer();
    private static SortedSet<Gateway> gateways = Gateways.getInstance().getAll();
    private DefaultComboBoxModel model = new DefaultComboBoxModel(gateways.toArray());
    /** used only for non-existing gateways */
    private String gatewayName;
    
    public GatewayComboBox() {
        filterGateways();
        setModel(model);
        setRenderer(cellRenderer);
        if (model.getSize() > 0) {
            setSelectedIndex(0);
        }
        
        //add listener to gateway filter patterns
        config.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!"gatewayFilter".equals(evt.getPropertyName())) {
                    return;
                }
                filterGateways();
                if (model.getSize() > 0) {
                    setSelectedIndex(0);
                }
            }
        });

        //add listener to gateway updates
        Gateways.getInstance().addValuedListener(new ValuedListener<Gateways.Events, Gateway>() {
            @Override
            public void eventOccured(ValuedEvent<Events, Gateway> e) {
                switch (e.getEvent()) {
                    case ADDED_GATEWAY:
                    case ADDED_GATEWAYS:
                    case CLEARED_GATEWAYS:
                    case REMOVED_GATEWAY:
                    case REMOVED_GATEWAYS:
                        updateGateways();
                }
            }
        });

        //add listener to change tooltip depending on the chosen gateway
        this.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                switch (e.getStateChange()) {
                    case ItemEvent.DESELECTED:
                        setToolTipText(null);
                        break;
                    case ItemEvent.SELECTED:
                        setToolTipText(cellRenderer.generateTooltip(getSelectedGateway()));
                        break;
                }
            }
        });
    }
    
    /** Get selected gateway in list or null if none selected. */
    public Gateway getSelectedGateway() {
        return (Gateway) getSelectedItem();
    }
    
    /** Get name of the selected gateway.
     * If unknown gateway selected (selection cleared), it returns previously inserted
     * gateway name (may be null).
     */
    public String getSelectedGatewayName() {
        return (getSelectedGateway() != null ? getSelectedGateway().getName() : gatewayName);
    }
    
    /** Set currently selected gateway by it's name.
     * Use null for clearing the selection. Non-existing gateway will also clear the selection.
     */
    public void setSelectedGateway(String gatewayName) {
        this.gatewayName = gatewayName;
        Gateway gateway = Gateways.getGateway(gatewayName);
        if (model.getIndexOf(gateway) < 0) {
            setSelectedItem(null);
        } else {
            setSelectedItem(gateway);
        }
    }
    
    /** Select gateway according to phone number or phone number prefix.
     * Searches through available (displayed) gateways and selects the best
     * suited on supporting this phone number. Doesn't change selection if no 
     * such gateway is found.
     *
     * @param number phone number or it's prefix. The minimum length is two characters,
     *               for shorter input (or null) the method does nothing.
     */
    public void selectSuggestedGateway(String number) {
        TreeSet<Gateway> visibleGateways = new TreeSet<Gateway>();
        for (int i = 0; i < model.getSize(); i++) {
            Gateway op = (Gateway) model.getElementAt(i);
            visibleGateways.add(op);
        }
        
        Gateway gateway = Gateways.suggestGateway(number, visibleGateways);
        
        //none suitable gateway found, do nothing
        if (gateway == null) {
            return;
        }
        
        //select suggested
        setSelectedGateway(gateway.getName());
    }

    /** Renderer for items in GatewayComboBox */
    private static class GatewayComboBoxRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final URL keyringIconURI = getClass().getResource(RES + "keyring-16.png");
        private final String pattern = l10n.getString("GatewayComboBox.gatewayTooltip");
        private final String noReg = l10n.getString("GatewayComboBox.noRegistration");
        private final String registration = MessageFormat.format("<img src=\"{0}\"> ", keyringIconURI) +
                l10n.getString("GatewayComboBox.needRegistration");
        private final String international = l10n.getString("GatewayComboBox.international");

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = lafRenderer.getListCellRendererComponent(list, value, 
                    index, isSelected, cellHasFocus);
            if (!(value instanceof Gateway)) {
                return c;
            }
            JLabel label = (JLabel) c;
            Gateway gateway = (Gateway)value;
            label.setText(gateway.getName());
            label.setIcon(gateway.getIcon());

            label.setToolTipText(generateTooltip(gateway));
            return label;
        }

        /** Generate tooltip with gateway info */
        private String generateTooltip(Gateway gateway) {
            if (gateway == null) {
                return null;
            }
            String country = CountryPrefix.extractCountryCode(gateway.getName());
            String local = MessageFormat.format(l10n.getString("GatewayComboBox.onlyCountry"), country);
            String description = WordUtils.wrap(gateway.getDescription(), 50, "<br>&nbsp;&nbsp;", false);

            String tooltip = MessageFormat.format(pattern,
                    gateway.getName(), gateway.getWebsite(), description,
                    gateway.isLoginRequired() ? registration : noReg,
                    Gateways.convertDelayToHumanString(gateway.getDelayBetweenMessages(), false),
                    country.equals(CountryPrefix.INTERNATIONAL_CODE) ? international : local,
                    gateway.getVersion());
            return tooltip;
        }
    }
    
    /** Iterates through all gateways and leaves in the model only those which
     * matches the user configured patterns
     */
    private void filterGateways() {
        model.removeAllElements();
        String[] patterns = config.getGatewayFilter().split(",");
        ArrayList<Gateway> filtered = new ArrayList<Gateway>();
        oper: for (Gateway gateway : gateways.toArray(new Gateway[0])) {
            for (String pattern : patterns) {
                if (gateway.getName().contains(pattern)) {
                    filtered.add(gateway);
                    continue oper;
                }
            }
        }
        for (Gateway gateway : filtered) {
            model.addElement(gateway);
        }
    }

    /** Update model when gateways are updated */
    private void updateGateways() {
        String opName = getSelectedGatewayName();
        gateways = Gateways.getInstance().getAll();
        model.removeAllElements();
        for (Gateway op : gateways) {
            model.addElement(op);
        }
        filterGateways();
        setSelectedGateway(opName);
    }
}
