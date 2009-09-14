/*
 * UnifiedToolbarSupport.java
 */
package esmska.integration;

import esmska.Context;
import esmska.gui.MainFrame;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
        this.frame = Context.mainFrame;

        // add custom window listener for setting border
        frame.addWindowFocusListener(new UnifiedToolbarWindowAdapter());

        // install listeners on toolbar
        final JToolBar toolbar = frame.getToolbar();
        toolbar.setDoubleBuffered(true);
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
    private static class UnifiedToolbarUI extends ToolBarUI {

        private static final Color ACTIVE_TOP_GRADIENT_COLOR = new Color(0xbcbcbc);
        private static final Color ACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0x9a9a9a);
        private static final Color INACTIVE_TOP_GRADIENT_COLOR = new Color(0xe4e4e4);
        private static final Color INACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0xd1d1d1);

        private static final MainFrame FRAME = MainFrame.getInstance();
        private static final IntegrationAdapter ADAPTER = IntegrationAdapter.getInstance();

        /**
         * Paints gradient. Colors depends on focused state of main window.
         *
         * @param g graphics
         * @param c painted component
         */
        @Override
        public void paint(Graphics g, JComponent c) {

            int height = c.getHeight();
            boolean focused = FRAME.isFocused() || ADAPTER.isModalSheetVisible();

            Color topColor = focused ? ACTIVE_TOP_GRADIENT_COLOR : INACTIVE_TOP_GRADIENT_COLOR;
            Color bottomColor = focused ? ACTIVE_BOTTOM_GRADIENT_COLOR : INACTIVE_BOTTOM_GRADIENT_COLOR;

            GradientPaint paint = new GradientPaint(0, 1, topColor, 0, height, bottomColor);

            Graphics2D graphics2D = (Graphics2D) g.create();
            graphics2D.setPaint(paint);
            graphics2D.fillRect(0, 0, c.getWidth(), height);

            graphics2D.dispose();
        }
    }

    /**
     * Window adapter for setting border. Color of bottom line depends on focus
     * state of window. Setting new border also solves problem with repainting
     * of gradient. Border adds some more space to match HIG more closely.
     *
     * @author Marian Bouček
     */
    private class UnifiedToolbarWindowAdapter extends WindowAdapter {

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
            frame.getToolbar().setBorder(activeBorder);
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            frame.getToolbar().setBorder(inactiveBorder);
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
}
