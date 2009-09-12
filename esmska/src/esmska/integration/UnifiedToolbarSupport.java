/*
 * UnifiedToolbarSupport.java
 */
package esmska.integration;

import esmska.gui.MainFrame;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ToolBarUI;

/**
 * Swing support for Unified toolbar introduced in Mac OS X 10.5 Leopard.
 * 
 * @author  Marian Bouček
 */
public class UnifiedToolbarSupport extends MouseAdapter {

    private final MainFrame frame;
    
    private transient int startX;
    private transient int startY;

    /**
     * Creates new instance and install support.
     * 
     * @param frame main frame
     */
    public UnifiedToolbarSupport() {
        this.frame = MainFrame.getInstance();

        // install listeners on toolbar
        final JToolBar toolbar = frame.getToolbar();
        toolbar.addMouseListener(this);
        toolbar.addMouseMotionListener(this);

        // remove border from buttons
        for (Component c : toolbar.getComponents()) {
            if (c instanceof JButton) {
                JButton button = (JButton) c;
                button.setBorderPainted(false);
            }
        }

        // set background painter
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                toolbar.setUI(new UnifiedToolbarUI());
                toolbar.invalidate();
            }
        });
    }

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
        Point windowPosition = frame.getLocationOnScreen();

        windowPosition.x += e.getX() - startX;
        windowPosition.y += e.getY() - startY;

        frame.setLocation(windowPosition);
    }

    /**
     * Custom UI that paints Unified toolbar. This code is based on
     * <a href="http://code.google.com/p/macwidgets/">Mac Widgets</a>.
     * 
     * @author Kenneth Orr
     */
    private class UnifiedToolbarUI extends ToolBarUI {

        private Color ACTIVE_TOP_GRADIENT_COLOR = new Color(0xbcbcbc);
        private Color ACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0x9a9a9a);
        private Color INACTIVE_TOP_GRADIENT_COLOR = new Color(0xe4e4e4);
        private Color INACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0xd1d1d1);

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D graphics2D = (Graphics2D) g.create();

            paint(graphics2D, c, c.getWidth(), c.getHeight());

            graphics2D.dispose();
            super.paint(graphics2D, c);
        }

        public void paint(Graphics2D graphics2D, Component component, int width, int height) {

            boolean focused = MainFrame.getInstance().isFocused();

            Color topColor = focused ? ACTIVE_TOP_GRADIENT_COLOR : INACTIVE_TOP_GRADIENT_COLOR;
            Color bottomColor = focused ? ACTIVE_BOTTOM_GRADIENT_COLOR : INACTIVE_BOTTOM_GRADIENT_COLOR;

            GradientPaint paint = new GradientPaint(0, 1, topColor, 0, height, bottomColor);
            graphics2D.setPaint(paint);
            graphics2D.fillRect(0, 0, width, height);
        }
    }
}
