/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/** Class managing all gateways
 * @author ripper
 */
public class Gateways {

    public static enum Events {
        ADDED_GATEWAY,
        ADDED_GATEWAYS,
        REMOVED_GATEWAY,
        REMOVED_GATEWAYS,
        CLEARED_GATEWAYS,
        FAVORITES_UPDATED,
        HIDDEN_UPDATED,
    }

    /** shared instance */
    private static final Config config = Config.getInstance();
    private static final Gateways instance = new Gateways();
    private static final Logger logger = Logger.getLogger(Gateways.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final SortedSet<Gateway> gateways = Collections.synchronizedSortedSet(new TreeSet<Gateway>());
    private static final HashSet<DeprecatedGateway> deprecatedGateways = new HashSet<DeprecatedGateway>();
    private static final Keyring keyring = Keyring.getInstance();
    private static final ScriptEngineManager manager = new ScriptEngineManager();

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, Gateway> valuedSupport = new ValuedEventSupport<Events, Gateway>(this);
    public void addValuedListener(ValuedListener<Events, Gateway> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, Gateway> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    /** Disabled constructor */
    private Gateways() {
        config.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // update favorite gateways on change
                if ("favoriteGateways".equals(evt.getPropertyName())) {
                    updateFavorites();
                }
                // update hidden gateways on change
                if ("hiddenGateways".equals(evt.getPropertyName())) {
                    updateHidden();
                }
            }
        });
    }

    /** Get shared instance */
    public static Gateways getInstance() {
        return instance;
    }

    /** Get unmodifiable collection of all gateways sorted by name */
    public SortedSet<Gateway> getAll() {
        return Collections.unmodifiableSortedSet(gateways);
    }

    /** Add new gateway
     * @param gateway new gateway, not null
     * @return See {@link Collection#add}
     */
    public boolean add(Gateway gateway) {
        Validate.notNull(gateway);

        logger.log(Level.FINE, "Adding new gateway: {0}", gateway);
        boolean added = gateways.add(gateway);

        if (added) {
            updateFavorites();
            updateHidden();
            valuedSupport.fireEventOccured(Events.ADDED_GATEWAY, gateway);
        }
        return added;
    }

    /** Add new gateways
     * @param gateways collection of gateways, not null, no null element
     * @return See {@link Collection#addAll}
     */
    public boolean addAll(Collection<Gateway> gateways) {
        Validate.notNull(gateways);
        Validate.noNullElements(gateways);

        logger.log(Level.FINE, "Adding {0} gateways: {1}",
                new Object[]{gateways.size(), gateways});
        boolean changed = Gateways.gateways.addAll(gateways);

        if (changed) {
            updateFavorites();
            updateHidden();
            valuedSupport.fireEventOccured(Events.ADDED_GATEWAYS, null);
        }
        return changed;
    }

    /** Remove existing gateway
     * @param gateway gateway to be removed, not null
     * @return See {@link Collection#remove}
     */
    public boolean remove(Gateway gateway) {
        Validate.notNull(gateway);

        logger.log(Level.FINE, "Removing gateway: {0}", gateway);
        boolean removed = gateways.remove(gateway);

        if (removed) {
            valuedSupport.fireEventOccured(Events.REMOVED_GATEWAY, gateway);
        }
        return removed;
    }

    /** Remove existing gateways
     * @param gateways collection of gateways to be removed, not null, no null element
     * @return See {@link Collection#removeAll}
     */
    public boolean removeAll(Collection<Gateway> gateways) {
        Validate.notNull(gateways);
        Validate.noNullElements(gateways);

        logger.log(Level.FINE, "Removing {0} gateways: {1}",
                new Object[]{gateways.size(), gateways});
        boolean changed = Gateways.gateways.removeAll(gateways);

        if (changed) {
            valuedSupport.fireEventOccured(Events.REMOVED_GATEWAYS, null);
        }
        return changed;
    }

    /** Remove all gateways */
    public void clear() {
        logger.fine("Removing all gateways");
        gateways.clear();

        valuedSupport.fireEventOccured(Events.CLEARED_GATEWAYS, null);
    }

    /** Search for an existing gateway
     * @param gateway gateway to be searched, not null
     * @return See {@link Collection#contains}
     */
    public boolean contains(Gateway gateway) {
        Validate.notNull(gateway);
        return gateways.contains(gateway);
    }

    /** Return number of gateways
     * @return See {@link Collection#size}
     */
    public int size() {
        return gateways.size();
    }

    /** Return if there are no gateways
     * @return See {@link Collection#isEmpty}
     */
    public boolean isEmpty() {
        return gateways.isEmpty();
    }

    /** Get set of currently deprecated gateways */
    public HashSet<DeprecatedGateway> getDeprecatedGateways() {
        return deprecatedGateways;
    }

    /** Set currently deprecated gateways. May be null to clear them. */
    public void setDeprecatedGateways(Set<DeprecatedGateway> deprecatedGateways) {
        Gateways.deprecatedGateways.clear();
        if (deprecatedGateways != null) {
            Gateways.deprecatedGateways.addAll(deprecatedGateways);
        }
    }

    /** Find gateway by name.
     * @param gatewayName Name of the gateway. Search is case sensitive. May be null.
     * @return Gateway implementation, when a gateway with such name is found.
     *         If multiple such gateways are found, returns the first one found.
     *         Returns null if no gateway is found or provided name was null.
     */
    public Gateway get(String gatewayName) {
        if (gatewayName == null) {
            return null;
        }

        synchronized(gateways) {
            for (Gateway gateway : gateways) {
                if (gateway.getName().equals(gatewayName)) {
                    return gateway;
                }
            }
        }
        return null;
    }

    /** Returns whether the gateway is a fake one (used just for development
     * purposes).
     */
    public static boolean isFakeGateway(String gatewayName) {
        return StringUtils.contains(gatewayName, "]fake");
    }

    /** Guess gateway according to phone number or phone number prefix.
     * Searches through all visible (non-hidden) gateways and finds the best
     * suited ones (one or many) supporting this phone number. <br/><br/>
     *
     * Algorithm:
     * <ol>
     * <li>Any fake gateway is disqualified.</li>
     * <li>Any gateway that has some supported prefixes listed and yet is not matching
     * the number is disqualified.</li>
     * <li>If some gateways are marked as favorite:
     *  <ol type=a>
     *   <li>Discard gateways that have the number outside their preferred prefixes.</li>
     *   <li>Return the one that is assigned to most contacts.</li>
     *   <li>If multiple gateways have the same (highest) score, return all of them.</li>
     *   <li>Selection ends here.</li>
     *  </ol>
     * </li>
     * <li>If user has some contacts defined, count their numbers for each gateway:
     *  <ol type=a>
     *   <li>Discard gateways that have the number outside their preferred prefixes.</li>
     *   <li>Gateway with the highest number of contacts win.</li>
     *   <li>If multiple gateways have the same (highest) score, return all of them.</li>
     *   <li>Selection ends here.</li>
     *  </ol>
     * </li>
     * <li>Do the last-resort algorithm:
     *  <ol type=a>
     *   <li>All gateways have 0 points by default.</li>
     *   <li>If a gateway requires login, but no credentials are filled in, subtract 1 point.</li>
     *   <li>If a gateway has preferred prefixes defined and they don't match the number, subtract 1 point.</li>
     *   <li>Return all gateways with the highest score. Selection ends here.</li>
     *  </ol>
     * </li>
     * </ol>
     *
     * @param number phone number or its prefix. The minimum length is two characters,
     *  for shorter input (or null) the method does nothing.
     * @return tuple consisting of: 1. list of suggested gateways (may be empty); 2. boolean whether this suggestion
     * is recommended (the decision was based on favorite gateways or the number of gateways users)
     * or completely arbitrary (the last-resort algorithm was used).
     */
    public Tuple<ArrayList<Gateway>, Boolean> suggestGateway(String number) {
        if (number == null || number.length() < 2) {
            return new Tuple<ArrayList<Gateway>, Boolean>(new ArrayList<Gateway>(), false);
        }
        
        SortedSet<Gateway> selectedGateways = getVisible();
        ArrayList<Gateway> result = new ArrayList<Gateway>();
        HashMap<String,Integer> usage = computeGatewayUsage();

        // select only those gateways that support this number and are not fake
        for (Iterator<Gateway> it = selectedGateways.iterator(); it.hasNext(); ) {
            Gateway gw = it.next();
            if (isFakeGateway(gw.getName())) {
                it.remove();
            } else if (!isNumberSupported(gw, number)) {
                it.remove();
            } else {
                // keep it in
            }
        }

        //search through favorite gateways
        int max = 0; //popular gateways may have even zero contacts
        for (Gateway gw : selectedGateways) {
            if (!gw.isFavorite()) {
                continue;
            }
            if (!isNumberPreferred(gw, number)) {
                continue;
            }
            int popularity = usage.get(gw.getName());
            if (popularity > max) {
                max = popularity;
                result.clear();
                result.add(gw);
            } else if (popularity == max) {
                result.add(gw);
            }
        }
        if (!result.isEmpty()) {
            return new Tuple<ArrayList<Gateway>, Boolean>(result, true);
        }

        //search through just popularity
        max = 1; //gateways must have at least one contact
        for (Gateway gw : selectedGateways) {
            if (!isNumberPreferred(gw, number)) {
                continue;
            }
            int popularity = usage.get(gw.getName());
            if (popularity > max) {
                max = popularity;
                result.clear();
                result.add(gw);
            } else if (popularity == max) {
                result.add(gw);
            }
        }
        if (!result.isEmpty()) {
            return new Tuple<ArrayList<Gateway>, Boolean>(result, true);
        }

        //use last-resort algorithm
        // map of gateway -> score
        HashMap<Gateway, Integer> scores = new HashMap<Gateway, Integer>();
        for (Gateway gw : selectedGateways) {
            scores.put(gw, 0);
            if (gw.isLoginRequired() && keyring.getKey(gw.getName()) == null) {
                scores.put(gw, scores.get(gw) - 1);
            }
            if (!isNumberPreferred(gw, number)) {
                scores.put(gw, scores.get(gw) - 1);
            }
        }
        max = Integer.MIN_VALUE;
        for (Gateway gw : selectedGateways) {
            int score = scores.get(gw);
            if (score > max) {
                max = score;
                result.clear();
                result.add(gw);
            } else if (score == max) {
                result.add(gw);
            }
        }
        return new Tuple<ArrayList<Gateway>, Boolean>(result, false);
    }

    /** Compute how many contacts use which gateway and return it as map <gw name; # of contacts>
     */
    private HashMap<String, Integer> computeGatewayUsage() {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        for (Contact contact : Contacts.getInstance().getAll()) {
            String gw = contact.getGateway();
            if (result.containsKey(gw)) {
                result.put(gw, result.get(gw) + 1);
            } else {
                result.put(gw, 1);
            }
        }
        for (Gateway gw : getAll()) {
            if (!result.containsKey(gw.getName())) {
                result.put(gw.getName(), 0);
            }
        }
        return result;
    }

    /** Returns whether gateway matches the number with its supported prefixes.
     * 
     * @param gateway gateway
     * @param number phone number
     * @return true if at least one of supported prefixes matches the number or
     * if the gateway does not have any supported prefixes; false otherwise
     */
    public static boolean isNumberSupported(Gateway gateway, String number) {
        String[] supportedPrefixes = gateway.getSupportedPrefixes();
        if (supportedPrefixes.length == 0) {
            // no supported prefixes -> gateway sends anywhere
            return true;
        }

        boolean matched = false;
        for (String prefix : supportedPrefixes) {
            if (number.startsWith(prefix)) {
                matched = true;
                break;
            }
        }

        return matched;
    }

    /** Returns whether gateway matches the number with its preferred prefixes.
     *
     * @param gateway gateway
     * @param number phone number
     * @return true if at least one of preferred prefixes matches the number or
     * if the gateway does not have any preferred prefixes; false otherwise
     * (or when gateway or number is null, or if number is shorter then 2 characters)
     */
    public static boolean isNumberPreferred(Gateway gateway, String number) {
        if (gateway == null || number == null || number.length() < 2) {
            return false;
        }

        String[] preferredPrefixes = gateway.getPreferredPrefixes();
        if (preferredPrefixes.length == 0) {
            // no preferred prefixes -> gateway sends anywhere in supported prefixes
            return true;
        }

        boolean matched = false;
        for (String prefix : preferredPrefixes) {
            if (number.startsWith(prefix)) {
                matched = true;
                break;
            }
        }

        return matched;
    }

    /** Convert message delay to more human readable string delay.
     * @param delay number of seconds (or milliseconds) of the delay
     * @param inMilliseconds if true,then <code>delay</code> is specified in
     * milliseconds, otherwise in seconds
     * @return human readable string of the delay, eg: "3h 15m 47s"
     */
    public static String convertDelayToHumanString(long delay, boolean inMilliseconds) {
        if (inMilliseconds) {
            delay = Math.round(delay / 1000.0);
        }
        long seconds = delay % 60;
        long minutes = (delay / 60) % 60;
        long hours = delay / 3600;

        StringBuilder builder = new StringBuilder();
        builder.append(seconds);
        builder.append(l10n.getString("QueuePanel.second_shortcut"));
        if (minutes > 0) {
            builder.insert(0, l10n.getString("QueuePanel.minute_shortcut") + " ");
            builder.insert(0, minutes);
        }
        if (hours > 0) {
            builder.insert(0, l10n.getString("QueuePanel.hour_shortcut") + " ");
            builder.insert(0, hours);
        }

        return builder.toString();
    }

    /** Parse GatewayInfo implementation from the provided URL.
     * @param script URL (file or jar) of gateway script
     * @return GatewayInfo implementation
     * @throws IOException when there is problem accessing the script file
     * @throws ScriptException when the script is not valid
     * @throws IntrospectionException when current JRE does not support JavaScript execution
     */
    public static GatewayInfo parseInfo(URL script) throws IOException, ScriptException, IntrospectionException {
        logger.log(Level.FINER, "Parsing info of script: {0}", script.toExternalForm());
        ScriptEngine jsEngine = manager.getEngineByName("js");
        if (jsEngine == null) {
            throw new IntrospectionException("JavaScript execution not supported");
        }
        Invocable invocable = (Invocable) jsEngine;
        Reader reader = null;
        try {
            reader = new InputStreamReader(script.openStream(), "UTF-8");
            //the script must be evaluated before extracting the interface
            jsEngine.eval(reader);
            GatewayInfo gatewayInfo = invocable.getInterface(GatewayInfo.class);
            return gatewayInfo;
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error closing script: " + script.toExternalForm(), ex);
            }
        }
    }

    /** Get gateways marked as favorites */
    public TreeSet<Gateway> getFavorites() {
        TreeSet<Gateway> favorites = new TreeSet<Gateway>();
        for (Gateway gw : getAll()) {
            if (gw.isFavorite()) {
                favorites.add(gw);
            }
        }
        return favorites;
    }

    /** Get gateways marked as hidden */
    public TreeSet<Gateway> getHidden() {
        TreeSet<Gateway> hidden = new TreeSet<Gateway>();
        for (Gateway gw : getAll()) {
            if (gw.isHidden()) {
                hidden.add(gw);
            }
        }
        return hidden;
    }

    /** Get just the visible (non-hidden) gateways */
    public TreeSet<Gateway> getVisible() {
        TreeSet<Gateway> visible = new TreeSet<Gateway>();
        for (Gateway gw : getAll()) {
            if (!gw.isHidden()) {
                visible.add(gw);
            }
        }
        return visible;
    }

    /** Reload favorite gateways (after some change) */
    private void updateFavorites() {
        Set<Gateway> oldFavorites = new HashSet<Gateway>(getFavorites());
        for (Gateway gw : oldFavorites) {
            gw.setFavorite(false);
        }
        HashSet<String> favorites = new HashSet<String>(Arrays.asList(config.getFavoriteGateways()));
        HashSet<String> nonexistent = new HashSet<String>();
        for (String gw : favorites)  {
            Gateway gateway = get(gw);
            if (gateway == null) {
                nonexistent.add(gw);
            } else {
                gateway.setFavorite(true);
            }
        }
        Set<Gateway> newFavorites = getFavorites();

        if (!nonexistent.isEmpty()) {
            logger.log(Level.FINE, "Found non-existent favorite gateways, removing from favorites: {0}", nonexistent);
            favorites = new HashSet<String>(favorites);
            favorites.removeAll(nonexistent);
            config.setFavoriteGateways(favorites.toArray(new String[]{}));
        }

        if (!CollectionUtils.isEqualCollection(oldFavorites, newFavorites)) {
            valuedSupport.fireEventOccured(Events.FAVORITES_UPDATED, null);
        }
    }

    /** Reload hidden gateways (after some change) */
    private void updateHidden() {
        Set<Gateway> oldHidden = new HashSet<Gateway>(getHidden());
        for (Gateway gw : oldHidden) {
            gw.setHidden(false);
        }

        Set<String> hidden = new HashSet<String>(Arrays.asList(config.getHiddenGateways()));
        HashSet<String> nonexistent = new HashSet<String>();
        for (String gw : hidden)  {
            Gateway gateway = get(gw);
            if (gateway == null) {
                nonexistent.add(gw);
            } else {
                gateway.setHidden(true);
            }
        }
        Set<Gateway> newHidden = getHidden();

        if (!nonexistent.isEmpty()) {
            logger.log(Level.FINE, "Found non-existent hidden gateways, removing from hiddens: {0}", nonexistent);
            hidden = new HashSet<String>(hidden);
            hidden.removeAll(nonexistent);
            config.setHiddenGateways(hidden.toArray(new String[]{}));
        }

        if (!CollectionUtils.isEqualCollection(oldHidden, newHidden)) {
            valuedSupport.fireEventOccured(Events.HIDDEN_UPDATED, null);
        }
    }
}
