/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import java.io.File;
import javax.swing.Icon;

/** Interface for telephone operator.
 *
 * @author ripper
 */
public interface Operator extends OperatorInfo, Comparable<Operator> {

    /** Location of operator script file. */
    File getScript();

    /** Operator logo icon.
     * Should be a 16x16px PNG with transparent background.
     */
    Icon getIcon();
    
}
