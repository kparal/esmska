package esmska.integration.mac;

import esmska.Context;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Window adapter for setting border.
 *
 * Color of bottom line depends on focus state of window. Setting new border
 * also solves problem with repainting of gradient. Border adds some more space
 * to match HIG more closely.
 *
 * @author Marian Bouček
 */
public class UnifiedToolbarWindowAdapter extends WindowAdapter {

    private Border activeBorder;
    private Border inactiveBorder;

    /**
     * Creates new instance of window adapter.
     */
    public UnifiedToolbarWindowAdapter() {
        activeBorder = createBorderWithColor(new Color(64, 64, 64));
        inactiveBorder = createBorderWithColor(new Color(135, 135, 135));
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        Context.mainFrame.getToolbar().setBorder(activeBorder);
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        Context.mainFrame.getToolbar().setBorder(inactiveBorder);
    }

    /**
     * Creates border with specified bottom line color.
     *
     * @param c bottom line color
     * @return border
     */
    private Border createBorderWithColor(Color c) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, c),
                BorderFactory.createEmptyBorder(5, 0, 3, 0));
    }
}
