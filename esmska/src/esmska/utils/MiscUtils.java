/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.utils;

import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.apache.commons.lang.StringEscapeUtils;

/** Various helper methods.
 *
 * @author ripper
 */
public class MiscUtils {
    public static enum Direction {
        WIDTH,
        HEIGHT,
        BOTH
    };

    private static final Logger logger = Logger.getLogger(MiscUtils.class.getName());


    /** Escape text using html entities.
     *  Fixes bug in OpenJDK where scaron entity is not replaced by 'š' and
     *  euro by '€'.
     *
     * @see ​StringEscapeUtils#escapeHtml(String)
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return input;
        }
        String output = StringEscapeUtils.escapeHtml(input);
        output = output.replaceAll("\\&scaron;", "\\&#353;");
        output = output.replaceAll("\\&Scaron;", "\\&#352;");
        output = output.replaceAll("\\&euro;", "\\&#8364;");
        return output;
    }

    /** Strip html tags from text.
     *
     * @param input input text
     * @return text with all html tags removed. Entities encoded by html codes
     * are unescaped back to standard characters.
     */
    public static String stripHtml(String input) {
        if (input == null) {
            return input;
        }
        String output = input.replaceAll("\\<.*?>","");
        output = StringEscapeUtils.unescapeHtml(output);
        return output;
    }

    /** Tell whether some JComponent is cropped (not displayed fully).
     *
     * @param component component
     * @return true if it is cropped, false otherwise
     */
    public static boolean isCropped(JComponent component) {
        component.revalidate();
        Dimension bounds = component.getBounds().getSize();
        Dimension visible = component.getVisibleRect().getSize();
        return !(bounds.equals(visible));
    }

    /** Tell whether some container wants to be resized in a direction
     *
     * @param container container
     * @param direction direction, BOTH when null
     * @return true if the container wants to be resized, false otherwise
     */
    public static boolean needsResize(JComponent container, Direction direction) {
        if (direction == null) {
            direction = Direction.BOTH;
        }
        container.revalidate();
        Dimension size = container.getSize();
        Dimension prefSize = container.getPreferredSize();
        boolean needWidth = size.getWidth() < prefSize.getWidth();
        boolean needHeight = size.getHeight() < prefSize.getHeight();
        switch (direction) {
            case WIDTH:
                return needWidth;
            case HEIGHT:
                return needHeight;
            case BOTH:
                return needWidth || needHeight;
            default:
                String message = "Unknown direction: " + direction;
                logger.log(Level.SEVERE, message);
                throw new IllegalStateException(message);
        }
    }
}
