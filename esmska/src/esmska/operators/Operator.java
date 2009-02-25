/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import esmska.utils.L10N;
import java.net.URL;
import javax.swing.Icon;

/** Interface for telephone operator.
 *
 * @author ripper
 */
public interface Operator extends OperatorInfo, Comparable<Operator> {

    public static final String UNKNOWN = L10N.l10nBundle.getString("Operator.unknown"); //TODO: rework to enum

    /** URL of operator script (file or jar URL). */
    URL getScript();

    /** Operator logo icon.
     * Should be a 16x16px PNG with transparent background.
     */
    Icon getIcon();
    
}
