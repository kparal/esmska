package esmska.gui;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JLabel;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

/**
 * Shows HTML text in a JLabel. This only adds the feature of executing links
 * in the label, normal JLabel's do the rest of the work.
 *
 * Sets hand cursor when mouse is over a hyperlink. After clicking on a hyperlink
 * sends a ValuedEvent.
 * 
 * @author Jeffrey Bush
 * @see <a href="http://forums.sun.com/thread.jspa?forumID=57&threadID=574895">Original source code</a>
 */
public class JHtmlLabel extends JLabel implements MouseListener, MouseMotionListener {

    public static enum Events {
        /** Hyperlink clicked. Event value: String with URL of the hyperlink. */
        LINK_CLICKED
    }

    private AccessibleJLabel acc = null;

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, String> valuedSupport = new ValuedEventSupport<Events, String>(this);
    public void addValuedListener(ValuedListener<Events, String> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, String> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    public JHtmlLabel() {
        this(null);
    }

    public JHtmlLabel(String text) {
        super(text);
        acc = (AccessibleJLabel) getAccessibleContext();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        AttributeSet charAttr = acc.getCharacterAttribute(acc.getIndexAtPoint(e.getPoint()));
        if (charAttr == null) {
            return;
        }
        AttributeSet attr = (AttributeSet) charAttr.getAttribute(HTML.Tag.A);
        if (attr == null) {
            return;
        }
        String url = (String) attr.getAttribute(HTML.Attribute.HREF);
        if (url != null) {
            valuedSupport.fireEventOccured(Events.LINK_CLICKED, url);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        AttributeSet charAttr = acc.getCharacterAttribute(acc.getIndexAtPoint(e.getPoint()));
        AttributeSet attr = null;
        if (charAttr != null) {
                attr = (AttributeSet) charAttr.getAttribute(HTML.Tag.A);
        }
        if (attr == null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            setToolTipText(null);
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText((String) attr.getAttribute(HTML.Attribute.HREF));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }
}