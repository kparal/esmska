package esmska.data;

import java.util.HashMap;
import javax.swing.ImageIcon;

/** Class with static references to frequently used icons
 *
 * @author ripper
 */
public class Icons {
    private static final String RES = "/esmska/resources/";
    private static final HashMap<String, ImageIcon> cache = new HashMap<String, ImageIcon>();

    public static final ImageIcon STATUS_MESSAGE = get("message-16.png");
    public static final ImageIcon STATUS_WARNING = get("warning-16.png");
    public static final ImageIcon STATUS_INFO = get("info-16.png");
    public static final ImageIcon STATUS_ERROR = get("error-16.png");
    public static final ImageIcon STATUS_BLANK = get("blank-16.png");
    public static final ImageIcon STATUS_UPDATE = get("update-16.png");
    public static final ImageIcon STATUS_UPDATE_IMPORTANT = get("updateImportant-16.png");
    public static final ImageIcon GATEWAY_BLANK = STATUS_BLANK;
    public static final ImageIcon GATEWAY_DEFAULT = get("gateway-16.png");

    public static final ImageIcon INFO_SMALL = get("info-22.png");
    public static final ImageIcon WARNING_SMALL = get("warning-22.png");
    public static final ImageIcon ERROR_SMALL = get("error-22.png");
    public static final ImageIcon UPDATE_SMALL = get("update-22.png");
    public static final ImageIcon UPDATE_IMPORTANT_SMALL = get("updateImportant-22.png");

    /** Return an ImageIcon of a requested name loaded from default resource directory.
     * Icons are cached for faster access. */
    public static ImageIcon get(String name) {
        ImageIcon icon = cache.get(name);
        if (icon == null) {
            icon = new ImageIcon(Icons.class.getResource(RES + name));
            cache.put(name, icon);
        }
        return icon;
    }
}
