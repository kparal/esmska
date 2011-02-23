/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import esmska.data.Icons;
import java.awt.Color;
import java.awt.Font;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;

/** A JLabel displaying various information and warnings. It can contain
 * html links and its appearance is slightly modified to be more eye-catching.
 *
 * @author ripper
 */
public class InfoLabel extends JHtmlLabel {
    private static final Logger logger = Logger.getLogger(InfoLabel.class.getName());

    /** Semantic type of the label, this influences the appearance */
    public enum Type {
        /* Important info for user to notice */
        IMPORTANT,
        /* Tip for improving work, hint, etc */
        TIP,
    }

    /** Create new InfoLabel with type IMPORTANT */
    public InfoLabel() {
        this(Type.IMPORTANT);
    }

    /** Create new InfoLabel of given type */
    public InfoLabel(Type type) {
        this.setText("== INFO ==");
        this.setFont(this.getFont().deriveFont((this.getFont().getStyle() | Font.ITALIC)));
        this.setOpaque(true);
        this.setIcon(Icons.INFO_SMALL);

        switch (type) {
            case IMPORTANT:
                this.setBackground(new Color(240, 240, 159));
                this.setBorder(BorderFactory.createLineBorder(new Color(255, 164, 0)));
                break;
            case TIP:
                this.setBackground(new Color(186, 208, 240));
                this.setBorder(BorderFactory.createLineBorder(new Color(0, 103, 250)));
                break;
            default:
                logger.log(Level.SEVERE, "Unknown type: {0}", type);
        }
    }
}
