package esmska.integration.mac;

import esmska.Context;
import esmska.integration.IntegrationAdapter;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ToolBarUI;

/**
 * Custom UI that paints Unified toolbar. This code is based on
 * <a href="http://code.google.com/p/macwidgets/">Mac Widgets</a>.
 *
 * @author Kenneth Orr
 */
public class UnifiedToolbarUI extends ToolBarUI {

    private static final Color ACTIVE_TOP_GRADIENT_COLOR = new Color(0xbcbcbc);
    private static final Color ACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0x9a9a9a);
    private static final Color INACTIVE_TOP_GRADIENT_COLOR = new Color(0xe4e4e4);
    private static final Color INACTIVE_BOTTOM_GRADIENT_COLOR = new Color(0xd1d1d1);
    private static final IntegrationAdapter ADAPTER = IntegrationAdapter.getInstance();

    /**
     * Install support for unified toolbar.
     */
    public static void installSupport() {
        // set background painter only on Mac OS X 10.5 Leopard
        // on 10.6 it has been fixed and works properly
        if (System.getProperty("os.version").contains("10.5")) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    Context.mainFrame.getToolbar().setUI(new UnifiedToolbarUI());
                    Context.mainFrame.getToolbar().invalidate();
                }
            });
        }
    }

    /**
     * Paints gradient. Colors depends on focused state of main window.
     *
     * @param g graphics
     * @param c painted component
     */
    @Override
    public void paint(Graphics g, JComponent c) {

        int height = c.getHeight();
        boolean focused = Context.mainFrame.isFocused() || ADAPTER.isModalSheetVisible();

        Color topColor = focused ? ACTIVE_TOP_GRADIENT_COLOR : INACTIVE_TOP_GRADIENT_COLOR;
        Color bottomColor = focused ? ACTIVE_BOTTOM_GRADIENT_COLOR : INACTIVE_BOTTOM_GRADIENT_COLOR;

        GradientPaint paint = new GradientPaint(0, 1, topColor, 0, height, bottomColor);

        Graphics2D graphics2D = (Graphics2D) g.create();
        graphics2D.setPaint(paint);
        graphics2D.fillRect(0, 0, c.getWidth(), height);

        graphics2D.dispose();
    }
}
