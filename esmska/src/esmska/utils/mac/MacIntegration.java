/*
 * @(#) MacIntegration.java     10-05-2008  18:59
 */
package esmska.utils.mac;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;
import java.awt.event.ActionEvent;

/**
 * Integration for Mac OS X.
 * 
 * @author  Marian Boucek
 * @version 1.0
 */
public class MacIntegration implements ApplicationListener {
    
    private ActionBean actionBean;
    
    /**
     * Constructor
     */
    public MacIntegration() {
        super();
        Application app = new Application();
        
        app.setEnabledAboutMenu(true);
        app.setEnabledPreferencesMenu(true);
        
        app.addApplicationListener(this);
    }
    
    public void setActionBean(ActionBean bean) {
        this.actionBean = bean;
    }
    
    // implementation of app interface -----------------------------------------
    @Override
    public void handleAbout(ApplicationEvent e) {
        e.setHandled(true);
        actionBean.getAboutAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "aboutSelected"));
    }
    
    @Override
    public void handleOpenApplication(ApplicationEvent e) {
        e.setHandled(false);
    }
    
    @Override
    public void handleOpenFile(ApplicationEvent e) {
        e.setHandled(false);
    }
    
    @Override
    public void handlePreferences(ApplicationEvent e) {
        e.setHandled(true);
        actionBean.getConfigAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "configSelected"));
    }
    
    @Override
    public void handlePrintFile(ApplicationEvent e) {
        e.setHandled(false);
    }
    
    @Override
    public void handleQuit(ApplicationEvent e) {
        e.setHandled(true);
        actionBean.getQuitAction().actionPerformed(new ActionEvent(e.getSource(),
                ActionEvent.ACTION_PERFORMED, "quitSelected"));
    }
    
    @Override
    public void handleReOpenApplication(ApplicationEvent e) {
        e.setHandled(false);
    }
}
