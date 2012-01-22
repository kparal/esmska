package esmska.integration.mac;

import esmska.Context;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Support for dragging main frame component.
 *
 * @author Marian Bouček
 */
public class MouseDragSupport extends MouseAdapter {

    private transient int startX;
    private transient int startY;

    /**
     * Remember start position of mouse in window.
     *
     * @param e user pressed mouse button
     */
    @Override
    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
    }

    /**
     * Move window to new position according to new mouse position.
     *
     * @param e user is dragged toolbar
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        Point windowPosition = Context.mainFrame.getLocationOnScreen();

        windowPosition.x += e.getX() - startX;
        windowPosition.y += e.getY() - startY;

        Context.mainFrame.setLocation(windowPosition);
    }
}
