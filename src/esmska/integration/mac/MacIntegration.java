/*
 * @(#) MacIntegration.java     10-05-2008  18:59
 */
package esmska.integration.mac;

import com.apple.eawt.Application;
import com.apple.eio.FileManager;
import esmska.Context;
import esmska.gui.MainFrame;
import esmska.gui.NotificationIcon;
import esmska.gui.ThemeManager;
import esmska.integration.ActionBean;
import esmska.integration.IntegrationAdapter;
import esmska.integration.mac.handler.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.PopupMenu;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Integration for Mac OS X.
 *
 * @author Marian Boucek
 */
public class MacIntegration extends IntegrationAdapter {

    private static final Logger logger = Logger.getLogger(MacIntegration.class.getName());
    private static final String PROGRAM_DIRNAME = "Esmska";
    private static final String LOG_FILENAME = "Esmska.log";
    private static final Color LEOPARD_PANEL_COLOR = new Color(232, 232, 232);

    private ModalSheetCounter modalSheetCounter;

    /**
     * Perform initialization of Mac integration.
     */
    @Override
    protected void initialize() {
        super.initialize();

        modalSheetCounter = new ModalSheetCounter();
    }

    /**
     * Activates integration.
     */
    @Override
    public void activateGUI() {
        addEventHandlers();
        setupAqua();
    }

    /**
     * Adds event listener to application.
     */
    private void addEventHandlers() {
        Application app = Application.getApplication();

        app.setAboutHandler(new MacAboutHandler(bean));
        app.setPreferencesHandler(new MacPreferencesHandler(bean));
        app.setOpenFileHandler(new MacOpenFilesHandler());
        app.setQuitHandler(new MacQuitHandler(bean));

        app.addAppEventListener(new MacAppReOpenedListener());
        app.addAppEventListener(new MacUserSessionListener());
        app.addAppEventListener(new MacSystemSleepListener());

        // set application menubar
        app.setDefaultMenuBar(Context.mainFrame.getJMenuBar());
    }

    /**
     * Setup some additional Aqua tweaks.
     */
    private void setupAqua() {
        // turn off mnemonics, tool tips and icons from menu (only while using Aqua)
        if (! ThemeManager.isAquaCurrentLaF()) {
            return;
        }

        MainFrame frame = Context.mainFrame;
        JMenuBar bar = frame.getJMenuBar();

        for (Component menu : bar.getComponents()) {
            JMenu m = (JMenu) menu;
            m.setMnemonic(-1);
            m.setToolTipText(null);

            for (Component c : m.getPopupMenu().getComponents()) {
                if (c instanceof JMenuItem) {
                    JMenuItem i = (JMenuItem) c;
                    i.setIcon(null);
                    i.setMnemonic(-1);
                    i.setToolTipText(null);
                } else if (c instanceof JMenu) {
                    JMenu jm = (JMenu) c;
                    jm.setIcon(null);
                    jm.setMnemonic(-1);
                    jm.setToolTipText(null);
                }
            }
        }

        // initialize unified toolbar support
        UnifiedToolbarSupport.installSupport();

        // set background color to all panels
        // this color doesnt depend on focused state of window
        setBackgroundForPanelsRecursively(frame);

        // add some more space to match HIG closely
        frame.getStatusPanel().setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
    }

    /**
     * Set Leopard background for panels recursively.
     *
     * @param frame window
     */
    private static void setBackgroundForPanelsRecursively(JFrame frame) {
        Container contentPane = frame.getContentPane();
        contentPane.setBackground(LEOPARD_PANEL_COLOR);

        changeBackgroundRecursively(contentPane);
    }

    /**
     * Recursively go through hierarchy of components and sets containers
     * background color to Leopard.
     *
     * @param rootComponent root component
     */
    private static void changeBackgroundRecursively(Component rootComponent) {
        // toolbar has different look, skip it
        if (rootComponent instanceof JToolBar) {
            return;
        }

        // scan through panels and split panes
        if (rootComponent instanceof JPanel || rootComponent instanceof JSplitPane) {
            Container container = (Container) rootComponent;
            container.setBackground(LEOPARD_PANEL_COLOR);

            Component[] components = container.getComponents();
            for (Component c : components) {
                changeBackgroundRecursively(c);
            }
        }
    }

    @Override
    public File getConfigDir(File defaultConfigDir) {
        //Config dir should be in ~/Library/Preferences, but it should store only
        //Esmska.xml, nothing more. For simplicity we have decided to put everything
        //into data dir.
        return getDataDir(defaultConfigDir);
    }

    @Override
    public File getDataDir(File defaultDataDir) {
        try {
            return new File(FileManager.findFolder(FileManager.kUserDomain, FileManager.OSTypeToInt("asup")));
        } catch (FileNotFoundException ex) {
            logger.log(Level.WARNING, "Could not find directory for data files", ex);
            return new File(System.getProperty("user.home") + "/Library/Application Support");
        }
    }

    @Override
    public File getLogFile(File defaultLogFile) {
        String dir;
        try {
            dir = FileManager.findFolder(FileManager.kUserDomain, FileManager.OSTypeToInt("logs"));
        } catch (FileNotFoundException ex) {
            logger.log(Level.WARNING, "Could not find directory for log files", ex);
            dir = System.getProperty("user.home") + "/Library/Logs";
        }
        return new File(dir, LOG_FILENAME);
    }

    @Override
    public String getProgramDirName(String defaultProgramDirName) {
        // honor Mac OS X conventions and use capitalized name of directory
        return PROGRAM_DIRNAME;
    }

    /**
     * @see esmska.integration.IntegrationAdapter#setActionBean(esmska.integration.ActionBean)
     */
    @Override
    public void setActionBean(ActionBean bean) {
        super.setActionBean(bean);

        NotificationIcon icon = NotificationIcon.getInstance();
        if (icon != null) {
            PopupMenu menu = icon.getPopup();
            Application.getApplication().setDockMenu(menu);
        }
    }

    @Override
    public void registerModalSheet(JDialog dialog) {
        dialog.removeWindowListener(modalSheetCounter);
        dialog.addWindowListener(modalSheetCounter);
    }

    @Override
    public boolean isModalSheetVisible() {
        return modalSheetCounter.isModalSheetVisible();
    }

    /**
     * @see IntegrationAdapter#setSMSCount(Integer)
     */
    @Override
    public void setSMSCount(Integer count) {
        if (count == null || count <= 0) {
            Application.getApplication().setDockIconBadge(null);
        } else {
            Application.getApplication().setDockIconBadge(count.toString());
        }
    }
}
