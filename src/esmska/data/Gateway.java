/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package esmska.data;

import esmska.utils.L10N;
import java.net.URL;
import javax.swing.Icon;

/** Interface for web SMS gateway.
 *
 * @author ripper
 */
public interface Gateway extends GatewayInfo, Comparable<Gateway> {

    public static final String UNKNOWN = L10N.l10nBundle.getString("Gateway.unknown");

    /** URL of gateway script (file or jar URL). */
    URL getScript();

    /** Gateway logo icon.
     * Should be a 16x16px PNG with transparent background.
     */
    Icon getIcon();

    /** Return whether this gateway has been marked as user favorite */
    boolean isFavorite();

    /** Set this gateway as user favorite */
    void setFavorite(boolean favorite);

    /** Return whether this gateway has been marked as hidden from the user interface */
    boolean isHidden();

    /** Set this gateway as hidden from user interface */
    void setHidden(boolean hidden);
}
