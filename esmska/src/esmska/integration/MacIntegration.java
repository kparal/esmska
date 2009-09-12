/*
 * @(#) MacIntegration.java     10-05-2008  18:59
 */
package esmska.integration;

import java.awt.PopupMenu;
import java.awt.event.ActionEvent;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

import com.apple.eio.FileManager;
import esmska.gui.ImportFrame;
import esmska.gui.ThemeManager;
import esmska.gui.MainFrame;
import esmska.gui.NotificationIcon;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * Integration for Mac OS X.
 * 
 * @author Marian Boucek
 */
public class MacIntegration extends IntegrationAdapter implements ApplicationListener {

    private static final Logger logger = Logger.getLogger(MacIntegration.class.getName());
    private static final String PROGRAM_DIRNAME = "Esmska";
    private static final String LOG_FILENAME = "Esmska.log";
    private static final Color LEOPARD_PANEL_COLOR = new Color(232, 232, 232);

    private Application app;

    @Override
    protected void initialize() {
        app = new Application();
    }

    @Override
    public void activateGUI() {
        // make it more Mac-like, listen for Mac events
        app.setEnabledAboutMenu(true);
        app.setEnabledPreferencesMenu(true);
        app.addApplicationListener(this);

        // turn off mnemonics, tool tips and icons from menu
        // if we are not using Aqua l&f there is no need to strict use of HIG
        if (ThemeManager.isAquaCurrentLaF()) {
            MainFrame frame = MainFrame.getInstance();
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

            // create unified toolbar support
            new UnifiedToolbarSupport();

            // set background color to all panels
            // this color doesnt depend on focused state of window
            frame.getSMSPanel().setBackground(LEOPARD_PANEL_COLOR);
            frame.getContactPanel().setBackground(LEOPARD_PANEL_COLOR);
            frame.getQueuePanel().setBackground(LEOPARD_PANEL_COLOR);
            frame.getHorizontalSplitPane().setBackground(LEOPARD_PANEL_COLOR);
            frame.getVerticalSplitPane().setBackground(LEOPARD_PANEL_COLOR);
            frame.getStatusPanel().setBackground(LEOPARD_PANEL_COLOR);
            frame.getContentPane().setBackground(LEOPARD_PANEL_COLOR);
        }
    }

    @Override
    public File getConfigDir(File defaultConfigDir) {
        //Config dir should be in ~/Library/Preferences, but it should store only
        //Esmska.xml, nothing more. For simplicity we have decided to put all
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
        try {
            return new File(FileManager.findFolder(FileManager.kUserDomain, FileManager.OSTypeToInt("logs")),
                    LOG_FILENAME);
        } catch (FileNotFoundException ex) {
            logger.log(Level.WARNING, "Could not find directory for log files", ex);
            return new File(System.getProperty("user.home") + "/Library/Logs", LOG_FILENAME);
        }
    }

    @Override
    public String getProgramDirName(String defaultProgramDirName) {
        //Mac users are used to capitalized names
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
            app.setDockMenu(menu);
        }
    }

    // public interface -------------------------------------------------------
    /**
     * @see IntegrationAdapter#setSMSCount(Integer)
     */
    @Override
    public void setSMSCount(Integer count) {
        if (count == null || count <= 0) {
            app.setDockIconBadge(null);
        } else {
            app.setDockIconBadge(count.toString());
        }
    }

    // implementation of app interface ----------------------------------------
    /**
     * @see com.apple.eawt.ApplicationListener#handleAbout(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handleAbout(ApplicationEvent e) {
        e.setHandled(true);
        bean.getAboutAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "aboutSelected"));
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handleOpenApplication(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handleOpenApplication(ApplicationEvent e) {
        e.setHandled(false);
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handleOpenFile(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handleOpenFile(ApplicationEvent e) {
        e.setHandled(true);

        //import dropped contacts
        String fileName = e.getFilename();
        ImportFrame importFrame = new ImportFrame();
        importFrame.setLocationRelativeTo(MainFrame.getInstance());
        importFrame.importVCardFile(fileName);
        importFrame.setVisible(true);
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handlePreferences(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handlePreferences(ApplicationEvent e) {
        e.setHandled(true);
        bean.getConfigAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "configSelected"));
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handlePrintFile(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handlePrintFile(ApplicationEvent e) {
        e.setHandled(false);
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handleQuit(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handleQuit(ApplicationEvent e) {
        e.setHandled(true);
        bean.getQuitAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "quitSelected"));
    }

    /**
     * @see com.apple.eawt.ApplicationListener#handleReOpenApplication(com.apple.eawt.ApplicationEvent)
     */
    @Override
    public void handleReOpenApplication(ApplicationEvent e) {
        e.setHandled(true);
        MainFrame.getInstance().setVisible(true);
    }
}
