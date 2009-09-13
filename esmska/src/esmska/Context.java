/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska;

import esmska.gui.MainFrame;
import esmska.persistence.PersistenceManager;

/** Main program context. References to important class instances are accessible
 * here.
 *
 * @author ripper
 */
public class Context {
    /** Instance of PersistenceManager. Never null after main program
     * inicialization. */
    public static PersistenceManager persistenceManager;
    /** Instance of MainFrame. Never null after MainFrame inicialization.
     */
    public static MainFrame mainFrame;
}
