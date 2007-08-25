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
import javax.swing.UIManager;
import persistence.PersistenceManager;

/**
 *
 * @author ripper
 */
public class ThemeManager {
    public static String LAF_SYSTEM = "System";
    public static String LAF_CROSSPLATFORM =  "Crossplatform";
    public static String LAF_JGOODIES = "JGoodies";
    
    private ThemeManager() {
    }
    
    public static void setLaF() throws Exception {
        String laf = PersistenceManager.getConfig().getLookAndFeel();
        
        if (laf.equals(LAF_SYSTEM)) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
        } else if (laf.equals(LAF_CROSSPLATFORM)) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            
        } else if (laf.equals(LAF_JGOODIES)) {
            String themeString = PersistenceManager.getConfig().getLafJGoodiesTheme();
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
            
        } else {
            throw new IllegalArgumentException("Unknown LaF name");
        }
        
    }
    
}
