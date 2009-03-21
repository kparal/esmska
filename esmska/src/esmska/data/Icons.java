/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import javax.swing.ImageIcon;

/** Class with static references to frequently used icons
 *
 * @author ripper
 */
public class Icons {

    private static final String RES = "/esmska/resources/";
    public static final ImageIcon STATUS_MESSAGE = new ImageIcon(Icons.class.getResource(RES + "message-16.png"));
    public static final ImageIcon STATUS_WARNING = new ImageIcon(Icons.class.getResource(RES + "warning-16.png"));
    public static final ImageIcon STATUS_INFO = new ImageIcon(Icons.class.getResource(RES + "info-16.png"));
    public static final ImageIcon STATUS_ERROR = new ImageIcon(Icons.class.getResource(RES + "error-16.png"));
    public static final ImageIcon STATUS_BLANK = new ImageIcon(Icons.class.getResource(RES + "blank-16.png"));
    public static final ImageIcon STATUS_UPDATE = new ImageIcon(Icons.class.getResource(RES + "update-16.png"));
    public static final ImageIcon STATUS_UPDATE_IMPORTANT = new ImageIcon(Icons.class.getResource(RES + "updateImportant-16.png"));
    public static final ImageIcon OPERATOR_BLANK = STATUS_BLANK;
    public static final ImageIcon OPERATOR_DEFAULT = new ImageIcon(Icons.class.getResource(RES + "operator-16.png"));
}
