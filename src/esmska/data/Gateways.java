/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package esmska.data;

import esmska.data.event.ValuedEventSupport;
import esmska.data.event.ValuedListener;
import esmska.utils.L10N;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
        CLEARED_GATEWAYS
    }

    /** shared instance */
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

    /** Disabled contructor */
    private Gateways() {
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
    public static Gateway getGateway(String gatewayName) {
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
     * Searches through gateways and finds the best suited one one
     * supporting this phone number. <br/><br/>
     *
     * Algorithm:
     * <ol>
     * <li>Any fake gateway is disqualified.</li>
     * <li>Any gateway that has some supported prefixes listed and yet is not matching
     * the number is disqualified.</li>
     * <li>Gateways are rated by login requirements:
     *  <ol>
     *  <li>Login required, credentials not filled in: 0 points</li>
     *  <li>Login required, credentials filled in: 1 point</li>
     *  <li>No login required: 2 points</li>
     *  </ol>
     * </li>
     * <li>Gateways are rated by preferred prefixes:
     *  <ol>
     *  <li>Gateway has preferred prefixes and they don't match the number: -2 points</li>
     *  <li>Gateway has preferred prefixes and they match the number: 0 points</li>
     *  <li>Gateway has no preferred prefixes: 1 point</li>
     *  </ol>
     * </li>
     * <li>Gateway with highest value wins.</li>
     * <li>In case of draw, higher value from login requirements wins.</li>
     * <li>In case of draw, the gateways are preferred by the country for which
     * they are defined: country of the user > international > any other country</li>
     * <li>In case of draw, the first one is picked.</li>
     * </ol>
     *
     * @param number phone number or it's prefix. The minimum length is two characters,
     *  for shorter input (or null) the method does nothing.
     * @param customGateways collection of gateways in which to search.
     *  Use null for searching in all currently available gateways.
     * @return the suggested gateway or null if none found
     */
    public static Gateway suggestGateway(String number, Collection<Gateway> customGateways) {
        if (number == null || number.length() < 2) {
            return null;
        }

        Collection<Gateway> selectedGateways =
                (customGateways != null ? customGateways : gateways);

        synchronized(selectedGateways) {
            // map of gateway -> tuple(login credentials value, summary value)
            TreeMap<Gateway, Tuple<Integer, Integer>> all = new TreeMap<Gateway, Tuple<Integer, Integer>>();

            // select only those gateways that support this number and are not fake
            for (Gateway gateway : selectedGateways) {
                if (!Gateways.isFakeGateway(gateway.getName()) &&
                        Gateways.isNumberSupported(gateway, number)) {
                    all.put(gateway, new Tuple<Integer, Integer>(0, 0));
                }
            }

            if (all.isEmpty()) {
                // very improbable, at least international gateways should support it always
                return null;
            }

            // rank them according to login requirements
            for (Entry<Gateway, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Gateway gateway = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                int rank = 0;
                if (gateway.isLoginRequired()) {
                    if (keyring.getKey(gateway.getName()) == null) {
                        // credentials not filled in
                        rank = 0;
                    } else {
                        // credentials filled in
                        rank = 1;
                    }
                } else {
                    rank = 2;
                }
                tuple.set1(rank);
                tuple.set2(tuple.get2() + rank);
            }

            // rank them according to preferred prefixes
            for (Entry<Gateway, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Gateway gateway = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                int rank = 0;
                if (gateway.getPreferredPrefixes().length == 0) {
                    // no preferred prefixes
                    rank = 1;
                } else if (Gateways.isNumberPreferred(gateway, number)) {
                    // preferred prefixes match
                    rank = 0;
                } else {
                    // preferred prefixes don't match
                    rank = -2;
                }
                tuple.set2(tuple.get2() + rank);
            }

            // get highest summary ranks
            TreeMap<Gateway, Tuple<Integer, Integer>> winners = new TreeMap<Gateway, Tuple<Integer, Integer>>();
            int max = Integer.MIN_VALUE;
            for (Entry<Gateway, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Gateway gateway = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                if (tuple.get2() > max) {
                    max = tuple.get2();
                    winners.clear();
                }
                if (tuple.get2() == max) {
                    winners.put(gateway, tuple);
                }
            }

            // do we have a winner?
            if (winners.size() == 1) {
                return winners.keySet().iterator().next();
            }

            // we have a draw
            // let's compute highest login requirement rank
            TreeMap<Gateway, Tuple<Integer, Integer>> winnersLogin = new TreeMap<Gateway, Tuple<Integer, Integer>>();
            max = Integer.MIN_VALUE;
            for (Entry<Gateway, Tuple<Integer,Integer>> entry : winners.entrySet()) {
                Gateway gateway = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                if (tuple.get1() > max) {
                    max = tuple.get1();
                    winnersLogin.clear();
                }
                if (tuple.get1() == max) {
                    winnersLogin.put(gateway, tuple);
                }
            }

            // do we have a winner?
            if (winnersLogin.size() == 1) {
                return winnersLogin.keySet().iterator().next();
            }

            // we have a draw
            // let's compare by gateway country
            String userCountry = CountryPrefix.getCountryCode(Config.getInstance().getCountryPrefix());
            // find same country as user country
            for (Gateway gateway : winnersLogin.keySet()) {
                String country = CountryPrefix.extractCountryCode(gateway.getName());
                if (StringUtils.equals(country, userCountry)) {
                    return gateway;
                }
            }
            // find international gateway
            for (Gateway gateway : winnersLogin.keySet()) {
                String country = CountryPrefix.extractCountryCode(gateway.getName());
                if (StringUtils.equals(country, CountryPrefix.INTERNATIONAL_CODE)) {
                    return gateway;
                }
            }

            // we haven't found a winner
            // there should be at least one gateway left, otherwise there is an error somewhere
            if (winnersLogin.isEmpty()) {
                logger.log(Level.WARNING, "No gateways left for comparison when " +
                    "suggesting the best one, there must be an error somewhere.");
                return null;
            }
            // ah well, let's pick the first one available
            return winnersLogin.keySet().iterator().next();

        }
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
     * @return true if at least one of preferred prefixes matches the number of
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
}
