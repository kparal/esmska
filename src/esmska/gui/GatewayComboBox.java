/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.gui;

import esmska.data.CountryPrefix;
import esmska.data.Gateways;
import esmska.data.Gateway;
import esmska.data.Gateways.Events;
import esmska.data.Tuple;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.math.RandomUtils;

/** JComboBox showing available gateways.
 *
 * @author ripper
 */
public class GatewayComboBox extends JComboBox {
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final String RES = "/esmska/resources/";
    private static final GatewayComboBoxRenderer cellRenderer = new GatewayComboBoxRenderer();
    private static final Gateways gateways = Gateways.getInstance();
    private DefaultComboBoxModel model = new DefaultComboBoxModel();
    /** used only for non-existing gateways */
    private String gatewayName;
    /** Current phone number filter */
    private String filter;
    
    public GatewayComboBox() {
        updateGateways();
        setModel(model);
        setRenderer(cellRenderer);
        if (model.getSize() > 0) {
            setSelectedIndex(0);
        }
        
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
                    case FAVORITES_UPDATED:
                    case HIDDEN_UPDATED:
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
        Gateway gateway = gateways.get(gatewayName);
        if (model.getIndexOf(gateway) < 0) {
            setSelectedItem(null);
        } else {
            setSelectedItem(gateway);
        }
    }
    
    /** Select gateway according to phone number or phone number prefix.
     * Searches through available (displayed) gateways and selects the best
     * suited on supporting this phone number. Clear selection if no
     * such gateway is found or just non-recommended gateways are suggested.
     *
     * @param number phone number or it's prefix. The minimum length is two characters,
     *               for shorter input (or null) the method does nothing.
     * @return boolean whether there were more than 1 options for the suggested gateway
     *         (and therefore some choice was done)
     */
    public boolean selectSuggestedGateway(String number) {
        Tuple<ArrayList<Gateway>, Boolean> tuple = gateways.suggestGateway(number);
        ArrayList<Gateway> gws = tuple.get1();
        boolean recommended = tuple.get2();
        if (gws.isEmpty()) {
            setSelectedGateway(null);
        } else if (gws.contains(getSelectedGateway())) {
            //suggested gateway already selected, do nothing
        } else {
            if (recommended) {
                //recommended, select random one
                setSelectedGateway(gws.get(RandomUtils.nextInt(gws.size())).getName());
            } else {
                //not recommended, leave selection empty and let user click
                //on "Suggest" button if he wants
                setSelectedGateway(null);
            }
        }
        return gws.size() > 1;
    }

    /** If there are more than 1 suggested gateways for this phone number,
     * this method will select the next one.
     */
    public void selectNextSuggestedGateway(String number) {
        ArrayList<Gateway> gws = gateways.suggestGateway(number).get1();
        if (!gws.isEmpty()) {
            int index = gws.indexOf(getSelectedGateway());
            if (index >= 0) {
                //traverse to next gateway
                index = (index + 1) % gws.size();
            } else {
                index = RandomUtils.nextInt(gws.size());
            }
            setSelectedGateway(gws.get(index).getName());
        } else {
            setSelectedGateway(null);
        }
    }

    /** Filter available gateways to display only those that are capable
     * of sending defined phone number or phone number prefix.
     *
     * @param number phone number or its prefix; use null to clear filter
     */
    public void setFilter(String number) {
        this.filter = number;
        updateGateways();
    }

    /** Update model when gateways are updated or when filter is changed */
    private void updateGateways() {
        String opName = getSelectedGatewayName();
        model.removeAllElements();
        for (Gateway gw : gateways.getVisible()) {
            if (StringUtils.isEmpty(filter) || Gateways.isNumberSupported(gw, filter)) {
                model.addElement(gw);
            }
        }
        setSelectedGateway(opName);
    }

    /** Renderer for items in GatewayComboBox */
    public static class GatewayComboBoxRenderer extends DefaultListCellRenderer {
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
            adjustLabel(label, gateway);
            return label;
        }

        /** Do all the adjustments to a JLabel needed to render this item properly */
        public void adjustLabel(JLabel label, Gateway gateway) {
            label.setText(gateway.getName());
            label.setIcon(gateway.getIcon());
            label.setToolTipText(generateTooltip(gateway));
            if (gateway.isHidden()) {
                label.setForeground(SystemColor.textInactiveText);
            }
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
    
}
