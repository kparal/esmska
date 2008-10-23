/*
 * ConfigFrame.java
 *
 * Created on 20. červenec 2007, 18:59
 */

package esmska.gui;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import esmska.*;
import esmska.ThemeManager.LAF;
import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Keyring;
import esmska.integration.MacUtils;
import esmska.operators.Operator;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.SkinInfo;
import esmska.persistence.PersistenceManager;
import esmska.transfer.ProxyManager;
import esmska.utils.AbstractDocumentListener;
import esmska.utils.L10N;
import esmska.utils.DialogButtonSorter;
import esmska.utils.JavaType;
import esmska.utils.Nullator;
import java.awt.Toolkit;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.InputVerifier;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Configure settings form
 *
 * @author  ripper
 */
public class ConfigFrame extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(ConfigFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final Keyring keyring = PersistenceManager.getKeyring();
    /* when to take updates seriously */
    private boolean fullyInicialized;
    /* the active LaF when dialog is opened, needed for live-updating LaF skins */
    private LAF lafWhenLoaded;
    private DefaultComboBoxModel lafModel = new DefaultComboBoxModel();
    
    /** Creates new form ConfigFrame */
    public ConfigFrame() {
        initComponents();
        
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
        
        //add development panel in development versions
        if (Config.getLatestVersion().contains("beta")) {
            tabbedPane.addTab("Devel", develPanel);
        }
        
        //set tab mnemonics
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            L10N.setLocalizedText(tabbedPane, i, tabbedPane.getTitleAt(i));
        }
        
        //add LaFs to combo box
        for (LAF laf : LAF.values()) {
            if (ThemeManager.isLaFSupported(laf)) {
                lafModel.addElement(laf);
            }
        }
        
        //select current laf and remember it
        lafWhenLoaded = config.getLookAndFeel();
        if (lafModel.getIndexOf(lafWhenLoaded) >= 0) {
            lafModel.setSelectedItem(lafWhenLoaded);
        } else {
            logger.warning("Current LaF '" + lafWhenLoaded + "' not present in " +
                    "the list of available LaFs!");
        }
        
        //update themes according to current laf
        updateThemeComboBox();
        
        //update other components
        updateCountryCode();
        if (!NotificationIcon.isSupported()) {
            notificationAreaCheckBox.setSelected(false);
        }

        //end of init
        closeButton.requestFocusInWindow();
        fullyInicialized = true;
    }
    
    /** Update theme according to L&F */
    private void updateThemeComboBox() {
        themeComboBox.setEnabled(false);
        LAF laf = (LAF) lafComboBox.getSelectedItem();
        
        if (laf.equals(LAF.JGOODIES)) {
            ArrayList<String> themes = new ArrayList<String>();
            for (Object o : PlasticLookAndFeel.getInstalledThemes())
                themes.add(((PlasticTheme)o).getName());
            themeComboBox.setModel(new DefaultComboBoxModel(themes.toArray()));
            themeComboBox.setSelectedItem(config.getLafJGoodiesTheme());
            themeComboBox.setEnabled(true);
        }
        
        else if (laf.equals(LAF.SUBSTANCE)) {
            ArrayList<String> themes = new ArrayList<String>();
            new SubstanceLookAndFeel();
            for (SkinInfo skinInfo : SubstanceLookAndFeel.getAllSkins().values())
                themes.add(skinInfo.getDisplayName());
            themeComboBox.setModel(new DefaultComboBoxModel(themes.toArray()));
            themeComboBox.setSelectedItem(config.getLafSubstanceSkin());
            themeComboBox.setEnabled(true);
        }
    }
    
    /** Update country code according to country  */
    private void updateCountryCode() {
        String countryPrefix = countryPrefixTextField.getText();
        String countryCode = CountryPrefix.getCountryCode(countryPrefix);
        if (Nullator.isEmpty(countryCode)) {
            countryCodeLabel.setText(l10n.getString("ConfigFrame.unknown_state"));
        } else {
            countryCodeLabel.setText(
                    MessageFormat.format(l10n.getString("ConfigFrame.state"), countryCode));
        }
    }
    
    /** Reaction for operator key (login, password) change */
    private void updateKeyring() {
        Operator operator = operatorComboBox.getSelectedOperator();
        if (operator == null)
            return;
        
        String[] key = new String[]{loginTextField.getText(), 
            new String(passwordField.getPassword())};
        
        if (Nullator.isEmpty(key[0]) && Nullator.isEmpty(key[1])) {
            //if both empty, remove the key
            keyring.removeKey(operator.getName());
        } else {
            //else update/set the key
            keyring.putKey(operator.getName(), key);
        }
    }
    
    /** Reaction to proxy configuration change */
    private void updateProxy() {
        boolean useProxy = useProxyCheckBox.isSelected();
        boolean sameProxy = sameProxyCheckBox.isSelected();
        if (useProxy) {
            if (sameProxy) {
                ProxyManager.setProxy(httpProxyTextField.getText());
            } else {
                ProxyManager.setProxy(httpProxyTextField.getText(),
                        httpsProxyTextField.getText(),
                        socksProxyTextField.getText());
            }
        } else {
            ProxyManager.setProxy(null);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        config = PersistenceManager.getConfig();
        develPanel = new javax.swing.JPanel();
        forgetLayoutCheckBox = new javax.swing.JCheckBox();
        tabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        removeAccentsCheckBox = new javax.swing.JCheckBox();
        checkUpdatesCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        lafComboBox = new javax.swing.JComboBox();
        substanceWarning = new javax.swing.JLabel();
        lookLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        themeComboBox = new javax.swing.JComboBox();
        themeLabel = new javax.swing.JLabel();
        windowDecorationsCheckBox = new javax.swing.JCheckBox();
        windowCenteredCheckBox = new javax.swing.JCheckBox();
        toolbarVisibleCheckBox = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        notificationAreaCheckBox = new javax.swing.JCheckBox();
        tipsCheckBox = new javax.swing.JCheckBox();
        startMinimizedCheckBox = new javax.swing.JCheckBox();
        substanceWarningMark = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        useSenderIDCheckBox = new javax.swing.JCheckBox();
        senderNumberTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        senderNameTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        countryPrefixTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        operatorFilterTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        countryCodeLabel = new javax.swing.JLabel();
        demandDeliveryReportCheckBox = new javax.swing.JCheckBox();
        jPanel4 = new javax.swing.JPanel();
        operatorComboBox = new esmska.gui.OperatorComboBox();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        loginTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();
        jLabel12 = new javax.swing.JLabel();
        clearKeyringButton = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        reducedHistoryCheckBox = new javax.swing.JCheckBox();
        reducedHistorySpinner = new javax.swing.JSpinner();
        jLabel18 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        useProxyCheckBox = new javax.swing.JCheckBox();
        httpProxyTextField = new javax.swing.JTextField();
        sameProxyCheckBox = new javax.swing.JCheckBox();
        httpsProxyTextField = new javax.swing.JTextField();
        socksProxyTextField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        closeButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(forgetLayoutCheckBox, l10n.getString("ConfigFrame.forgetLayoutCheckBox.text")); // NOI18N
        forgetLayoutCheckBox.setToolTipText(l10n.getString("ConfigFrame.forgetLayoutCheckBox.toolTipText")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${forgetLayout}"), forgetLayoutCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout develPanelLayout = new javax.swing.GroupLayout(develPanel);
        develPanel.setLayout(develPanelLayout);
        develPanelLayout.setHorizontalGroup(
            develPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(develPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(forgetLayoutCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        develPanelLayout.setVerticalGroup(
            develPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(develPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(forgetLayoutCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(l10n.getString("ConfigFrame.title")); // NOI18N
        setIconImage(new ImageIcon(getClass().getResource(RES + "config-48.png")).getImage());
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeAccentsCheckBox, l10n.getString("ConfigFrame.removeAccentsCheckBox.text")); // NOI18N
        removeAccentsCheckBox.setToolTipText(l10n.getString("ConfigFrame.removeAccentsCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${removeAccents}"), removeAccentsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(checkUpdatesCheckBox, l10n.getString("ConfigFrame.checkUpdatesCheckBox.text")); // NOI18N
        checkUpdatesCheckBox.setToolTipText(l10n.getString("ConfigFrame.checkUpdatesCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${checkForUpdates}"), checkUpdatesCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeAccentsCheckBox)
                    .addComponent(checkUpdatesCheckBox))
                .addContainerGap(395, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(removeAccentsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkUpdatesCheckBox)
                .addContainerGap(339, Short.MAX_VALUE))
        );

        tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel1.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/config-16.png")), jPanel1); // NOI18N

        lafComboBox.setModel(lafModel);
        lafComboBox.setToolTipText(l10n.getString("ConfigFrame.lafComboBox.toolTipText")); // NOI18N
        lafComboBox.setRenderer(new LaFComboRenderer());
        lafComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lafComboBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(substanceWarning, l10n.getString("ConfigFrame.substanceWarning.text")); // NOI18N
        substanceWarning.setToolTipText(l10n.getString("ConfigFrame.substanceWarning.toolTipText")); // NOI18N
        substanceWarning.setVisible(JavaType.isOpenJDK());

        lookLabel.setLabelFor(lafComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(lookLabel, l10n.getString("ConfigFrame.lookLabel.text")); // NOI18N
        lookLabel.setToolTipText(lafComboBox.getToolTipText());

        org.openide.awt.Mnemonics.setLocalizedText(jLabel7, l10n.getString("ConfigFrame.jLabel7.text")); // NOI18N

        themeComboBox.setToolTipText(l10n.getString("ConfigFrame.themeComboBox.toolTipText")); // NOI18N
        themeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                themeComboBoxActionPerformed(evt);
            }
        });

        themeLabel.setLabelFor(themeComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(themeLabel, l10n.getString("ConfigFrame.themeLabel.text")); // NOI18N
        themeLabel.setToolTipText(themeComboBox.getToolTipText());

        org.openide.awt.Mnemonics.setLocalizedText(windowDecorationsCheckBox, l10n.getString("ConfigFrame.windowDecorationsCheckBox.text")); // NOI18N
        windowDecorationsCheckBox.setToolTipText(l10n.getString("ConfigFrame.windowDecorationsCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${lafWindowDecorated}"), windowDecorationsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(windowCenteredCheckBox, l10n.getString("ConfigFrame.windowCenteredCheckBox.text")); // NOI18N
        windowCenteredCheckBox.setToolTipText(l10n.getString("ConfigFrame.windowCenteredCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${startCentered}"), windowCenteredCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(toolbarVisibleCheckBox, l10n.getString("ConfigFrame.toolbarVisibleCheckBox.text")); // NOI18N
        toolbarVisibleCheckBox.setToolTipText(l10n.getString("ConfigFrame.toolbarVisibleCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${toolbarVisible}"), toolbarVisibleCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, "*"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(notificationAreaCheckBox, l10n.getString("ConfigFrame.notificationAreaCheckBox.text")); // NOI18N
        notificationAreaCheckBox.setToolTipText(l10n.getString("ConfigFrame.notificationAreaCheckBox.toolTipText")); // NOI18N
        notificationAreaCheckBox.setEnabled(NotificationIcon.isSupported());

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${notificationIconVisible}"), notificationAreaCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        notificationAreaCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                notificationAreaCheckBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(tipsCheckBox, l10n.getString("ConfigFrame.tipsCheckBox.text")); // NOI18N
        tipsCheckBox.setToolTipText(l10n.getString("ConfigFrame.tipsCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${showTips}"), tipsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(startMinimizedCheckBox, l10n.getString("ConfigFrame.startMinimizedCheckBox.text")); // NOI18N
        startMinimizedCheckBox.setToolTipText(l10n.getString("ConfigFrame.startMinimizedCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${startMinimized}"), startMinimizedCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, notificationAreaCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), startMinimizedCheckBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        org.openide.awt.Mnemonics.setLocalizedText(substanceWarningMark, "**"); // NOI18N
        substanceWarningMark.setVisible(JavaType.isOpenJDK());

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(lookLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(substanceWarningMark))
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 681, Short.MAX_VALUE)
                    .addComponent(windowCenteredCheckBox)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(startMinimizedCheckBox))
                    .addComponent(tipsCheckBox)
                    .addComponent(notificationAreaCheckBox)
                    .addComponent(toolbarVisibleCheckBox)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(themeLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(themeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(windowDecorationsCheckBox)
                    .addComponent(substanceWarning))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lafComboBox, themeComboBox});

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lookLabel, themeLabel});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lookLabel)
                    .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(substanceWarningMark))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(themeLabel)
                    .addComponent(themeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(windowDecorationsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(windowCenteredCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(toolbarVisibleCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(notificationAreaCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(startMinimizedCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tipsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 139, Short.MAX_VALUE)
                .addComponent(substanceWarning)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {lafComboBox, themeComboBox});

        tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel3.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/appearance-16.png")), jPanel3); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(useSenderIDCheckBox, l10n.getString("ConfigFrame.useSenderIDCheckBox.text")); // NOI18N
        useSenderIDCheckBox.setToolTipText(l10n.getString("ConfigFrame.useSenderIDCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${useSenderID}"), useSenderIDCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        senderNumberTextField.setColumns(13);
        senderNumberTextField.setToolTipText(l10n.getString("ConfigFrame.senderNumberTextField.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${senderNumber}"), senderNumberTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useSenderIDCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), senderNumberTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel1.setLabelFor(senderNumberTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, l10n.getString("ConfigFrame.jLabel1.text")); // NOI18N
        jLabel1.setToolTipText(senderNumberTextField.getToolTipText());

        senderNameTextField.setColumns(13);
        senderNameTextField.setToolTipText(l10n.getString("ConfigFrame.senderNameTextField.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${senderName}"), senderNameTextField, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useSenderIDCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), senderNameTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel3.setLabelFor(senderNameTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, l10n.getString("ConfigFrame.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(senderNameTextField.getToolTipText());

        countryPrefixTextField.setColumns(5);
        countryPrefixTextField.setToolTipText(l10n.getString("ConfigFrame.countryPrefixTextField.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${countryPrefix}"), countryPrefixTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        countryPrefixTextField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String prefix = countryPrefixTextField.getText();
                return prefix.length() == 0 || FormChecker.checkCountryPrefix(prefix);
            }
        });
        countryPrefixTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateCountryCode();
            }
        });

        jLabel2.setLabelFor(countryPrefixTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, l10n.getString("ConfigFrame.jLabel2.text")); // NOI18N
        jLabel2.setToolTipText(countryPrefixTextField.getToolTipText());

        operatorFilterTextField.setColumns(13);
        operatorFilterTextField.setToolTipText(l10n.getString("ConfigFrame.operatorFilterTextField.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${operatorFilter}"), operatorFilterTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel8.setLabelFor(operatorFilterTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel8, l10n.getString("ConfigFrame.jLabel8.text")); // NOI18N
        jLabel8.setToolTipText(operatorFilterTextField.getToolTipText());

        org.openide.awt.Mnemonics.setLocalizedText(countryCodeLabel, "<<(country: XX)>>"); // NOI18N
        countryCodeLabel.setToolTipText(l10n.getString("ConfigFrame.countryCodeLabel.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(demandDeliveryReportCheckBox, l10n.getString("ConfigFrame.demandDeliveryReportCheckBox.text")); // NOI18N
        demandDeliveryReportCheckBox.setToolTipText(l10n.getString("ConfigFrame.demandDeliveryReportCheckBox.toolTipText")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${demandDeliveryReport}"), demandDeliveryReportCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(useSenderIDCheckBox)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(countryPrefixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(countryCodeLabel))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(operatorFilterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(senderNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(senderNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(demandDeliveryReportCheckBox))
                .addContainerGap(222, Short.MAX_VALUE))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel3});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(countryPrefixTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(countryCodeLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(operatorFilterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(useSenderIDCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(senderNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(senderNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(demandDeliveryReportCheckBox)
                .addContainerGap(241, Short.MAX_VALUE))
        );

        tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel2.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/operator-16.png")), jPanel2); // NOI18N

        operatorComboBoxItemStateChanged(null);
        operatorComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                operatorComboBoxItemStateChanged(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel9, l10n.getString("ConfigFrame.jLabel9.text")); // NOI18N

        jLabel10.setLabelFor(operatorComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel10, l10n.getString("ConfigFrame.jLabel10.text")); // NOI18N
        jLabel10.setToolTipText(operatorComboBox.getToolTipText());

        loginTextField.setColumns(15);
        loginTextField.setToolTipText(l10n.getString("ConfigFrame.loginTextField.toolTipText")); // NOI18N
        loginTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateKeyring();
            }
        });

        jLabel11.setLabelFor(loginTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel11, l10n.getString("ConfigFrame.jLabel11.text")); // NOI18N
        jLabel11.setToolTipText(loginTextField.getToolTipText());

        passwordField.setColumns(15);
        passwordField.setToolTipText(l10n.getString("ConfigFrame.passwordField.toolTipText")); // NOI18N
        passwordField.enableInputMethods(true);
        passwordField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateKeyring();
            }
        });

        jLabel12.setLabelFor(passwordField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel12, l10n.getString("ConfigFrame.jLabel12.text")); // NOI18N
        jLabel12.setToolTipText(passwordField.getToolTipText());

        clearKeyringButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/clear-22.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(clearKeyringButton, l10n.getString("ConfigFrame.clearKeyringButton.text")); // NOI18N
        clearKeyringButton.setToolTipText(l10n.getString("ConfigFrame.clearKeyringButton.toolTipText")); // NOI18N
        clearKeyringButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearKeyringButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel13, l10n.getString("ConfigFrame.jLabel13.text")); // NOI18N
        jLabel13.setToolTipText(MessageFormat.format(l10n.getString("ConfigFrame.user_directory"),
            PersistenceManager.getUserDir().getAbsolutePath()));

    javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
    jPanel4.setLayout(jPanel4Layout);
    jPanel4Layout.setHorizontalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel4Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 681, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(passwordField))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(operatorComboBox, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loginTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addComponent(clearKeyringButton)
                .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, 681, Short.MAX_VALUE))
            .addContainerGap())
    );

    jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel12});

    jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {loginTextField, operatorComboBox, passwordField});

    jPanel4Layout.setVerticalGroup(
        jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel4Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jLabel9)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel10)
                .addComponent(operatorComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel11)
                .addComponent(loginTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel12)
                .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(clearKeyringButton)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 188, Short.MAX_VALUE)
            .addComponent(jLabel13)
            .addContainerGap())
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel4.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/keyring-16.png")), jPanel4); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(reducedHistoryCheckBox, l10n.getString("ConfigFrame.reducedHistoryCheckBox.text")); // NOI18N
    reducedHistoryCheckBox.setToolTipText(l10n.getString("ConfigFrame.reducedHistoryCheckBox.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${reducedHistory}"), reducedHistoryCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    reducedHistoryCheckBox.setText(reducedHistoryCheckBox.getText().replaceFirst("\\{0\\}.*$", "").trim());

    reducedHistorySpinner.setToolTipText(reducedHistoryCheckBox.getToolTipText());

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${reducedHistoryCount}"), reducedHistorySpinner, org.jdesktop.beansbinding.BeanProperty.create("value"));
    bindingGroup.addBinding(binding);
    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, reducedHistoryCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), reducedHistorySpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    ((SpinnerNumberModel)reducedHistorySpinner.getModel()).setMinimum(new Integer(0));

    org.openide.awt.Mnemonics.setLocalizedText(jLabel18, "<<days.>>"); // NOI18N
    jLabel18.setToolTipText(reducedHistoryCheckBox.getToolTipText());
    jLabel18.setText(l10n.getString("ConfigFrame.reducedHistoryCheckBox.text").replaceFirst("^.*\\{0\\}", "").trim());

    javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
    jPanel6.setLayout(jPanel6Layout);
    jPanel6Layout.setHorizontalGroup(
        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel6Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(reducedHistoryCheckBox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(reducedHistorySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel18)
            .addContainerGap(135, Short.MAX_VALUE))
    );
    jPanel6Layout.setVerticalGroup(
        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel6Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(reducedHistoryCheckBox)
                .addComponent(reducedHistorySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel18))
            .addContainerGap())
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel6.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/lock-16.png")), jPanel6); // NOI18N

    org.openide.awt.Mnemonics.setLocalizedText(useProxyCheckBox, l10n.getString("ConfigFrame.useProxyCheckBox.text")); // NOI18N
    useProxyCheckBox.setToolTipText(l10n.getString("ConfigFrame.useProxyCheckBox.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${useProxy}"), useProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);

    useProxyCheckBox.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            useProxyCheckBoxItemStateChanged(evt);
        }
    });

    httpProxyTextField.setColumns(20);
    httpProxyTextField.setToolTipText(l10n.getString("ConfigFrame.httpProxyTextField.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${httpProxy}"), httpProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useProxyCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), httpProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    httpProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            updateProxy();
        }
    });

    org.openide.awt.Mnemonics.setLocalizedText(sameProxyCheckBox, l10n.getString("ConfigFrame.sameProxyCheckBox.text")); // NOI18N
    sameProxyCheckBox.setToolTipText(l10n.getString("ConfigFrame.sameProxyCheckBox.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${sameProxy}"), sameProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
    bindingGroup.addBinding(binding);
    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useProxyCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), sameProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    sameProxyCheckBox.addItemListener(new java.awt.event.ItemListener() {
        public void itemStateChanged(java.awt.event.ItemEvent evt) {
            sameProxyCheckBoxItemStateChanged(evt);
        }
    });

    httpsProxyTextField.setToolTipText(l10n.getString("ConfigFrame.httpsProxyTextField.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${httpsProxy}"), httpsProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, sameProxyCheckBox, org.jdesktop.beansbinding.ELProperty.create("${enabled && !selected}"), httpsProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    httpsProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            updateProxy();
        }
    });

    socksProxyTextField.setToolTipText(l10n.getString("ConfigFrame.socksProxyTextField.toolTipText")); // NOI18N

    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${socksProxy}"), socksProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
    bindingGroup.addBinding(binding);
    binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, sameProxyCheckBox, org.jdesktop.beansbinding.ELProperty.create("${enabled && !selected}"), socksProxyTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
    bindingGroup.addBinding(binding);

    socksProxyTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
        @Override
        public void onUpdate(DocumentEvent e) {
            updateProxy();
        }
    });

    jLabel14.setLabelFor(httpProxyTextField);
    org.openide.awt.Mnemonics.setLocalizedText(jLabel14, l10n.getString("ConfigFrame.jLabel14.text")); // NOI18N
    jLabel14.setToolTipText(httpProxyTextField.getToolTipText());

    jLabel15.setLabelFor(httpsProxyTextField);
    org.openide.awt.Mnemonics.setLocalizedText(jLabel15, l10n.getString("ConfigFrame.jLabel15.text")); // NOI18N
    jLabel15.setToolTipText(httpsProxyTextField.getToolTipText());

    jLabel16.setLabelFor(socksProxyTextField);
    org.openide.awt.Mnemonics.setLocalizedText(jLabel16, l10n.getString("ConfigFrame.jLabel16.text")); // NOI18N
    jLabel16.setToolTipText(socksProxyTextField.getToolTipText());

    org.openide.awt.Mnemonics.setLocalizedText(jLabel17, l10n.getString("ConfigFrame.jLabel17.text")); // NOI18N

    javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
    jPanel5.setLayout(jPanel5Layout);
    jPanel5Layout.setHorizontalGroup(
        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel5Layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(useProxyCheckBox)
                .addGroup(jPanel5Layout.createSequentialGroup()
                    .addGap(21, 21, 21)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addGap(12, 12, 12)
                            .addComponent(sameProxyCheckBox))
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addComponent(jLabel14)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(httpProxyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addComponent(jLabel15)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(httpsProxyTextField))
                        .addGroup(jPanel5Layout.createSequentialGroup()
                            .addComponent(jLabel16)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(socksProxyTextField))))
                .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, 681, Short.MAX_VALUE))
            .addContainerGap())
    );

    jPanel5Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel14, jLabel15, jLabel16});

    jPanel5Layout.setVerticalGroup(
        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(jPanel5Layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(useProxyCheckBox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel14)
                .addComponent(httpProxyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(sameProxyCheckBox)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel15)
                .addComponent(httpsProxyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel16)
                .addComponent(socksProxyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 243, Short.MAX_VALUE)
            .addComponent(jLabel17)
            .addContainerGap())
    );

    tabbedPane.addTab(l10n.getString("ConfigFrame.jPanel5.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/connection-16.png")), jPanel5); // NOI18N

    closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
    org.openide.awt.Mnemonics.setLocalizedText(closeButton, l10n.getString("Close_")); // NOI18N
    closeButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            closeButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 710, Short.MAX_VALUE)
                .addComponent(closeButton))
            .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(closeButton)
            .addContainerGap())
    );

    bindingGroup.bind();

    pack();
    }// </editor-fold>//GEN-END:initComponents
        
    private void themeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_themeComboBoxActionPerformed
        LAF laf = (LAF) lafComboBox.getSelectedItem();

        if (laf.equals(LAF.JGOODIES)) {
            config.setLafJGoodiesTheme((String) themeComboBox.getSelectedItem());
        } else if (laf.equals(LAF.SUBSTANCE)) {
            config.setLafSubstanceSkin((String) themeComboBox.getSelectedItem());
        }

        //update skin in realtime
        if (fullyInicialized && lafWhenLoaded.equals(lafComboBox.getSelectedItem())) {
            try {
                ThemeManager.setLaF();
                SwingUtilities.updateComponentTreeUI(MainFrame.getInstance());
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Problem while live-updating the look&feel skin", ex);
            }
        }
    }//GEN-LAST:event_themeComboBoxActionPerformed
    
    private void lafComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lafComboBoxActionPerformed
        if (!fullyInicialized) {
            return;
        }
        LAF laf = (LAF) lafComboBox.getSelectedItem();
        config.setLookAndFeel(laf);
        updateThemeComboBox();
    }//GEN-LAST:event_lafComboBoxActionPerformed
                            
    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_closeButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        //check validity of country prefix
        String prefix = countryPrefixTextField.getText();
        if (prefix.length() > 0 && !FormChecker.checkCountryPrefix(prefix)) {
            config.setCountryPrefix("");
        }
    }//GEN-LAST:event_formWindowClosed

    private void operatorComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_operatorComboBoxItemStateChanged
        Operator operator = operatorComboBox.getSelectedOperator();
        String[] key = keyring.getKey(operator != null ? operator.getName() : null);
        if (key == null) {
            loginTextField.setText(null);
            passwordField.setText(null);
        } else {
            loginTextField.setText(key[0]);
            passwordField.setText(key[1]);
        }
    }//GEN-LAST:event_operatorComboBoxItemStateChanged

    private void clearKeyringButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearKeyringButtonActionPerformed
        String deleteOption = l10n.getString("Delete");
        String cancelOption = l10n.getString("Cancel");
        Object[] options = DialogButtonSorter.sortOptions(cancelOption, deleteOption);
        String message = l10n.getString("ConfigFrame.remove_credentials");

        //show dialog
        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, options, cancelOption);
        JDialog dialog = pane.createDialog(ConfigFrame.this, null);
        dialog.setResizable(true);
        MacUtils.setDocumentModalDialog(dialog);
        dialog.pack();
        dialog.setVisible(true);

        //return if should not delete
        if (!deleteOption.equals(pane.getValue())) {
            return;
        }
        
        keyring.clearKeys();
        operatorComboBoxItemStateChanged(null);
    }//GEN-LAST:event_clearKeyringButtonActionPerformed

    private void useProxyCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_useProxyCheckBoxItemStateChanged
        updateProxy();
    }//GEN-LAST:event_useProxyCheckBoxItemStateChanged

    private void sameProxyCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sameProxyCheckBoxItemStateChanged
        updateProxy();
    }//GEN-LAST:event_sameProxyCheckBoxItemStateChanged

private void notificationAreaCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_notificationAreaCheckBoxActionPerformed
        if (notificationAreaCheckBox.isSelected()) {
            NotificationIcon.install();
        } else {
            NotificationIcon.uninstall();
        }
}//GEN-LAST:event_notificationAreaCheckBoxActionPerformed
    
    private class LaFComboRenderer extends DefaultListCellRenderer {
        private final ListCellRenderer lafRenderer = new JList().getCellRenderer();
        private final String LAF_SYSTEM = l10n.getString("ConfigFrame.system_look");
        private final String LAF_CROSSPLATFORM = l10n.getString("ConfigFrame.multiplatform_look");
        private final String LAF_GTK = "GTK";
        private final String LAF_JGOODIES = "JGoodies";
        private final String LAF_SUBSTANCE = "Substance";
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) lafRenderer.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            
            if (!(value instanceof LAF)) {
                return label;
            }
            
            LAF laf = (LAF) value;
            String name = l10n.getString("ConfigFrame.unknown_look");
            switch (laf) {
                case SYSTEM: 
                    name = LAF_SYSTEM;
                    break;
                case CROSSPLATFORM:
                    name = LAF_CROSSPLATFORM; 
                    break;
                case GTK:
                    name = LAF_GTK;
                    break;
                case JGOODIES: 
                    name = LAF_JGOODIES;
                    break;
                case SUBSTANCE: 
                    name = LAF_SUBSTANCE;
                    break;
                default: 
                    logger.severe("Uknown LaF: " + laf);
                    break;
            }
            label.setText(name);
            
            return label;
        }
        
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkUpdatesCheckBox;
    private javax.swing.JButton clearKeyringButton;
    private javax.swing.JButton closeButton;
    private esmska.data.Config config;
    private javax.swing.JLabel countryCodeLabel;
    private javax.swing.JTextField countryPrefixTextField;
    private javax.swing.JCheckBox demandDeliveryReportCheckBox;
    private javax.swing.JPanel develPanel;
    private javax.swing.JCheckBox forgetLayoutCheckBox;
    private javax.swing.JTextField httpProxyTextField;
    private javax.swing.JTextField httpsProxyTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JComboBox lafComboBox;
    private javax.swing.JTextField loginTextField;
    private javax.swing.JLabel lookLabel;
    private javax.swing.JCheckBox notificationAreaCheckBox;
    private esmska.gui.OperatorComboBox operatorComboBox;
    private javax.swing.JTextField operatorFilterTextField;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JCheckBox reducedHistoryCheckBox;
    private javax.swing.JSpinner reducedHistorySpinner;
    private javax.swing.JCheckBox removeAccentsCheckBox;
    private javax.swing.JCheckBox sameProxyCheckBox;
    private javax.swing.JTextField senderNameTextField;
    private javax.swing.JTextField senderNumberTextField;
    private javax.swing.JTextField socksProxyTextField;
    private javax.swing.JCheckBox startMinimizedCheckBox;
    private javax.swing.JLabel substanceWarning;
    private javax.swing.JLabel substanceWarningMark;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JComboBox themeComboBox;
    private javax.swing.JLabel themeLabel;
    private javax.swing.JCheckBox tipsCheckBox;
    private javax.swing.JCheckBox toolbarVisibleCheckBox;
    private javax.swing.JCheckBox useProxyCheckBox;
    private javax.swing.JCheckBox useSenderIDCheckBox;
    private javax.swing.JCheckBox windowCenteredCheckBox;
    private javax.swing.JCheckBox windowDecorationsCheckBox;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    
}
