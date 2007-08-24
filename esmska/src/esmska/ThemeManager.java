/*
 * ThemeManager.java
 *
 * Created on 24. srpen 2007, 23:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package esmska;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import persistence.PersistenceManager;

/**
 *
 * @author ripper
 */
public class ThemeManager {
    public static String LAF_SYSTEM = "System";
    public static String LAF_CROSSPLATFORM =  "Crossplatform";
    
    private ThemeManager() {
    }
    
    public static void setLaF() throws Exception {
        String laf = PersistenceManager.getConfig().getLookAndFeel();
        if (laf.equals(LAF_SYSTEM))
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        else if (laf.equals(LAF_CROSSPLATFORM))
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        else
            throw new IllegalArgumentException("Unknown LaF name");
    }
    
}
