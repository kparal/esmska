package esmska.integration.mac;

import esmska.Context;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 * Swing support for Unified toolbar introduced in Mac OS X 10.5 Leopard.
 *
 * @author  Marian Bouček
 */
public class UnifiedToolbarSupport {

    private UnifiedToolbarSupport() {
    }

    public static void installSupport() {
        // add custom window listener for setting border
        Context.mainFrame.addWindowFocusListener(new UnifiedToolbarWindowAdapter());

        // install listeners on toolbar
        JToolBar toolbar = Context.mainFrame.getToolbar();
        toolbar.setDoubleBuffered(true);

        MouseDragSupport support = new MouseDragSupport();
        toolbar.addMouseListener(support);
        toolbar.addMouseMotionListener(support);

        // remove border and text from buttons
        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton) {
                JButton button = (JButton) c;
                button.setBorderPainted(false);
                button.setText(null);
            }
        }
    }
}
