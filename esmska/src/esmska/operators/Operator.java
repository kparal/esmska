/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.operators;

import java.io.File;
import javax.swing.Icon;

/**
 *
 * @author ripper
 */
public interface Operator extends OperatorInfo, Comparable<Operator> {

    /** Location of script for sending sms. */
    File getScript();

    /** Operator logo icon.
     * Should be a 16x16px PNG.
     */
    Icon getIcon();
    
}
