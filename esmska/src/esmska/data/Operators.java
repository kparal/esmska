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

/** Class managing all operators
 * @author ripper
 */
public class Operators {

    public static enum Events {
        ADDED_OPERATOR,
        ADDED_OPERATORS,
        REMOVED_OPERATOR,
        REMOVED_OPERATORS,
        CLEARED_OPERATORS
    }

    /** shared instance */
    private static final Operators instance = new Operators();
    private static final Logger logger = Logger.getLogger(Operators.class.getName());
    private static final ResourceBundle l10n = L10N.l10nBundle;
    private static final SortedSet<Operator> operators = Collections.synchronizedSortedSet(new TreeSet<Operator>());
    private static final HashSet<DeprecatedOperator> deprecatedOperators = new HashSet<DeprecatedOperator>();
    private static final Keyring keyring = Keyring.getInstance();
    private static final ScriptEngineManager manager = new ScriptEngineManager();

    // <editor-fold defaultstate="collapsed" desc="ValuedEvent support">
    private ValuedEventSupport<Events, Operator> valuedSupport = new ValuedEventSupport<Events, Operator>(this);
    public void addValuedListener(ValuedListener<Events, Operator> valuedListener) {
        valuedSupport.addValuedListener(valuedListener);
    }
    public void removeValuedListener(ValuedListener<Events, Operator> valuedListener) {
        valuedSupport.removeValuedListener(valuedListener);
    }
    // </editor-fold>

    /** Disabled contructor */
    private Operators() {
    }

    /** Get shared instance */
    public static Operators getInstance() {
        return instance;
    }

    /** Get unmodifiable collection of all operators sorted by name */
    public SortedSet<Operator> getAll() {
        return Collections.unmodifiableSortedSet(operators);
    }

    /** Add new operator
     * @param operator new operator, not null
     * @return See {@link Collection#add}
     */
    public boolean add(Operator operator) {
        Validate.notNull(operator);

        logger.log(Level.FINE, "Adding new operator: {0}", operator);
        boolean added = operators.add(operator);

        if (added) {
            valuedSupport.fireEventOccured(Events.ADDED_OPERATOR, operator);
        }
        return added;
    }

    /** Add new operators
     * @param operators collection of operators, not null, no null element
     * @return See {@link Collection#addAll}
     */
    public boolean addAll(Collection<Operator> operators) {
        Validate.notNull(operators);
        Validate.noNullElements(operators);

        logger.log(Level.FINE, "Adding {0} operators: {1}",
                new Object[]{operators.size(), operators});
        boolean changed = Operators.operators.addAll(operators);

        if (changed) {
            valuedSupport.fireEventOccured(Events.ADDED_OPERATORS, null);
        }
        return changed;
    }

    /** Remove existing operator
     * @param operator operator to be removed, not null
     * @return See {@link Collection#remove}
     */
    public boolean remove(Operator operator) {
        Validate.notNull(operator);

        logger.log(Level.FINE, "Removing operator: {0}", operator);
        boolean removed = operators.remove(operator);

        if (removed) {
            valuedSupport.fireEventOccured(Events.REMOVED_OPERATOR, operator);
        }
        return removed;
    }

    /** Remove existing operators
     * @param operators collection of operators to be removed, not null, no null element
     * @return See {@link Collection#removeAll}
     */
    public boolean removeAll(Collection<Operator> operators) {
        Validate.notNull(operators);
        Validate.noNullElements(operators);

        logger.log(Level.FINE, "Removing {0} operators: {1}",
                new Object[]{operators.size(), operators});
        boolean changed = Operators.operators.removeAll(operators);

        if (changed) {
            valuedSupport.fireEventOccured(Events.REMOVED_OPERATORS, null);
        }
        return changed;
    }

    /** Remove all operators */
    public void clear() {
        logger.fine("Removing all operators");
        operators.clear();

        valuedSupport.fireEventOccured(Events.CLEARED_OPERATORS, null);
    }

    /** Search for an existing operator
     * @param operator operator to be searched, not null
     * @return See {@link Collection#contains}
     */
    public boolean contains(Operator operator) {
        Validate.notNull(operator);
        return operators.contains(operator);
    }

    /** Return number of operators
     * @return See {@link Collection#size}
     */
    public int size() {
        return operators.size();
    }

    /** Return if there are no operators
     * @return See {@link Collection#isEmpty}
     */
    public boolean isEmpty() {
        return operators.isEmpty();
    }

    /** Get set of currently deprecated operators */
    public HashSet<DeprecatedOperator> getDeprecatedOperators() {
        return deprecatedOperators;
    }

    /** Set currently deprecated operators. May be null to clear them. */
    public void setDeprecatedOperators(Set<DeprecatedOperator> deprecatedOperators) {
        Operators.deprecatedOperators.clear();
        if (deprecatedOperators != null) {
            Operators.deprecatedOperators.addAll(deprecatedOperators);
        }
    }

    /** Find operator by name.
     * @param operatorName Name of the operator. Search is case sensitive. May be null.
     * @return Operator implementation, when an operator with such name is found.
     *         If multiple such operators are found, returns the first one found.
     *         Returns null if no operator is found or provided name was null.
     */
    public static Operator getOperator(String operatorName) {
        if (operatorName == null) {
            return null;
        }

        synchronized(operators) {
            for (Operator operator : operators) {
                if (operator.getName().equals(operatorName)) {
                    return operator;
                }
            }
        }
        return null;
    }

    /** Returns whether the operator is a fake one (used just for development
     * purposes).
     */
    public static boolean isFakeOperator(String operatorName) {
        return StringUtils.contains(operatorName, "]fake");
    }

    /** Guess operator according to phone number or phone number prefix.
     * Searches through operators and finds the best suited one one
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
     * @param customOperators collection of operators in which to search.
     *  Use null for searching in all currently available operators.
     * @return the suggested operator or null if none found
     */
    public static Operator suggestOperator(String number, Collection<Operator> customOperators) {
        if (number == null || number.length() < 2) {
            return null;
        }

        Collection<Operator> selectedOperators =
                (customOperators != null ? customOperators : operators);

        synchronized(selectedOperators) {
            // map of operator -> tuple(login credentials value, summary value)
            TreeMap<Operator, Tuple<Integer, Integer>> all = new TreeMap<Operator, Tuple<Integer, Integer>>();

            // select only those operators that support this number and are not fake
            for (Operator operator : selectedOperators) {
                if (!Operators.isFakeOperator(operator.getName()) &&
                        Operators.isNumberSupported(operator, number)) {
                    all.put(operator, new Tuple<Integer, Integer>(0, 0));
                }
            }

            if (all.isEmpty()) {
                // very improbable, at least international gateways should support it always
                return null;
            }

            // rank them according to login requirements
            for (Entry<Operator, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Operator operator = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                int rank = 0;
                if (operator.isLoginRequired()) {
                    if (keyring.getKey(operator.getName()) == null) {
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
            for (Entry<Operator, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Operator operator = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                int rank = 0;
                if (operator.getPreferredPrefixes().length == 0) {
                    // no preferred prefixes
                    rank = 1;
                } else if (Operators.isNumberPreferred(operator, number)) {
                    // preferred prefixes match
                    rank = 0;
                } else {
                    // preferred prefixes don't match
                    rank = -2;
                }
                tuple.set2(tuple.get2() + rank);
            }

            // get highest summary ranks
            TreeMap<Operator, Tuple<Integer, Integer>> winners = new TreeMap<Operator, Tuple<Integer, Integer>>();
            int max = Integer.MIN_VALUE;
            for (Entry<Operator, Tuple<Integer,Integer>> entry : all.entrySet()) {
                Operator operator = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                if (tuple.get2() > max) {
                    max = tuple.get2();
                    winners.clear();
                }
                if (tuple.get2() == max) {
                    winners.put(operator, tuple);
                }
            }

            // do we have a winner?
            if (winners.size() == 1) {
                return winners.keySet().iterator().next();
            }

            // we have a draw
            // let's compute highest login requirement rank
            TreeMap<Operator, Tuple<Integer, Integer>> winnersLogin = new TreeMap<Operator, Tuple<Integer, Integer>>();
            max = Integer.MIN_VALUE;
            for (Entry<Operator, Tuple<Integer,Integer>> entry : winners.entrySet()) {
                Operator operator = entry.getKey();
                Tuple<Integer, Integer> tuple = entry.getValue();
                if (tuple.get1() > max) {
                    max = tuple.get1();
                    winnersLogin.clear();
                }
                if (tuple.get1() == max) {
                    winnersLogin.put(operator, tuple);
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
            for (Operator operator : winnersLogin.keySet()) {
                String country = CountryPrefix.extractCountryCode(operator.getName());
                if (StringUtils.equals(country, userCountry)) {
                    return operator;
                }
            }
            // find international operator
            for (Operator operator : winnersLogin.keySet()) {
                String country = CountryPrefix.extractCountryCode(operator.getName());
                if (StringUtils.equals(country, CountryPrefix.INTERNATIONAL_CODE)) {
                    return operator;
                }
            }

            // we haven't found a winner
            // there should be at least one operator left, otherwise there is an error somewhere
            if (winnersLogin.isEmpty()) {
                logger.log(Level.WARNING, "No gateways left for comparison when " +
                    "suggesting the best one, there must be an error somewhere.");
                return null;
            }
            // ah well, let's pick the first one available
            return winnersLogin.keySet().iterator().next();

        }
    }

    /** Returns whether operator matches the number with its supported prefixes.
     * 
     * @param operator operator
     * @param number phone number
     * @return true if at least one of supported prefixes matches the number or
     * if the operator does not have any supported prefixes; false otherwise
     */
    public static boolean isNumberSupported(Operator operator, String number) {
        String[] supportedPrefixes = operator.getSupportedPrefixes();
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

    /** Returns whether operator matches the number with its preferred prefixes.
     *
     * @param operator operator
     * @param number phone number
     * @return true if at least one of preferred prefixes matches the number of
     * if the operator does not have any preferred prefixes; false otherwise
     * (or when operator or number is null, or if number is shorter then 2 characters)
     */
    public static boolean isNumberPreferred(Operator operator, String number) {
        if (operator == null || number == null || number.length() < 2) {
            return false;
        }

        String[] preferredPrefixes = operator.getPreferredPrefixes();
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

    /** Parse OperatorInfo implementation from the provided URL.
     * @param script URL (file or jar) of operator script
     * @return OperatorInfo implementation
     * @throws IOException when there is problem accessing the script file
     * @throws ScriptException when the script is not valid
     * @throws IntrospectionException when current JRE does not support JavaScript execution
     */
    public static OperatorInfo parseInfo(URL script) throws IOException, ScriptException, IntrospectionException {
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
            OperatorInfo operatorInfo = invocable.getInterface(OperatorInfo.class);
            return operatorInfo;
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error closing script: " + script.toExternalForm(), ex);
            }
        }
    }
}
