/*
 * ThemeManager.java
 *
 * Created on 24. srpen 2007, 23:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import com.jgoodies.looks.plastic.theme.ExperienceGreen;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.FieldOfWheatSkin;
import org.jvnet.substance.skin.SkinInfo;
import persistence.ConfigBean;
import persistence.PersistenceManager;

/** Manage and set look and feel
 *
 * @author ripper
 */
public class ThemeManager {
    public static String LAF_SYSTEM = "System";
    public static String LAF_CROSSPLATFORM =  "Crossplatform";
    public static String LAF_JGOODIES = "JGoodies";
    public static String LAF_SUBSTANCE = "Substance";
    
    private ThemeManager() {
    }
    
    /* Set look and feel found in configuration */
    public static void setLaF() throws Exception {
        ConfigBean config = PersistenceManager.getConfig();
        String laf = config.getLookAndFeel();
        
        JFrame.setDefaultLookAndFeelDecorated(config.isLafWindowDecorated());
        JDialog.setDefaultLookAndFeelDecorated(config.isLafWindowDecorated());
        
        if (laf.equals(LAF_SYSTEM)) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
        } else if (laf.equals(LAF_CROSSPLATFORM)) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
        } else if (laf.equals(LAF_JGOODIES)) {
            String themeString = config.getLafJGoodiesTheme();
            PlasticTheme theme = null;
            for (Object o : PlasticLookAndFeel.getInstalledThemes()) {
                PlasticTheme ptheme = (PlasticTheme) o;
                if (ptheme.getName().equals(themeString)) {
                    theme = ptheme;
                    break;
                }
            }
            PlasticLookAndFeel.setPlasticTheme(theme != null? theme : new ExperienceBlue());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            
        } else if (laf.equals(LAF_SUBSTANCE)) {
            String skinString = config.getLafSubstanceSkin();
            String skin = null;
            new SubstanceLookAndFeel();
            for (SkinInfo skinInfo : SubstanceLookAndFeel.getAllSkins().values()) {
                if (skinInfo.getDisplayName().equals(skinString)) {
                    skin = skinInfo.getClassName();
                    break;
                }
            }
            SubstanceLookAndFeel.setSkin(skin != null? skin : new FieldOfWheatSkin().getClass().getName());
            UIManager.setLookAndFeel(new SubstanceLookAndFeel());
            
        } else {
            throw new IllegalArgumentException("Unknown LaF name");
        }
        
    }
    
}
