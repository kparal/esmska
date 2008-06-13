/*
 * ConfigFrame.java
 *
 * Created on 20. červenec 2007, 18:59
 */

package esmska.gui;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import esmska.*;
import esmska.data.Config;
import esmska.data.CountryPrefix;
import esmska.data.Icons;
import esmska.data.Keyring;
import esmska.operators.Operator;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.SkinInfo;
import esmska.persistence.PersistenceManager;
import esmska.transfer.ProxyManager;
import esmska.utils.AbstractDocumentListener;
import esmska.utils.Nullator;
import java.awt.Toolkit;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.InputVerifier;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/** Configure settings form
 *
 * @author  ripper
 */
public class ConfigFrame extends javax.swing.JFrame {
    private static final Logger logger = Logger.getLogger(ConfigFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final Keyring keyring = PersistenceManager.getKeyring();
    /* when to take updates seriously */
    private boolean fullyInicialized;
    private final String LAF_SYSTEM = "Systémový";
    private final String LAF_CROSSPLATFORM = "Meziplatformní";
    private final String LAF_GTK = "GTK";
    private final String LAF_JGOODIES = "JGoodies";
    private final String LAF_SUBSTANCE = "Substance";
    /* the active LaF when dialog is opened, needed for live-updating LaF skins */
    private String lafWhenLoaded;
    
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
        
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_O);
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_V);
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_P);
        tabbedPane.setMnemonicAt(3, KeyEvent.VK_H);
        tabbedPane.setMnemonicAt(4, KeyEvent.VK_S); 
        tabbedPane.setMnemonicAt(5, KeyEvent.VK_N);
        tabbedPane.setIconAt(0, new ImageIcon(getClass().getResource(RES + "config-16.png")));
        tabbedPane.setIconAt(1, new ImageIcon(getClass().getResource(RES + "appearance-small.png")));
        tabbedPane.setIconAt(2, Icons.OPERATOR_DEFAULT);
        tabbedPane.setIconAt(3, new ImageIcon(getClass().getResource(RES + "keyring-16.png")));
        tabbedPane.setIconAt(4, new ImageIcon(getClass().getResource(RES + "lock-16.png")));
        tabbedPane.setIconAt(5, new ImageIcon(getClass().getResource(RES + "connection-16.png")));
        closeButton.requestFocusInWindow();
        
        lafComboBox.setModel(new DefaultComboBoxModel(new String[] {
            LAF_SYSTEM, LAF_CROSSPLATFORM, LAF_GTK, LAF_JGOODIES, LAF_SUBSTANCE}));
        if (config.getLookAndFeel().equals(ThemeManager.LAF_SYSTEM))
            lafComboBox.setSelectedItem(LAF_SYSTEM);
        else if (config.getLookAndFeel().equals(ThemeManager.LAF_CROSSPLATFORM))
            lafComboBox.setSelectedItem(LAF_CROSSPLATFORM);
        else if (config.getLookAndFeel().equals(ThemeManager.LAF_GTK))
            lafComboBox.setSelectedItem(LAF_GTK);
        else if (config.getLookAndFeel().equals(ThemeManager.LAF_JGOODIES))
            lafComboBox.setSelectedItem(LAF_JGOODIES);
        else if (config.getLookAndFeel().equals(ThemeManager.LAF_SUBSTANCE))
            lafComboBox.setSelectedItem(LAF_SUBSTANCE);
        lafWhenLoaded = (String) lafComboBox.getSelectedItem();
        
        updateThemeComboBox();

        if (!NotificationIcon.isSupported()) {
            notificationAreaCheckBox.setSelected(false);
        }
        
        fullyInicialized = true;
    }
    
    /** Update theme according to L&F */
    private void updateThemeComboBox() {
        themeComboBox.setEnabled(false);
        String laf = (String) lafComboBox.getSelectedItem();
        
        if (laf.equals(LAF_JGOODIES)) {
            ArrayList<String> themes = new ArrayList<String>();
            for (Object o : PlasticLookAndFeel.getInstalledThemes())
                themes.add(((PlasticTheme)o).getName());
            themeComboBox.setModel(new DefaultComboBoxModel(themes.toArray()));
            themeComboBox.setSelectedItem(config.getLafJGoodiesTheme());
            themeComboBox.setEnabled(true);
        }
        
        else if (laf.equals(LAF_SUBSTANCE)) {
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
            countryCodeLabel.setText("(neznámý stát)");
        } else {
            countryCodeLabel.setText("(stát: " + countryCode + ")");
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
        rememberLayoutCheckBox = new javax.swing.JCheckBox();
        tabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        removeAccentsCheckBox = new javax.swing.JCheckBox();
        checkUpdatesCheckBox = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        lafComboBox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        themeComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        windowDecorationsCheckBox = new javax.swing.JCheckBox();
        windowCenteredCheckBox = new javax.swing.JCheckBox();
        toolbarVisibleCheckBox = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        notificationAreaCheckBox = new javax.swing.JCheckBox();
        tipsCheckBox = new javax.swing.JCheckBox();
        startMinimizedCheckBox = new javax.swing.JCheckBox();
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

        rememberLayoutCheckBox.setMnemonic('r');
        rememberLayoutCheckBox.setText("Pamatovat rozvržení formuláře");
        rememberLayoutCheckBox.setToolTipText("<html>\nPoužije aktuální rozměry programu a prvků formuláře při příštím spuštění programu\n</html>");

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${rememberLayout}"), rememberLayoutCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout develPanelLayout = new javax.swing.GroupLayout(develPanel);
        develPanel.setLayout(develPanelLayout);
        develPanelLayout.setHorizontalGroup(
            develPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(develPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rememberLayoutCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        develPanelLayout.setVerticalGroup(
            develPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(develPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(rememberLayoutCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Nastavení - Esmska");
        setIconImage(new ImageIcon(getClass().getResource(RES + "config-48.png")).getImage());
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        removeAccentsCheckBox.setMnemonic('d');
        removeAccentsCheckBox.setText("Ze zpráv odstraňovat diakritiku");
        removeAccentsCheckBox.setToolTipText("<html>\nPřed odesláním zprávy z ní odstraní všechna diakritická znaménka\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${removeAccents}"), removeAccentsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        checkUpdatesCheckBox.setMnemonic('n');
        checkUpdatesCheckBox.setText("Kontrolovat po startu novou verzi programu");
        checkUpdatesCheckBox.setToolTipText("<html>\nPo spuštění programu zkontrolovat, zda nevyšla novější<br>\nverze programu, a případně upozornit ve stavovém řádku\n</html>");

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
                .addContainerGap(352, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(removeAccentsCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkUpdatesCheckBox)
                .addContainerGap(297, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Obecné", jPanel1);

        lafComboBox.setToolTipText("<html>\nUmožní vám změnit vzhled programu\n</html>");
        lafComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lafComboBoxActionPerformed(evt);
            }
        });

        jLabel4.setDisplayedMnemonic('d');
        jLabel4.setLabelFor(lafComboBox);
        jLabel4.setText("Vzhled:");
        jLabel4.setToolTipText(lafComboBox.getToolTipText());

        jLabel7.setText("<html><i>\n* Pro projevení změn je nutný restart programu!\n</i></html>");

        themeComboBox.setToolTipText("<html>\nBarevná schémata pro zvolený vzhled\n</html>");
        themeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                themeComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setDisplayedMnemonic('m');
        jLabel6.setLabelFor(themeComboBox);
        jLabel6.setText("Motiv:");
        jLabel6.setToolTipText(themeComboBox.getToolTipText());

        windowDecorationsCheckBox.setMnemonic('k');
        windowDecorationsCheckBox.setText("Použít vzhled i na okraje oken *");
        windowDecorationsCheckBox.setToolTipText("<html>\nZda má místo operačního systému vykreslovat<br>\nrámečky oken zvolený vzhled\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${lafWindowDecorated}"), windowDecorationsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        windowCenteredCheckBox.setMnemonic('u');
        windowCenteredCheckBox.setText("Spustit program uprostřed obrazovky");
        windowCenteredCheckBox.setToolTipText("<html>Zda-li nechat umístění okna programu na operačním systému,<br>\nnebo ho umístit vždy doprostřed obrazovky</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${startCentered}"), windowCenteredCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        toolbarVisibleCheckBox.setMnemonic('n');
        toolbarVisibleCheckBox.setText("Zobrazit panel nástrojů");
        toolbarVisibleCheckBox.setToolTipText("<html>\nZobrazit panel nástrojů, který umožňuje rychlejší ovládání myší některých akcí\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${toolbarVisible}"), toolbarVisibleCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        jLabel5.setText("*");

        notificationAreaCheckBox.setMnemonic('o');
        notificationAreaCheckBox.setText("Umístit ikonu do oznamovací oblasti");
        notificationAreaCheckBox.setToolTipText("<html>\nZobrazit ikonu v oznamovací oblasti správce oken (tzv. <i>system tray</i>).<br>\n<br>\nPozor, u moderních kompozitních správců oken (Compiz, Beryl, ...) je<br>\ntato funkce dostupná až od Java 6 Update 10.\n</html>");
        notificationAreaCheckBox.setEnabled(NotificationIcon.isSupported());

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${notificationIconVisible}"), notificationAreaCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        notificationAreaCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                notificationAreaCheckBoxActionPerformed(evt);
            }
        });

        tipsCheckBox.setMnemonic('t');
        tipsCheckBox.setText("Po spuštění zobrazit tip programu");
        tipsCheckBox.setToolTipText("<html>\nPo spuštění programu zobrazit ve stavovém řádku<br>\nnáhodný tip ohledně práce s programem\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${showTips}"), tipsCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        startMinimizedCheckBox.setMnemonic('y');
        startMinimizedCheckBox.setText("Po startu skrýt program do ikony");
        startMinimizedCheckBox.setToolTipText("<html>\nProgram bude okamžitě po spuštění schován<br>\ndo ikony v oznamovací oblasti\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${startMinimized}"), startMinimizedCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, notificationAreaCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected && enabled}"), startMinimizedCheckBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(windowCenteredCheckBox)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(startMinimizedCheckBox))
                    .addComponent(tipsCheckBox)
                    .addComponent(notificationAreaCheckBox)
                    .addComponent(toolbarVisibleCheckBox)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(themeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5))))
                    .addComponent(windowDecorationsCheckBox)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {lafComboBox, themeComboBox});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lafComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 118, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {lafComboBox, themeComboBox});

        tabbedPane.addTab("Vzhled", jPanel3);

        useSenderIDCheckBox.setMnemonic('d');
        useSenderIDCheckBox.setText("Připojovat ke zprávě podpis odesilatele");
        useSenderIDCheckBox.setToolTipText("<html>\nPři připojení podpisu přijde SMS adresátovi ze zadaného čísla<br>\na podepsaná daným jménem. Tuto funkci podporují pouze někteří operátoři.\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${useSenderID}"), useSenderIDCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        senderNumberTextField.setColumns(13);
        senderNumberTextField.setToolTipText("Číslo odesilatele v mezinárodním formátu (začínající na znak \"+\")");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${senderNumber}"), senderNumberTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useSenderIDCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), senderNumberTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel1.setDisplayedMnemonic('l');
        jLabel1.setLabelFor(senderNumberTextField);
        jLabel1.setText("Číslo:");
        jLabel1.setToolTipText(senderNumberTextField.getToolTipText());

        senderNameTextField.setColumns(13);
        senderNameTextField.setToolTipText("<html>\nVyplněné jméno zabírá ve zprávě místo, avšak není vidět,<br>\ntakže obarvování textu zprávy a ukazatel počtu sms<br>\nnebudou zdánlivě spolu souhlasit\n</html>\n\n");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${senderName}"), senderNameTextField, org.jdesktop.beansbinding.BeanProperty.create("text_ON_ACTION_OR_FOCUS_LOST"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useSenderIDCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), senderNameTextField, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        jLabel3.setDisplayedMnemonic('m');
        jLabel3.setLabelFor(senderNameTextField);
        jLabel3.setText("Jméno:");
        jLabel3.setToolTipText(senderNameTextField.getToolTipText());

        countryPrefixTextField.setColumns(5);
        countryPrefixTextField.setToolTipText("<html>\nMezinárodní předčíslí země, začínající na znak \"+\".<br>\nPři vyplnění se dané předčíslí bude předpokládat u všech čísel, které nebudou<br>\nzadány v mezinárodním formátu. Taktéž se zkrátí zobrazení těchto čísel v mnoha popiscích.\n</html>");

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

        jLabel2.setDisplayedMnemonic('d');
        jLabel2.setLabelFor(countryPrefixTextField);
        jLabel2.setText("Výchozí předčíslí země:");
        jLabel2.setToolTipText(countryPrefixTextField.getToolTipText());

        operatorFilterTextField.setColumns(13);
        operatorFilterTextField.setToolTipText("<html>\nV seznamu operátorů budou zobrazeni pouze ti, kteří budou<br>\nobsahovat ve svém názvu vzor zadaný v tomto poli. Jednoduše tak<br>\nlze například zobrazit pouze operátory pro zemi XX zadáním vzoru [XX].<br>\nLze zadat více vzorů oddělených čárkou. Bere se ohled na velikost písmen.\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${operatorFilter}"), operatorFilterTextField, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel8.setDisplayedMnemonic('t');
        jLabel8.setLabelFor(operatorFilterTextField);
        jLabel8.setText("Zobrazovat pouze brány operátorů mající v názvu:");
        jLabel8.setToolTipText(operatorFilterTextField.getToolTipText());

        countryCodeLabel.setText("(stát: XX)");
        countryCodeLabel.setToolTipText("Kód státu, pro který jste vyplnili telefonní předčíslí");

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
                            .addComponent(senderNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
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
                .addContainerGap(224, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Operátoři", jPanel2);

        operatorComboBoxItemStateChanged(null);
        operatorComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                operatorComboBoxItemStateChanged(evt);
            }
        });

        jLabel9.setText("<html>\nZde si můžete nastavit své přihlašovací údaje pro operátory, kteří to vyžadují.\n</html>");

        jLabel10.setDisplayedMnemonic('r');
        jLabel10.setLabelFor(operatorComboBox);
        jLabel10.setText("Operátor:");
        jLabel10.setToolTipText(operatorComboBox.getToolTipText());

        loginTextField.setColumns(15);
        loginTextField.setToolTipText("Uživatelské jméno potřebné k přihlášení k webové bráně operátora");
        loginTextField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateKeyring();
            }
        });

        jLabel11.setDisplayedMnemonic('u');
        jLabel11.setLabelFor(loginTextField);
        jLabel11.setText("Uživatelské jméno:");
        jLabel11.setToolTipText(loginTextField.getToolTipText());

        passwordField.setColumns(15);
        passwordField.setToolTipText("Heslo příslušející k danému uživatelskému jménu");
        passwordField.enableInputMethods(true);
        passwordField.getDocument().addDocumentListener(new AbstractDocumentListener() {
            @Override
            public void onUpdate(DocumentEvent e) {
                updateKeyring();
            }
        });

        jLabel12.setDisplayedMnemonic('s');
        jLabel12.setLabelFor(passwordField);
        jLabel12.setText("Heslo:");
        jLabel12.setToolTipText(passwordField.getToolTipText());

        clearKeyringButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/clear-22.png"))); // NOI18N
        clearKeyringButton.setMnemonic('d');
        clearKeyringButton.setText("Odstranit všechny přihlašovací údaje");
        clearKeyringButton.setToolTipText("Vymaže uživatelské jména a hesla u všech operátorů");
        clearKeyringButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearKeyringButtonActionPerformed(evt);
            }
        });

        jLabel13.setText("<html><i>\nAčkoliv se hesla ukládají šifrovaně, lze se k původnímu obsahu dostat. Důrazně doporučujeme nastavit si přístupová práva tak, aby k <u>adresáři s konfiguračními soubory programu</u> neměl přístup žádný jiný uživatel.\n</i></html>");
        jLabel13.setToolTipText("<html>Uživatelský adresář programu:<br>"
            + PersistenceManager.getUserDir().getAbsolutePath()
            + "</html>");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
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
                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel10, jLabel11, jLabel12});

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 161, Short.MAX_VALUE)
                .addComponent(jLabel13)
                .addContainerGap())
        );

        tabbedPane.addTab("Přihlašovací údaje", jPanel4);

        reducedHistoryCheckBox.setMnemonic('m');
        reducedHistoryCheckBox.setText("Omezit historii odeslaných zpráv pouze na posledních");
        reducedHistoryCheckBox.setToolTipText("<html>\nPři ukončení programu se uloží historie odeslaných zpráv<BR>\npouze za zvolené poslední období\n</html>");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${reducedHistory}"), reducedHistoryCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        reducedHistorySpinner.setToolTipText(reducedHistoryCheckBox.getToolTipText());

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${reducedHistoryCount}"), reducedHistorySpinner, org.jdesktop.beansbinding.BeanProperty.create("value"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, reducedHistoryCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), reducedHistorySpinner, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        ((SpinnerNumberModel)reducedHistorySpinner.getModel()).setMinimum(new Integer(0));

        jLabel18.setText("dní.");

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
                .addContainerGap(188, Short.MAX_VALUE))
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

        tabbedPane.addTab("Soukromí", jPanel6);

        useProxyCheckBox.setMnemonic('x');
        useProxyCheckBox.setText("Používat proxy server *");
        useProxyCheckBox.setToolTipText("Zda pro připojení používat proxy server");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${useProxy}"), useProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);

        useProxyCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                useProxyCheckBoxItemStateChanged(evt);
            }
        });

        httpProxyTextField.setColumns(20);
        httpProxyTextField.setToolTipText("<html>\nAdresa HTTP proxy serveru ve formátu \"host\" nebo \"host:port\".\nNapříklad: proxy.firma.com:80\n</html>");

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

        sameProxyCheckBox.setMnemonic('t');
        sameProxyCheckBox.setText("Pro všechny protokoly použít tuto proxy");
        sameProxyCheckBox.setToolTipText("Pro všechny protokoly se použije adresa zadaná v políčku \"HTTP proxy\".");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, config, org.jdesktop.beansbinding.ELProperty.create("${sameProxy}"), sameProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("selected"));
        bindingGroup.addBinding(binding);
        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ, useProxyCheckBox, org.jdesktop.beansbinding.ELProperty.create("${selected}"), sameProxyCheckBox, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        sameProxyCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sameProxyCheckBoxItemStateChanged(evt);
            }
        });

        httpsProxyTextField.setToolTipText("<html>\nAdresa HTTPS proxy serveru ve formátu \"host\" nebo \"host:port\".\nNapříklad: proxy.firma.com:443\n</html>");

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

        socksProxyTextField.setToolTipText("<html>\nAdresa SOCKS proxy serveru ve formátu \"host\" nebo \"host:port\".\nNapříklad: proxy.firma.com:1080\n</html>");

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

        jLabel14.setDisplayedMnemonic('t');
        jLabel14.setLabelFor(httpProxyTextField);
        jLabel14.setText("HTTP proxy:");
        jLabel14.setToolTipText(httpProxyTextField.getToolTipText());

        jLabel15.setDisplayedMnemonic('s');
        jLabel15.setLabelFor(httpsProxyTextField);
        jLabel15.setText("HTTPS proxy:");
        jLabel15.setToolTipText(httpsProxyTextField.getToolTipText());

        jLabel16.setDisplayedMnemonic('c');
        jLabel16.setLabelFor(socksProxyTextField);
        jLabel16.setText("SOCKS proxy:");
        jLabel16.setToolTipText(socksProxyTextField.getToolTipText());

        jLabel17.setText("<html><i>\n* Pokud nejste k Internetu připojeni přímo, je nutné, aby vaše proxy fungovala jako SOCKS proxy server.\n</i></html>");

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
                    .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 201, Short.MAX_VALUE)
                .addComponent(jLabel17)
                .addContainerGap())
        );

        tabbedPane.addTab("Připojení", jPanel5);

        closeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/esmska/resources/close-22.png"))); // NOI18N
        closeButton.setMnemonic('z');
        closeButton.setText("Zavřít");
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
                    .addComponent(tabbedPane)
                    .addComponent(closeButton))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(closeButton)
                .addContainerGap())
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents
        
    private void themeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_themeComboBoxActionPerformed
        String laf = (String) lafComboBox.getSelectedItem();

        if (laf.equals(LAF_JGOODIES)) {
            config.setLafJGoodiesTheme((String) themeComboBox.getSelectedItem());
        } else if (laf.equals(LAF_SUBSTANCE)) {
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
        if (!fullyInicialized)
            return;
        String laf = (String) lafComboBox.getSelectedItem();
        
        if (laf.equals(LAF_SYSTEM))
            config.setLookAndFeel(ThemeManager.LAF_SYSTEM);
        else if (laf.equals(LAF_CROSSPLATFORM))
            config.setLookAndFeel(ThemeManager.LAF_CROSSPLATFORM);
        else if (laf.equals(LAF_GTK))
            config.setLookAndFeel(ThemeManager.LAF_GTK);
        else if (laf.equals(LAF_JGOODIES))
            config.setLookAndFeel(ThemeManager.LAF_JGOODIES);
        else if (laf.equals(LAF_SUBSTANCE))
            config.setLookAndFeel(ThemeManager.LAF_SUBSTANCE);
        
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
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkUpdatesCheckBox;
    private javax.swing.JButton clearKeyringButton;
    private javax.swing.JButton closeButton;
    private esmska.data.Config config;
    private javax.swing.JLabel countryCodeLabel;
    private javax.swing.JTextField countryPrefixTextField;
    private javax.swing.JPanel develPanel;
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
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
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
    private javax.swing.JCheckBox notificationAreaCheckBox;
    private esmska.gui.OperatorComboBox operatorComboBox;
    private javax.swing.JTextField operatorFilterTextField;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JCheckBox reducedHistoryCheckBox;
    private javax.swing.JSpinner reducedHistorySpinner;
    private javax.swing.JCheckBox rememberLayoutCheckBox;
    private javax.swing.JCheckBox removeAccentsCheckBox;
    private javax.swing.JCheckBox sameProxyCheckBox;
    private javax.swing.JTextField senderNameTextField;
    private javax.swing.JTextField senderNumberTextField;
    private javax.swing.JTextField socksProxyTextField;
    private javax.swing.JCheckBox startMinimizedCheckBox;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JComboBox themeComboBox;
    private javax.swing.JCheckBox tipsCheckBox;
    private javax.swing.JCheckBox toolbarVisibleCheckBox;
    private javax.swing.JCheckBox useProxyCheckBox;
    private javax.swing.JCheckBox useSenderIDCheckBox;
    private javax.swing.JCheckBox windowCenteredCheckBox;
    private javax.swing.JCheckBox windowDecorationsCheckBox;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
    
}
