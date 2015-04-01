package esmska;

import esmska.gui.MainFrame;
import esmska.persistence.PersistenceManager;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Main program context. References to important class instances are accessible
 * here.
 *
 * @author ripper
 */
public class Context {
    // <editor-fold defaultstate="collapsed" desc="PropertyChange support">
    private static PropertyChangeSupport changeSupport = new PropertyChangeSupport(new Object());
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    // </editor-fold>
    
    /** Instance of PersistenceManager. Never null after main program
     * inicialization. */
    public static PersistenceManager persistenceManager;
    /** Instance of MainFrame. Never null after MainFrame inicialization.
     */
    public static MainFrame mainFrame;

    private static boolean gatewaysLoaded;
    /** Whether all gateways were already loaded.*/
    public static boolean gatewaysLoaded() {
        return gatewaysLoaded;
    }
    public static void setGatewaysLoaded(boolean gatewaysLoaded) {
        boolean old = Context.gatewaysLoaded;
        if (old != gatewaysLoaded) {
            Context.gatewaysLoaded = gatewaysLoaded;
            changeSupport.firePropertyChange("gatewaysLoaded", old, Context.gatewaysLoaded);
            updateLoadTriggers();
        }
    }
    
    private static boolean everythingLoaded;
    /** Whether all user data were already loaded.*/
    public static boolean everythingLoaded() {
        return everythingLoaded;
    }
    public static void setEverythingLoaded(boolean everythingLoaded) {
        boolean old = Context.everythingLoaded;
        if (old != everythingLoaded) {
            Context.everythingLoaded = everythingLoaded;
            changeSupport.firePropertyChange("everythingLoaded", old, Context.everythingLoaded);
        }
    }
    
    /** Set everythingLoaded according to all other load triggers. */
    private static void updateLoadTriggers() {
        setEverythingLoaded(gatewaysLoaded());
    }
}
