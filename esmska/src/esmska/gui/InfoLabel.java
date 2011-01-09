/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.gui;

import esmska.data.Icons;
import esmska.data.event.ValuedEvent;
import esmska.data.event.ValuedListener;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;

/** A JLabel displaying various information and warnings. It can contain
 * html links and its appearance is slightly modified to be more eye-catching.
 *
 * @author ripper
 */
public class InfoLabel extends JHtmlLabel {

    public InfoLabel() {
        this.setText("== INFO ==");
        this.setIcon(Icons.INFO_SMALL);
        this.setBackground(new Color(240, 240, 159));
        this.setFont(this.getFont().deriveFont((this.getFont().getStyle() | Font.ITALIC)));
        this.setBorder(BorderFactory.createLineBorder(new Color(255, 164, 0)));
        this.setOpaque(true);
    }
}
