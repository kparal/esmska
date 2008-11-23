/*
 * ThemeManager.java
 *
 * Created on 24. srpen 2007, 23:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import com.sun.java.swing.plaf.gtk.GTKLookAndFeel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.SkinInfo;
import esmska.data.Config;
import esmska.persistence.PersistenceManager;
import esmska.utils.JavaType;
import esmska.utils.OSType;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.substance.skin.SaharaSkin;

/** Manage and set look and feel
 *
 * @author ripper
 */
public class ThemeManager {
    public enum LAF {
        SYSTEM, CROSSPLATFORM, GTK, JGOODIES, SUBSTANCE
    }

    private static final Logger logger = Logger.getLogger(ThemeManager.class.getName());

    /** Disabled constructor */
    private ThemeManager() {
    }

    /* Set look and feel found in configuration
     * @throws Throwable when chosen look and feel can't be set
     */
    public static void setLaF() throws Throwable {
        Config config = PersistenceManager.getConfig();
        ThemeManager.LAF laf = config.getLookAndFeel();
        
        JFrame.setDefaultLookAndFeelDecorated(config.isLafWindowDecorated());
        JDialog.setDefaultLookAndFeelDecorated(config.isLafWindowDecorated());
        
        switch (laf) {
            case SYSTEM:
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                break;
            case CROSSPLATFORM:
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                break;
            case GTK:
                UIManager.setLookAndFeel(GTKLookAndFeel.class.getName());
                break;
            case JGOODIES:
                String themeString = config.getLafJGoodiesTheme();
                PlasticTheme theme = null;
                for (Object o : PlasticLookAndFeel.getInstalledThemes()) {
                    PlasticTheme ptheme = (PlasticTheme) o;
                    if (ptheme.getName().equals(themeString)) {
                        theme = ptheme;
                        break;
                    }
                }
                PlasticLookAndFeel.setPlasticTheme(theme != null ? theme : new ExperienceBlue());
                UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
                break;
            case SUBSTANCE:
                String skinString = config.getLafSubstanceSkin();
                String skin = null;
                new SubstanceLookAndFeel();
                for (SkinInfo skinInfo : SubstanceLookAndFeel.getAllSkins().values()) {
                    if (skinInfo.getDisplayName().equals(skinString)) {
                        skin = skinInfo.getClassName();
                        break;
                    }
                }
                SubstanceLookAndFeel.setSkin(skin != null ? skin : new SaharaSkin().getClass().getName());
                UIManager.setLookAndFeel(new SubstanceLookAndFeel());
                
                //set Substance specific addons
                UIManager.put(LafWidget.TEXT_EDIT_CONTEXT_MENU, Boolean.TRUE);
                
                break;
            default:
                throw new IllegalArgumentException("Unknown LaF name");
        }

        logger.fine("New LaF set: " + UIManager.getLookAndFeel());
    }
    
    /** Returns whether GTK is current look and feel */
    public static boolean isGTKCurrentLaF() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf == null) {
            return false;
        }
        return laf.getName().equals("GTK look and feel");
    }

    /** Returns whether Aqua (Mac OS native) is current look and feel */
    public static boolean isAquaCurrentLaF() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf == null) {
            return false;
        }
        return laf.getName().equals("Mac OS X");
    }
    
    /** Returns whether specified LaF is supported on current configuration
     * (operating system, java version, etc).
     */
    public static boolean isLaFSupported(LAF laf) {
        switch (laf) {
            case SYSTEM:
                return true;
            case CROSSPLATFORM: 
                return true;
            case GTK: 
                return !OSType.isWindows();
            case JGOODIES: 
                return true;
            case SUBSTANCE: 
                return true;
            default: 
                throw new IllegalArgumentException("Uknown LAF: " + laf);
        }
    }
    
    /** Propose the best LaF for current system, based on java type and
     * operating system. Useful for program first run.
     * @return the best LaF for this platform
     */
    public static LAF suggestBestLAF() {
        LAF laf = LAF.SUBSTANCE;
        
        //set system LaF on OpenJDK, because Substance throws exceptions
        if (JavaType.isOpenJDK()) {
            laf = LAF.SYSTEM;
        }
        //set system LaF on Apple, because Apple users are used to consistent look
        if (JavaType.isAppleJava()) {
            laf = LAF.SYSTEM;
        }
        //set system LaF on KDE3, because there is a problem with small fonts in Substance
        String KDEVersion = OSType.getKDEDesktopVersion();
        if (KDEVersion != null && KDEVersion.startsWith("3")) {
            laf = LAF.SYSTEM;
        }

        logger.finer("Suggested LaF: " + laf);
        return laf;
    }
    
}
