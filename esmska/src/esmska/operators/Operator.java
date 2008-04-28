/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import java.net.URL;
import javax.swing.Icon;

/** Interface for telephone operator.
 *
 * @author ripper
 */
public interface Operator extends OperatorInfo, Comparable<Operator> {

    /** URL of operator script (file or jar URL). */
    URL getScript();

    /** Operator logo icon.
     * Should be a 16x16px PNG with transparent background.
     */
    Icon getIcon();
    
}
