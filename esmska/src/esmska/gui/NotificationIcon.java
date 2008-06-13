/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import esmska.utils.OSType;
import java.awt.Dimension;

/** Display icon in the notification area (aka system tray)
 *
 * @author ripper
 */
public class NotificationIcon {

    private static NotificationIcon instance;
    private static boolean installed;
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());
    private static final String RES = "/esmska/resources/";
    private static final String pauseQueue = "Pozastavit frontu sms";
    private static final String unpauseQueue = "Znovu spustit frontu sms";
    private static final String showWindow = "Zobrazit program";
    private static final String hideWindow = "Skrýt program";
    
    private PopupMenu popup = null;
    private TrayIcon trayIcon = null;
    private MenuItem toggleItem,  pauseQueueItem,  historyItem,  configItem,  
            quitItem, separatorItem;

    private NotificationIcon() {
        // show/hide main window
        toggleItem = new MenuItem("Skrýt/zobrazit program");
        toggleItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleMainFrameVisibility();
            }
        });

        // pause/unpause sms queue
        pauseQueueItem = new MenuItem("Pozastavit/spustit odesílání");
        pauseQueueItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                MainFrame.getInstance().getQueuePanel().
                        getSMSQueuePauseAction().actionPerformed(e);
            }
        });

        // show history
        historyItem = new MenuItem("Historie");
        historyItem.addActionListener(MainFrame.getInstance().getHistoryAction());

        // show settings
        configItem = new MenuItem("Nastavení");
        configItem.addActionListener(MainFrame.getInstance().getConfigAction());

        // exit program
        quitItem = new MenuItem("Ukončit");
        quitItem.addActionListener(MainFrame.getInstance().getQuitAction());

        // separator
        separatorItem = new MenuItem("-");
        
        // popup menu
        popup = new PopupMenu();
        MainFrame.getInstance().add(popup); //every popup must have parent
        
        // populate menu
        popup.add(toggleItem);
        popup.add(pauseQueueItem);
        popup.add(historyItem);
        popup.add(configItem);
        popup.add(separatorItem);
        popup.add(quitItem);

        //unpopulate menu on some platforms
        switch (OSType.detect()) {
            //on MAC, it's not needed to have items to system provided actions
            case MAC_OS_X:
                popup.remove(toggleItem);
                popup.remove(configItem);
                popup.remove(separatorItem);
                popup.remove(quitItem);
                break;
        }
        
        // add default action on left click
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                //single left click toggles window
                if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    toggleMainFrameVisibility();
                }
                //update labels on items
                updateItems();
            }
        };

        //choose best icon size
        String logo = "esmska.png";
        if (isSupported()) {
            Dimension size = SystemTray.getSystemTray().getTrayIconSize();
            if (size.getWidth() <= 16 && size.getHeight() <= 16) {
                logo = "esmska-16.png";
            } else if (size.getWidth() <= 32 && size.getHeight() <= 32) {
                logo = "esmska-32.png";
            } else if (size.getWidth() <= 64 && size.getHeight() <= 64) {
                logo = "esmska-64.png";
            }
        }
        
        // construct a TrayIcon
        Image image = new ImageIcon(getClass().getResource(RES + logo)).getImage();
        trayIcon = new TrayIcon(image, "Esmska", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(mouseAdapter);
    }

    /** Get instance of NotificationIcon. This class is singleton.
     * @return instance if notification area is supported, null otherwise
     */
    public static NotificationIcon getInstance() {
        if (!isSupported()) {
            return null;
        }
        if (instance == null) {
            instance = new NotificationIcon();
        }
        return instance;
    }

    /** Show or hide main window. Icon must be installed first. */
    public static void toggleMainFrameVisibility() {
        if (!installed) {
            return;
        }
        MainFrame frame = MainFrame.getInstance();
        
        //if iconified, just deiconify and return
        if ((frame.getExtendedState() & JFrame.ICONIFIED) != 0) {
            int state = frame.getExtendedState() & ~JFrame.ICONIFIED; //erase iconify bit
            frame.setExtendedState(state);
            return;
        }
        
        //toggle visibility
        frame.setVisible(!frame.isVisible());
    }

    /** Update labels in popup menu according to current program state */
    private void updateItems() {
        MainFrame frame = MainFrame.getInstance();
        
        boolean queuePaused = frame.getQueuePanel().isPaused();
        pauseQueueItem.setLabel(queuePaused ? unpauseQueue : pauseQueue);
        //visible if visible and not iconified
        boolean visible = frame.isVisible() && (frame.getExtendedState() & JFrame.ICONIFIED) == 0;
        toggleItem.setLabel(visible ? hideWindow : showWindow);
    }

    /** Install a new icon in the notification area. If an icon is already installed,
     * nothing happens. If notification area is not supported, nothing happens.
     */
    public static void install() {
        if (!isSupported()) {
            return;
        }
        getInstance();

        SystemTray tray = SystemTray.getSystemTray();
        try {
            tray.remove(instance.trayIcon);
            tray.add(instance.trayIcon);
            installed = true;
        } catch (AWTException ex) {
            logger.log(Level.WARNING, "Can't install program icon in the notification area", ex);
        }
    }

    /** Remove an icon from notification area. If there is no icon installed,
     * nothing happens. If notification area is not supported, nothing happens.
     */
    public static void uninstall() {
        if (!isSupported()) {
            return;
        }
        getInstance();

        SystemTray tray = SystemTray.getSystemTray();
        tray.remove(instance.trayIcon);
        installed = false;
    }

    /** Returns whether notification area is supported on this system. */
    public static boolean isSupported() {
        return SystemTray.isSupported();
    }
    
    /** Returns whether the notification icon is currently installed */
    public static boolean isInstalled() {
        return installed;
    }
    
    /** Returns the popup menu on notification icon. */
    public PopupMenu getPopup() {
        return popup;
    }
}
