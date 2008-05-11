/*
 * @(#) ActionBean.java    11-05-2008   14:27
 */
package esmska.utils.mac;

import javax.swing.Action;

/**
 * Bean for action classes.
 *
 * @author  Marian Boucek
 * @version 1.0
 */
public class ActionBean {
    
    private Action quitAction;
    private Action aboutAction;
    private Action configAction;
    
    // setters (must be called with non null value -----------------------------
    public void setAboutAction(Action aboutAction) {
        this.aboutAction = aboutAction;
    }
    
    public void setConfigAction(Action configAction) {
        this.configAction = configAction;
    }
    
    public void setQuitAction(Action quitAction) {
        this.quitAction = quitAction;
    }
    
    // package-protected getters -----------------------------------------------
    Action getAboutAction() {
        return aboutAction;
    }
    
    Action getConfigAction() {
        return configAction;
    }
    
    Action getQuitAction() {
        return quitAction;
    }
}
